# Internationalisation (i18n) - ANGEL Update Service

## Vue d'Ensemble

Le service supporte plusieurs langues et régions avec du contenu localisé et des configurations spécifiques par pays.

## Structure Multi-Langues

### 1. Organisation des Données

```
data/
├── fr/                    # France
│   ├── _metadata.json    # Métadonnées pays
│   ├── national/         # Contenu national
│   └── regions/
│       ├── idf/          # Île-de-France
│       ├── paca/         # Provence-Alpes-Côte d'Azur
│       ├── bretagne/     # Bretagne
│       └── ...
├── en/                    # English
│   ├── gb/               # Great Britain
│   │   ├── national/
│   │   └── regions/
│   │       ├── london/
│   │       ├── scotland/
│   │       └── wales/
│   └── us/               # United States
│       ├── national/
│       └── states/
│           ├── ca/       # California
│           ├── ny/       # New York
│           └── tx/       # Texas
├── es/                    # Español
│   ├── es/               # España
│   │   └── regions/
│   │       ├── madrid/
│   │       ├── catalunya/
│   │       └── andalucia/
│   └── mx/               # México
├── de/                    # Deutsch
│   ├── de/               # Deutschland
│   ├── at/               # Österreich
│   └── ch/               # Schweiz
└── ...
```

### 2. Configuration des Langues

#### Fichier de Configuration

```yaml
# config/languages.yml
languages:
  supported:
    - code: fr
      name: Français
      countries:
        - code: FR
          name: France
          regions:
            - code: IDF
              name: Île-de-France
              cities: [Paris, Versailles, Boulogne]
            - code: PACA
              name: Provence-Alpes-Côte d'Azur
              cities: [Marseille, Nice, Toulon]
            - code: BRE
              name: Bretagne
              cities: [Rennes, Brest, Quimper]
        - code: BE
          name: Belgique
          regions:
            - code: WAL
              name: Wallonie
            - code: BRU
              name: Bruxelles
        - code: CH
          name: Suisse
          regions:
            - code: GE
              name: Genève
            - code: VD
              name: Vaud
    
    - code: en
      name: English
      countries:
        - code: GB
          name: United Kingdom
          regions:
            - code: ENG
              name: England
              subregions:
                - code: LON
                  name: London
                - code: MAN
                  name: Manchester
            - code: SCO
              name: Scotland
            - code: WAL
              name: Wales
            - code: NIR
              name: Northern Ireland
        - code: US
          name: United States
          regions:
            - code: CA
              name: California
            - code: NY
              name: New York
            - code: TX
              name: Texas
    
    - code: es
      name: Español
      countries:
        - code: ES
          name: España
          regions:
            - code: MAD
              name: Madrid
            - code: CAT
              name: Catalunya
            - code: AND
              name: Andalucía
        - code: MX
          name: México
        - code: AR
          name: Argentina
    
    - code: de
      name: Deutsch
      countries:
        - code: DE
          name: Deutschland
          regions:
            - code: BAY
              name: Bayern
            - code: BER
              name: Berlin
            - code: NRW
              name: Nordrhein-Westfalen
        - code: AT
          name: Österreich
        - code: CH
          name: Schweiz
```

### 3. Modèle de Données Java

```java
// Region.java
@Entity
@Table(name = "regions")
public class Region {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true)
    private String code;  // ex: "FR-IDF"
    
    private String languageCode;  // ex: "fr"
    private String countryCode;   // ex: "FR"
    private String regionCode;    // ex: "IDF"
    
    private String name;
    private String nativeName;
    
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;
    
    // Relations
    @ManyToOne
    private Country country;
    
    @OneToMany(mappedBy = "region")
    private List<Content> contents;
}

// Country.java
@Entity
@Table(name = "countries")
public class Country {
    @Id
    private String code;  // ISO 3166-1 alpha-2
    
    private String name;
    private String nativeName;
    private String languageCode;
    private String timezone;
    private String currency;
    
    @OneToMany(mappedBy = "country")
    private List<Region> regions;
}

// LocalizedContent.java
@Entity
@Table(name = "localized_contents")
public class LocalizedContent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String contentType;
    private String languageCode;
    private String countryCode;
    private String regionCode;
    
    @Lob
    private String content;
    
    @Column(columnDefinition = "jsonb")
    private Map<String, String> translations;
    
    private LocalDateTime lastUpdated;
    private String version;
}
```

### 4. Service de Localisation

```java
@Service
public class LocalizationService {
    
    @Autowired
    private RegionRepository regionRepository;
    
    @Autowired
    private ContentRepository contentRepository;
    
    /**
     * Récupère le contenu localisé avec fallback
     */
    public Content getLocalizedContent(
            String contentType,
            String languageCode,
            String countryCode,
            String regionCode) {
        
        // 1. Essayer région spécifique
        Optional<Content> content = contentRepository
            .findByTypeAndLocation(contentType, languageCode, countryCode, regionCode);
        
        if (content.isPresent()) {
            return content.get();
        }
        
        // 2. Fallback sur le pays
        content = contentRepository
            .findByTypeAndLocation(contentType, languageCode, countryCode, null);
        
        if (content.isPresent()) {
            return content.get();
        }
        
        // 3. Fallback sur la langue
        content = contentRepository
            .findByTypeAndLocation(contentType, languageCode, null, null);
        
        if (content.isPresent()) {
            return content.get();
        }
        
        // 4. Fallback sur l'anglais
        return contentRepository
            .findByTypeAndLocation(contentType, "en", null, null)
            .orElseThrow(() -> new ContentNotFoundException(
                "No content found for type: " + contentType));
    }
    
    /**
     * Détecte automatiquement la localisation
     */
    public Locale detectLocale(HttpServletRequest request) {
        // 1. Vérifier le header Accept-Language
        String acceptLanguage = request.getHeader("Accept-Language");
        if (acceptLanguage != null) {
            return Locale.forLanguageTag(acceptLanguage.split(",")[0]);
        }
        
        // 2. Vérifier l'IP pour la géolocalisation
        String ip = request.getRemoteAddr();
        Optional<String> country = geoIpService.getCountryByIp(ip);
        
        if (country.isPresent()) {
            return getLocaleForCountry(country.get());
        }
        
        // 3. Défaut
        return Locale.ENGLISH;
    }
}
```

### 5. APIs Externes par Région

```yaml
# config/api-sources.yml
api_sources:
  news:
    fr:
      FR:
        national:
          - name: "France Info"
            url: "https://api.franceinfo.fr"
            api_key: ${FRANCEINFO_API_KEY}
          - name: "Le Monde"
            url: "https://api.lemonde.fr"
            api_key: ${LEMONDE_API_KEY}
        regional:
          IDF:
            - name: "Le Parisien"
              url: "https://api.leparisien.fr"
          PACA:
            - name: "La Provence"
              url: "https://api.laprovence.com"
          BRE:
            - name: "Ouest France"
              url: "https://api.ouest-france.fr"
    
    en:
      GB:
        national:
          - name: "BBC News"
            url: "https://api.bbc.co.uk/news"
            api_key: ${BBC_API_KEY}
          - name: "The Guardian"
            url: "https://content.guardianapis.com"
            api_key: ${GUARDIAN_API_KEY}
        regional:
          SCO:
            - name: "The Scotsman"
              url: "https://api.scotsman.com"
      US:
        national:
          - name: "CNN"
            url: "https://api.cnn.com"
          - name: "Reuters"
            url: "https://api.reuters.com"
        states:
          CA:
            - name: "LA Times"
              url: "https://api.latimes.com"
          NY:
            - name: "NY Times"
              url: "https://api.nytimes.com"
    
  weather:
    global:
      - name: "OpenWeatherMap"
        url: "https://api.openweathermap.org"
        supports_all: true
    
    fr:
      FR:
        - name: "Météo France"
          url: "https://api.meteofrance.fr"
          priority: 1
    
    en:
      US:
        - name: "NOAA"
          url: "https://api.weather.gov"
          priority: 1
    
    de:
      DE:
        - name: "DWD"
          url: "https://api.dwd.de"
          priority: 1
```

### 6. Formats de Données Localisés

```java
@Component
public class LocaleFormatter {
    
    /**
     * Formate une date selon la locale
     */
    public String formatDate(LocalDateTime date, Locale locale) {
        DateTimeFormatter formatter = DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.MEDIUM)
            .withLocale(locale);
        return date.format(formatter);
    }
    
    /**
     * Formate un nombre selon la locale
     */
    public String formatNumber(double number, Locale locale) {
        NumberFormat formatter = NumberFormat.getInstance(locale);
        return formatter.format(number);
    }
    
    /**
     * Formate une devise selon la locale
     */
    public String formatCurrency(double amount, String currencyCode, Locale locale) {
        Currency currency = Currency.getInstance(currencyCode);
        NumberFormat formatter = NumberFormat.getCurrencyInstance(locale);
        formatter.setCurrency(currency);
        return formatter.format(amount);
    }
    
    /**
     * Obtient le format de température
     */
    public String formatTemperature(double temp, Locale locale) {
        if (locale.getCountry().equals("US")) {
            // Fahrenheit pour les US
            double fahrenheit = temp * 9/5 + 32;
            return String.format("%.1f°F", fahrenheit);
        } else {
            // Celsius pour le reste
            return String.format("%.1f°C", temp);
        }
    }
}
```

### 7. Messages Internationalisés

```properties
# messages_fr.properties
welcome.message=Bienvenue sur ANGEL Update Service
update.available=Une mise à jour est disponible
update.none=Aucune mise à jour disponible
error.not_found=Contenu introuvable
error.server=Erreur serveur

# messages_en.properties
welcome.message=Welcome to ANGEL Update Service
update.available=An update is available
update.none=No updates available
error.not_found=Content not found
error.server=Server error

# messages_es.properties
welcome.message=Bienvenido a ANGEL Update Service
update.available=Hay una actualización disponible
update.none=No hay actualizaciones disponibles
error.not_found=Contenido no encontrado
error.server=Error del servidor

# messages_de.properties
welcome.message=Willkommen bei ANGEL Update Service
update.available=Ein Update ist verfügbar
update.none=Keine Updates verfügbar
error.not_found=Inhalt nicht gefunden
error.server=Serverfehler
```

### 8. Controller Internationalisé

```java
@RestController
@RequestMapping("/api/v1")
public class UpdateController {
    
    @Autowired
    private LocalizationService localizationService;
    
    @Autowired
    private MessageSource messageSource;
    
    @PostMapping("/update/check")
    public ResponseEntity<UpdateResponse> checkUpdate(
            @RequestBody UpdateRequest request,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            HttpServletRequest httpRequest) {
        
        // Détection de la locale
        Locale locale = localizationService.detectLocale(httpRequest);
        
        // Récupération du contenu localisé
        UpdateResponse response = updateService.checkForUpdates(
            request.getCountryCode(),
            request.getRegionCode(),
            locale
        );
        
        // Message localisé
        if (response.hasUpdates()) {
            response.setMessage(messageSource.getMessage(
                "update.available", null, locale));
        } else {
            response.setMessage(messageSource.getMessage(
                "update.none", null, locale));
        }
        
        return ResponseEntity.ok(response);
    }
}
```

### 9. Tests de Localisation

```java
@SpringBootTest
@AutoConfigureMockMvc
public class LocalizationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    public void testFrenchContent() throws Exception {
        mockMvc.perform(post("/api/v1/update/check")
                .header("Accept-Language", "fr-FR")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"countryCode\": \"FR\", \"regionCode\": \"IDF\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Une mise à jour est disponible"));
    }
    
    @Test
    public void testEnglishContent() throws Exception {
        mockMvc.perform(post("/api/v1/update/check")
                .header("Accept-Language", "en-US")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"countryCode\": \"US\", \"regionCode\": \"CA\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("An update is available"));
    }
    
    @Test
    public void testContentFallback() throws Exception {
        // Test fallback: région -> pays -> langue -> anglais
        mockMvc.perform(post("/api/v1/update/check")
                .header("Accept-Language", "fr-BE")  // Belgique
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"countryCode\": \"BE\", \"regionCode\": \"BRU\" }"))
                .andExpect(status().isOk());
    }
}
```

### 10. Gestion des Fuseaux Horaires

```java
@Service
public class TimezoneService {
    
    private static final Map<String, String> COUNTRY_TIMEZONES = Map.of(
        "FR", "Europe/Paris",
        "GB", "Europe/London",
        "US", "America/New_York",  // Default, varies by state
        "ES", "Europe/Madrid",
        "DE", "Europe/Berlin"
    );
    
    public ZoneId getTimezone(String countryCode, String regionCode) {
        // Cas spéciaux pour les US
        if ("US".equals(countryCode)) {
            switch (regionCode) {
                case "CA": return ZoneId.of("America/Los_Angeles");
                case "NY": return ZoneId.of("America/New_York");
                case "TX": return ZoneId.of("America/Chicago");
                default: return ZoneId.of("America/New_York");
            }
        }
        
        return ZoneId.of(COUNTRY_TIMEZONES.getOrDefault(
            countryCode, "UTC"));
    }
    
    public LocalDateTime convertToLocalTime(LocalDateTime utcTime, 
                                           String countryCode, 
                                           String regionCode) {
        ZoneId targetZone = getTimezone(countryCode, regionCode);
        return utcTime.atZone(ZoneId.of("UTC"))
                     .withZoneSameInstant(targetZone)
                     .toLocalDateTime();
    }
}
```

## Support Multi-Langues dans l'UI

```javascript
// i18n.js
class I18n {
    constructor() {
        this.locale = this.detectLocale();
        this.messages = {};
        this.loadMessages();
    }
    
    detectLocale() {
        return navigator.language || 'en-US';
    }
    
    async loadMessages() {
        const response = await fetch(`/i18n/messages_${this.locale}.json`);
        this.messages = await response.json();
    }
    
    t(key, params = {}) {
        let message = this.messages[key] || key;
        Object.keys(params).forEach(param => {
            message = message.replace(`{${param}}`, params[param]);
        });
        return message;
    }
}

const i18n = new I18n();
```

---

🌍 **Note**: L'internationalisation complète nécessite une attention particulière aux formats de dates, nombres, devises et aux spécificités culturelles de chaque région.
