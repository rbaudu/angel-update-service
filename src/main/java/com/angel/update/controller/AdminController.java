package com.angel.update.controller;

import com.angel.update.model.Content;
import com.angel.update.model.CollectorStatus;
import com.angel.update.service.ContentManagerService;
import com.angel.update.service.CollectorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Controller d'administration
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "API d'administration")
public class AdminController {

    private final ContentManagerService contentManagerService;
    private final CollectorService collectorService;

    @PostMapping("/upload")
    @Operation(summary = "Upload manuel de contenu")
    public ResponseEntity<Map<String, Object>> uploadContent(
            @RequestParam("file") MultipartFile file,
            @RequestParam String contentType,
            @RequestParam String countryCode,
            @RequestParam(required = false) String regionCode,
            @RequestParam(required = false) String tags,
            @RequestParam(defaultValue = "NORMAL") String priority) {
        
        log.info("Uploading content: type={}, country={}, region={}, file={}",
                contentType, countryCode, regionCode, file.getOriginalFilename());
        
        try {
            Content content = contentManagerService.uploadContent(
                    file, contentType, countryCode, regionCode, tags, priority);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "contentId", content.getId(),
                    "message", "Content uploaded successfully"
            ));
        } catch (Exception e) {
            log.error("Error uploading content", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage()
                    ));
        }
    }

    @GetMapping("/collectors")
    @Operation(summary = "Liste des collecteurs")
    public ResponseEntity<List<CollectorStatus>> getCollectors() {
        return ResponseEntity.ok(collectorService.getAllCollectorStatus());
    }

    @PostMapping("/collectors/{id}/toggle")
    @Operation(summary = "Activer/Désactiver un collecteur")
    public ResponseEntity<Map<String, Object>> toggleCollector(@PathVariable String id) {
        log.info("Toggling collector: {}", id);
        
        boolean newState = collectorService.toggleCollector(id);
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "enabled", newState,
                "message", "Collector " + (newState ? "enabled" : "disabled")
        ));
    }

    @PostMapping("/collectors/{id}/run")
    @Operation(summary = "Exécuter un collecteur manuellement")
    public ResponseEntity<Map<String, Object>> runCollector(@PathVariable String id) {
        log.info("Running collector manually: {}", id);
        
        try {
            collectorService.runCollectorNow(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Collector started successfully"
            ));
        } catch (Exception e) {
            log.error("Error running collector", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage()
                    ));
        }
    }

    @PostMapping("/cache/clear")
    @Operation(summary = "Vider le cache")
    public ResponseEntity<Map<String, Object>> clearCache(
            @RequestParam(required = false) String cacheType) {
        
        log.info("Clearing cache: {}", cacheType != null ? cacheType : "all");
        
        contentManagerService.clearCache(cacheType);
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Cache cleared successfully"
        ));
    }

    @GetMapping("/metrics")
    @Operation(summary = "Métriques en temps réel")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        // TODO: Implémenter les métriques
        return ResponseEntity.ok(Map.of(
                "requestsPerSecond", 0,
                "cacheHitRate", 0.0,
                "activeConnections", 0,
                "errorsPerMinute", 0
        ));
    }
}
