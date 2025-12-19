#!/bin/bash
set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🚀 Initializing MyBank App Environment...${NC}"

KAFKA_NAMESPACE="kafka"
KAFKA_RELEASE="kafka"
KAFKA_VALUES="helm/kafka/values-standalone.yaml"
KAFKA_VERSION="30.1.5"
KAFKA_CHART="oci://registry-1.docker.io/bitnamicharts/kafka"
KAFKA_TOPICS=(
  "account-created"
  "account-updated"
  "deposit-completed"
  "withdrawal-completed"
  "transfer-initiated"
  "transfer-completed"
  "transfer-failed"
  "notification-event"
  "exchange-rates"
  "service-logs"
)

OBSERVABILITY_ONLY="${OBSERVABILITY_ONLY:-false}"

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

# 2. Deploy Kafka (Bitnami, KRaft)
echo -e "${BLUE}📡 Deploying Kafka (Bitnami) in namespace '${KAFKA_NAMESPACE}'...${NC}"

# Navigate to repo root (script dir)
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

kubectl create namespace "${KAFKA_NAMESPACE}" --dry-run=client -o yaml | kubectl apply -f -
helm upgrade --install "${KAFKA_RELEASE}" "${KAFKA_CHART}" \
  --version "${KAFKA_VERSION}" \
  --namespace "${KAFKA_NAMESPACE}" \
  --create-namespace \
  -f "${KAFKA_VALUES}" \
  --wait \
  --timeout 10m

echo -e "${BLUE}⏳ Waiting for Kafka pods to be ready...${NC}"
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=kafka \
  --namespace "${KAFKA_NAMESPACE}" \
  --timeout=300s || true

echo -e "${BLUE}📝 Creating Kafka topics (idempotent)...${NC}"
KAFKA_POD=$(kubectl get pod -n "${KAFKA_NAMESPACE}" -l app.kubernetes.io/name=kafka,app.kubernetes.io/instance="${KAFKA_RELEASE}" -o jsonpath='{.items[0].metadata.name}')
if [ -z "$KAFKA_POD" ]; then
  echo "❌ Could not find Kafka pod in namespace ${KAFKA_NAMESPACE}"
  exit 1
fi
for topic in "${KAFKA_TOPICS[@]}"; do
  kubectl exec -n "${KAFKA_NAMESPACE}" "${KAFKA_POD}" -- \
    kafka-topics.sh --create --if-not-exists \
    --bootstrap-server localhost:9092 \
    --topic "${topic}" \
    --partitions 3 \
    --replication-factor 1 \
    --config retention.ms=604800000 \
    --config min.insync.replicas=1 || true
done

# 3. Deploy Observability Stack (Prometheus, Grafana, Zipkin, ELK)
echo -e "${BLUE}📊 Deploying Observability Stack...${NC}"
kubectl create namespace observability --dry-run=client -o yaml | kubectl apply -f -

# Only uninstall if FORCE_REDEPLOY environment variable is set
# This prevents unnecessary pod restarts on every run
if [ "${FORCE_REDEPLOY:-false}" = "true" ]; then
  echo -e "${BLUE}  🔧 Force redeploy: Cleaning up existing releases...${NC}"
  helm uninstall prometheus -n observability 2>/dev/null || true
  helm uninstall grafana -n observability 2>/dev/null || true
  helm uninstall zipkin -n observability 2>/dev/null || true
  helm uninstall elasticsearch -n observability 2>/dev/null || true
  helm uninstall logstash -n observability 2>/dev/null || true
  helm uninstall kibana -n observability 2>/dev/null || true
  # Wait a bit for resources to be cleaned up
  sleep 5
else
  echo -e "${BLUE}  ℹ️  Using helm upgrade (pods won't restart unless config changed)${NC}"
  echo -e "${BLUE}     To force redeploy, run: FORCE_REDEPLOY=true ./start.sh${NC}"
fi

echo -e "${BLUE}  📈 Deploying Prometheus (Metrics Collection)...${NC}"
helm upgrade --install prometheus oci://registry-1.docker.io/bitnamicharts/prometheus \
  --version 2.1.23 -n observability -f helm/observability/values-prometheus-simple.yaml \
  --timeout 10m || echo "⚠️  Prometheus deployment failed, continuing..."

echo -e "${BLUE}  📊 Deploying Grafana (Metrics Dashboards)...${NC}"
# Create ConfigMap for Spring Boot dashboard
if [ -f "$SCRIPT_DIR/helm/observability/dashboards/spring-boot.json" ]; then
  echo -e "${BLUE}    Creating Grafana dashboard ConfigMap...${NC}"
  kubectl create configmap grafana-spring-boot-dashboard \
    --from-file=spring-boot.json="$SCRIPT_DIR/helm/observability/dashboards/spring-boot.json" \
    -n observability --dry-run=client -o yaml | kubectl apply -f - || true
fi
helm upgrade --install grafana oci://registry-1.docker.io/bitnamicharts/grafana \
  --version 12.1.8 -n observability -f helm/observability/values-grafana-simple.yaml \
  --timeout 10m || echo "⚠️  Grafana deployment failed, continuing..."

echo -e "${BLUE}  🔍 Deploying Zipkin (Distributed Tracing)...${NC}"
helm upgrade --install zipkin oci://registry-1.docker.io/bitnamicharts/zipkin \
  --version 1.3.11 -n observability -f helm/observability/values-zipkin.yaml \
  --timeout 15m || echo "⚠️  Zipkin deployment failed, continuing..."

echo -e "${BLUE}  📦 Deploying Elasticsearch (Log Storage)...${NC}"
helm upgrade --install elasticsearch oci://registry-1.docker.io/bitnamicharts/elasticsearch \
  --version 22.1.6 -n observability -f helm/observability/values-elasticsearch.yaml \
  --timeout 20m || echo "⚠️  Elasticsearch deployment failed, continuing..."

# Wait for Elasticsearch to be ready before deploying dependent services
echo -e "${BLUE}  ⏳ Waiting for Elasticsearch to be ready...${NC}"
ELASTICSEARCH_SELECTOR="app.kubernetes.io/name=elasticsearch,app.kubernetes.io/instance=elasticsearch"
if ! kubectl wait --for=condition=ready pod -l "${ELASTICSEARCH_SELECTOR}" -n observability --timeout=600s; then
  echo "❌ Elasticsearch pods did not become ready. Current status:"
  kubectl get pods -n observability -l "${ELASTICSEARCH_SELECTOR}" || true
  exit 1
fi

echo -e "${BLUE}  🔄 Deploying Logstash (Log Processing)...${NC}"
helm upgrade --install logstash oci://registry-1.docker.io/bitnamicharts/logstash \
  --version 7.0.11 -n observability -f helm/observability/values-logstash.yaml \
  --timeout 15m || echo "⚠️  Logstash deployment failed, continuing..."

echo -e "${BLUE}  📝 Deploying Kibana (Log Visualization)...${NC}"
# Ensure Elasticsearch is ready before Kibana
echo -e "${BLUE}    Verifying Elasticsearch is accessible...${NC}"
ELASTICSEARCH_READY=false
for i in {1..30}; do
  if kubectl get svc elasticsearch -n observability &>/dev/null || kubectl get svc elasticsearch-master -n observability &>/dev/null; then
    ELASTICSEARCH_READY=true
    break
  fi
  sleep 2
done

if [ "$ELASTICSEARCH_READY" = "true" ]; then
  helm upgrade --install kibana oci://registry-1.docker.io/bitnamicharts/kibana \
    --version 12.1.10 -n observability -f helm/observability/values-kibana.yaml \
    --timeout 15m || echo "⚠️  Kibana deployment failed, continuing..."
else
  echo "⚠️  Elasticsearch service not found, skipping Kibana deployment"
fi

echo -e "${BLUE}  ⏳ Waiting for observability pods to be ready (this may take a few minutes)...${NC}"
echo -e "${BLUE}    Note: Some pods may fail due to image pull issues - this is expected if Docker Hub rate limits are hit${NC}"
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=prometheus -n observability --timeout=120s 2>/dev/null || echo "⚠️  Prometheus pods not ready (check image pull issues)"
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=grafana -n observability --timeout=120s 2>/dev/null || echo "⚠️  Grafana pods not ready (check image pull issues)"
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=zipkin -n observability --timeout=120s 2>/dev/null || echo "⚠️  Zipkin pods not ready (check image pull issues)"
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=elasticsearch -n observability --timeout=120s 2>/dev/null || echo "⚠️  Elasticsearch pods not ready (check image pull issues)"
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=logstash -n observability --timeout=120s 2>/dev/null || echo "⚠️  Logstash pods not ready (check image pull issues)"
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=kibana -n observability --timeout=120s 2>/dev/null || echo "⚠️  Kibana pods not ready (check image pull issues)"

echo -e "${BLUE}  📋 Observability Status Summary:${NC}"
RUNNING_OBS=$(kubectl get pods -n observability --no-headers 2>/dev/null | grep Running | wc -l)
TOTAL_OBS=$(kubectl get pods -n observability --no-headers 2>/dev/null | wc -l)
echo -e "${BLUE}    Running: ${RUNNING_OBS}/${TOTAL_OBS} pods${NC}"
if [ "$RUNNING_OBS" -lt "$TOTAL_OBS" ]; then
  echo -e "${BLUE}    ⚠️  Some observability pods may have image pull issues${NC}"
  echo -e "${BLUE}    This is expected if Bitnami images are unavailable or require authentication${NC}"
fi

echo -e "${GREEN}✅ Observability stack deployment completed${NC}"

if [ "${OBSERVABILITY_ONLY}" = "true" ]; then
  echo -e "${GREEN}✅ OBSERVABILITY_ONLY=true set - skipping application deployment${NC}"
  echo "You can now port-forward observability services as needed."
  exit 0
fi

# 4. Deploy with Helm
echo -e "${BLUE}☸️  Deploying Helm Charts...${NC}"

cd "$SCRIPT_DIR/helm"

# Update dependencies for the umbrella chart
echo "📥 Updating dependencies..."
helm dependency update my-bank-app

# Install or Upgrade the release
echo "🚀 Installing/Upgrading 'my-bank-app' release..."
helm upgrade --install my-bank-app ./my-bank-app \
  --set kafka.enabled=false \
  --set global.kafka.bootstrapServers=kafka.kafka.svc.cluster.local:9092

echo -e "${GREEN}✅ Deployment commands executed successfully!${NC}"
echo ""
echo "════════════════════════════════════════════════════════════════"
echo "📝 Next Steps:"
echo "════════════════════════════════════════════════════════════════"
echo ""
echo "1️⃣  Watch the pods status:"
echo "   kubectl get pods -w"
echo ""
echo "2️⃣  Once pods are running, access the Banking App:"
echo "   - Add \"$(minikube ip) bank.local\" to /etc/hosts"
echo "   - Open http://bank.local"
echo "   - Or port-forward: kubectl port-forward svc/front-ui 8086:8086"
echo ""
echo "3️⃣  Access Observability Tools (port-forward in separate terminals):"
echo "   kubectl -n observability port-forward svc/grafana 3000:80 &"
echo "   kubectl -n observability port-forward svc/prometheus-server 9090:80 &"
echo "   kubectl -n observability port-forward svc/zipkin 9411:9411 &"
echo "   kubectl -n observability port-forward svc/kibana 5601:5601 &"
echo ""
echo "   📊 Grafana:    http://localhost:3000 (admin/admin123)"
echo "   📈 Prometheus: http://localhost:9090"
echo "   🔍 Zipkin:     http://localhost:9411"
echo "   📝 Kibana:     http://localhost:5601"
echo ""
echo "   💡 Tip: Grafana has pre-configured dashboards #12900 and #4701"
echo ""
echo "4️⃣  Test Users (login at http://bank.local):"
echo "   - admin / password123 (200,000 RUB)"
echo "   - jane  / password123 (7,500 RUB)"
echo "   - john  / password123 (5,000 RUB)"
echo ""
echo "════════════════════════════════════════════════════════════════"


