# Guide de Configuration - ANGEL Update Service

## Table des Matières

1. [Configuration Spring Boot](#configuration-spring-boot)
2. [Configuration Base de Données](#configuration-base-de-données)
3. [Configuration Cache](#configuration-cache)
4. [Configuration des Collecteurs](#configuration-des-collecteurs)
5. [Configuration Sécurité](#configuration-sécurité)
6. [Configuration Performance](#configuration-performance)
7. [Profiles d'Environnement](#profiles-denvironnement)

## Configuration Spring Boot

### Fichier Principal: `application.yml`

```yaml
spring:
  application:
    name: angel-update-service
  
  # Configuration serveur
  server:
    port: 8080
    compression:
      enabled: true
      mime-types: application/json,application/xml,text/html,text/xml,text/plain
    
  # Configuration JPA/Hibernate
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        jdbc:
          batch_size: 20
        order_inserts: true
        order_updates: true
```

## Configuration Base de Données

### PostgreSQL Principal

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:angeldb}
    username: ${DB_USER:angel}
    password: ${DB_PASSWORD:changeme}
    driver-class-name: org.postgresql.Driver
    
    # Pool de connexions HikariCP
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      connection-timeout: 20000
      leak-detection-threshold: 60000
```

### Flyway Migration

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    validate-on-migrate: true
```

## Configuration Cache

### Cache Multi-Niveaux

```yaml
angel:
  cache:
    # Cache L1 - Caffeine (In-Memory)
    caffeine:
      enabled: true
      spec:
        news: maximumSize=100,expireAfterWrite=30m
        weather: maximumSize=50,expireAfterWrite=15m
        recipes: maximumSize=200,expireAfterWrite=1h
        stories: maximumSize=500,expireAfterWrite=24h
    
    # Cache L2 - Redis
    redis:
      enabled: true
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      timeout: 2000ms
      
      # TTL par type de contenu (secondes)
      ttl:
        news: 3600          # 1 heure
        weather: 1800       # 30 minutes
        recipes: 7200       # 2 heures
        stories: 86400      # 24 heures
        
      # Configuration du pool
      lettuce:
        pool:
          max-active: 10
          max-idle: 5
          min-idle: 2
```

## Configuration des Collecteurs

### APIs Externes

```yaml
angel:
  collectors:
    enabled: true
    mock-mode: false  # Active le mode mock pour tests
    
    # Configuration par type de collecteur
    news:
      enabled: true
      schedule: "0 */30 * * * *"  # Cron expression
      sources:
        fr:
          national:
            - name: "FranceInfo"
              url: "https://api.franceinfo.fr/v1/news"
              api-key: ${NEWS_FR_API_KEY}
              rate-limit: 100  # requêtes par heure
            - name: "AFP"
              url: "https://api.afp.com/v1/news"
              api-key: ${AFP_API_KEY}
          regional:
            idf:
              - name: "ParisInfo"
                url: "https://api.paris.fr/news"
        en:
          national:
            - name: "Reuters"
              url: "https://api.reuters.com/v1/news"
              api-key: ${REUTERS_API_KEY}
    
    weather:
      enabled: true
      schedule: "0 */15 * * * *"
      providers:
        - name: "OpenWeatherMap"
          url: "https://api.openweathermap.org/data/2.5"
          api-key: ${OPENWEATHER_API_KEY}
          priority: 1
        - name: "MeteoFrance"
          url: "https://api.meteofrance.fr/v1"
          api-key: ${METEOFRANCE_API_KEY}
          priority: 2
          regions: ["FR"]  # Uniquement pour la France
    
    recipes:
      enabled: true
      schedule: "0 0 6 * * *"  # 6h du matin
      daily-limit: 10  # Nombre de recettes quotidiennes
      sources:
        - name: "Spoonacular"
          url: "https://api.spoonacular.com/recipes"
          api-key: ${SPOONACULAR_API_KEY}
        - name: "Edamam"
          url: "https://api.edamam.com/api/recipes/v2"
          app-id: ${EDAMAM_APP_ID}
          app-key: ${EDAMAM_APP_KEY}
    
    discoveries:
      enabled: true
      schedule: "0 0 */6 * * *"  # Toutes les 6 heures
      sources:
        scientific:
          - name: "PubMed"
            url: "https://eutils.ncbi.nlm.nih.gov/entrez/eutils"
            api-key: ${PUBMED_API_KEY}
          - name: "ArXiv"
            url: "https://export.arxiv.org/api/query"
        medical:
          - name: "ClinicalTrials"
            url: "https://clinicaltrials.gov/api/v2"
```

### Scheduling Configuration

```yaml
spring:
  task:
    scheduling:
      pool:
        size: 10
      thread-name-prefix: "collector-"
    execution:
      pool:
        core-size: 5
        max-size: 20
        queue-capacity: 100
      thread-name-prefix: "async-"
```

## Configuration Sécurité

### API Security

```yaml
angel:
  security:
    # API Key Authentication
    api-key:
      enabled: true
      header-name: "X-API-Key"
      query-param: "apiKey"  # Fallback
    
    # Rate Limiting
    rate-limit:
      enabled: true
      default:
        requests-per-second: 10
        burst-capacity: 20
      
      # Limites par type de client
      by-tier:
        basic:
          requests-per-second: 5
          burst-capacity: 10
        premium:
          requests-per-second: 50
          burst-capacity: 100
        unlimited:
          requests-per-second: -1  # Pas de limite
    
    # CORS Configuration
    cors:
      enabled: true
      allowed-origins:
        - "http://localhost:*"
        - "https://*.angel-assistant.com"
      allowed-methods:
        - GET
        - POST
        - PUT
        - DELETE
      allowed-headers:
        - "*"
      exposed-headers:
        - "X-Rate-Limit-Remaining"
        - "X-Rate-Limit-Reset"
```

### Encryption

```yaml
angel:
  security:
    encryption:
      # Chiffrement des données sensibles
      algorithm: "AES/GCM/NoPadding"
      key-size: 256
      
      # Jasypt pour les propriétés
      jasypt:
        password: ${JASYPT_PASSWORD}
        algorithm: "PBEWITHHMACSHA512ANDAES_256"
```

## Configuration Performance

### Thread Pools & Async

```yaml
angel:
  performance:
    # Thread pool principal
    thread-pool:
      core-size: 10
      max-size: 50
      queue-capacity: 500
      keep-alive: 60s
      
    # Circuit Breaker (Resilience4j)
    circuit-breaker:
      instances:
        external-api:
          failure-rate-threshold: 50
          slow-call-rate-threshold: 50
          slow-call-duration-threshold: 2s
          sliding-window-type: COUNT_BASED
          sliding-window-size: 100
          minimum-number-of-calls: 10
          wait-duration-in-open-state: 60s
    
    # Retry Configuration
    retry:
      instances:
        external-api:
          max-attempts: 3
          wait-duration: 1s
          retry-exceptions:
            - java.io.IOException
            - java.net.SocketTimeoutException
```

### Compression & Optimization

```yaml
angel:
  optimization:
    # Compression ZIP
    zip:
      compression-level: 6  # 0-9, défaut 6
      buffer-size: 8192
      
    # Taille des fichiers
    file:
      max-zip-size: 52428800  # 50MB
      max-content-size: 10485760  # 10MB par fichier
      
    # Batch Processing
    batch:
      size: 100
      timeout: 30s
```

## Profiles d'Environnement

### Profile Development (`application-dev.yml`)

```yaml
spring:
  profiles:
    active: dev
  
  # Hot reload
  devtools:
    restart:
      enabled: true
    livereload:
      enabled: true
  
  # Logging détaillé
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true

logging:
  level:
    com.angel: DEBUG
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG

angel:
  collectors:
    mock-mode: true  # Utilise des données mockées
  
  cache:
    ttl:
      news: 300  # 5 minutes en dev
      weather: 180  # 3 minutes en dev
```

### Profile Production (`application-prod.yml`)

```yaml
spring:
  profiles:
    active: prod
  
  jpa:
    show-sql: false
    hibernate:
      ddl-auto: validate

logging:
  level:
    root: WARN
    com.angel: INFO
  
  # Logs structurés JSON
  pattern:
    console: '{"timestamp":"%d","level":"%level","logger":"%logger","message":"%msg"}%n'

angel:
  performance:
    thread-pool:
      core-size: 20
      max-size: 100
  
  cache:
    redis:
      cluster:
        nodes:
          - redis-node1:6379
          - redis-node2:6379
          - redis-node3:6379
```

### Profile Test (`application-test.yml`)

```yaml
spring:
  profiles:
    active: test
  
  # Base H2 en mémoire
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  
  jpa:
    hibernate:
      ddl-auto: create-drop

angel:
  collectors:
    enabled: false  # Désactive les collecteurs
  
  cache:
    redis:
      enabled: false  # Utilise uniquement Caffeine
```

## Variables d'Environnement

### Liste des Variables Requises

```bash
# Base de données
DB_HOST=localhost
DB_PORT=5432
DB_NAME=angeldb
DB_USER=angel
DB_PASSWORD=your-secure-password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=your-redis-password

# APIs Externes
OPENWEATHER_API_KEY=your-api-key
REUTERS_API_KEY=your-api-key
SPOONACULAR_API_KEY=your-api-key
PUBMED_API_KEY=your-api-key

# Sécurité
JASYPT_PASSWORD=your-encryption-password
JWT_SECRET=your-jwt-secret

# Monitoring
PROMETHEUS_ENABLED=true
GRAFANA_ENABLED=true
```

### Fichier `.env` Example

```bash
# Development Environment
ENVIRONMENT=dev
DEBUG_MODE=true

# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=angeldb_dev
DB_USER=angel_dev
DB_PASSWORD=dev_password_123

# Cache
REDIS_HOST=localhost
REDIS_PORT=6379

# External APIs (Dev Keys)
OPENWEATHER_API_KEY=dev_key_weather
REUTERS_API_KEY=dev_key_reuters
```

## Validation de Configuration

### Health Checks

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true
  
  health:
    redis:
      enabled: true
    db:
      enabled: true
```

### Configuration Validator

```java
@Component
@ConfigurationProperties(prefix = "angel")
@Validated
public class AngelConfiguration {
    
    @NotNull
    @Valid
    private CacheConfig cache;
    
    @NotNull
    @Valid
    private CollectorsConfig collectors;
    
    // Validation au démarrage
    @PostConstruct
    public void validate() {
        // Vérification des API keys
        // Vérification des connexions
        // Vérification des permissions fichiers
    }
}
```

## Tips de Configuration

1. **Utilisez des secrets managers** : HashiCorp Vault, AWS Secrets Manager
2. **Externalisez la configuration** : Spring Cloud Config Server
3. **Monitoring de configuration** : Actuator endpoints
4. **Rotation des secrets** : Automatisez avec Kubernetes Secrets
5. **Configuration as Code** : Versionnez toutes les configurations

---

📝 **Note**: Toujours valider la configuration avant le déploiement avec `mvn spring-boot:run -Dspring.profiles.active=validate`
