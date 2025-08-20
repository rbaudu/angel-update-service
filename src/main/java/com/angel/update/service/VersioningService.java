package com.angel.update.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service de gestion des versions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VersioningService {
    
    // Cache des versions par région
    private final Map<String, String> versionCache = new ConcurrentHashMap<>();
    
    // Notes de version par version
    private final Map<String, String> releaseNotesCache = new ConcurrentHashMap<>();
    
    // Dates de release par version
    private final Map<String, LocalDateTime> releaseDateCache = new ConcurrentHashMap<>();
    
    /**
     * Obtient la dernière version disponible pour une région
     */
    public String getLatestVersion(String countryCode, String regionCode) {
        String regionKey = buildRegionKey(countryCode, regionCode);
        
        // Pour l'exemple, utiliser la date actuelle comme version
        String version = versionCache.computeIfAbsent(regionKey, key -> {
            LocalDateTime now = LocalDateTime.now();
            return now.format(DateTimeFormatter.ofPattern("yyyy.MM.dd.HH"));
        });
        
        log.debug("Latest version for {}: {}", regionKey, version);
        return version;
    }
    
    /**
     * Compare deux versions pour déterminer si la première est plus récente
     */
    public boolean isNewerVersion(String version1, String version2) {
        if (version1 == null || version2 == null) {
            return false;
        }
        
        try {
            // Conversion simple basée sur le format yyyy.MM.dd.HH
            String[] parts1 = version1.split("\\.");
            String[] parts2 = version2.split("\\.");
            
            for (int i = 0; i < Math.min(parts1.length, parts2.length); i++) {
                int v1 = Integer.parseInt(parts1[i]);
                int v2 = Integer.parseInt(parts2[i]);
                
                if (v1 > v2) return true;
                if (v1 < v2) return false;
            }
            
            // Si toutes les parties sont égales, la version avec plus de parties est plus récente
            return parts1.length > parts2.length;
            
        } catch (NumberFormatException e) {
            log.warn("Error comparing versions {} and {}", version1, version2, e);
            return false;
        }
    }
    
    /**
     * Met à jour la version pour une région
     */
    public void updateVersion(String countryCode, String regionCode, String newVersion) {
        String regionKey = buildRegionKey(countryCode, regionCode);
        String oldVersion = versionCache.put(regionKey, newVersion);
        releaseDateCache.put(newVersion, LocalDateTime.now());
        
        log.info("Version updated for {}: {} -> {}", regionKey, oldVersion, newVersion);
    }
    
    /**
     * Détermine si une mise à jour est obligatoire
     */
    public boolean isMandatoryUpdate(String currentVersion, String latestVersion) {
        if (currentVersion == null || latestVersion == null) {
            return false;
        }
        
        try {
            String[] currentParts = currentVersion.split("\\.");
            String[] latestParts = latestVersion.split("\\.");
            
            if (currentParts.length >= 2 && latestParts.length >= 2) {
                // Mise à jour obligatoire si l'année ou le mois a changé
                int currentYear = Integer.parseInt(currentParts[0]);
                int currentMonth = Integer.parseInt(currentParts[1]);
                int latestYear = Integer.parseInt(latestParts[0]);
                int latestMonth = Integer.parseInt(latestParts[1]);
                
                return (latestYear > currentYear) || 
                       (latestYear == currentYear && latestMonth > currentMonth);
            }
            
        } catch (NumberFormatException e) {
            log.warn("Error determining mandatory update for versions {} and {}", 
                    currentVersion, latestVersion, e);
        }
        
        return false;
    }
    
    /**
     * Obtient la date de release d'une version
     */
    public LocalDateTime getReleaseDate(String version) {
        return releaseDateCache.computeIfAbsent(version, v -> {
            try {
                // Tenter de parser la version comme date
                String[] parts = v.split("\\.");
                if (parts.length >= 3) {
                    int year = Integer.parseInt(parts[0]);
                    int month = Integer.parseInt(parts[1]);
                    int day = Integer.parseInt(parts[2]);
                    int hour = parts.length >= 4 ? Integer.parseInt(parts[3]) : 0;
                    
                    return LocalDateTime.of(year, month, day, hour, 0);
                }
            } catch (Exception e) {
                log.warn("Could not parse release date from version: {}", version, e);
            }
            
            return LocalDateTime.now();
        });
    }
    
    /**
     * Obtient les notes de version
     */
    public String getReleaseNotes(String version) {
        return releaseNotesCache.computeIfAbsent(version, v -> {
            // Notes génériques pour l'exemple
            return String.format("Mise à jour automatique des contenus - Version %s\n" +
                    "- Actualisation des données météo\n" +
                    "- Nouvelles actualités disponibles\n" +
                    "- Optimisations de performance", v);
        });
    }
    
    /**
     * Définit des notes de version personnalisées
     */
    public void setReleaseNotes(String version, String notes) {
        releaseNotesCache.put(version, notes);
        log.info("Release notes set for version: {}", version);
    }
    
    /**
     * Génère une nouvelle version basée sur la timestamp actuelle
     */
    public String generateNewVersion() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd.HH"));
    }
    
    /**
     * Génère une version avec un numéro de build
     */
    public String generateVersionWithBuild(int buildNumber) {
        String baseVersion = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        return String.format("%s.%03d", baseVersion, buildNumber);
    }
    
    private String buildRegionKey(String countryCode, String regionCode) {
        return regionCode != null && !regionCode.isEmpty() 
                ? countryCode + "-" + regionCode 
                : countryCode;
    }
}