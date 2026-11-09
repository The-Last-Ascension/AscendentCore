package com.thelastascension.ascendentCore.i18n;

import com.thelastascension.ascendentCore.AscendentCore;
import com.thelastascension.ascendentCore.model.PlayerProfile;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor transversal de traducción y localización de The Last Ascension.
 * Regla inquebrantable: Español (es_es) siempre obligatorio y por defecto,
 * con soporte secundario en Inglés (en_us) y auditoría automática de claves sin español.
 */
public class LangManager {

    public static final String LANG_ES = "es_es";
    public static final String LANG_EN = "en_us";

    private final AscendentCore core;
    private final Map<String, Map<String, String>> translations = new ConcurrentHashMap<>();

    public LangManager(AscendentCore core) {
        this.core = core;
        translations.put(LANG_ES, new ConcurrentHashMap<>());
        translations.put(LANG_EN, new ConcurrentHashMap<>());
    }

    public void loadCoreLanguages() {
        translations.get(LANG_ES).clear();
        translations.get(LANG_EN).clear();
        registerPluginLanguageFiles(core, "lang/es_es.yml", "lang/en_us.yml");
    }

    /**
     * Permite que tanto AscendentCore como cualquier plugin modular (AscendentCombat, AscendentClasses, etc.)
     * registren sus archivos de idioma en el diccionario centralizado.
     */
    public void registerPluginLanguageFiles(Plugin plugin, String esResourcePath, String enResourcePath) {
        File esFile = new File(plugin.getDataFolder(), esResourcePath);
        if (!esFile.exists() && plugin.getResource(esResourcePath) != null) {
            plugin.saveResource(esResourcePath, false);
        }
        File enFile = new File(plugin.getDataFolder(), enResourcePath);
        if (!enFile.exists() && plugin.getResource(enResourcePath) != null) {
            plugin.saveResource(enResourcePath, false);
        }

        if (esFile.exists()) {
            loadYamlIntoMap(YamlConfiguration.loadConfiguration(esFile), translations.get(LANG_ES));
        }
        if (enFile.exists()) {
            loadYamlIntoMap(YamlConfiguration.loadConfiguration(enFile), translations.get(LANG_EN));
        }

        if (core.getConfig().getBoolean("server-info.strict-spanish-audit", true)) {
            auditMissingSpanishKeys(plugin.getName());
        }
    }

    private void loadYamlIntoMap(YamlConfiguration yaml, Map<String, String> target) {
        for (String key : yaml.getKeys(true)) {
            if (!yaml.isConfigurationSection(key)) {
                target.put(key, String.valueOf(yaml.get(key)));
            }
        }
    }

    private void auditMissingSpanishKeys(String sourcePluginName) {
        Set<String> enKeys = translations.get(LANG_EN).keySet();
        Map<String, String> esMap = translations.get(LANG_ES);
        for (String key : enKeys) {
            if (!esMap.containsKey(key)) {
                core.getLogger().warning("[Auditoría Español] El módulo " + sourcePluginName
                        + " tiene la clave '" + key + "' en inglés pero NO en español (es_es.yml).");
            }
        }
    }

    public String getRaw(String lang, String key) {
        String normalizedLang = (lang != null && translations.containsKey(lang.toLowerCase()))
                ? lang.toLowerCase()
                : LANG_ES;
        Map<String, String> langMap = translations.get(normalizedLang);
        String val = langMap.get(key);
        if (val == null) {
            val = translations.get(LANG_ES).getOrDefault(key, key);
        }
        return val;
    }

    public String format(String lang, String key, Map<String, String> placeholders) {
        String text = getRaw(lang, key);
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                text = text.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return colorize(text);
    }

    public String formatWithPrefix(String lang, String key, Map<String, String> placeholders) {
        String prefix = colorize(getRaw(lang, "prefix"));
        return prefix + format(lang, key, placeholders);
    }

    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        String lang = resolveLanguage(sender);
        sender.sendMessage(formatWithPrefix(lang, key, placeholders));
    }

    public void send(CommandSender sender, String key) {
        send(sender, key, null);
    }

    public void sendRaw(CommandSender sender, String key, Map<String, String> placeholders) {
        String lang = resolveLanguage(sender);
        sender.sendMessage(format(lang, key, placeholders));
    }

    public String resolveLanguage(CommandSender sender) {
        if (sender instanceof Player player && core.getProfileManager() != null) {
            PlayerProfile profile = core.getProfileManager().getCachedProfile(player.getUniqueId());
            if (profile != null) {
                return profile.getLanguage();
            }
        }
        return core.getConfig().getString("server-info.default-language", LANG_ES);
    }

    @SuppressWarnings("deprecation")
    public static String colorize(String input) {
        return input == null ? "" : ChatColor.translateAlternateColorCodes('&', input);
    }

    public static Map<String, String> placeholders(String... keyValues) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            map.put(keyValues[i], keyValues[i + 1]);
        }
        return map;
    }
}
