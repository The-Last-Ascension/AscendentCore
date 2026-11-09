package com.thelastascension.ascendentCore.module;

import com.thelastascension.ascendentCore.AscendentCore;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registro central donde se conectan los plugins modulares de The Last Ascension.
 */
public class ModuleRegistry {

    private final AscendentCore core;
    private final Map<String, AscendentModule> modules = new ConcurrentHashMap<>();

    public ModuleRegistry(AscendentCore core) {
        this.core = core;
    }

    public void registerModule(AscendentModule module) {
        modules.put(module.getModuleId().toLowerCase(), module);
        core.getLogger().info("Módulo conectado: " + module.getModuleId() + " v" + module.getVersion());
    }

    public void unregisterModule(String moduleId) {
        if (moduleId != null) {
            modules.remove(moduleId.toLowerCase());
        }
    }

    public AscendentModule getModule(String moduleId) {
        return moduleId == null ? null : modules.get(moduleId.toLowerCase());
    }

    public Collection<AscendentModule> getRegisteredModules() {
        return Collections.unmodifiableCollection(modules.values());
    }

    public void reloadAllModules() {
        for (AscendentModule module : modules.values()) {
            module.onReload();
        }
    }
}
