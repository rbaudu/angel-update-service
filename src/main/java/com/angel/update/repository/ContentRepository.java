package com.angel.update.repository;

import com.angel.update.model.Content;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository pour la gestion des contenus
 */
@Repository
public interface ContentRepository extends JpaRepository<Content, Long> {
    
    /**
     * Trouve les contenus par pays et région dans une plage de dates
     */
    @Query("SELECT c FROM Content c WHERE " +
           "c.countryCode = :countryCode AND " +
           "(:regionCode IS NULL OR c.regionCode = :regionCode) AND " +
           "c.publishedAt BETWEEN :fromDate AND :toDate AND " +
           "c.status = 'ACTIVE'")
    List<Content> findByCountryAndRegionAndDateRange(
            @Param("countryCode") String countryCode,
            @Param("regionCode") String regionCode,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );
    
    /**
     * Trouve les contenus actifs par type et localisation
     */
    @Query("SELECT c FROM Content c WHERE " +
           "c.contentType = :contentType AND " +
           "c.countryCode = :countryCode AND " +
           "(:regionCode IS NULL OR c.regionCode = :regionCode) AND " +
           "c.status = 'ACTIVE' " +
           "ORDER BY c.priority DESC, c.publishedAt DESC")
    List<Content> findActiveContent(
            @Param("contentType") String contentType,
            @Param("countryCode") String countryCode,
            @Param("regionCode") String regionCode
    );
    
    /**
     * Trouve les contenus par type et pays
     */
    List<Content> findByContentTypeAndCountryCodeAndStatusOrderByPublishedAtDesc(
            String contentType, String countryCode, Content.ContentStatus status);
    
    /**
     * Trouve les contenus modifiés après une date
     */
    @Query("SELECT c FROM Content c WHERE c.lastModified > :since AND c.status = 'ACTIVE'")
    List<Content> findModifiedSince(@Param("since") LocalDateTime since);
    
    /**
     * Compte les contenus par type et pays
     */
    @Query("SELECT COUNT(c) FROM Content c WHERE " +
           "c.contentType = :contentType AND " +
           "c.countryCode = :countryCode AND " +
           "c.status = 'ACTIVE'")
    Long countActiveContentByTypeAndCountry(
            @Param("contentType") String contentType,
            @Param("countryCode") String countryCode
    );
    
    /**
     * Trouve les contenus par checksum pour détecter les doublons
     */
    List<Content> findByChecksum(String checksum);
    
    /**
     * Supprime les anciens contenus (soft delete)
     */
    @Query("UPDATE Content c SET c.status = 'ARCHIVED' WHERE " +
           "c.publishedAt < :cutoffDate AND c.status = 'ACTIVE'")
    void archiveOldContent(@Param("cutoffDate") LocalDateTime cutoffDate);
}