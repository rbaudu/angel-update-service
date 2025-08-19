package com.angel.update.controller;

import com.angel.update.model.UpdateRequest;
import com.angel.update.model.UpdateResponse;
import com.angel.update.service.UpdateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller principal pour les mises à jour
 */
@RestController
@RequestMapping("/api/v1/update")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Update", description = "API de mise à jour")
public class UpdateController {

    private final UpdateService updateService;

    @PostMapping("/check")
    @Operation(summary = "Vérifier les mises à jour disponibles")
    public ResponseEntity<UpdateResponse> checkUpdate(
            @Valid @RequestBody UpdateRequest request,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        
        log.info("Checking updates for country: {}, region: {}, version: {}",
                request.getCountryCode(), request.getRegionCode(), request.getCurrentVersion());
        
        UpdateResponse response = updateService.checkForUpdates(
                request.getCountryCode(),
                request.getRegionCode(),
                request.getCurrentVersion(),
                acceptLanguage
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/download/{version}")
    @Operation(summary = "Télécharger un package de mise à jour")
    public ResponseEntity<Resource> downloadUpdate(
            @PathVariable String version,
            @RequestParam String countryCode,
            @RequestParam(required = false) String regionCode) {
        
        log.info("Downloading update version: {} for country: {}, region: {}",
                version, countryCode, regionCode);
        
        Resource resource = updateService.getUpdatePackage(version, countryCode, regionCode);
        
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"update-" + version + ".zip\"")
                .body(resource);
    }

    @GetMapping("/version")
    @Operation(summary = "Obtenir la version actuelle du service")
    public ResponseEntity<String> getVersion() {
        return ResponseEntity.ok(updateService.getCurrentServiceVersion());
    }
}
