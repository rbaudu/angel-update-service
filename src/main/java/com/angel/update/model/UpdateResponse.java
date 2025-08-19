package com.angel.update.model;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Réponse de vérification de mise à jour
 */
@Data
@Builder
public class UpdateResponse {
    
    private boolean hasUpdates;
    
    private String latestVersion;
    
    private String downloadUrl;
    
    private long packageSize;
    
    private String checksum;
    
    private List<String> changedFiles;
    
    private Map<String, Integer> changesSummary;
    
    private LocalDateTime releaseDate;
    
    private String releaseNotes;
    
    private String message;
    
    private boolean mandatory;
    
    private LocalDateTime nextCheckTime;
}
