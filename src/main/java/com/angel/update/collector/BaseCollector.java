package com.angel.update.collector;

/**
 * Interface de base pour tous les collecteurs
 */
public interface BaseCollector {
    
    /**
     * Identifiant unique du collecteur
     */
    String getId();
    
    /**
     * Nom du collecteur
     */
    String getName();
    
    /**
     * Type de collecteur (news, weather, recipes, etc.)
     */
    String getType();
    
    /**
     * Expression cron pour la planification
     */
    String getSchedule();
    
    /**
     * Indique si le collecteur est activé
     */
    boolean isEnabled();
    
    /**
     * Active/désactive le collecteur
     */
    void setEnabled(boolean enabled);
    
    /**
     * Exécute la collecte
     */
    void collect() throws Exception;
    
    /**
     * Valide la configuration du collecteur
     */
    boolean validateConfiguration();
}
