package com.thelastascension.ascendentCore.model;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * Perfil unificado del jugador en memoria RAM (sincronizado asíncronamente con SQLite/MySQL).
 * Separa los Puntos de Estadística Globales (persisten al cambiar de clase estilo Trove)
 * del progreso específico de cada clase.
 */
public class PlayerProfile {

    private final UUID uuid;
    private String playerName;
    private String language = "es_es";

    // Progresión global del jugador (persiste entre cambios de clase)
    private int globalLevel = 1;
    private double globalExperience = 0.0;
    private int freeStatPoints = 0;
    private long coins = 100;
    private int unlockedTalismanSlots = 1;
    private String activeClassId = "none";

    // Estadísticas base + puntos globales invertidos por el jugador
    private final Map<StatType, Double> baseStats = new EnumMap<>(StatType.class);
    private final Map<StatType, Integer> investedStatPoints = new EnumMap<>(StatType.class);
    private final Map<ElementType, Double> elementalDamageBonus = new EnumMap<>(ElementType.class);
    private final Map<ElementType, Double> elementalResistance = new EnumMap<>(ElementType.class);

    // Estado volátil de combate (Stun estilo Grounded 2 y estado Abatido/Revivir)
    private transient double currentStun = 0.0;
    private transient boolean stunned = false;
    private transient boolean downed = false;

    public PlayerProfile(UUID uuid, String playerName) {
        this.uuid = uuid;
        this.playerName = playerName;
        for (StatType stat : StatType.values()) {
            baseStats.put(stat, stat.getDefaultValue());
            investedStatPoints.put(stat, 0);
        }
        for (ElementType element : ElementType.values()) {
            elementalDamageBonus.put(element, 0.0);
            elementalResistance.put(element, 0.0);
        }
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = (language != null && !language.isBlank()) ? language.toLowerCase() : "es_es";
    }

    public int getGlobalLevel() {
        return globalLevel;
    }

    public void setGlobalLevel(int globalLevel) {
        this.globalLevel = Math.max(1, globalLevel);
    }

    public double getGlobalExperience() {
        return globalExperience;
    }

    public void setGlobalExperience(double globalExperience) {
        this.globalExperience = Math.max(0.0, globalExperience);
    }

    public int getFreeStatPoints() {
        return freeStatPoints;
    }

    public void setFreeStatPoints(int freeStatPoints) {
        this.freeStatPoints = Math.max(0, freeStatPoints);
    }

    public long getCoins() {
        return coins;
    }

    public void setCoins(long coins) {
        this.coins = Math.max(0L, coins);
    }

    public void addCoins(long amount) {
        this.coins = Math.max(0L, this.coins + amount);
    }

    public int getUnlockedTalismanSlots() {
        return unlockedTalismanSlots;
    }

    public void setUnlockedTalismanSlots(int unlockedTalismanSlots) {
        this.unlockedTalismanSlots = Math.max(1, unlockedTalismanSlots);
    }

    public String getActiveClassId() {
        return activeClassId;
    }

    public void setActiveClassId(String activeClassId) {
        this.activeClassId = activeClassId != null ? activeClassId : "none";
    }

    public double getStat(StatType type) {
        double base = baseStats.getOrDefault(type, type.getDefaultValue());
        int points = investedStatPoints.getOrDefault(type, 0);
        double perPointBonus = switch (type) {
            case MAX_HEALTH -> 10.0;
            case DEFENSE, PHYSICAL_DAMAGE, MAGIC_DAMAGE, STUN_DAMAGE -> 2.0;
            case MAX_STUN -> 5.0;
            default -> 0.5;
        };
        return base + (points * perPointBonus);
    }

    public void setBaseStat(StatType type, double value) {
        baseStats.put(type, value);
    }

    public int getInvestedPoints(StatType type) {
        return investedStatPoints.getOrDefault(type, 0);
    }

    public void setInvestedPoints(StatType type, int points) {
        investedStatPoints.put(type, Math.max(0, points));
    }

    public Map<StatType, Integer> getInvestedStatPointsMap() {
        return investedStatPoints;
    }

    public double getElementalDamageBonus(ElementType element) {
        return elementalDamageBonus.getOrDefault(element, 0.0);
    }

    public void setElementalDamageBonus(ElementType element, double value) {
        elementalDamageBonus.put(element, value);
    }

    public double getElementalResistance(ElementType element) {
        return elementalResistance.getOrDefault(element, 0.0);
    }

    public void setElementalResistance(ElementType element, double value) {
        elementalResistance.put(element, value);
    }

    public double getCurrentStun() {
        return currentStun;
    }

    public void setCurrentStun(double currentStun) {
        this.currentStun = Math.max(0.0, Math.min(currentStun, getStat(StatType.MAX_STUN)));
    }

    public boolean isStunned() {
        return stunned;
    }

    public void setStunned(boolean stunned) {
        this.stunned = stunned;
    }

    public boolean isDowned() {
        return downed;
    }

    public void setDowned(boolean downed) {
        this.downed = downed;
    }
}
