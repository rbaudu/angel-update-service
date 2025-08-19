package com.angel.update.service;

import com.angel.update.model.Content;
import com.angel.update.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service de gestion des contenus
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ContentManagerService {
    
    private final ContentRepository contentRepository;
    private final String baseDataPath = "/data";
    
    /**
     * Upload un nouveau contenu
     */
    public Content uploadContent(MultipartFile file, String contentType, 
                                String countryCode, String regionCode, 
                                String tags, String priority) throws IOException {
        
        // Construire le chemin du fichier
        String filePath = buildFilePath(contentType, countryCode, regionCode, file.getOriginalFilename());
        Path targetPath = Paths.get(baseDataPath, filePath);
        
        // Créer les répertoires si nécessaire
        Files.createDirectories(targetPath.getParent());
        
        // Sauvegarder le fichier
        Files.write(targetPath, file.getBytes());
        
        // Créer l'entité Content
        Content content = new Content();
        content.setContentType(contentType);
        content.setCountryCode(countryCode);
        content.setRegionCode(regionCode);
        content.setLanguageCode(detectLanguage(countryCode));
        content.setFilePath(filePath);
        content.setContent(new String(file.getBytes()));
        content.setFileSize(file.getSize());
        content.setPriority(Content.ContentPriority.valueOf(priority));
        content.setStatus(Content.ContentStatus.ACTIVE);
        content.setPublishedAt(LocalDateTime.now());
        
        // Ajouter les tags
        if (tags != null && !tags.isEmpty()) {
            Set<String> tagSet = Set.of(tags.split(","));
            content.setTags(tagSet.stream()
                    .map(String::trim)
                    .collect(Collectors.toSet()));
        }
        
        // Calculer le checksum
        content.setChecksum(calculateChecksum(file.getBytes()));
        
        // Sauvegarder en base
        Content saved = contentRepository.save(content);
        
        log.info("Content uploaded successfully: {}", saved.getId());
        
        return saved;
    }
    
    /**
     * Récupère les fichiers modifiés entre deux versions
     */
    @Cacheable(value = "changedFiles", key = "#countryCode + '-' + #regionCode + '-' + #fromVersion + '-' + #toVersion")
    public List<String> getChangedFiles(String countryCode, String regionCode, 
                                       String fromVersion, String toVersion) {
        
        LocalDateTime fromDate = versionToDate(fromVersion);
        LocalDateTime toDate = versionToDate(toVersion);
        
        List<Content> contents = contentRepository.findByCountryAndRegionAndDateRange(
                countryCode, regionCode, fromDate, toDate);
        
        return contents.stream()
                .map(Content::getFilePath)
                .collect(Collectors.toList());
    }
    
    /**
     * Vide le cache
     */
    @CacheEvict(value = {"contents", "changedFiles"}, allEntries = true)
    public void clearCache(String cacheType) {
        log.info("Cache cleared: {}", cacheType != null ? cacheType : "all");
    }
    
    /**
     * Récupère le contenu actif par type et localisation
     */
    @Cacheable(value = "contents", key = "#contentType + '-' + #countryCode + '-' + #regionCode")
    public List<Content> getActiveContent(String contentType, String countryCode, String regionCode) {
        return contentRepository.findActiveContent(contentType, countryCode, regionCode);
    }
    
    /**
     * Met à jour le statut d'un contenu
     */
    public void updateContentStatus(Long contentId, Content.ContentStatus newStatus) {
        contentRepository.findById(contentId).ifPresent(content -> {
            content.setStatus(newStatus);
            contentRepository.save(content);
            log.info("Content {} status updated to {}", contentId, newStatus);
        });
    }
    
    private String buildFilePath(String contentType, String countryCode, 
                                String regionCode, String filename) {
        StringBuilder path = new StringBuilder();
        path.append(countryCode.toLowerCase()).append("/");
        
        if (regionCode != null && !regionCode.isEmpty()) {
            path.append("regions/").append(regionCode.toLowerCase()).append("/");
        } else {
            path.append("national/");
        }
        
        path.append(contentType).append("/");
        path.append(filename);
        
        return path.toString();
    }
    
    private String detectLanguage(String countryCode) {
        return switch (countryCode) {
            case "FR", "BE", "CH" -> "fr";
            case "GB", "US" -> "en";
            case "ES", "MX", "AR" -> "es";
            case "DE", "AT" -> "de";
            default -> "en";
        };
    }
    
    private String calculateChecksum(byte[] data) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data);
            return bytesToHex(hash);
        } catch (Exception e) {
            log.error("Error calculating checksum", e);
            return "";
        }
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    private LocalDateTime versionToDate(String version) {
        // Simple conversion pour l'exemple
        // En production, utiliser un vrai système de versioning
        return LocalDateTime.now().minusDays(Long.parseLong(version.replace(".", "")));
    }
}
