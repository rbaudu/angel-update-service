# ANGEL Update Service

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://www.oracle.com/java/)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-Ready-blue.svg)](https://kubernetes.io/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## 🎯 Description

ANGEL Update Service est un microservice Spring Boot hautement scalable conçu pour gérer les mises à jour de contenu et de configuration pour le Virtual Assistant Angel. Il fournit des mises à jour différentielles sous forme de packages ZIP, avec support de multiples régions et langues.

### Caractéristiques Principales

- 🚀 **Haute Performance** : Cache multi-niveaux (Caffeine L1 + Redis L2)
- 🌍 **Multi-régional** : Support de multiples pays et régions
- 🔄 **Mises à jour automatiques** : Collecte de données depuis APIs externes
- 📦 **Packages optimisés** : ZIP différentiels pour minimiser la bande passante
- 🎮 **Scalabilité horizontale** : Architecture cloud-native Kubernetes
- 🛡️ **Résilient** : Circuit breakers, retry policies, fallbacks
- 📊 **Observable** : Métriques Prometheus, logs structurés, tracing distribué

## 📚 Documentation

| Document | Description |
|----------|-------------|
| [CONFIGURATION.md](docs/CONFIGURATION.md) | Guide complet de configuration du service |
| [WEB_INTERFACE.md](docs/WEB_INTERFACE.md) | Interface d'administration web |
| [INTERNATIONALISATION.md](docs/INTERNATIONALISATION.md) | Support multi-langues et régions |
| [DEPLOYMENT.md](docs/DEPLOYMENT.md) | Guide de déploiement (Docker, K8s, Minikube) |

## 🏗️ Architecture

```
┌─────────────────┐     ┌──────────────┐     ┌─────────────┐
│ Virtual         │────▶│ Load         │────▶│ Spring Boot │
│ Assistant       │     │ Balancer     │     │ Instances   │
│ (Angel)         │     └──────────────┘     └─────────────┘
└─────────────────┘                                 │
                                                    ▼
┌─────────────────┐     ┌──────────────┐     ┌─────────────┐
│ External APIs   │────▶│ Collectors   │────▶│ Redis Cache │
│ (News, Weather) │     │ (Scheduled)  │     └─────────────┘
└─────────────────┘     └──────────────┘           │
                                                    ▼
                                            ┌─────────────┐
                                            │ PostgreSQL  │
                                            │ (Metadata)  │
                                            └─────────────┘
```

## 🚀 Quick Start

### Prérequis

- Java 17+
- Maven 3.9+
- Docker & Docker Compose
- Minikube (pour développement local Kubernetes)

### Installation Locale

```bash
# Cloner le repository
git clone https://github.com/rbaudu/angel-update-service.git
cd angel-update-service

# Compiler le projet
mvn clean package

# Lancer avec Docker Compose
docker-compose -f docker/docker-compose.local.yml up
```

### Déploiement Minikube

```bash
# Démarrer Minikube
minikube start --cpus=4 --memory=8192

# Build et déployer
./scripts/build-and-deploy.sh

# Accéder à l'application
./scripts/port-forward.sh
# Application disponible sur http://localhost:8080
```

## 📁 Structure du Projet

```
angel-update-service/
├── src/
│   ├── main/
│   │   ├── java/com/angel/update/
│   │   │   ├── controller/        # REST Controllers
│   │   │   ├── service/          # Business Logic
│   │   │   ├── collector/        # External API Collectors
│   │   │   ├── scheduler/        # Scheduled Tasks
│   │   │   ├── repository/       # Data Access Layer
│   │   │   └── config/           # Configuration Classes
│   │   └── resources/
│   │       ├── application.yml   # Main Configuration
│   │       └── static/           # Web UI Resources
│   └── test/                     # Unit & Integration Tests
├── k8s-dev/                      # Kubernetes Manifests
├── docker/                       # Docker Configuration
├── scripts/                      # Utility Scripts
└── docs/                         # Documentation
```

## 🔧 Configuration

Configuration principale dans `application.yml`:

```yaml
angel:
  update:
    cache:
      ttl:
        news: 3600        # 1 heure
        weather: 1800     # 30 minutes
    collectors:
      enabled: true
      schedule:
        news: "0 */30 * * * *"    # Toutes les 30 min
        weather: "0 */15 * * * *"  # Toutes les 15 min
```

## 📊 APIs et Endpoints

### Endpoints Principaux

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/api/v1/update/check` | Vérifier les mises à jour disponibles |
| GET | `/api/v1/update/download/{version}` | Télécharger un package ZIP |
| GET | `/actuator/health` | Health check |
| GET | `/actuator/metrics` | Métriques Prometheus |

### Exemple de Requête

```bash
curl -X POST http://localhost:8080/api/v1/update/check \
  -H "Content-Type: application/json" \
  -d '{
    "countryCode": "FR",
    "regionCode": "IDF",
    "currentVersion": "1.0.0"
  }'
```

## 🌐 Sources de Données

### APIs Externes Intégrées

- **Météo**: OpenWeatherMap, Météo-France
- **Actualités**: Reuters, AFP, France Info
- **Recettes**: Spoonacular, Edamam
- **Découvertes**: PubMed, ArXiv, Nature

## 📈 Monitoring

- **Métriques**: Prometheus + Grafana
- **Logs**: ELK Stack (Elasticsearch, Logstash, Kibana)
- **Tracing**: Zipkin
- **Alerting**: AlertManager

## 🔒 Sécurité

- Authentication par API Key
- Rate limiting configurable
- HTTPS obligatoire en production
- Validation et sanitization des entrées
- Secrets management avec Kubernetes Secrets

## 🧪 Tests

```bash
# Tests unitaires
mvn test

# Tests d'intégration
mvn verify

# Tests de charge (avec K6)
k6 run scripts/load-test.js
```

## 🤝 Contribution

Les contributions sont les bienvenues ! Merci de :

1. Fork le projet
2. Créer une branche feature (`git checkout -b feature/AmazingFeature`)
3. Commit vos changements (`git commit -m 'Add AmazingFeature'`)
4. Push vers la branche (`git push origin feature/AmazingFeature`)
5. Ouvrir une Pull Request

## 📄 License

Ce projet est sous licence MIT. Voir le fichier [LICENSE](LICENSE) pour plus de détails.

## 👤 Auteur

**rbaudu**
- GitHub: [@rbaudu](https://github.com/rbaudu)

## 🙏 Remerciements

- Spring Boot Team
- Kubernetes Community
- Contributeurs Open Source

---

⭐ N'hésitez pas à mettre une étoile si ce projet vous aide !
