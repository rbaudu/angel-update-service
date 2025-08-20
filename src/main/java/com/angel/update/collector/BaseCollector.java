package com.angel.update.collector;

import com.angel.update.model.CollectorStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;

/**
 * Classe de base abstraite pour tous les collecteurs
 */
@Slf4j
public abstract class BaseCollector {
    
    @Value("${angel.collectors.enabled:true}")
    protected boolean globalEnabled = true;
    
    protected CollectorStatus.Status currentStatus = CollectorStatus.Status.INACTIVE;
    protected String lastStatusMessage = "";
    protected LocalDateTime lastRun;
    protected boolean enabled = true;
    
    /**
     * Nom unique du collecteur
     */
    public abstract String getCollectorName();
    
    /**
     * Type de contenu collecté
     */
    public abstract String getContentType();
    
    /**
     * ID du collecteur (basé sur le nom)
     */
    public String getId() {
        return getCollectorName().toLowerCase();
    }
    
    /**
     * Nom affiché du collecteur
     */
    public String getName() {
        return getCollectorName();
    }
    
    /**
     * Type du collecteur
     */
    public String getType() {
        return getContentType();
    }
    
    /**
     * Expression cron par défaut (peut être surchargée)
     */
    public String getSchedule() {
        return "0 */30 * * * *"; // Par défaut, toutes les 30 minutes
    }
    
    /**
     * Indique si le collecteur est activé
     */
    public boolean isEnabled() {
        return globalEnabled && enabled;
    }
    
    /**
     * Active/désactive le collecteur
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        log.info("Collector {} enabled: {}", getCollectorName(), enabled);
    }
    
    /**
     * Méthode de collecte (à implémenter dans les classes filles)
     */
    public abstract void collect() throws Exception;
    
    /**
     * Met à jour le statut du collecteur
     */
    protected void updateCollectorStatus(CollectorStatus.Status status, String message) {
        this.currentStatus = status;
        this.lastStatusMessage = message;
        this.lastRun = LocalDateTime.now();
        log.info("Collector {} status updated: {} - {}", getCollectorName(), status, message);
    }
    
    /**
     * Obtient le statut actuel
     */
    public CollectorStatus getStatus() {
        return new CollectorStatus(
                getCollectorName(),
                getContentType(),
                currentStatus,
                lastStatusMessage,
                lastRun
        );
    }
    
    /**
     * Valide la configuration du collecteur
     */
    public boolean validateConfiguration() {
        return true; // Override dans les classes filles si nécessaire
    }
}
