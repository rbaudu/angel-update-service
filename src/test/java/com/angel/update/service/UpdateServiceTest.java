package com.angel.update.service;

import com.angel.update.model.UpdateResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour UpdateService
 */
@ExtendWith(MockitoExtension.class)
class UpdateServiceTest {
    
    @Mock
    private ContentManagerService contentManagerService;
    
    @Mock
    private ZipBuilderService zipBuilderService;
    
    @Mock
    private VersioningService versioningService;
    
    @Mock
    private CacheService cacheService;
    
    private UpdateService updateService;
    
    @BeforeEach
    void setUp() {
        updateService = new UpdateService(
                contentManagerService,
                zipBuilderService,
                versioningService,
                cacheService
        );
    }
    
    @Test
    void checkForUpdates_WhenNoUpdatesAvailable_ShouldReturnNoUpdatesResponse() {
        // Given
        String countryCode = "FR";
        String regionCode = "IDF";
        String currentVersion = "1.0.0";
        String acceptLanguage = "fr";
        String latestVersion = "1.0.0";
        
        when(cacheService.getUpdateResponse(anyString())).thenReturn(null);
        when(versioningService.getLatestVersion(countryCode, regionCode)).thenReturn(latestVersion);
        when(versioningService.isNewerVersion(latestVersion, currentVersion)).thenReturn(false);
        
        // When
        UpdateResponse response = updateService.checkForUpdates(countryCode, regionCode, currentVersion, acceptLanguage);
        
        // Then
        assertNotNull(response);
        assertFalse(response.isHasUpdates());
        assertEquals(currentVersion, response.getLatestVersion());
        assertEquals("No updates available", response.getMessage());
        assertNotNull(response.getNextCheckTime());
        
        verify(cacheService).putUpdateResponse(anyString(), eq(response));
    }
    
    @Test
    void checkForUpdates_WhenUpdatesAvailable_ShouldReturnUpdateResponse() {
        // Given
        String countryCode = "FR";
        String regionCode = "IDF";
        String currentVersion = "1.0.0";
        String acceptLanguage = "fr";
        String latestVersion = "1.1.0";
        List<String> changedFiles = List.of("news/article1.json", "weather/paris.json");
        String packagePath = "/data/packages/update-fr-idf-1.1.0.zip";
        
        when(cacheService.getUpdateResponse(anyString())).thenReturn(null);
        when(versioningService.getLatestVersion(countryCode, regionCode)).thenReturn(latestVersion);
        when(versioningService.isNewerVersion(latestVersion, currentVersion)).thenReturn(true);
        when(contentManagerService.getChangedFiles(countryCode, regionCode, currentVersion, latestVersion))
                .thenReturn(changedFiles);
        when(zipBuilderService.buildUpdatePackage(countryCode, regionCode, currentVersion, latestVersion, changedFiles))
                .thenReturn(packagePath);
        when(zipBuilderService.getPackageSize(packagePath)).thenReturn(1024L);
        when(zipBuilderService.calculateChecksum(packagePath)).thenReturn("abc123");
        when(versioningService.getReleaseDate(latestVersion)).thenReturn(LocalDateTime.now());
        when(versioningService.getReleaseNotes(latestVersion)).thenReturn("New features and bug fixes");
        when(versioningService.isMandatoryUpdate(currentVersion, latestVersion)).thenReturn(false);
        
        // When
        UpdateResponse response = updateService.checkForUpdates(countryCode, regionCode, currentVersion, acceptLanguage);
        
        // Then
        assertNotNull(response);
        assertTrue(response.isHasUpdates());
        assertEquals(latestVersion, response.getLatestVersion());
        assertEquals("/api/v1/update/download/" + latestVersion, response.getDownloadUrl());
        assertEquals(1024L, response.getPackageSize());
        assertEquals("abc123", response.getChecksum());
        assertEquals(changedFiles, response.getChangedFiles());
        assertEquals("New features and bug fixes", response.getReleaseNotes());
        assertFalse(response.isMandatory());
        assertEquals("Update available", response.getMessage());
        
        verify(cacheService).putUpdateResponse(anyString(), eq(response));
    }
    
    @Test
    void checkForUpdates_WhenCacheHit_ShouldReturnCachedResponse() {
        // Given
        String countryCode = "FR";
        String regionCode = "IDF";
        String currentVersion = "1.0.0";
        String acceptLanguage = "fr";
        
        UpdateResponse cachedResponse = UpdateResponse.builder()
                .hasUpdates(true)
                .latestVersion("1.1.0")
                .message("Cached response")
                .build();
        
        when(cacheService.getUpdateResponse(anyString())).thenReturn(cachedResponse);
        
        // When
        UpdateResponse response = updateService.checkForUpdates(countryCode, regionCode, currentVersion, acceptLanguage);
        
        // Then
        assertEquals(cachedResponse, response);
        assertEquals("Cached response", response.getMessage());
        
        // Vérifier que les autres services ne sont pas appelés
        verifyNoInteractions(versioningService, contentManagerService, zipBuilderService);
    }
    
    @Test
    void getUpdatePackage_WhenPackageExists_ShouldReturnResource() {
        // Given
        String version = "1.1.0";
        String countryCode = "FR";
        String regionCode = "IDF";
        String packagePath = "/data/packages/update-fr-idf-1.1.0.zip";
        
        when(zipBuilderService.getPackagePath(version, countryCode, regionCode)).thenReturn(packagePath);
        
        // When & Then
        assertThrows(RuntimeException.class, () -> {
            updateService.getUpdatePackage(version, countryCode, regionCode);
        });
        
        verify(zipBuilderService).getPackagePath(version, countryCode, regionCode);
    }
    
    @Test
    void getCurrentServiceVersion_ShouldReturnVersion() {
        // When
        String version = updateService.getCurrentServiceVersion();
        
        // Then
        assertNotNull(version);
        assertEquals("1.0.0", version); // Version par défaut quand le package n'est pas disponible
    }
}