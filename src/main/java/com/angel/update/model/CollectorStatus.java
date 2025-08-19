package com.angel.update.model;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Statut d'un collecteur
 */
@Data
@Builder
public class CollectorStatus {
    
    private String id;
    
    private String name;
    
    private String type;
    
    private boolean enabled;
    
    private String schedule;
    
    private LocalDateTime lastRun;
    
    private LocalDateTime nextRun;
    
    private Status status;
    
    private String lastError;
    
    private long successCount;
    
    private long errorCount;
    
    private double averageExecutionTime;
    
    private long lastExecutionTime;
    
    public enum Status {
        IDLE, RUNNING, SUCCESS, ERROR, DISABLED
    }
}
