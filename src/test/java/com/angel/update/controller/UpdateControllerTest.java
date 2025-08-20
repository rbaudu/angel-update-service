package com.angel.update.controller;

import com.angel.update.config.TestSecurityConfig;
import com.angel.update.model.UpdateRequest;
import com.angel.update.model.UpdateResponse;
import com.angel.update.service.UpdateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration pour UpdateController
 */
@WebMvcTest(controllers = UpdateController.class)
@Import(TestSecurityConfig.class)
class UpdateControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private UpdateService updateService;
    
    @Test
    void checkUpdate_WithValidRequest_ShouldReturnUpdateResponse() throws Exception {
        // Given
        UpdateRequest request = new UpdateRequest();
        request.setCountryCode("FR");
        request.setRegionCode("IDF");
        request.setCurrentVersion("1.0.0");
        
        UpdateResponse response = UpdateResponse.builder()
                .hasUpdates(true)
                .latestVersion("1.1.0")
                .downloadUrl("/api/v1/update/download/1.1.0")
                .packageSize(1024L)
                .checksum("abc123")
                .message("Update available")
                .nextCheckTime(LocalDateTime.now().plusHours(1))
                .build();
        
        when(updateService.checkForUpdates(eq("FR"), eq("IDF"), eq("1.0.0"), any()))
                .thenReturn(response);
        
        // When & Then
        mockMvc.perform(post("/api/v1/update/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.hasUpdates").value(true))
                .andExpect(jsonPath("$.latestVersion").value("1.1.0"));
        
        // Vérifier que le service mock a été appelé
        verify(updateService).checkForUpdates(eq("FR"), eq("IDF"), eq("1.0.0"), any());
    }
    
    @Test
    void checkUpdate_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Given
        UpdateRequest request = new UpdateRequest();
        request.setCountryCode("INVALID"); // Code pays invalide
        request.setCurrentVersion("1.0.0");
        
        // When & Then
        mockMvc.perform(post("/api/v1/update/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void checkUpdate_WithMissingCountryCode_ShouldReturnBadRequest() throws Exception {
        // Given
        UpdateRequest request = new UpdateRequest();
        request.setCurrentVersion("1.0.0");
        // countryCode manquant
        
        // When & Then
        mockMvc.perform(post("/api/v1/update/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void downloadUpdate_WithValidParameters_ShouldReturnFile() throws Exception {
        // Given
        String version = "1.1.0";
        String countryCode = "FR";
        String regionCode = "IDF";
        Resource resource = new ByteArrayResource("fake zip content".getBytes());
        
        when(updateService.getUpdatePackage(version, countryCode, regionCode))
                .thenReturn(resource);
        
        // When & Then
        mockMvc.perform(get("/api/v1/update/download/{version}", version)
                .param("countryCode", countryCode)
                .param("regionCode", regionCode))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/octet-stream"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"update-1.1.0.zip\""));
    }
    
    @Test
    void downloadUpdate_WithMissingCountryCode_ShouldReturnBadRequest() throws Exception {
        // Given
        String version = "1.1.0";
        
        // When & Then
        mockMvc.perform(get("/api/v1/update/download/{version}", version))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void getVersion_ShouldReturnCurrentVersion() throws Exception {
        // Given
        when(updateService.getCurrentServiceVersion()).thenReturn("1.0.0");
        
        // When & Then
        mockMvc.perform(get("/api/v1/update/version"))
                .andExpect(status().isOk())
                .andExpect(content().string("1.0.0"));
    }
}