package com.thelastascension.ascendentCore.model;

/**
 * Sistema de elementos de The Last Ascension (Hexágono Natural de 6 + Dualidad Cósmica de 2).
 * Nota: Los nombres e interacciones pueden ajustarse a medida que evolucione el balance de clases.
 */
public enum ElementType {
    IGNEO("elements.igneo", "Quemado"),
    SYLVA("elements.sylva", "Infestado"),
    TERRA("elements.terra", "Cristalizado"),
    FULMEN("elements.fulmen", "Electrificado"),
    CRIO("elements.crio", "Escarchado"),
    CEFIRO("elements.cefiro", "Vendaval"),
    UMBRA("elements.umbra", "Maldición de Alma"),
    ASTRAL("elements.astral", "Marca Estelar");

    private final String langKey;
    private final String defaultStatusName;

    ElementType(String langKey, String defaultStatusName) {
        this.langKey = langKey;
        this.defaultStatusName = defaultStatusName;
    }

    public String getLangKey() {
        return langKey;
    }

    public String getDefaultStatusName() {
        return defaultStatusName;
    }

    /**
     * Devuelve el elemento contra el que este elemento es Supereficaz (x1.5 daño).
     */
    public ElementType getStrongAgainst() {
        return switch (this) {
            case IGNEO -> SYLVA;
            case SYLVA -> TERRA;
            case TERRA -> FULMEN;
            case FULMEN -> CRIO;
            case CRIO -> CEFIRO;
            case CEFIRO -> IGNEO;
            case UMBRA -> ASTRAL;
            case ASTRAL -> UMBRA;
        };
    }

    /**
     * Calcula el multiplicador de eficacia elemental (1.5 supereficaz, 0.5 resistente, 1.0 neutro).
     */
    public double getMultiplierAgainst(ElementType defenderElement) {
        if (defenderElement == null) return 1.0;
        if (this.getStrongAgainst() == defenderElement) {
            return 1.5;
        }
        // En el hexágono natural, el defensor resiste (x0.5) al elemento que domina
        if (this != UMBRA && this != ASTRAL && defenderElement.getStrongAgainst() == this) {
            return 0.5;
        }
        return 1.0;
    }
}
