# Guide de Déploiement - ANGEL Update Service

## Table des Matières

1. [Déploiement Local (Development)](#déploiement-local-development)
2. [Déploiement Docker](#déploiement-docker)
3. [Déploiement Minikube](#déploiement-minikube)
4. [Déploiement Production Kubernetes](#déploiement-production-kubernetes)
5. [CI/CD Pipeline](#cicd-pipeline)
6. [Monitoring et Observabilité](#monitoring-et-observabilité)

## Déploiement Local (Development)

### Prérequis

- Java 17+
- Maven 3.9+
- PostgreSQL 15+
- Redis 7+

### Installation

```bash
# 1. Cloner le repository
git clone https://github.com/rbaudu/angel-update-service.git
cd angel-update-service

# 2. Installer les dépendances
mvn clean install

# 3. Configurer la base de données
createdb angeldb
psql angeldb < scripts/init-db.sql

# 4. Démarrer Redis
redis-server

# 5. Configurer les variables d'environnement
cp .env.example .env
# Éditer .env avec vos valeurs

# 6. Lancer l'application
mvn spring-boot:run -Dspring.profiles.active=dev
```

## Déploiement Docker

### Docker Compose Local

```yaml
# docker-compose.local.yml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: angeldb
      POSTGRES_USER: angel
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U angel"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    command: redis-server --appendonly yes
    volumes:
      - redis_data:/data
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  app:
    build:
      context: .
      dockerfile: docker/Dockerfile
    ports:
      - "8080:8080"
      - "5005:5005"  # Debug port
    environment:
      SPRING_PROFILES_ACTIVE: docker
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: angeldb
      DB_USER: angel
      DB_PASSWORD: ${DB_PASSWORD}
      REDIS_HOST: redis
      REDIS_PORT: 6379
      JAVA_OPTS: -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    volumes:
      - ./data:/data
      - ./logs:/logs

volumes:
  postgres_data:
  redis_data:
```

### Dockerfile Production

```dockerfile
# docker/Dockerfile
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Build application
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create user
RUN addgroup -g 1000 angel && \
    adduser -D -u 1000 -G angel angel

# Install health check tools
RUN apk add --no-cache curl

# Copy JAR
COPY --from=builder --chown=angel:angel /app/target/*.jar app.jar

# Create directories
RUN mkdir -p /data /logs /cache && \
    chown -R angel:angel /data /logs /cache

USER angel

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
```

## Déploiement Minikube

### Installation et Configuration

```bash
# 1. Démarrer Minikube
minikube start \
  --cpus=4 \
  --memory=8192 \
  --disk-size=20g \
  --driver=docker

# 2. Activer les addons
minikube addons enable ingress
minikube addons enable metrics-server
minikube addons enable dashboard

# 3. Configurer Docker pour Minikube
eval $(minikube docker-env)

# 4. Build l'image
docker build -f docker/Dockerfile -t angel-update-service:dev .

# 5. Déployer
./scripts/deploy-minikube.sh
```

### Script de Déploiement Minikube

```bash
#!/bin/bash
# scripts/deploy-minikube.sh

set -e

echo "🚀 Déploiement sur Minikube..."

# Créer namespace
kubectl apply -f k8s-dev/namespace.yaml

# Configmaps et Secrets
kubectl apply -f k8s-dev/configmap.yaml
kubectl apply -f k8s-dev/secrets.yaml

# Déployer PostgreSQL
echo "📦 Déploiement PostgreSQL..."
kubectl apply -f k8s-dev/postgres/
kubectl wait --for=condition=ready pod -l app=postgres -n angel-update-dev --timeout=60s

# Déployer Redis
echo "📦 Déploiement Redis..."
kubectl apply -f k8s-dev/redis/
kubectl wait --for=condition=ready pod -l app=redis -n angel-update-dev --timeout=60s

# Déployer l'application
echo "📦 Déploiement Application..."
kubectl apply -f k8s-dev/app/

# Attendre que l'application soit prête
kubectl wait --for=condition=ready pod -l app=angel-update-service -n angel-update-dev --timeout=120s

# Afficher l'état
echo "✅ Déploiement terminé!"
kubectl get all -n angel-update-dev

# Port forwarding
echo "🔌 Configuration du port-forwarding..."
kubectl port-forward -n angel-update-dev service/angel-update-service 8080:8080 &

echo "📍 Application disponible sur http://localhost:8080"
```

## Déploiement Production Kubernetes

### Configuration Kubernetes Production

```yaml
# k8s-prod/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: angel-update-service
  namespace: angel-prod
  labels:
    app: angel-update-service
    version: v1
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app: angel-update-service
  template:
    metadata:
      labels:
        app: angel-update-service
        version: v1
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8080"
        prometheus.io/path: "/actuator/prometheus"
    spec:
      affinity:
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
          - weight: 100
            podAffinityTerm:
              labelSelector:
                matchExpressions:
                - key: app
                  operator: In
                  values:
                  - angel-update-service
              topologyKey: kubernetes.io/hostname
      
      containers:
      - name: angel-update-service
        image: registry.example.com/angel-update-service:${VERSION}
        imagePullPolicy: Always
        
        ports:
        - name: http
          containerPort: 8080
          protocol: TCP
        
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod,kubernetes"
        - name: JAVA_OPTS
          value: "-Xms1g -Xmx2g -XX:+UseG1GC"
        
        envFrom:
        - configMapRef:
            name: angel-config
        - secretRef:
            name: angel-secrets
        
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
        
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 3
        
        volumeMounts:
        - name: data
          mountPath: /data
        - name: cache
          mountPath: /cache
      
      volumes:
      - name: data
        persistentVolumeClaim:
          claimName: angel-data-pvc
      - name: cache
        emptyDir:
          sizeLimit: 5Gi
---
apiVersion: v1
kind: Service
metadata:
  name: angel-update-service
  namespace: angel-prod
spec:
  type: ClusterIP
  selector:
    app: angel-update-service
  ports:
  - name: http
    port: 8080
    targetPort: 8080
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: angel-update-service-hpa
  namespace: angel-prod
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: angel-update-service
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:
      - type: Percent
        value: 100
        periodSeconds: 60
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Percent
        value: 50
        periodSeconds: 60
```

### Ingress Production

```yaml
# k8s-prod/ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: angel-update-ingress
  namespace: angel-prod
  annotations:
    kubernetes.io/ingress.class: nginx
    cert-manager.io/cluster-issuer: letsencrypt-prod
    nginx.ingress.kubernetes.io/rate-limit: "100"
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/force-ssl-redirect: "true"
spec:
  tls:
  - hosts:
    - api.angel-update.com
    secretName: angel-update-tls
  rules:
  - host: api.angel-update.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: angel-update-service
            port:
              number: 8080
```

## CI/CD Pipeline

### GitHub Actions

```yaml
# .github/workflows/deploy.yml
name: Build and Deploy

on:
  push:
    branches: [main]
    tags:
      - 'v*'
  pull_request:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Cache Maven dependencies
      uses: actions/cache@v3
      with:
        path: ~/.m2
        key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
    
    - name: Run tests
      run: mvn clean test
    
    - name: Generate test report
      uses: dorny/test-reporter@v1
      if: success() || failure()
      with:
        name: Maven Tests
        path: target/surefire-reports/*.xml
        reporter: java-junit

  build:
    needs: test
    runs-on: ubuntu-latest
    if: github.event_name == 'push'
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Build application
      run: mvn clean package -DskipTests
    
    - name: Set up Docker Buildx
      uses: docker/setup-buildx-action@v2
    
    - name: Login to Registry
      uses: docker/login-action@v2
      with:
        registry: ${{ secrets.REGISTRY_URL }}
        username: ${{ secrets.REGISTRY_USERNAME }}
        password: ${{ secrets.REGISTRY_PASSWORD }}
    
    - name: Build and push Docker image
      uses: docker/build-push-action@v4
      with:
        context: .
        file: ./docker/Dockerfile
        push: true
        tags: |
          ${{ secrets.REGISTRY_URL }}/angel-update-service:latest
          ${{ secrets.REGISTRY_URL }}/angel-update-service:${{ github.sha }}
        cache-from: type=gha
        cache-to: type=gha,mode=max

  deploy:
    needs: build
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Configure kubectl
      uses: azure/setup-kubectl@v3
      with:
        version: 'v1.28.0'
    
    - name: Set up Kube config
      run: |
        mkdir -p $HOME/.kube
        echo "${{ secrets.KUBE_CONFIG }}" | base64 -d > $HOME/.kube/config
    
    - name: Deploy to Kubernetes
      run: |
        kubectl set image deployment/angel-update-service \
          angel-update-service=${{ secrets.REGISTRY_URL }}/angel-update-service:${{ github.sha }} \
          -n angel-prod
        
        kubectl rollout status deployment/angel-update-service -n angel-prod
    
    - name: Verify deployment
      run: |
        kubectl get pods -n angel-prod
        kubectl get services -n angel-prod
```

## Monitoring et Observabilité

### Stack de Monitoring

```yaml
# k8s-monitoring/prometheus-values.yaml
prometheus:
  prometheusSpec:
    serviceMonitorSelectorNilUsesHelmValues: false
    retention: 30d
    storageSpec:
      volumeClaimTemplate:
        spec:
          accessModes: ["ReadWriteOnce"]
          resources:
            requests:
              storage: 50Gi

grafana:
  enabled: true
  adminPassword: ${GRAFANA_ADMIN_PASSWORD}
  ingress:
    enabled: true
    hosts:
      - grafana.angel-update.com
  datasources:
    datasources.yaml:
      apiVersion: 1
      datasources:
        - name: Prometheus
          type: prometheus
          url: http://prometheus-server
          access: proxy
          isDefault: true

alertmanager:
  enabled: true
  config:
    route:
      group_by: ['alertname', 'cluster', 'service']
      group_wait: 10s
      group_interval: 10s
      repeat_interval: 12h
      receiver: 'slack'
    receivers:
    - name: 'slack'
      slack_configs:
      - api_url: ${SLACK_WEBHOOK_URL}
        channel: '#angel-alerts'
```

### Dashboards Grafana

```json
// grafana-dashboard.json
{
  "dashboard": {
    "title": "ANGEL Update Service Metrics",
    "panels": [
      {
        "title": "Request Rate",
        "targets": [
          {
            "expr": "rate(http_server_requests_seconds_count[5m])"
          }
        ]
      },
      {
        "title": "Response Time P99",
        "targets": [
          {
            "expr": "histogram_quantile(0.99, http_server_requests_seconds_bucket)"
          }
        ]
      },
      {
        "title": "Cache Hit Rate",
        "targets": [
          {
            "expr": "rate(cache_hits_total[5m]) / rate(cache_requests_total[5m])"
          }
        ]
      },
      {
        "title": "Error Rate",
        "targets": [
          {
            "expr": "rate(http_server_requests_seconds_count{status=~'5..'}[5m])"
          }
        ]
      }
    ]
  }
}
```

## Scripts Utilitaires

### Health Check

```bash
#!/bin/bash
# scripts/health-check.sh

ENDPOINT="${1:-http://localhost:8080}"

echo "Checking health of $ENDPOINT"

response=$(curl -s -o /dev/null -w "%{http_code}" $ENDPOINT/actuator/health)

if [ $response -eq 200 ]; then
    echo "✅ Service is healthy"
    exit 0
else
    echo "❌ Service is unhealthy (HTTP $response)"
    exit 1
fi
```

### Rollback

```bash
#!/bin/bash
# scripts/rollback.sh

NAMESPACE="angel-prod"
DEPLOYMENT="angel-update-service"

echo "Rolling back $DEPLOYMENT in $NAMESPACE"

kubectl rollout undo deployment/$DEPLOYMENT -n $NAMESPACE
kubectl rollout status deployment/$DEPLOYMENT -n $NAMESPACE

echo "✅ Rollback completed"
```

## Checklist de Déploiement

### Pré-Production

- [ ] Tests unitaires passent
- [ ] Tests d'intégration passent
- [ ] Code review approuvée
- [ ] Documentation à jour
- [ ] Variables d'environnement configurées
- [ ] Secrets Kubernetes créés
- [ ] Backup de la base de données

### Production

- [ ] Tag de version créé
- [ ] Image Docker construite et poussée
- [ ] Deployment Kubernetes appliqué
- [ ] Health checks verts
- [ ] Monitoring actif
- [ ] Logs centralisés fonctionnels
- [ ] Tests de smoke passent
- [ ] Documentation de release

---

⚠️ **Important**: Toujours tester en environnement de staging avant la production!
