#!/bin/bash
set -e

echo "🔨 Building Spring Boot application..."
mvn clean package -DskipTests

echo "🐳 Building Docker image..."
eval $(minikube docker-env)
docker build -f docker/Dockerfile.dev -t angel-update-service:dev .

echo "📦 Applying Kubernetes configurations..."
kubectl apply -f k8s-dev/namespace.yaml
kubectl apply -f k8s-dev/configmap.yaml
kubectl apply -f k8s-dev/secrets.yaml

echo "🗄️ Deploying PostgreSQL..."
kubectl apply -f k8s-dev/postgres/

echo "💾 Deploying Redis..."
kubectl apply -f k8s-dev/redis/

echo "⏳ Waiting for databases to be ready..."
echo "  - Waiting for PostgreSQL (this may take a few minutes for first deployment)..."
kubectl wait --for=condition=ready pod -l app=postgres -n angel-update-dev --timeout=300s
echo "  - Waiting for Redis..."
kubectl wait --for=condition=ready pod -l app=redis -n angel-update-dev --timeout=60s
echo "✅ Databases are ready!"

echo "🚀 Deploying application..."
kubectl apply -f k8s-dev/app/

echo "✅ Deployment complete!"
echo ""
echo "📊 To access the application:"
echo "   1. Run: minikube tunnel (in another terminal)"
echo "   2. Add to /etc/hosts: 127.0.0.1 angel-update.local"
echo "   3. Access: http://angel-update.local"
echo ""
echo "Or use port-forward:"
echo "   ./scripts/port-forward.sh"
