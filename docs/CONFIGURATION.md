# Guide de Configuration - ANGEL Update Service

## Table des Matières

1. [Configuration des Ports et Profils](#configuration-des-ports-et-profils)
2. [Configuration Spring Boot](#configuration-spring-boot)
3. [Configuration Base de Données](#configuration-base-de-donnees)
4. [Configuration Cache](#configuration-cache)
5. [Configuration des Collecteurs](#configuration-des-collecteurs)
6. [Configuration Sécurité](#configuration-securite)
7. [Configuration Performance](#configuration-performance)
8. [Profiles d'Environnement](#profiles-denvironnement)
9. [Profils et Fichiers de Configuration](#profils-et-fichiers-de-configuration)

## 1. Configuration des Ports et Profils

### 🔌 Changement du Port d'Accès

Le port par défaut de l'application est **8080**. Voici comment le modifier :

#### **Méthode 1 : Modification des Fichiers de Configuration**

**Fichier principal** (`src/main/resources/application.yml`) :
```yaml
server:
  port: 8080  # Changez ce port selon vos besoins

spring:
  profiles:
    active: dev
  
  application:
    name: angel-update-service
```

**Configuration par profil** :
- **Développement** (`application-dev.yml`) : Port 8080
- **Production** (`application-prod.yml`) : Port 9090  
- **Tests** (`application-test.yml`) : Port aléatoire (0)

#### **Méthode 2 : Via Arguments de Ligne de Commande**

```bash
# Démarrage avec port personnalisé
java -jar angel-update-service.jar --server.port=3000

# Avec Maven
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=3000"
```

#### **Méthode 3 : Variable d'Environnement**

```bash
# Linux/Mac
export SERVER_PORT=3000
java -jar angel-update-service.jar

# Windows
set SERVER_PORT=3000
java -jar angel-update-service.jar
```

### 🎯 Utilisation des Profils Spring Boot

#### **Résumé des Profils Disponibles**

| Profil | Port | Base de Données | Logs | Collecteurs | Usage |
|--------|------|----------------|------|-------------|--------|
| **dev** | 8080 | PostgreSQL + H2 | DEBUG détaillés | Mode mock activé | Développement local |
| **prod** | 9090 | PostgreSQL | WARN/INFO + JSON | APIs réelles | Production |
| **test** | Random | H2 mémoire | Minimal | Désactivés | Tests automatisés |

#### **Activation des Profils**

**1. Par ligne de commande :**
```bash
# Développement (par défaut)
java -jar angel-update-service.jar

# Production 
java -jar angel-update-service.jar --spring.profiles.active=prod

# Test
java -jar angel-update-service.jar --spring.profiles.active=test
```

**2. Via Maven :**
```bash
# Développement
mvn spring-boot:run

# Production
mvn spring-boot:run -Dspring-boot.run.profiles=prod

# Test  
mvn test
```

**3. Variable d'environnement :**
```bash
# Linux/Mac
export SPRING_PROFILES_ACTIVE=prod
java -jar angel-update-service.jar

# Windows
set SPRING_PROFILES_ACTIVE=prod
java -jar angel-update-service.jar
```

**4. Dans IDE (IntelliJ/Eclipse) :**
- **VM Options:** `-Dspring.profiles.active=prod`
- **Program arguments:** `--spring.profiles.active=prod`
- **Environment variables:** `SPRING_PROFILES_ACTIVE=prod`

#### **Configuration Spécifique par Profil**

**Profile DEV** (`application-dev.yml`) :
```yaml
server:
  port: 8080  # Port pour développement

spring:
  profiles:
    active: dev
    
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        
  devtools:
    restart:
      enabled: true
    livereload:
      enabled: true
      
logging:
  level:
    com.angel: DEBUG
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql: TRACE
    
angel:
  collectors:
    mock-mode: true  # Données mockées pour le développement
  cache:
    ttl:
      news: 300      # 5 minutes
      weather: 180   # 3 minutes
```

**Profile PROD** (`application-prod.yml`) :
```yaml
server:
  port: 9090  # Port différent pour production

spring:
  profiles:
    active: prod
    
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    
  devtools:
    restart:
      enabled: false
    livereload:
      enabled: false
      
logging:
  level:
    root: WARN
    com.angel: INFO
  pattern:
    console: '{"timestamp":"%d","level":"%level","logger":"%logger","message":"%msg"}%n'
    
angel:
  collectors:
    mock-mode: false  # APIs réelles en production
  performance:
    thread-pool:
      core-size: 20
      max-size: 100
  cache:
    redis:
      cluster:
        enabled: true
```

**Profile TEST** (`application-test.yml`) :
```yaml
server:
  port: 0  # Port aléatoire pour éviter les conflits

spring:
  profiles:
    active: test
    
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password:
    
  jpa:
    hibernate:
      ddl-auto: create-drop
    database-platform: org.hibernate.dialect.H2Dialect
    
  redis:
    host: localhost
    port: 6379
    
angel:
  collectors:
    enabled: false  # Collecteurs désactivés pour les tests
  cache:
    redis:
      enabled: false  # Utilise uniquement le cache mémoire
```

#### **URLs d'Accès par Profil**

- **Développement** : http://localhost:8080
- **Production** : http://localhost:9090
- **Interface Admin** : http://localhost:{PORT}/
- **API Health** : http://localhost:{PORT}/actuator/health
- **Swagger UI** : http://localhost:{PORT}/swagger-ui.html

### 🚢 Port-Forwarding avec Kubernetes/Minikube

#### **Configuration Port-Forward pour Minikube**

Le service est déployé dans Kubernetes avec la configuration suivante :
- **Namespace** : `angel-update-dev`
- **Service** : `angel-update-service`
- **Port container** : `8080`
- **Port service** : `8080`

#### **Commande Port-Forward**

**Format général :**
```bash
kubectl port-forward -n <namespace> service/<service-name> <port-local>:<port-container>
```

**Pour forwarder sur le port 8181 :**
```bash
kubectl port-forward -n angel-update-dev service/angel-update-service 8181:8080
```

- `8181` = Port sur votre machine locale (Minikube host)
- `8080` = Port du container/service dans Kubernetes

#### **Méthodes d'exécution**

**1. Via le script automatique :**
```bash
./scripts/port-forward.sh
```

**2. Commande directe (foreground) :**
```bash
kubectl port-forward -n angel-update-dev service/angel-update-service 8181:8080
```

**3. En arrière-plan :**
```bash
kubectl port-forward -n angel-update-dev service/angel-update-service 8181:8080 &
```

**4. Port-forwarding multiple :**
```bash
# Application + Base de données + Redis
kubectl port-forward -n angel-update-dev service/angel-update-service 8181:8080 &
kubectl port-forward -n angel-update-dev service/postgres-service 5432:5432 &
kubectl port-forward -n angel-update-dev service/redis-service 6379:6379 &
```

#### **Vérifications avant Port-Forward**

```bash
# Vérifier que le namespace existe
kubectl get ns angel-update-dev

# Vérifier que les services existent
kubectl get svc -n angel-update-dev

# Vérifier que les pods sont running
kubectl get pods -n angel-update-dev

# Voir les logs si problème
kubectl logs -n angel-update-dev deployment/angel-update-service

# Vérifier la configuration du service
kubectl describe svc -n angel-update-dev angel-update-service
```

#### **URLs d'accès après Port-Forward**

- **Application** : http://localhost:8181
- **Interface Admin** : http://localhost:8181/
- **API Health** : http://localhost:8181/actuator/health
- **Swagger UI** : http://localhost:8181/swagger-ui.html
- **PostgreSQL** : localhost:5432
- **Redis** : localhost:6379

#### **Troubleshooting Port-Forward**

**Problème courant 1 - Service non trouvé :**
```bash
# Vérifier le nom exact du service
kubectl get svc -n angel-update-dev -o wide

# Vérifier les labels
kubectl get svc -n angel-update-dev --show-labels
```

**Problème courant 2 - Port déjà utilisé :**
```bash
# Vérifier quel processus utilise le port
netstat -tulpn | grep :8181
# ou sur Windows
netstat -an | findstr :8181

# Utiliser un autre port local
kubectl port-forward -n angel-update-dev service/angel-update-service 8182:8080
```

**Problème courant 3 - Pods pas prêts :**
```bash
# Attendre que les pods soient prêts
kubectl wait --for=condition=ready pod -l app=angel-update-service -n angel-update-dev --timeout=300s
```

#### **Script Port-Forward Automatique**

Le script `scripts/port-forward.sh` est configuré pour forwarder automatiquement :

```bash
#!/bin/bash
echo "🔌 Setting up port forwarding..."
echo "Application: http://localhost:8181"
echo "PostgreSQL: localhost:5432" 
echo "Redis: localhost:6379"
echo "Press Ctrl+C to stop"

kubectl port-forward -n angel-update-dev service/angel-update-service 8181:8080 &
kubectl port-forward -n angel-update-dev service/postgres-service 5432:5432 &
kubectl port-forward -n angel-update-dev service/redis-service 6379:6379 &

wait
```

**Utilisation :**
```bash
# Rendre exécutable (Linux/Mac)
chmod +x scripts/port-forward.sh

# Exécuter
./scripts/port-forward.sh

# Arrêter avec Ctrl+C
```

### 📝 Bonnes Pratiques

1. **Toujours spécifier le profil** en production : `--spring.profiles.active=prod`
2. **Utiliser des ports différents** par environnement pour éviter les conflits
3. **Variables d'environnement** pour les secrets : `DB_PASSWORD`, `API_KEYS`
4. **Logging approprié** : DEBUG en dev, INFO/WARN en prod
5. **Cache adapté** : TTL courts en dev, optimisés en prod
6. **Port-forwarding** : Vérifier les services avant de forwarder les ports
7. **Kubernetes** : Toujours spécifier le namespace correct

## 2. Configuration Spring Boot

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

## 3. Configuration Base de Données

### PostgreSQL Principal

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:angeldb}
    username: ${DB_USER:angel}
    password: ${DB_PASSWORD:changeme123}
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

## 4. Configuration Cache

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

## 5. Configuration des Collecteurs

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

## 6. Configuration Sécurité

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

## 7. Configuration Performance

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

## 8. Profiles d'Environnement

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
DB_PASSWORD=changeme123

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

## Architecture de Cache Multi-Niveaux (Redis + Caffeine)

### 🔄 Vue d'ensemble de l'Architecture de Cache

ANGEL Update Service utilise une **architecture de cache multi-niveaux** pour optimiser les performances et réduire la charge sur la base de données.

```
Client → Application → L1 (Caffeine) → L2 (Redis) → Base de données PostgreSQL
```

### 📚 Composants du Cache

#### 🚀 **L1 - Caffeine (Cache de Premier Niveau)**

**Caractéristiques :**
- **Type** : Cache **in-memory** (en mémoire JVM)
- **Rapidité** : **Ultra-rapide** (~1-5 nanosecondes)
- **Portée** : **Local** à l'instance de l'application
- **Capacité** : **Limitée** par la RAM de l'application
- **Persistance** : **Non persistant** (perdu au redémarrage)

**Configuration dans `application.yml` :**
```yaml
angel:
  cache:
    caffeine:
      enabled: true
      spec:
        news: maximumSize=100,expireAfterWrite=5m
        weather: maximumSize=50,expireAfterWrite=3m
        recipes: maximumSize=200,expireAfterWrite=30m
        stories: maximumSize=500,expireAfterWrite=1h
        updateResponses: maximumSize=1000,expireAfterWrite=30m
```

#### 🌐 **L2 - Redis (Cache de Deuxième Niveau)**

**Caractéristiques :**
- **Type** : Cache **distribué** (serveur externe)
- **Rapidité** : **Rapide** (~1-5 millisecondes)
- **Portée** : **Partagé** entre toutes les instances
- **Capacité** : **Grande** (serveur dédié avec plusieurs GB)
- **Persistance** : **Optionnellement persistant**

**Configuration dans `application.yml` :**
```yaml
angel:
  cache:
    redis:
      enabled: true
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      timeout: 2000ms
      
      # TTL par type de contenu (secondes)
      ttl:
        news: 900          # 15 minutes
        weather: 600       # 10 minutes  
        recipes: 7200      # 2 heures
        stories: 21600     # 6 heures
        updateResponses: 3600  # 1 heure
        
      # Configuration du pool de connexions
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5
          max-wait: 2000ms
```

### ⚡ Stratégies de Cache

#### **1. Stratégie de Lecture (Cache-Aside Pattern)**

```java
// Implémentation dans CacheService.java
@Cacheable(value = "updateResponses", key = "#cacheKey")
public UpdateResponse getUpdateResponse(String cacheKey) {
    // 1. Vérification automatique L1 (Caffeine) via @Cacheable
    // 2. Si L1 Miss → Vérification L2 (Redis) 
    return getFromRedis(cacheKey, UpdateResponse.class);
    // 3. Si L2 Miss → Requête base de données (implicite)
}
```

**Flux de lecture :**
1. **L1 Hit** : Données en Caffeine → **Réponse en ~2ns**
2. **L1 Miss, L2 Hit** : Données en Redis → **Réponse en ~3ms** + mise en cache L1
3. **L1+L2 Miss** : Requête DB → **Réponse en ~50ms** + mise en cache L1+L2

#### **2. Stratégie d'Écriture (Write-Through Pattern)**

```java
@CachePut(value = "updateResponses", key = "#cacheKey")
public UpdateResponse putUpdateResponse(String cacheKey, UpdateResponse response) {
    // 1. Écriture automatique L1 (Caffeine) via @CachePut
    // 2. Écriture explicite L2 (Redis)
    putInRedis(cacheKey, response, DEFAULT_UPDATE_RESPONSE_TTL);
    return response;
}
```

### 📊 TTL (Time-To-Live) par Type de Contenu

| Type de Contenu | TTL L1 (Caffeine) | TTL L2 (Redis) | Justification |
|------------------|-------------------|----------------|---------------|
| **Actualités** | 5 minutes | 15 minutes | Fraîcheur critique |
| **Météo** | 3 minutes | 10 minutes | Données temps réel |
| **Recettes** | 30 minutes | 2 heures | Contenu semi-statique |
| **Découvertes** | 1 heure | 6 heures | Contenu quasi-statique |
| **Réponses Update** | 30 minutes | 1 heure | Optimisation API |
| **Contenu général** | 30 minutes | 30 minutes | Défaut |

### 🗂️ Structure des Clés de Cache

#### **Clés Géographiques**
```
news:{countryCode}:{regionCode}
weather:{countryCode}:{regionCode}
content:{type}:{countryCode}:{regionCode}
updateResponse:{countryCode}:{regionCode}:{version}
```

**Exemples :**
```
news:FR:IDF                    # Actualités France Île-de-France
news:US:national               # Actualités États-Unis nationales
weather:GB:LON                 # Météo Grande-Bretagne Londres
content:recipes:IT:national    # Recettes Italie nationales
updateResponse:DE:BY:1.2.0     # Réponse update Allemagne Bavière v1.2.0
```

### 🔧 Configuration par Environnement

#### **Développement (`application-dev.yml`)**
```yaml
angel:
  cache:
    caffeine:
      spec:
        news: maximumSize=50,expireAfterWrite=5m
        weather: maximumSize=25,expireAfterWrite=3m
    redis:
      enabled: true
      ttl:
        news: 300      # 5 minutes (TTL courts pour dev)
        weather: 180   # 3 minutes
```

#### **Production (`application-prod.yml`)**
```yaml
angel:
  cache:
    caffeine:
      spec:
        news: maximumSize=500,expireAfterWrite=15m
        weather: maximumSize=200,expireAfterWrite=10m
    redis:
      enabled: true
      cluster:
        enabled: true
        nodes:
          - redis-cluster-1:6379
          - redis-cluster-2:6379
          - redis-cluster-3:6379
      ttl:
        news: 900      # 15 minutes (TTL optimisés pour prod)
        weather: 600   # 10 minutes
```

#### **Tests (`application-test.yml`)**
```yaml
angel:
  cache:
    redis:
      enabled: false  # Utilise uniquement Caffeine en test
    caffeine:
      spec:
        news: maximumSize=10,expireAfterWrite=1m
        weather: maximumSize=5,expireAfterWrite=30s
```

### 🛠️ Opérations de Maintenance du Cache

#### **Éviction Sélective**
```java
// Vider le cache par pattern géographique
cacheService.evictCache("news:FR:*");     // Toutes les news de France
cacheService.evictCache("weather:US:*");  // Toute la météo des États-Unis

// Vider par type de contenu
cacheService.evictCache("content:recipes:*");  // Toutes les recettes
```

#### **Éviction Totale (Emergency)**
```java
// Vide tous les caches L1 + L2
cacheService.evictAllCache();
```

#### **Monitoring du Cache**
```java
// Statistiques de performance
CacheStats stats = cacheService.getCacheStats();
boolean redisHealthy = stats.isRedisConnected();
```

### 📈 Métriques de Performance

#### **Gains de Performance Attendus**

| Scénario | Sans Cache | Avec L1+L2 | Gain |
|----------|-----------|------------|------|
| **Actualités populaires** | 50ms | 2ns | **99.996%** |
| **Météo fréquente** | 80ms | 3ms | **96.25%** |
| **Updates répétées** | 120ms | 2ns | **99.998%** |
| **Premier accès** | 50ms | 50ms + cache | **0% (mais cache)** |

#### **Réduction de Charge DB**

- **Taux de hit L1** attendu : **85-90%**
- **Taux de hit L2** attendu : **95-98%**
- **Réduction requêtes DB** : **~95%**

### 🔧 Configuration Avancée

#### **Tuning Caffeine**
```yaml
angel:
  cache:
    caffeine:
      spec:
        # Taille maximale + éviction par écriture
        news: maximumSize=1000,expireAfterWrite=15m
        
        # Taille + éviction par accès + écriture  
        weather: maximumSize=500,expireAfterAccess=5m,expireAfterWrite=10m
        
        # Éviction par poids (pour gros objets)
        stories: maximumWeight=10MB,expireAfterWrite=2h
```

#### **Configuration Redis Cluster**
```yaml
spring:
  redis:
    cluster:
      nodes:
        - redis-node1.internal:6379
        - redis-node2.internal:6379  
        - redis-node3.internal:6379
      max-redirects: 3
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5
        max-wait: 2000ms
      cluster:
        refresh:
          adaptive: true
          period: 30s
```

### 🚨 Troubleshooting Cache

#### **Problèmes Courants**

**1. Cache L1 non fonctionnel :**
```bash
# Vérifier les annotations Spring Cache
# Vérifier @EnableCaching dans la configuration
# Logs : "Cache 'updateResponses' not found"
```

**2. Redis indisponible :**
```bash
# Vérifier la connectivité
redis-cli -h localhost -p 6379 ping

# Logs d'application
# WARN: Redis connection check failed
```

**3. TTL trop courts/longs :**
```bash
# Monitorer les hit rates
# Ajuster les TTL selon les métriques business
```

#### **Commandes de Debug**

```bash
# Vérifier les clés Redis
redis-cli keys "news:*"
redis-cli ttl "news:FR:IDF"

# Monitorer Redis
redis-cli monitor

# Statistiques Redis
redis-cli info stats
```

### 💡 Bonnes Pratiques

1. **TTL adaptatifs** : Courts pour données critiques, longs pour contenu statique
2. **Clés structurées** : Utiliser des patterns cohérents pour l'éviction
3. **Monitoring** : Surveiller hit rates et performance
4. **Fallback graceful** : Application fonctionnelle même si cache indisponible  
5. **Éviction intelligente** : Par géographie et type de contenu
6. **Tests** : Valider le comportement avec/sans cache
7. **Sécurité** : Chiffrer les données sensibles même en cache

## 9. Profils et Fichiers de Configuration

### 📁 Structure et Rôle des Fichiers de Configuration

Spring Boot utilise un système de profils pour gérer différentes configurations selon l'environnement d'exécution.

#### **Hiérarchie des Fichiers**

```
src/main/resources/
├── application.yml           # Configuration commune (toujours chargée)
├── application-dev.yml       # Profil développement
├── application-test.yml      # Profil tests automatiques
└── application-prod.yml      # Profil production
```

### 🔄 Comment Spring Sélectionne le Profil

#### **1. Fichier Principal (`application.yml`)**

```yaml
spring:
  profiles:
    active: dev  # Profil actif par défaut
  application:
    name: angel-update-service

# Configuration commune à tous les environnements
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

**Caractéristiques :**
- **Toujours chargé en premier**
- Contient la configuration **commune** à tous les environnements
- Définit le profil par défaut (`dev`)
- Les autres profils **surchargent** ces valeurs

#### **2. Profil Développement (`application-dev.yml`)**

**Utilisation :** Développement local avec PostgreSQL/Redis

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/angeldb
    username: angel
    password: changeme123
  
  jpa:
    hibernate:
      ddl-auto: update  # Auto-création/modification des tables
    show-sql: true      # Affiche les requêtes SQL
  
  flyway:
    enabled: true
    baseline-on-migrate: true

logging:
  level:
    com.angel: DEBUG
    org.hibernate.SQL: DEBUG

angel:
  collectors:
    enabled: false     # Désactivé pour éviter les appels API
    mock-mode: true    # Utilise des données mockées
  cache:
    redis:
      enabled: false   # Simplifie le développement
```

**Activation :**
```bash
mvn spring-boot:run  # Par défaut (défini dans application.yml)
```

#### **3. Profil Test (`application-test.yml`)**

**Utilisation :** Tests unitaires et d'intégration automatiques

```yaml
server:
  port: 0  # Port aléatoire (évite les conflits)

spring:
  datasource:
    url: jdbc:h2:mem:testdb  # Base H2 en mémoire
    driver-class-name: org.h2.Driver
  
  jpa:
    hibernate:
      ddl-auto: create-drop  # Recrée le schéma à chaque test
  
  flyway:
    enabled: false  # H2 gère le schéma

angel:
  collectors:
    enabled: false    # Pas d'appels externes
  cache:
    redis:
      enabled: false  # Pas de dépendance Redis
```

**Activation automatique :**
```bash
mvn test        # Activé automatiquement par Spring Boot Test
mvn verify      # Pour les tests d'intégration
```

**Dans les tests Java :**
```java
@SpringBootTest
@ActiveProfiles("test")  // Souvent implicite
class UpdateServiceTest {
    // Utilise automatiquement application-test.yml
}
```

#### **4. Profil Production (`application-prod.yml`)**

**Utilisation :** Environnement de production

```yaml
server:
  port: 9090  # Port différent pour éviter les conflits

spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://postgres-service:5432/angeldb}
    username: ${DB_USERNAME:angel}
    password: ${DB_PASSWORD}  # Obligatoire depuis variable env
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
  
  jpa:
    hibernate:
      ddl-auto: validate  # Pas de modification du schéma
    show-sql: false
  
  flyway:
    enabled: true

logging:
  level:
    root: WARN
    com.angel: INFO
  pattern:
    console: '{"timestamp":"%d","level":"%level","message":"%msg"}%n'

angel:
  collectors:
    enabled: true
    mock-mode: false  # APIs réelles
  cache:
    redis:
      enabled: true
      cluster:
        enabled: true
```

**Activation :**
```bash
java -jar app.jar --spring.profiles.active=prod
# ou
export SPRING_PROFILES_ACTIVE=prod
java -jar app.jar
```

### 📊 Ordre de Chargement et Priorité

#### **Mécanisme de Surcharge**

1. **`application.yml`** est chargé en premier (base)
2. **`application-{profile}.yml`** est chargé ensuite
3. Les propriétés du profil **surchargent** celles de base
4. Les variables d'environnement **surchargent** tout

**Exemple concret :**

```yaml
# application.yml
server:
  port: 8080
  compression:
    enabled: true

management:
  endpoints:
    web:
      exposure:
        include: health,info

# application-prod.yml
server:
  port: 9090  # Surcharge le port

# management.endpoints reste inchangé (hérité)
```

**Résultat en production :**
- Port : 9090 (surchargé)
- Compression : true (hérité)
- Endpoints : health,info (hérité)

### 🚀 Méthodes d'Activation des Profils

#### **1. Variable d'Environnement (Recommandé pour conteneurs)**
```bash
export SPRING_PROFILES_ACTIVE=prod
java -jar angel-update-service.jar

# Docker
docker run -e SPRING_PROFILES_ACTIVE=prod image:tag

# Kubernetes
env:
  - name: SPRING_PROFILES_ACTIVE
    value: "prod"
```

#### **2. Argument JVM**
```bash
java -jar angel-update-service.jar --spring.profiles.active=prod
# ou
java -Dspring.profiles.active=prod -jar angel-update-service.jar
```

#### **3. Maven**
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

#### **4. Configuration IDE**
- **IntelliJ IDEA** : Run Configuration → Environment variables → `SPRING_PROFILES_ACTIVE=prod`
- **Eclipse** : Run Configuration → Arguments → Program arguments : `--spring.profiles.active=prod`
- **VS Code** : launch.json → `"env": {"SPRING_PROFILES_ACTIVE": "prod"}`

### 🎯 Pourquoi le Profil Test ?

Le profil **test** est essentiel pour garantir des tests **isolés**, **rapides** et **reproductibles** :

#### **Avantages du Profil Test**

| Aspect | Dev/Prod | Test | Avantage |
|--------|----------|------|----------|
| **Base de données** | PostgreSQL externe | H2 en mémoire | Pas d'installation requise |
| **Port** | 8080/9090 fixes | Aléatoire (0) | Tests parallèles possibles |
| **Cache Redis** | Serveur externe | Désactivé | Tests sans infrastructure |
| **Collecteurs API** | Appels réels | Désactivés | Tests prédictibles et rapides |
| **Données** | Persistantes | Volatiles | Isolation totale entre tests |
| **Migrations** | Flyway | DDL auto | Pas de gestion de versions |

#### **Exemple d'Utilisation**

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class UpdateControllerTest {
    
    @Test
    void testCheckUpdate() {
        // Ce test utilise automatiquement :
        // - H2 en mémoire (pas PostgreSQL)
        // - Port aléatoire (pas 8080)
        // - Pas de Redis
        // - Pas d'appels API externes
        // - Données isolées de ce test uniquement
    }
}
```

### 📋 Résumé des Profils

| Profil | Fichier | Usage | Base de données | Cache | Collecteurs |
|--------|---------|-------|-----------------|-------|-------------|
| **dev** | `application-dev.yml` | Développement local | PostgreSQL local | Caffeine seul | Mode mock |
| **test** | `application-test.yml` | Tests automatiques | H2 mémoire | Caffeine seul | Désactivés |
| **prod** | `application-prod.yml` | Production | PostgreSQL cluster | Caffeine + Redis | APIs réelles |

### 🔧 Commandes Pratiques

```bash
# Développement (profil par défaut)
mvn spring-boot:run

# Tests (automatique)
mvn test

# Production locale (pour tests)
mvn spring-boot:run -Dspring-boot.run.profiles=prod

# Vérifier le profil actif
curl http://localhost:8080/actuator/env | grep "activeProfiles"

# Démarrage avec plusieurs profils
java -jar app.jar --spring.profiles.active=prod,monitoring
```

### ✅ Bonnes Pratiques

1. **Ne jamais modifier `application.yml` pour un environnement spécifique** - Utilisez les profils
2. **Utiliser des variables d'environnement pour les secrets** - Jamais en dur dans les YAML
3. **Tester avec le bon profil** - `mvn test` utilise automatiquement le profil test
4. **Un seul JAR pour tous les environnements** - Le profil change, pas le binaire
5. **Documenter les variables d'environnement requises** par profil
6. **Valider la configuration au démarrage** avec des health checks
7. **Logger le profil actif** au démarrage pour traçabilité

### 🚨 Dépannage

**Profil non chargé :**
```bash
# Vérifier dans les logs au démarrage
"The following profiles are active: dev"

# Si aucun profil, vérifier application.yml
spring.profiles.active: dev
```

**Mauvaise configuration chargée :**
```bash
# Vérifier l'ordre de priorité :
# 1. Variables d'environnement
# 2. Arguments ligne de commande  
# 3. application-{profile}.yml
# 4. application.yml
```

**Tests échouent localement :**
```bash
# S'assurer que le profil test est utilisé
mvn test -Dspring.profiles.active=test

# Vérifier que H2 est dans les dépendances
# scope: test dans pom.xml
```

---

📝 **Note**: Toujours valider la configuration avant le déploiement avec `mvn spring-boot:run -Dspring.profiles.active=validate`
