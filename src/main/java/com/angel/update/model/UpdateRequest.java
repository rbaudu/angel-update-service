package com.angel.update.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Requête de vérification de mise à jour
 */
@Data
public class UpdateRequest {
    
    @NotBlank(message = "Country code is required")
    @Pattern(regexp = "^[A-Z]{2}$", message = "Country code must be 2 uppercase letters")
    private String countryCode;
    
    @Pattern(regexp = "^[A-Z]{2,3}$", message = "Region code must be 2-3 uppercase letters")
    private String regionCode;
    
    @NotBlank(message = "Current version is required")
    @Pattern(regexp = "^\\d+\\.\\d+\\.\\d+$", message = "Version must be in format X.Y.Z")
    private String currentVersion;
    
    private String languageCode;
    
    private String clientId;
}
