#!/bin/bash
set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}🔍 Verifying Observability Integration...${NC}"
echo ""

# Check if observability namespace exists
OBSERVABILITY_NS="observability"
if ! kubectl get namespace "${OBSERVABILITY_NS}" &>/dev/null; then
    echo -e "${YELLOW}⚠️  Observability namespace not found. Deploy observability stack first.${NC}"
    echo "   Run: ./start.sh or deploy via Jenkins pipeline"
    exit 1
fi

# Services to test
SERVICES=(
    "accounts-service:8081"
    "cash-service:8082"
    "transfer-service:8083"
    "exchange-service:8087"
    "blocker-service:8089"
    "auth-server:8085"
    "front-ui:8086"
)

echo -e "${BLUE}📊 Testing Actuator Endpoints...${NC}"
echo ""

# Test health endpoints
HEALTH_OK=0
HEALTH_FAIL=0
for service_port in "${SERVICES[@]}"; do
    service=$(echo $service_port | cut -d: -f1)
    port=$(echo $service_port | cut -d: -f2)
    service_name="my-bank-app-${service}"
    
    if kubectl get svc "${service_name}" &>/dev/null; then
        if kubectl run "test-${service}-health" --image=curlimages/curl:latest --rm -i --restart=Never -- \
            curl -s --max-time 5 "http://${service_name}:${port}/actuator/health" &>/dev/null; then
            echo -e "${GREEN}✅ ${service}: Health endpoint accessible${NC}"
            ((HEALTH_OK++))
        else
            echo -e "${RED}❌ ${service}: Health endpoint failed${NC}"
            ((HEALTH_FAIL++))
        fi
    else
        echo -e "${YELLOW}⚠️  ${service}: Service not found${NC}"
    fi
done

echo ""
echo -e "${BLUE}📈 Testing Prometheus Metrics Endpoints...${NC}"
echo ""

# Test Prometheus endpoints
PROM_OK=0
PROM_FAIL=0
for service_port in "${SERVICES[@]}"; do
    service=$(echo $service_port | cut -d: -f1)
    port=$(echo $service_port | cut -d: -f2)
    service_name="my-bank-app-${service}"
    
    if kubectl get svc "${service_name}" &>/dev/null; then
        result=$(kubectl run "test-${service}-prom" --image=curlimages/curl:latest --rm -i --restart=Never -- \
            curl -s --max-time 5 "http://${service_name}:${port}/actuator/prometheus" 2>&1 | head -5)
        
        if echo "$result" | grep -q "jvm\|http\|process"; then
            echo -e "${GREEN}✅ ${service}: Prometheus metrics available${NC}"
            ((PROM_OK++))
        else
            echo -e "${YELLOW}⚠️  ${service}: Prometheus endpoint may need service rebuild${NC}"
            echo "   Response: $(echo "$result" | head -1)"
            ((PROM_FAIL++))
        fi
    fi
done

echo ""
echo -e "${BLUE}🔍 Checking Observability Components...${NC}"
echo ""

# Check Zipkin
if kubectl get svc -n "${OBSERVABILITY_NS}" zipkin &>/dev/null; then
    echo -e "${GREEN}✅ Zipkin service found${NC}"
else
    echo -e "${RED}❌ Zipkin service not found${NC}"
fi

# Check Prometheus
if kubectl get svc -n "${OBSERVABILITY_NS}" | grep -q prometheus; then
    echo -e "${GREEN}✅ Prometheus service found${NC}"
else
    echo -e "${RED}❌ Prometheus service not found${NC}"
fi

# Check Grafana
if kubectl get svc -n "${OBSERVABILITY_NS}" | grep -q grafana; then
    echo -e "${GREEN}✅ Grafana service found${NC}"
else
    echo -e "${RED}❌ Grafana service not found${NC}"
fi

# Check Elasticsearch
if kubectl get svc -n "${OBSERVABILITY_NS}" elasticsearch &>/dev/null; then
    echo -e "${GREEN}✅ Elasticsearch service found${NC}"
else
    echo -e "${RED}❌ Elasticsearch service not found${NC}"
fi

# Check Kibana
if kubectl get svc -n "${OBSERVABILITY_NS}" kibana &>/dev/null; then
    echo -e "${GREEN}✅ Kibana service found${NC}"
else
    echo -e "${RED}❌ Kibana service not found${NC}"
fi

echo ""
echo -e "${BLUE}📝 Summary:${NC}"
echo "  Health endpoints: ${HEALTH_OK} OK, ${HEALTH_FAIL} Failed"
echo "  Prometheus endpoints: ${PROM_OK} OK, ${PROM_FAIL} May need rebuild"
echo ""
echo -e "${YELLOW}💡 Note: If Prometheus endpoints fail, rebuild services with:${NC}"
echo "   mvn clean package -DskipTests"
echo "   Then rebuild Docker images and redeploy"
echo ""

