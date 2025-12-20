#!/bin/bash
set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}📊 Setting up Grafana Dashboards...${NC}"

GRAFANA_SVC="kube-prometheus-stack-grafana"
OBSERVABILITY_NS="observability"
DASHBOARDS_DIR="helm/grafana/dashboards"

# Check if Grafana is running
if ! kubectl get svc -n "${OBSERVABILITY_NS}" "${GRAFANA_SVC}" &>/dev/null; then
    echo -e "${RED}❌ Grafana service not found. Deploy observability stack first.${NC}"
    exit 1
fi

echo -e "${BLUE}Port-forwarding Grafana (this will run in background)...${NC}"
kubectl port-forward -n "${OBSERVABILITY_NS}" svc/"${GRAFANA_SVC}" 3000:80 &
PF_PID=$!
sleep 5

# Get Grafana admin password
GRAFANA_PASSWORD=$(kubectl get secret -n "${OBSERVABILITY_NS}" "${GRAFANA_SVC}" -o jsonpath='{.data.admin-password}' | base64 -d)

echo -e "${BLUE}Importing dashboards...${NC}"

# Import each dashboard
for dashboard_file in "${DASHBOARDS_DIR}"/*.json; do
    if [ -f "$dashboard_file" ]; then
        dashboard_name=$(basename "$dashboard_file" .json)
        echo "Importing ${dashboard_name}..."
        
        # Use Grafana API to import dashboard
        curl -X POST \
            -H "Content-Type: application/json" \
            -u "admin:${GRAFANA_PASSWORD}" \
            -d @"${dashboard_file}" \
            http://localhost:3000/api/dashboards/db \
            &>/dev/null || echo "  ⚠️  Dashboard ${dashboard_name} may already exist or had import issues"
    fi
done

# Kill port-forward
kill $PF_PID 2>/dev/null || true

echo -e "${GREEN}✅ Dashboard import completed${NC}"
echo -e "${BLUE}Access Grafana at: kubectl port-forward -n ${OBSERVABILITY_NS} svc/${GRAFANA_SVC} 3000:80${NC}"
echo -e "${BLUE}Login: admin / ${GRAFANA_PASSWORD}${NC}"

