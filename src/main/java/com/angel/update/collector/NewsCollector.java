package com.angel.update.collector;

import com.angel.update.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Collecteur d'actualités depuis des APIs externes
 */
@Component
@ConditionalOnProperty(name = "angel.collectors.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class NewsCollector extends BaseCollector {
    
    private final CacheService cacheService;
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${angel.collectors.news.api-key:demo}")
    private String apiKey;
    
    @Value("${angel.collectors.news.sources:reuters,afp}")
    private List<String> newsSources;
    
    @Value("${angel.collectors.mock-mode:false}")
    private boolean mockMode;
    
    @Override
    public String getCollectorName() {
        return "NewsCollector";
    }
    
    @Override
    public String getContentType() {
        return "news";
    }
    
    @Override
    public void collect() throws Exception {
        collectNews();
    }
    
    /**
     * Collecte programmée des actualités (toutes les 30 minutes)
     */
    @Scheduled(cron = "${angel.collectors.schedule.news:0 */30 * * * *}")
    public void collectNews() {
        if (!isEnabled()) {
            log.debug("News collector is disabled");
            return;
        }
        
        log.info("Starting news collection...");
        
        try {
            // Collecter pour les principaux pays
            collectForCountry("FR", null);
            collectForCountry("FR", "IDF"); // Île-de-France
            collectForCountry("US", null);
            collectForCountry("GB", null);
            collectForCountry("DE", null);
            
            updateCollectorStatus(com.angel.update.model.CollectorStatus.Status.ACTIVE, "News collection completed successfully");
            log.info("News collection completed successfully");
            
        } catch (Exception e) {
            updateCollectorStatus(com.angel.update.model.CollectorStatus.Status.ERROR, "News collection failed: " + e.getMessage());
            log.error("Error during news collection", e);
        }
    }
    
    /**
     * Collecte les actualités pour un pays spécifique
     */
    private void collectForCountry(String countryCode, String regionCode) {
        try {
            List<NewsArticle> articles;
            
            if (mockMode) {
                articles = generateMockNews(countryCode, regionCode);
            } else {
                articles = fetchNewsFromAPI(countryCode, regionCode);
            }
            
            if (!articles.isEmpty()) {
                // Mettre en cache
                cacheService.cacheNews(countryCode, regionCode, articles);
                
                // Sauvegarder en base (via ContentManagerService)
                saveArticles(articles, countryCode, regionCode);
                
                log.info("Collected {} news articles for {}-{}", 
                        articles.size(), countryCode, regionCode);
            }
            
        } catch (Exception e) {
            log.error("Error collecting news for {}-{}", countryCode, regionCode, e);
        }
    }
    
    /**
     * Récupère les actualités depuis l'API externe
     */
    private List<NewsArticle> fetchNewsFromAPI(String countryCode, String regionCode) {
        // Simulation d'appel API - à remplacer par de vrais appels
        String url = buildNewsApiUrl(countryCode, regionCode);
        
        try {
            // Exemple avec une API REST générique
            NewsApiResponse response = restTemplate.getForObject(url, NewsApiResponse.class);
            
            if (response != null && response.getArticles() != null) {
                return response.getArticles().stream()
                        .filter(this::isValidArticle)
                        .toList();
            }
            
        } catch (Exception e) {
            log.warn("Failed to fetch news from API: {}", url, e);
        }
        
        return List.of();
    }
    
    /**
     * Génère des actualités simulées pour le mode mock
     */
    private List<NewsArticle> generateMockNews(String countryCode, String regionCode) {
        return List.of(
                NewsArticle.builder()
                        .title("Actualité locale " + (regionCode != null ? regionCode : countryCode))
                        .content("Contenu de l'actualité simulée pour " + countryCode)
                        .source("Mock Source")
                        .publishedAt(LocalDateTime.now())
                        .countryCode(countryCode)
                        .regionCode(regionCode)
                        .category("general")
                        .language(getLanguageForCountry(countryCode))
                        .build(),
                
                NewsArticle.builder()
                        .title("Mise à jour économique")
                        .content("Information économique importante pour " + countryCode)
                        .source("Mock Economic News")
                        .publishedAt(LocalDateTime.now().minusMinutes(15))
                        .countryCode(countryCode)
                        .regionCode(regionCode)
                        .category("business")
                        .language(getLanguageForCountry(countryCode))
                        .build()
        );
    }
    
    private void saveArticles(List<NewsArticle> articles, String countryCode, String regionCode) {
        // Implémentation de la sauvegarde via ContentManagerService
        // Pour l'instant, juste un log
        log.debug("Saving {} articles for {}-{}", articles.size(), countryCode, regionCode);
    }
    
    private String buildNewsApiUrl(String countryCode, String regionCode) {
        // Construction d'URL basique - à adapter selon l'API utilisée
        return String.format("https://api.news.com/v1/articles?country=%s&apikey=%s", 
                countryCode.toLowerCase(), apiKey);
    }
    
    private boolean isValidArticle(NewsArticle article) {
        return article.getTitle() != null && !article.getTitle().trim().isEmpty() &&
               article.getContent() != null && !article.getContent().trim().isEmpty();
    }
    
    private String getLanguageForCountry(String countryCode) {
        return switch (countryCode) {
            case "FR" -> "fr";
            case "US", "GB" -> "en";
            case "DE" -> "de";
            case "ES" -> "es";
            default -> "en";
        };
    }
    
    /**
     * Classes pour la sérialisation des réponses API
     */
    public static class NewsApiResponse {
        private List<NewsArticle> articles;
        private String status;
        private int totalResults;
        
        // Getters et setters
        public List<NewsArticle> getArticles() { return articles; }
        public void setArticles(List<NewsArticle> articles) { this.articles = articles; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public int getTotalResults() { return totalResults; }
        public void setTotalResults(int totalResults) { this.totalResults = totalResults; }
    }
    
    public static class NewsArticle {
        private String title;
        private String content;
        private String source;
        private LocalDateTime publishedAt;
        private String countryCode;
        private String regionCode;
        private String category;
        private String language;
        private String imageUrl;
        
        public static Builder builder() {
            return new Builder();
        }
        
        // Getters et setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public LocalDateTime getPublishedAt() { return publishedAt; }
        public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
        public String getCountryCode() { return countryCode; }
        public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
        public String getRegionCode() { return regionCode; }
        public void setRegionCode(String regionCode) { this.regionCode = regionCode; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        
        public static class Builder {
            private final NewsArticle article = new NewsArticle();
            
            public Builder title(String title) { article.title = title; return this; }
            public Builder content(String content) { article.content = content; return this; }
            public Builder source(String source) { article.source = source; return this; }
            public Builder publishedAt(LocalDateTime publishedAt) { article.publishedAt = publishedAt; return this; }
            public Builder countryCode(String countryCode) { article.countryCode = countryCode; return this; }
            public Builder regionCode(String regionCode) { article.regionCode = regionCode; return this; }
            public Builder category(String category) { article.category = category; return this; }
            public Builder language(String language) { article.language = language; return this; }
            public Builder imageUrl(String imageUrl) { article.imageUrl = imageUrl; return this; }
            
            public NewsArticle build() { return article; }
        }
    }
}