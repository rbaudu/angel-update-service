#!/bin/bash
echo "🧹 Cleaning up Minikube deployment..."
kubectl delete namespace angel-update-dev
echo "✅ Cleanup complete!"
