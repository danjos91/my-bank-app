#!/bin/bash
set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🚀 Initializing MyBank App Environment...${NC}"

# 1. Check/Start Minikube
if ! command minikube version &> /dev/null; then
    echo "❌ Minikube is not installed. Please install it first."
    exit 1
fi

echo -e "${BLUE}Checking Minikube status...${NC}"
if ! minikube status > /dev/null 2>&1; then
    echo -e "${BLUE}📦 Starting Minikube...${NC}"
    minikube start --cpus 4 --memory 8192
    minikube addons enable ingress
else
    echo -e "${GREEN}✅ Minikube is running${NC}"
fi

# 2. Deploy with Helm
echo -e "${BLUE}☸️  Deploying Helm Charts...${NC}"

# Navigate to helm directory relative to script location
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR/helm"

# Update dependencies for the umbrella chart
echo "📥 Updating dependencies..."
helm dependency update my-bank-app

# Install or Upgrade the release
echo "🚀 Installing/Upgrading 'my-bank-app' release..."
helm upgrade --install my-bank-app ./my-bank-app

echo -e "${GREEN}✅ Deployment commands executed successfully!${NC}"
echo ""
echo "--------------------------------------------------------"
echo "📝 Next Steps:"
echo "1. Watch the pods status:"
echo "   kubectl get pods -w"
echo ""
echo "2. Once pods are running, access the UI:"
echo "   Then open http://bank.local/"
echo "--------------------------------------------------------"


