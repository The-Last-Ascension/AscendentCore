package com.thelastascension.ascendentCore.command;

import com.thelastascension.ascendentCore.AscendentCore;
import com.thelastascension.ascendentCore.i18n.LangManager;
import com.thelastascension.ascendentCore.model.PlayerProfile;
import com.thelastascension.ascendentCore.model.StatType;
import com.thelastascension.ascendentCore.module.AscendentModule;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * Comando central /ascendent (/asc) para diagnóstico, consulta de atributos,
 * cambio de idioma (es_es / en_us) y recarga de módulos.
 */
public class AscendentCommand implements CommandExecutor, TabCompleter {

    private final AscendentCore core;

    public AscendentCommand(AscendentCore core) {
        this.core = core;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String sub = args.length > 0 ? args[0].toLowerCase() : "info";

        switch (sub) {
            case "info" -> {
                core.getLang().send(sender, "system.enabled", LangManager.placeholders("version", core.getPluginMeta().getVersion()));
                sendModulesList(sender);
            }
            case "modulos", "modules" -> sendModulesList(sender);
            case "stats", "atributos" -> {
                Player target;
                if (args.length >= 2) {
                    target = Bukkit.getPlayerExact(args[1]);
                    if (target == null) {
                        core.getLang().send(sender, "system.player-not-found", LangManager.placeholders("player", args[1]));
                        return true;
                    }
                } else if (sender instanceof Player p) {
                    target = p;
                } else {
                    core.getLang().send(sender, "system.player-only");
                    return true;
                }
                sendPlayerStats(sender, target);
            }
            case "idioma", "lang" -> {
                if (!(sender instanceof Player player)) {
                    core.getLang().send(sender, "system.player-only");
                    return true;
                }
                if (args.length < 2) {
                    core.getLang().send(player, "system.lang-invalid");
                    return true;
                }
                String chosen = args[1].toLowerCase();
                if (!LangManager.LANG_ES.equals(chosen) && !LangManager.LANG_EN.equals(chosen)) {
                    core.getLang().send(player, "system.lang-invalid");
                    return true;
                }
                PlayerProfile profile = core.getProfileManager().getOrLoadProfile(player);
                profile.setLanguage(chosen);
                core.getDatabaseManager().runAsync(() -> core.getProfileManager().saveToDatabaseSync(profile));
                core.getLang().send(player, "system.lang-changed");
            }
            case "reload" -> {
                if (!sender.hasPermission("ascendent.admin")) {
                    core.getLang().send(sender, "system.no-permission");
                    return true;
                }
                core.reloadCore();
                core.getLang().send(sender, "system.reloaded");
            }
            default -> core.getLang().send(sender, "system.enabled", LangManager.placeholders("version", core.getPluginMeta().getVersion()));
        }
        return true;
    }

    private void sendModulesList(CommandSender sender) {
        Collection<AscendentModule> modules = core.getModuleRegistry().getRegisteredModules();
        core.getLang().sendRaw(sender, "system.modules-header", LangManager.placeholders("count", String.valueOf(modules.size())));
        if (modules.isEmpty()) {
            core.getLang().sendRaw(sender, "system.modules-empty", null);
        } else {
            for (AscendentModule mod : modules) {
                core.getLang().sendRaw(sender, "system.modules-entry", LangManager.placeholders(
                        "name", mod.getModuleId(),
                        "version", mod.getVersion()
                ));
            }
        }
    }

    private void sendPlayerStats(CommandSender viewer, Player target) {
        PlayerProfile profile = core.getProfileManager().getOrLoadProfile(target);
        core.getLang().sendRaw(viewer, "profile.stats-header", LangManager.placeholders(
                "player", target.getName(),
                "level", String.valueOf(profile.getGlobalLevel())
        ));
        core.getLang().sendRaw(viewer, "profile.stats-class", LangManager.placeholders(
                "class", profile.getActiveClassId(),
                "points", String.valueOf(profile.getFreeStatPoints())
        ));
        core.getLang().sendRaw(viewer, "profile.stats-economy", LangManager.placeholders(
                "coins", String.valueOf(profile.getCoins()),
                "talismans", String.valueOf(profile.getUnlockedTalismanSlots())
        ));
        core.getLang().sendRaw(viewer, "profile.stats-combat", LangManager.placeholders(
                "health", String.format("%.0f", profile.getStat(StatType.MAX_HEALTH)),
                "defense", String.format("%.0f", profile.getStat(StatType.DEFENSE)),
                "stun", String.format("%.0f", profile.getStat(StatType.MAX_STUN))
        ));
        core.getLang().sendRaw(viewer, "profile.stats-damage", LangManager.placeholders(
                "phys", String.format("%.0f", profile.getStat(StatType.PHYSICAL_DAMAGE)),
                "magic", String.format("%.0f", profile.getStat(StatType.MAGIC_DAMAGE))
        ));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Stream.of("info", "stats", "modulos", "idioma", "reload")
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        if (args.length == 2 && ("idioma".equalsIgnoreCase(args[0]) || "lang".equalsIgnoreCase(args[0]))) {
            return Stream.of("es_es", "en_us")
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}
