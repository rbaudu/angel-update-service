package com.angel.update.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Service de construction des packages ZIP de mise à jour
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ZipBuilderService {
    
    @Value("${angel.update.package.base-path:/data/packages}")
    private String packageBasePath;
    
    @Value("${angel.update.content.base-path:/data}")
    private String contentBasePath;
    
    /**
     * Construit un package de mise à jour différentiel
     */
    public String buildUpdatePackage(String countryCode, String regionCode, 
                                   String fromVersion, String toVersion, 
                                   List<String> changedFiles) {
        
        String packageFileName = buildPackageFileName(countryCode, regionCode, toVersion);
        String packagePath = Paths.get(packageBasePath, packageFileName).toString();
        
        try {
            // Créer le répertoire de package si nécessaire
            Files.createDirectories(Paths.get(packageBasePath));
            
            // Vérifier si le package existe déjà
            if (Files.exists(Paths.get(packagePath))) {
                log.info("Package already exists: {}", packagePath);
                return packagePath;
            }
            
            // Créer le fichier ZIP
            try (FileOutputStream fos = new FileOutputStream(packagePath);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {
                
                // Ajouter le manifeste
                addManifestToZip(zos, countryCode, regionCode, fromVersion, toVersion, changedFiles);
                
                // Ajouter chaque fichier modifié
                for (String filePath : changedFiles) {
                    addFileToZip(zos, filePath);
                }
                
                log.info("Created update package: {} with {} files", packagePath, changedFiles.size());
            }
            
            return packagePath;
            
        } catch (IOException e) {
            log.error("Error creating update package", e);
            throw new RuntimeException("Failed to create update package", e);
        }
    }
    
    /**
     * Récupère le chemin d'un package existant
     */
    public String getPackagePath(String version, String countryCode, String regionCode) {
        String packageFileName = buildPackageFileName(countryCode, regionCode, version);
        return Paths.get(packageBasePath, packageFileName).toString();
    }
    
    /**
     * Calcule la taille d'un package
     */
    public long getPackageSize(String packagePath) {
        try {
            return Files.size(Paths.get(packagePath));
        } catch (IOException e) {
            log.error("Error getting package size for: {}", packagePath, e);
            return 0;
        }
    }
    
    /**
     * Calcule le checksum d'un package
     */
    public String calculateChecksum(String packagePath) {
        try {
            byte[] fileBytes = Files.readAllBytes(Paths.get(packagePath));
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(fileBytes);
            return bytesToHex(hash);
        } catch (Exception e) {
            log.error("Error calculating checksum for: {}", packagePath, e);
            return "";
        }
    }
    
    /**
     * Nettoie les anciens packages
     */
    public void cleanupOldPackages(int maxAgeInDays) {
        try {
            Path packageDir = Paths.get(packageBasePath);
            if (!Files.exists(packageDir)) {
                return;
            }
            
            long cutoffTime = System.currentTimeMillis() - (maxAgeInDays * 24L * 60 * 60 * 1000);
            
            Files.walk(packageDir)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".zip"))
                    .forEach(path -> {
                        try {
                            if (Files.getLastModifiedTime(path).toMillis() < cutoffTime) {
                                Files.delete(path);
                                log.info("Deleted old package: {}", path);
                            }
                        } catch (IOException e) {
                            log.warn("Could not delete old package: {}", path, e);
                        }
                    });
                    
        } catch (IOException e) {
            log.error("Error during package cleanup", e);
        }
    }
    
    private String buildPackageFileName(String countryCode, String regionCode, String version) {
        String regionPart = (regionCode != null && !regionCode.isEmpty()) 
                ? "-" + regionCode.toLowerCase() 
                : "";
        return String.format("update-%s%s-%s.zip", 
                countryCode.toLowerCase(), regionPart, version);
    }
    
    private void addManifestToZip(ZipOutputStream zos, String countryCode, String regionCode,
                                 String fromVersion, String toVersion, List<String> changedFiles) 
            throws IOException {
        
        StringBuilder manifest = new StringBuilder();
        manifest.append("# Angel Update Package Manifest\n");
        manifest.append("version=").append(toVersion).append("\n");
        manifest.append("country=").append(countryCode).append("\n");
        manifest.append("region=").append(regionCode != null ? regionCode : "national").append("\n");
        manifest.append("from_version=").append(fromVersion).append("\n");
        manifest.append("created=").append(java.time.Instant.now()).append("\n");
        manifest.append("file_count=").append(changedFiles.size()).append("\n");
        manifest.append("\n# Changed Files:\n");
        
        for (String file : changedFiles) {
            manifest.append(file).append("\n");
        }
        
        ZipEntry manifestEntry = new ZipEntry("MANIFEST.txt");
        zos.putNextEntry(manifestEntry);
        zos.write(manifest.toString().getBytes());
        zos.closeEntry();
    }
    
    private void addFileToZip(ZipOutputStream zos, String filePath) throws IOException {
        Path sourceFile = Paths.get(contentBasePath, filePath);
        
        if (!Files.exists(sourceFile)) {
            log.warn("File not found, skipping: {}", sourceFile);
            return;
        }
        
        ZipEntry entry = new ZipEntry(filePath);
        zos.putNextEntry(entry);
        
        try (InputStream is = Files.newInputStream(sourceFile)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = is.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }
        }
        
        zos.closeEntry();
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}