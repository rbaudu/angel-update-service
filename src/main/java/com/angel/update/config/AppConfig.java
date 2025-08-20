package com.angel.update.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;
import java.util.Map;
import java.util.List;

/**
 * Configuration principale de l'application
 */
@Configuration
@ConfigurationProperties(prefix = "angel")
@Data
public class AppConfig {
    
    private UpdateConfig update = new UpdateConfig();
    private CacheConfig cache = new CacheConfig();
    private CollectorsConfig collectors = new CollectorsConfig();
    private SecurityConfig security = new SecurityConfig();
    private PerformanceConfig performance = new PerformanceConfig();
    
    @Data
    public static class UpdateConfig {
        private String baseDataPath = "/data";
        private String zipCachePath = "/cache/zips";
        private long maxZipSize = 52428800L; // 50MB
        private int compressionLevel = 6;
    }
    
    @Data
    public static class CacheConfig {
        private boolean enabled = true;
        private RedisConfig redis = new RedisConfig();
        private CaffeineConfig caffeine = new CaffeineConfig();
        
        @Data
        public static class RedisConfig {
            private String host;
            private int port;
            private String password;
            private Map<String, Integer> ttl = Map.of(
                "news", 3600,
                "weather", 1800,
                "recipes", 7200,
                "stories", 86400
            );
        }
        
        @Data
        public static class CaffeineConfig {
            private boolean enabled = true;
            private Map<String, String> spec = Map.of(
                "news", "maximumSize=100,expireAfterWrite=30m",
                "weather", "maximumSize=50,expireAfterWrite=15m",
                "recipes", "maximumSize=200,expireAfterWrite=1h",
                "stories", "maximumSize=500,expireAfterWrite=24h"
            );
        }
    }
    
    @Data
    public static class CollectorsConfig {
        private boolean enabled = true;
        private boolean mockMode = false;
        private Map<String, CollectorConfig> collectors = Map.of();
        
        @Data
        public static class CollectorConfig {
            private boolean enabled;
            private String schedule;
            private List<SourceConfig> sources;
        }
        
        @Data
        public static class SourceConfig {
            private String name;
            private String url;
            private String apiKey;
            private int rateLimit = 100;
        }
    }
    
    @Data
    public static class SecurityConfig {
        private ApiKeyConfig apiKey = new ApiKeyConfig();
        private RateLimitConfig rateLimit = new RateLimitConfig();
        
        @Data
        public static class ApiKeyConfig {
            private boolean enabled = true;
            private String headerName = "X-API-Key";
            private String queryParam = "apiKey";
        }
        
        @Data
        public static class RateLimitConfig {
            private boolean enabled = true;
            private int requestsPerSecond = 10;
            private int burstCapacity = 20;
        }
    }
    
    @Data
    public static class PerformanceConfig {
        private ThreadPoolConfig threadPool = new ThreadPoolConfig();
        
        @Data
        public static class ThreadPoolConfig {
            private int coreSize = 10;
            private int maxSize = 50;
            private int queueCapacity = 500;
            private int keepAliveSeconds = 60;
        }
    }
}
