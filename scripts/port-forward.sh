#!/bin/bash
echo "🔌 Setting up port forwarding..."
echo "Application: http://localhost:8080"
echo "PostgreSQL: localhost:5432"
echo "Redis: localhost:6379"
echo "Press Ctrl+C to stop"

kubectl port-forward -n angel-update-dev service/angel-update-service 8080:8080 &
kubectl port-forward -n angel-update-dev service/postgres-service 5432:5432 &
kubectl port-forward -n angel-update-dev service/redis-service 6379:6379 &

wait
