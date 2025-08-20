package com.angel.update.service;

import com.angel.update.model.UpdateResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Service de cache multi-niveaux (L1: Caffeine, L2: Redis)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    // TTL par défaut pour les différents types de cache
    private static final Duration DEFAULT_UPDATE_RESPONSE_TTL = Duration.ofHours(1);
    private static final Duration DEFAULT_CONTENT_TTL = Duration.ofMinutes(30);
    private static final Duration DEFAULT_NEWS_TTL = Duration.ofMinutes(15);
    private static final Duration DEFAULT_WEATHER_TTL = Duration.ofMinutes(10);
    
    /**
     * Cache de niveau 1 (Caffeine) pour les réponses de mise à jour
     */
    @Cacheable(value = "updateResponses", key = "#cacheKey")
    public UpdateResponse getUpdateResponse(String cacheKey) {
        // Si pas en cache L1, vérifier le cache L2 (Redis)
        return getFromRedis(cacheKey, UpdateResponse.class);
    }
    
    /**
     * Met en cache une réponse de mise à jour
     */
    @CachePut(value = "updateResponses", key = "#cacheKey")
    public UpdateResponse putUpdateResponse(String cacheKey, UpdateResponse response) {
        // Mettre également en cache L2 (Redis)
        putInRedis(cacheKey, response, DEFAULT_UPDATE_RESPONSE_TTL);
        return response;
    }
    
    /**
     * Cache pour les contenus par type
     */
    @Cacheable(value = "contents", key = "#contentType + '-' + #countryCode + '-' + #regionCode")
    public Object getContentCache(String contentType, String countryCode, String regionCode) {
        String cacheKey = buildContentCacheKey(contentType, countryCode, regionCode);
        return getFromRedis(cacheKey, Object.class);
    }
    
    /**
     * Met en cache du contenu
     */
    @CachePut(value = "contents", key = "#contentType + '-' + #countryCode + '-' + #regionCode")
    public Object putContentCache(String contentType, String countryCode, String regionCode, Object content) {
        String cacheKey = buildContentCacheKey(contentType, countryCode, regionCode);
        Duration ttl = getContentTTL(contentType);
        putInRedis(cacheKey, content, ttl);
        return content;
    }
    
    /**
     * Cache spécialisé pour les actualités
     */
    public void cacheNews(String countryCode, String regionCode, Object newsData) {
        String cacheKey = "news:" + countryCode + ":" + (regionCode != null ? regionCode : "national");
        putInRedis(cacheKey, newsData, DEFAULT_NEWS_TTL);
        log.debug("Cached news for {}", cacheKey);
    }
    
    public Object getCachedNews(String countryCode, String regionCode) {
        String cacheKey = "news:" + countryCode + ":" + (regionCode != null ? regionCode : "national");
        return getFromRedis(cacheKey, Object.class);
    }
    
    /**
     * Cache spécialisé pour la météo
     */
    public void cacheWeather(String countryCode, String regionCode, Object weatherData) {
        String cacheKey = "weather:" + countryCode + ":" + (regionCode != null ? regionCode : "national");
        putInRedis(cacheKey, weatherData, DEFAULT_WEATHER_TTL);
        log.debug("Cached weather for {}", cacheKey);
    }
    
    public Object getCachedWeather(String countryCode, String regionCode) {
        String cacheKey = "weather:" + countryCode + ":" + (regionCode != null ? regionCode : "national");
        return getFromRedis(cacheKey, Object.class);
    }
    
    /**
     * Vide le cache par pattern
     */
    @CacheEvict(value = {"updateResponses", "contents"}, allEntries = true)
    public void evictCache(String pattern) {
        if (pattern != null && !pattern.isEmpty()) {
            // Supprimer du cache Redis par pattern
            evictFromRedis(pattern);
        }
        log.info("Cache evicted for pattern: {}", pattern);
    }
    
    /**
     * Vide tout le cache
     */
    @CacheEvict(value = {"updateResponses", "contents", "news", "weather"}, allEntries = true)
    public void evictAllCache() {
        // Vider aussi Redis
        try {
            redisTemplate.getConnectionFactory().getConnection().flushDb();
            log.info("All caches evicted");
        } catch (Exception e) {
            log.warn("Error flushing Redis cache", e);
        }
    }
    
    /**
     * Obtient les statistiques du cache
     */
    public CacheStats getCacheStats() {
        // Implementation basique - à améliorer avec les vraies métriques Caffeine
        return CacheStats.builder()
                .redisConnected(isRedisConnected())
                .build();
    }
    
    /**
     * Vérifie la connectivité Redis
     */
    public boolean isRedisConnected() {
        try {
            redisTemplate.opsForValue().get("ping");
            return true;
        } catch (Exception e) {
            log.warn("Redis connection check failed", e);
            return false;
        }
    }
    
    // Méthodes utilitaires privées
    
    private <T> T getFromRedis(String key, Class<T> clazz) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                return objectMapper.readValue(value, clazz);
            }
        } catch (Exception e) {
            log.warn("Error reading from Redis cache: {}", key, e);
        }
        return null;
    }
    
    private void putInRedis(String key, Object value, Duration ttl) {
        try {
            String jsonValue = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, jsonValue, ttl.toSeconds(), TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Error writing to Redis cache: {}", key, e);
        }
    }
    
    private void evictFromRedis(String pattern) {
        try {
            var keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("Error evicting from Redis cache with pattern: {}", pattern, e);
        }
    }
    
    private String buildContentCacheKey(String contentType, String countryCode, String regionCode) {
        return String.format("content:%s:%s:%s", 
                contentType, countryCode, regionCode != null ? regionCode : "national");
    }
    
    private Duration getContentTTL(String contentType) {
        return switch (contentType.toLowerCase()) {
            case "news" -> DEFAULT_NEWS_TTL;
            case "weather" -> DEFAULT_WEATHER_TTL;
            case "recipes", "stories" -> Duration.ofHours(2);
            case "discoveries" -> Duration.ofHours(6);
            default -> DEFAULT_CONTENT_TTL;
        };
    }
    
    /**
     * Classe pour les statistiques de cache
     */
    public static class CacheStats {
        private final boolean redisConnected;
        
        private CacheStats(boolean redisConnected) {
            this.redisConnected = redisConnected;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public boolean isRedisConnected() {
            return redisConnected;
        }
        
        public static class Builder {
            private boolean redisConnected;
            
            public Builder redisConnected(boolean redisConnected) {
                this.redisConnected = redisConnected;
                return this;
            }
            
            public CacheStats build() {
                return new CacheStats(redisConnected);
            }
        }
    }
}