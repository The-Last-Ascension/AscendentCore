package com.thelastascension.ascendentCore.module;

import org.bukkit.plugin.Plugin;

/**
 * Interfaz que implementan todos los plugins modulares de The Last Ascension
 * (AscendentCombat, AscendentClasses, AscendentHousing, AscendentClans, etc.)
 * para registrarse en AscendentCore.
 */
public interface AscendentModule {

    /**
     * Identificador del módulo (ej. "AscendentCombat").
     */
    String getModuleId();

    /**
     * Versión semántica X.Y.Z del módulo (ej. "1.0.0").
     */
    String getVersion();

    /**
     * Instancia del plugin de Bukkit/Paper asociado.
     */
    Plugin getPlugin();

    /**
     * Invocado cuando el administrador ejecuta /ascendent reload.
     */
    default void onReload() {
    }
}
