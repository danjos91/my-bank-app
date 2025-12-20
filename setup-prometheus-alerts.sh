#!/bin/bash
set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}🚨 Setting up Prometheus Alert Rules...${NC}"

OBSERVABILITY_NS="observability"
ALERTS_DIR="helm/prometheus/alerts"
PROMETHEUS_RELEASE="kube-prometheus-stack"

# Check if Prometheus is running
if ! kubectl get svc -n "${OBSERVABILITY_NS}" | grep -q prometheus; then
    echo -e "${RED}❌ Prometheus service not found. Deploy observability stack first.${NC}"
    exit 1
fi

echo -e "${BLUE}Creating ConfigMap for alert rules...${NC}"

# Create ConfigMap with alert rules
kubectl create configmap prometheus-alerts \
    --from-file="${ALERTS_DIR}" \
    -n "${OBSERVABILITY_NS}" \
    --dry-run=client -o yaml | kubectl apply -f -

echo -e "${GREEN}✅ Alert rules ConfigMap created${NC}"
echo -e "${BLUE}Note: Update Prometheus configuration to use this ConfigMap${NC}"
echo -e "${BLUE}Or configure via Helm values:${NC}"
echo "  prometheus:"
echo "    prometheusSpec:"
echo "      ruleSelector:"
echo "        matchLabels:"
echo "          app: prometheus"
echo "      additionalScrapeConfigs: []"

