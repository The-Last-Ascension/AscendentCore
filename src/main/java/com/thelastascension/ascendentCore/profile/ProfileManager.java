package com.thelastascension.ascendentCore.profile;

import com.thelastascension.ascendentCore.AscendentCore;
import com.thelastascension.ascendentCore.i18n.LangManager;
import com.thelastascension.ascendentCore.model.PlayerProfile;
import com.thelastascension.ascendentCore.model.StatType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Gestiona la caché en memoria RAM de los perfiles de jugadores y su persistencia
 * asíncrona en la base de datos SQLite WAL / MySQL.
 */
public class ProfileManager implements Listener {

    private final AscendentCore core;
    private final Map<UUID, PlayerProfile> activeProfiles = new ConcurrentHashMap<>();

    public ProfileManager(AscendentCore core) {
        this.core = core;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }
        PlayerProfile profile = loadFromDatabaseSync(event.getUniqueId(), event.getName());
        activeProfiles.put(event.getUniqueId(), profile);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerProfile profile = activeProfiles.computeIfAbsent(
                player.getUniqueId(),
                id -> loadFromDatabaseSync(id, player.getName())
        );
        profile.setPlayerName(player.getName());
        core.getLang().send(player, "profile.loaded", LangManager.placeholders(
                "player", player.getName(),
                "level", String.valueOf(profile.getGlobalLevel()),
                "coins", String.valueOf(profile.getCoins())
        ));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        PlayerProfile profile = activeProfiles.remove(uuid);
        if (profile != null) {
            core.getDatabaseManager().runAsync(() -> saveToDatabaseSync(profile));
        }
    }

    public PlayerProfile getCachedProfile(UUID uuid) {
        return activeProfiles.get(uuid);
    }

    public PlayerProfile getOrLoadProfile(Player player) {
        return activeProfiles.computeIfAbsent(
                player.getUniqueId(),
                id -> loadFromDatabaseSync(id, player.getName())
        );
    }

    public void saveAllSync() {
        for (PlayerProfile profile : activeProfiles.values()) {
            saveToDatabaseSync(profile);
        }
    }

    public PlayerProfile loadFromDatabaseSync(UUID uuid, String playerName) {
        PlayerProfile profile = new PlayerProfile(uuid, playerName);
        applyConfiguredDefaults(profile);

        String sql = "SELECT * FROM ascendent_profiles WHERE uuid = ?";
        try (Connection conn = core.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    profile.setLanguage(rs.getString("language"));
                    profile.setGlobalLevel(rs.getInt("global_level"));
                    profile.setGlobalExperience(rs.getDouble("global_exp"));
                    profile.setFreeStatPoints(rs.getInt("free_stat_points"));
                    profile.setCoins(rs.getLong("coins"));
                    profile.setUnlockedTalismanSlots(rs.getInt("unlocked_talisman_slots"));
                    profile.setActiveClassId(rs.getString("active_class_id"));
                    deserializeInvestedStats(profile, rs.getString("invested_stats_json"));
                } else {
                    saveToDatabaseSync(profile);
                }
            }
        } catch (SQLException e) {
            core.getLogger().log(Level.SEVERE, "Error al cargar el perfil de " + playerName, e);
        }
        return profile;
    }

    public void saveToDatabaseSync(PlayerProfile profile) {
        String sql = core.getDatabaseManager().isMysqlMode()
                ? """
                  INSERT INTO ascendent_profiles
                  (uuid, player_name, language, global_level, global_exp, free_stat_points, coins, unlocked_talisman_slots, active_class_id, invested_stats_json)
                  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                  ON DUPLICATE KEY UPDATE
                  player_name=VALUES(player_name), language=VALUES(language), global_level=VALUES(global_level),
                  global_exp=VALUES(global_exp), free_stat_points=VALUES(free_stat_points), coins=VALUES(coins),
                  unlocked_talisman_slots=VALUES(unlocked_talisman_slots), active_class_id=VALUES(active_class_id),
                  invested_stats_json=VALUES(invested_stats_json)
                  """
                : """
                  INSERT INTO ascendent_profiles
                  (uuid, player_name, language, global_level, global_exp, free_stat_points, coins, unlocked_talisman_slots, active_class_id, invested_stats_json)
                  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                  ON CONFLICT(uuid) DO UPDATE SET
                  player_name=excluded.player_name, language=excluded.language, global_level=excluded.global_level,
                  global_exp=excluded.global_exp, free_stat_points=excluded.free_stat_points, coins=excluded.coins,
                  unlocked_talisman_slots=excluded.unlocked_talisman_slots, active_class_id=excluded.active_class_id,
                  invested_stats_json=excluded.invested_stats_json
                  """;

        try (Connection conn = core.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, profile.getUuid().toString());
            ps.setString(2, profile.getPlayerName());
            ps.setString(3, profile.getLanguage());
            ps.setInt(4, profile.getGlobalLevel());
            ps.setDouble(5, profile.getGlobalExperience());
            ps.setInt(6, profile.getFreeStatPoints());
            ps.setLong(7, profile.getCoins());
            ps.setInt(8, profile.getUnlockedTalismanSlots());
            ps.setString(9, profile.getActiveClassId());
            ps.setString(10, serializeInvestedStats(profile));
            ps.executeUpdate();
        } catch (SQLException e) {
            core.getLogger().log(Level.SEVERE, "Error al guardar el perfil de " + profile.getPlayerName(), e);
        }
    }

    private void applyConfiguredDefaults(PlayerProfile profile) {
        profile.setLanguage(core.getConfig().getString("server-info.default-language", LangManager.LANG_ES));
        profile.setCoins(core.getConfig().getLong("player-defaults.starting-coins", 100L));
        profile.setUnlockedTalismanSlots(core.getConfig().getInt("player-defaults.starting-talisman-slots", 1));
        profile.setBaseStat(StatType.MAX_HEALTH, core.getConfig().getDouble("player-defaults.base-health", 100.0));
        profile.setBaseStat(StatType.DEFENSE, core.getConfig().getDouble("player-defaults.base-defense", 10.0));
        profile.setBaseStat(StatType.PHYSICAL_DAMAGE, core.getConfig().getDouble("player-defaults.base-physical-damage", 10.0));
        profile.setBaseStat(StatType.MAGIC_DAMAGE, core.getConfig().getDouble("player-defaults.base-magic-damage", 10.0));
        profile.setBaseStat(StatType.MAX_STUN, core.getConfig().getDouble("player-defaults.base-max-stun", 100.0));
    }

    private String serializeInvestedStats(PlayerProfile profile) {
        return profile.getInvestedStatPointsMap().entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .map(e -> e.getKey().name() + ":" + e.getValue())
                .collect(Collectors.joining(","));
    }

    private void deserializeInvestedStats(PlayerProfile profile, String raw) {
        if (raw == null || raw.isBlank() || "{}".equals(raw)) return;
        for (String pair : raw.split(",")) {
            String[] kv = pair.split(":");
            if (kv.length == 2) {
                try {
                    StatType type = StatType.valueOf(kv[0].trim());
                    int points = Integer.parseInt(kv[1].trim());
                    profile.setInvestedPoints(type, points);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }
}
