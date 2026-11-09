package com.thelastascension.ascendentCore.model;

/**
 * Estadísticas globales unificadas compartidas por jugadores, clases, armaduras,
 * armas, pets, monturas y talismanes.
 */
public enum StatType {
    MAX_HEALTH("Vida Máxima", 100.0),
    DEFENSE("Defensa Física", 10.0),
    PHYSICAL_DAMAGE("Daño Físico", 10.0),
    MAGIC_DAMAGE("Daño Mágico", 10.0),
    ATTACK_SPEED("Velocidad de Ataque", 1.0),
    MOVE_SPEED("Velocidad de Movimiento", 1.0),
    CRIT_CHANCE("Probabilidad Crítica", 5.0),
    CRIT_DAMAGE("Daño Crítico", 150.0),
    MAX_STUN("Umbral de Stun (Postura)", 100.0),
    STUN_DAMAGE("Daño de Stun", 15.0);

    private final String displayNameEs;
    private final double defaultValue;

    StatType(String displayNameEs, double defaultValue) {
        this.displayNameEs = displayNameEs;
        this.defaultValue = defaultValue;
    }

    public String getDisplayNameEs() {
        return displayNameEs;
    }

    public double getDefaultValue() {
        return defaultValue;
    }
}
