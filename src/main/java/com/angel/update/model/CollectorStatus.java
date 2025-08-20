package com.angel.update.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Statut d'un collecteur
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectorStatus {
    
    private String id;
    
    private String name;
    
    private String type;
    
    private Status status;
    
    private String message;
    
    private LocalDateTime lastRun;
    
    private LocalDateTime nextRun;
    
    private boolean enabled;
    
    private String schedule;
    
    private long successCount;
    
    private long errorCount;
    
    private double averageExecutionTime;
    
    private long lastExecutionTime;
    
    private String lastError;
    
    public CollectorStatus(String name, String type, Status status, String message, LocalDateTime lastRun) {
        this.name = name;
        this.type = type;
        this.status = status;
        this.message = message;
        this.lastRun = lastRun;
    }
    
    public enum Status {
        IDLE, INACTIVE, ACTIVE, RUNNING, SUCCESS, ERROR, DISABLED
    }
}
