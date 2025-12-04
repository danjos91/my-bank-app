#!/bin/bash
echo "🔌 Setting up port forwarding..."

# Kill existing port forwards to avoid conflicts
pkill -f "kubectl port-forward" || true

echo "Forwarding Front UI (8086) - Optional if using Ingress (bank.local)..."
kubectl port-forward svc/my-bank-app-front-ui 8086:8086 > /dev/null 2>&1 &

echo "Forwarding Auth Server (8085) - REQUIRED for Login Redirect..."
kubectl port-forward svc/my-bank-app-auth-server 8085:8085 > /dev/null 2>&1 &

echo "✅ Port forwarding started in background."
echo ""
echo "👉 Access the app at: http://bank.local (or http://localhost:8086)"
echo ""
echo "⚠️  IMPORTANT: Ensure you have the following in your /etc/hosts:"
echo "   127.0.0.1 my-bank-app-auth-server"
echo "   127.0.0.1 bank.local"
echo ""
echo "Press Ctrl+C to stop (this script exits but forwards keep running)"

