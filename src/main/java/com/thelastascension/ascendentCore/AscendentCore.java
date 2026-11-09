package com.thelastascension.ascendentCore;

import com.thelastascension.ascendentCore.command.AscendentCommand;
import com.thelastascension.ascendentCore.database.DatabaseManager;
import com.thelastascension.ascendentCore.i18n.LangManager;
import com.thelastascension.ascendentCore.module.ModuleRegistry;
import com.thelastascension.ascendentCore.network.ClientBridge;
import com.thelastascension.ascendentCore.profile.ProfileManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * AscendentCore v1.0.0 — Núcleo central de The Last Ascension (Ascendant).
 * Proporciona la API transversal de Base de Datos (SQLite WAL / MySQL),
 * Localización (Español obligatorio + Inglés opcional), Perfiles de Jugador,
 * Estadísticas Unificadas, Elementos, Puente con AscendentClient y Registro Modular.
 */
public final class AscendentCore extends JavaPlugin {

    private static AscendentCore instance;

    private LangManager langManager;
    private DatabaseManager databaseManager;
    private ProfileManager profileManager;
    private ModuleRegistry moduleRegistry;
    private ClientBridge clientBridge;

    @Override
    public void onLoad() {
        instance = this;
        this.moduleRegistry = new ModuleRegistry(this);
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.langManager = new LangManager(this);
        this.langManager.loadCoreLanguages();

        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.initialize();

        this.profileManager = new ProfileManager(this);
        getServer().getPluginManager().registerEvents(this.profileManager, this);

        this.clientBridge = new ClientBridge(this);
        this.clientBridge.initialize();

        PluginCommand cmd = getCommand("ascendent");
        if (cmd != null) {
            AscendentCommand executor = new AscendentCommand(this);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }

        getLogger().info("============================================================");
        getLogger().info("  THE LAST ASCENSION (ASCENDANT) - AscendentCore v" + getPluginMeta().getVersion());
        getLogger().info("  Base de datos, Idiomas (ES/EN) y Registro Modular listos.");
        getLogger().info("============================================================");
    }

    @Override
    public void onDisable() {
        if (this.clientBridge != null) {
            this.clientBridge.shutdown();
        }
        if (this.profileManager != null) {
            this.profileManager.saveAllSync();
        }
        if (this.databaseManager != null) {
            this.databaseManager.shutdown();
        }
        getLogger().info("AscendentCore desactivado y perfiles guardados.");
    }

    public void reloadCore() {
        reloadConfig();
        this.langManager.loadCoreLanguages();
        this.moduleRegistry.reloadAllModules();
    }

    public static AscendentCore getInstance() {
        return instance;
    }

    public LangManager getLang() {
        return langManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ProfileManager getProfileManager() {
        return profileManager;
    }

    public ModuleRegistry getModuleRegistry() {
        return moduleRegistry;
    }

    public ClientBridge getClientBridge() {
        return clientBridge;
    }
}
