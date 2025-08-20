package com.angel.update.repository;

import com.angel.update.model.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour la gestion des régions
 */
@Repository
public interface RegionRepository extends JpaRepository<Region, Long> {
    
    /**
     * Trouve une région par son code
     */
    Optional<Region> findByCode(String code);
    
    /**
     * Trouve une région par pays et code région
     */
    Optional<Region> findByCountryCodeAndRegionCode(String countryCode, String regionCode);
    
    /**
     * Trouve toutes les régions d'un pays
     */
    List<Region> findByCountryCodeAndActiveTrue(String countryCode);
    
    /**
     * Trouve toutes les régions actives
     */
    List<Region> findByActiveTrue();
    
    /**
     * Trouve les régions par langue
     */
    @Query("SELECT r FROM Region r WHERE r.languageCode = :languageCode AND r.active = true")
    List<Region> findByLanguageCode(@Param("languageCode") String languageCode);
    
    /**
     * Trouve les régions par fuseau horaire
     */
    List<Region> findByTimezoneAndActiveTrue(String timezone);
    
    /**
     * Vérifie si une région existe et est active
     */
    boolean existsByCountryCodeAndRegionCodeAndActiveTrue(String countryCode, String regionCode);
}