package com.angel.update.service;

import com.angel.update.model.UpdateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Service principal de gestion des mises à jour
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateService {
    
    private final ContentManagerService contentManagerService;
    private final ZipBuilderService zipBuilderService;
    private final VersioningService versioningService;
    private final CacheService cacheService;
    
    /**
     * Vérifie les mises à jour disponibles
     */
    public UpdateResponse checkForUpdates(String countryCode, String regionCode, 
                                         String currentVersion, String acceptLanguage) {
        
        String cacheKey = buildCacheKey(countryCode, regionCode, currentVersion);
        
        // Vérifier le cache
        UpdateResponse cached = cacheService.getUpdateResponse(cacheKey);
        if (cached != null) {
            log.debug("Returning cached response for key: {}", cacheKey);
            return cached;
        }
        
        // Vérifier la version
        String latestVersion = versioningService.getLatestVersion(countryCode, regionCode);
        boolean hasUpdates = versioningService.isNewerVersion(latestVersion, currentVersion);
        
        if (!hasUpdates) {
            UpdateResponse response = UpdateResponse.builder()
                    .hasUpdates(false)
                    .latestVersion(currentVersion)
                    .message("No updates available")
                    .nextCheckTime(LocalDateTime.now().plusHours(1))
                    .build();
            
            cacheService.putUpdateResponse(cacheKey, response);
            return response;
        }
        
        // Construire la réponse avec mise à jour
        List<String> changedFiles = contentManagerService.getChangedFiles(
                countryCode, regionCode, currentVersion, latestVersion);
        
        String packagePath = zipBuilderService.buildUpdatePackage(
                countryCode, regionCode, currentVersion, latestVersion, changedFiles);
        
        UpdateResponse response = UpdateResponse.builder()
                .hasUpdates(true)
                .latestVersion(latestVersion)
                .downloadUrl("/api/v1/update/download/" + latestVersion)
                .packageSize(zipBuilderService.getPackageSize(packagePath))
                .checksum(zipBuilderService.calculateChecksum(packagePath))
                .changedFiles(changedFiles)
                .changesSummary(summarizeChanges(changedFiles))
                .releaseDate(versioningService.getReleaseDate(latestVersion))
                .releaseNotes(versioningService.getReleaseNotes(latestVersion))
                .message("Update available")
                .mandatory(versioningService.isMandatoryUpdate(currentVersion, latestVersion))
                .nextCheckTime(LocalDateTime.now().plusHours(6))
                .build();
        
        cacheService.putUpdateResponse(cacheKey, response);
        return response;
    }
    
    /**
     * Récupère le package de mise à jour
     */
    public Resource getUpdatePackage(String version, String countryCode, String regionCode) {
        try {
            String packagePath = zipBuilderService.getPackagePath(version, countryCode, regionCode);
            Path file = Paths.get(packagePath);
            Resource resource = new UrlResource(file.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Could not read update package: " + packagePath);
            }
        } catch (Exception e) {
            log.error("Error getting update package", e);
            throw new RuntimeException("Error getting update package", e);
        }
    }
    
    /**
     * Obtient la version actuelle du service
     */
    public String getCurrentServiceVersion() {
        return getClass().getPackage().getImplementationVersion() != null 
                ? getClass().getPackage().getImplementationVersion() 
                : "1.0.0";
    }
    
    private String buildCacheKey(String countryCode, String regionCode, String version) {
        return String.format("update:%s:%s:%s", 
                countryCode, 
                regionCode != null ? regionCode : "national", 
                version);
    }
    
    private Map<String, Integer> summarizeChanges(List<String> changedFiles) {
        Map<String, Integer> summary = new java.util.HashMap<>();
        
        for (String file : changedFiles) {
            String type = extractContentType(file);
            summary.merge(type, 1, Integer::sum);
        }
        
        return summary;
    }
    
    private String extractContentType(String filePath) {
        String[] parts = filePath.split("/");
        if (parts.length > 2) {
            return parts[2]; // Assume structure: /data/{lang}/{type}/...
        }
        return "other";
    }
}
