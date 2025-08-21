package com.angel.update.collector;

import com.angel.update.model.CollectorStatus;
import com.angel.update.model.Country;
import com.angel.update.model.Region;
import com.angel.update.repository.CountryRepository;
import com.angel.update.repository.RegionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Collecteur pour les actualités Google News RSS
 * Récupère les news régionales, nationales et internationales
 */
@Component
@Slf4j
public class GoogleNewsCollector extends BaseCollector {
    
    @Autowired
    private CountryRepository countryRepository;
    
    @Autowired
    private RegionRepository regionRepository;
    
    private static final String BASE_URL = "https://news.google.com/rss/search";
    private static final String DATA_DIR = "data/news";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    @Override
    public String getCollectorName() {
        return "GoogleNewsCollector";
    }
    
    @Override
    public String getContentType() {
        return "news";
    }
    
    @Override
    public void collect() throws Exception {
        try {
            log.info("Starting Google News collection");
            
            // Créer les répertoires nécessaires
            createDirectories();
            
            // Collecter les actualités régionales
            collectRegionalNews();
            
            // Collecter les actualités nationales
            collectNationalNews();
            
            // Collecter les actualités internationales
            collectInternationalNews();
            
            updateCollectorStatus(CollectorStatus.Status.SUCCESS, "Collection completed successfully");
            log.info("Google News collection completed");
            
        } catch (Exception e) {
            log.error("Error during Google News collection", e);
            updateCollectorStatus(CollectorStatus.Status.ERROR, "Collection failed: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Collecte les actualités régionales
     */
    private void collectRegionalNews() {
        log.info("Collecting regional news");
        
        List<Country> countries = countryRepository.findByActiveTrue();
        for (Country country : countries) {
            for (Region region : country.getRegions()) {
                if (region.getActive()) {
                    collectRegionalNewsForRegion(country, region);
                }
            }
        }
    }
    
    /**
     * Collecte les actualités pour une région spécifique
     */
    private void collectRegionalNewsForRegion(Country country, Region region) {
        try {
            String query = region.getName();
            String url = buildGoogleNewsUrl(query, country.getLanguageCode(), country.getCode());
            
            log.debug("Fetching regional news for {}/{}: {}", country.getCode(), region.getCode(), url);
            
            List<NewsItem> newsItems = parseRssFeed(url);
            List<String> processedTitles = processNewsItems(newsItems);
            
            // Sauvegarder dans data/news/regional/<pays>/<region>/<date>.txt
            String fileName = LocalDate.now().format(DATE_FORMAT) + ".txt";
            Path filePath = Paths.get(DATA_DIR, "regional", country.getCode(), region.getCode(), fileName);
            
            saveNewsToFile(filePath, processedTitles);
            log.info("Saved {} regional news items for {}/{}", processedTitles.size(), country.getCode(), region.getCode());
            
        } catch (Exception e) {
            log.error("Error collecting regional news for {}/{}", country.getCode(), region.getCode(), e);
        }
    }
    
    /**
     * Collecte les actualités nationales
     */
    private void collectNationalNews() {
        log.info("Collecting national news");
        
        List<Country> countries = countryRepository.findByActiveTrue();
        for (Country country : countries) {
            try {
                String query = country.getName();
                String url = buildGoogleNewsUrl(query, country.getLanguageCode(), country.getCode());
                
                log.debug("Fetching national news for {}: {}", country.getCode(), url);
                
                List<NewsItem> newsItems = parseRssFeed(url);
                List<String> processedTitles = processNewsItems(newsItems);
                
                // Sauvegarder dans data/news/national/<pays>/<date>.txt
                String fileName = LocalDate.now().format(DATE_FORMAT) + ".txt";
                Path filePath = Paths.get(DATA_DIR, "national", country.getCode(), fileName);
                
                saveNewsToFile(filePath, processedTitles);
                log.info("Saved {} national news items for {}", processedTitles.size(), country.getCode());
                
            } catch (Exception e) {
                log.error("Error collecting national news for {}", country.getCode(), e);
            }
        }
    }
    
    /**
     * Collecte les actualités internationales
     */
    private void collectInternationalNews() {
        log.info("Collecting international news");
        
        try {
            // Actualités internationales génériques
            String url = buildGoogleNewsUrl("world news", "en", "US");
            
            log.debug("Fetching international news: {}", url);
            
            List<NewsItem> newsItems = parseRssFeed(url);
            List<String> processedTitles = processNewsItems(newsItems);
            
            // Sauvegarder dans data/news/international/<date>.txt
            String fileName = LocalDate.now().format(DATE_FORMAT) + ".txt";
            Path filePath = Paths.get(DATA_DIR, "international", fileName);
            
            saveNewsToFile(filePath, processedTitles);
            log.info("Saved {} international news items", processedTitles.size());
            
        } catch (Exception e) {
            log.error("Error collecting international news", e);
        }
    }
    
    /**
     * Construit l'URL Google News RSS
     */
    private String buildGoogleNewsUrl(String query, String language, String country) {
        return String.format("%s?q=%s&hl=%s&gl=%s&ceid=%s:%s", 
                BASE_URL, 
                query.replace(" ", "%20"), 
                language, 
                country, 
                country, 
                language);
    }
    
    /**
     * Parse le flux RSS et extrait les éléments de news
     */
    private List<NewsItem> parseRssFeed(String urlString) throws Exception {
        List<NewsItem> newsItems = new ArrayList<>();
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new URL(urlString).openStream());
        
        NodeList items = document.getElementsByTagName("item");
        
        for (int i = 0; i < items.getLength(); i++) {
            Element item = (Element) items.item(i);
            
            String title = getElementText(item, "title");
            String link = getElementText(item, "link");
            String pubDate = getElementText(item, "pubDate");
            String description = getElementText(item, "description");
            
            if (title != null && !title.isEmpty()) {
                newsItems.add(new NewsItem(title, link, pubDate, description));
            }
        }
        
        return newsItems;
    }
    
    /**
     * Traite les éléments de news et nettoie les titres
     */
    private List<String> processNewsItems(List<NewsItem> newsItems) {
        List<String> processedTitles = new ArrayList<>();
        
        for (NewsItem item : newsItems) {
            String cleanTitle = cleanTitle(item.getTitle());
            if (!cleanTitle.isEmpty()) {
                processedTitles.add(cleanTitle);
            }
        }
        
        return processedTitles;
    }
    
    /**
     * Nettoie un titre : enlève la partie après le dernier '-' et améliore la lisibilité
     */
    private String cleanTitle(String title) {
        if (title == null || title.isEmpty()) {
            return "";
        }
        
        // Enlever la partie après le dernier '-' (source du média)
        int lastDashIndex = title.lastIndexOf('-');
        if (lastDashIndex > 0) {
            title = title.substring(0, lastDashIndex).trim();
        }
        
        // Nettoyer et améliorer la lisibilité
        title = title.replaceAll("\\s+", " "); // Normaliser les espaces
        title = title.replaceAll("^[\\s\\p{Punct}]+", ""); // Enlever ponctuation au début
        title = title.replaceAll("[\\s\\p{Punct}]+$", ""); // Enlever ponctuation à la fin
        
        // S'assurer que la phrase se termine correctement
        if (!title.isEmpty() && !title.matches(".*[.!?]$")) {
            title += ".";
        }
        
        return title;
    }
    
    /**
     * Sauvegarde les actualités dans un fichier
     */
    private void saveNewsToFile(Path filePath, List<String> newsItems) throws IOException {
        // Créer les répertoires parents si nécessaire
        Files.createDirectories(filePath.getParent());
        
        // Écrire les actualités dans le fichier
        StringBuilder content = new StringBuilder();
        content.append("# Actualités du ").append(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n\n");
        
        for (int i = 0; i < newsItems.size(); i++) {
            content.append(String.format("%d. %s\n", i + 1, newsItems.get(i)));
        }
        
        Files.write(filePath, content.toString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
    
    /**
     * Crée les répertoires nécessaires
     */
    private void createDirectories() throws IOException {
        Files.createDirectories(Paths.get(DATA_DIR, "regional"));
        Files.createDirectories(Paths.get(DATA_DIR, "national"));
        Files.createDirectories(Paths.get(DATA_DIR, "international"));
    }
    
    /**
     * Utilitaire pour extraire le texte d'un élément XML
     */
    private String getElementText(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return null;
    }
    
    /**
     * Classe interne pour représenter un élément de news
     */
    private static class NewsItem {
        private final String title;
        private final String link;
        private final String pubDate;
        private final String description;
        
        public NewsItem(String title, String link, String pubDate, String description) {
            this.title = title;
            this.link = link;
            this.pubDate = pubDate;
            this.description = description;
        }
        
        public String getTitle() { return title; }
        public String getLink() { return link; }
        public String getPubDate() { return pubDate; }
        public String getDescription() { return description; }
    }
}