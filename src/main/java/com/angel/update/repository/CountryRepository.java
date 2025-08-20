package com.angel.update.repository;

import com.angel.update.model.Country;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour la gestion des pays
 */
@Repository
public interface CountryRepository extends JpaRepository<Country, Long> {
    
    /**
     * Trouve un pays par son code
     */
    Optional<Country> findByCode(String code);
    
    /**
     * Trouve tous les pays actifs
     */
    List<Country> findByActiveTrue();
    
    /**
     * Trouve les pays par langue
     */
    @Query("SELECT c FROM Country c WHERE c.languageCode = :languageCode AND c.active = true")
    List<Country> findByLanguageCode(@Param("languageCode") String languageCode);
    
    /**
     * Trouve les pays par continent
     */
    List<Country> findByContinentAndActiveTrue(String continent);
    
    /**
     * Vérifie si un pays existe et est actif
     */
    boolean existsByCodeAndActiveTrue(String code);
    
    /**
     * Trouve les pays par devise (via metadata)
     */
    @Query("SELECT c FROM Country c WHERE JSON_EXTRACT(c.metadata, '$.currency') = :currency AND c.active = true")
    List<Country> findByCurrencyAndActiveTrue(@Param("currency") String currency);
    
    /**
     * Trouve les pays avec une population supérieure à un seuil (via metadata)
     */
    @Query("SELECT c FROM Country c WHERE CAST(JSON_EXTRACT(c.metadata, '$.population') AS INTEGER) > :minPopulation AND c.active = true")
    List<Country> findByPopulationGreaterThan(@Param("minPopulation") Integer minPopulation);
    
    /**
     * Trouve les pays par indicatif téléphonique (via metadata)
     */
    @Query("SELECT c FROM Country c WHERE JSON_EXTRACT(c.metadata, '$.phoneCode') = :phoneCode")
    Optional<Country> findByPhoneCode(@Param("phoneCode") String phoneCode);
}