# Collecteurs Angel Update Service

Ce document décrit les différents collecteurs de données disponibles dans le service Angel Update, leurs sources, leur analyse et leurs sorties.

## Vue d'ensemble

Les collecteurs sont des composants automatisés qui récupèrent des données depuis des sources externes pour alimenter l'assistant virtuel Angel. Ils s'exécutent de manière programmée et produisent des fichiers de contenu structurés qui sont ensuite empaquetés dans les mises à jour distribuées aux clients.

## Catégories de Collecteurs

### 📰 Actualités et Médias

#### GoogleNewsCollector

**Description :** Collecteur d'actualités via les flux RSS de Google News

**Source :** `https://news.google.com/rss/search`

**URLs d'accès :**
- **Régional :** `https://news.google.com/rss/search?q={région}&hl={langue}&gl={pays}&ceid={pays}:{langue}`
- **National :** `https://news.google.com/rss/search?q={pays}&hl={langue}&gl={pays}&ceid={pays}:{langue}`
- **International :** `https://news.google.com/rss/search?q=world%20news&hl=en&gl=US&ceid=US:en`

**Exemples d'URLs :**
```
Régional Occitanie : https://news.google.com/rss/search?q=Occitanie&hl=fr&gl=FR&ceid=FR:fr
National France : https://news.google.com/rss/search?q=France&hl=fr&gl=FR&ceid=FR:fr
International : https://news.google.com/rss/search?q=world%20news&hl=en&gl=US&ceid=US:en
```

**Analyse effectuée :**
- Parsing des flux RSS XML
- Extraction des titres d'articles
- Nettoyage des titres : suppression de la source médiatique (partie après le dernier `-`)
- Amélioration de la lisibilité : normalisation des espaces, ajout de ponctuation finale
- Filtrage du contenu pertinent

**Production :**
- **Fichiers générés :**
  - `data/news/regional/{pays}/{région}/{date}.txt` - Actualités régionales
  - `data/news/national/{pays}/{date}.txt` - Actualités nationales  
  - `data/news/international/{date}.txt` - Actualités internationales

- **Format de sortie :**
```
# Actualités du 20/08/2025

1. Orages, pluies diluviennes, grêle... C'est le déluge en Occitanie.
2. De violents orages et des routes inondées en Bretagne et en Occitanie.
3. Nouveau plan d'investissement annoncé par le gouvernement.
```

**Configuration :**
- Fréquence : Toutes les 30 minutes (`0 */30 * * * *`)
- Régions supportées : Toutes les régions actives en base
- Langues : Automatique selon la configuration du pays

---

### 🌤️ Météorologie

#### WeatherCollector

**Description :** Collecteur de données météorologiques (à implémenter)

**Source :** API météorologique (à définir)

**Statut :** 🔴 Non implémenté

---

### 🍽️ Gastronomie et Recettes

**Statut :** 🔴 Aucun collecteur implémenté

---

### 🎭 Découvertes et Culture

**Statut :** 🔴 Aucun collecteur implémenté

---

### 📚 Histoires et Contenus Narratifs

**Statut :** 🔴 Aucun collecteur implémenté

---

## Configuration des Collecteurs

Les collecteurs sont configurés dans `src/main/resources/application-{profile}.yml` :

```yaml
angel:
  collectors:
    enabled: true
    mock-mode: false
    collectors:
      GoogleNewsCollector:
        enabled: true
        schedule: "0 */30 * * * *"  # Cron expression
```

## Architecture Technique

### Classe BaseCollector

Tous les collecteurs héritent de `BaseCollector` qui fournit :
- Interface commune pour l'exécution
- Gestion du statut et des erreurs
- Méthodes utilitaires pour les requêtes HTTP
- Intégration avec le système de scheduling

### Workflow de Collection

1. **Découverte automatique** : Les collecteurs sont automatiquement détectés via Spring
2. **Initialisation** : Configuration et vérification de la validité
3. **Programmation** : Ajout au scheduler selon la cron expression
4. **Exécution** : Collecte des données selon la logique métier
5. **Sauvegarde** : Production des fichiers de contenu structurés
6. **Notification** : Mise à jour du statut et logs d'exécution

### Répertoires de Sortie

Structure des répertoires de données :
```
data/
├── news/
│   ├── regional/
│   │   ├── FR/
│   │   │   ├── IDF/
│   │   │   └── PACA/
│   │   └── US/
│   │       ├── CA/
│   │       └── NY/
│   ├── national/
│   │   ├── FR/
│   │   └── US/
│   └── international/
├── weather/
├── recipes/
├── discoveries/
└── stories/
```

## Surveillance et Monitoring

### Interface d'Administration

L'interface web d'administration (`http://localhost:8080`) permet de :
- Visualiser le statut de tous les collecteurs
- Déclencher manuellement une collecte
- Consulter les logs d'exécution
- Gérer la configuration en temps réel

### Métriques Disponibles

- Nombre d'exécutions réussies/échouées
- Durée moyenne d'exécution
- Dernière exécution réussie
- Prochaine exécution programmée
- Volume de données collectées

## Développement de Nouveaux Collecteurs

Pour ajouter un nouveau collecteur :

1. **Hériter de BaseCollector :**
```java
@Component
public class MonNouveauCollector extends BaseCollector {
    @Override
    public String getName() { return "MonNouveauCollector"; }
    
    @Override
    public void collect() {
        // Logique de collecte
    }
}
```

2. **Ajouter la configuration :**
```yaml
angel:
  collectors:
    collectors:
      MonNouveauCollector:
        enabled: true
        schedule: "0 0 */6 * * *"  # Toutes les 6 heures
```

3. **Documenter dans ce fichier**

---

## Roadmap

### Prochains Collecteurs Prévus

1. **WeatherCollector** - Données météorologiques détaillées
2. **RecipeCollector** - Recettes de cuisine régionales  
3. **EventsCollector** - Événements culturels et festivités
4. **NewsletterCollector** - Bulletins d'information locaux
5. **SportsCollector** - Résultats sportifs régionaux

### Améliorations Futures

- Système de cache intelligent pour éviter les doublons
- Détection automatique de contenu dupliqué
- API de configuration dynamique des collecteurs
- Intégration avec des systèmes de modération de contenu
- Support pour des sources de données en temps réel (WebSocket, SSE)

---

*Dernière mise à jour : 20 août 2025*