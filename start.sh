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
  "application-logs"
)

OBSERVABILITY_NAMESPACE="observability"
ZIPKIN_RELEASE="zipkin"
ZIPKIN_CHART="oci://registry-1.docker.io/bitnamicharts/openzipkin"
ZIPKIN_VERSION="1.0.0"
PROMETHEUS_STACK_RELEASE="kube-prometheus-stack"
PROMETHEUS_STACK_CHART="prometheus-community/kube-prometheus-stack"
PROMETHEUS_STACK_VERSION="55.0.0"
ELASTICSEARCH_RELEASE="elasticsearch"
ELASTICSEARCH_CHART="./helm/elasticsearch-simple"

# 1. Check Prerequisites
echo -e "${BLUE}🔍 Checking prerequisites...${NC}"

REQUIRED_TOOLS=("minikube" "docker" "mvn" "kubectl" "helm")
for tool in "${REQUIRED_TOOLS[@]}"; do
    if ! command -v "$tool" &> /dev/null; then
        echo "❌ $tool is not installed. Please install it first."
        exit 1
    fi
done

echo -e "${GREEN}✅ All prerequisites are installed${NC}"

# 2. Check/Start Minikube

echo -e "${BLUE}Checking Minikube status...${NC}"
if ! minikube status > /dev/null 2>&1; then
    echo -e "${BLUE}📦 Starting Minikube...${NC}"
    minikube start --cpus 4 --memory 8192
    minikube addons enable ingress
else
    echo -e "${GREEN}✅ Minikube is running${NC}"
fi

# 3. Deploy Kafka (Bitnami, KRaft)
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

# 3.5. Deploy Observability Stack
echo -e "${BLUE}📊 Deploying Observability Stack (Zipkin, Prometheus, Grafana, ELK)...${NC}"

# Create observability namespace
kubectl create namespace "${OBSERVABILITY_NAMESPACE}" --dry-run=client -o yaml | kubectl apply -f -

# Add Prometheus Helm repository
echo "Adding Prometheus Helm repository..."
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts || true
helm repo update

# Deploy Zipkin
echo -e "${BLUE}📈 Deploying Zipkin...${NC}"
helm upgrade --install "${ZIPKIN_RELEASE}" "${ZIPKIN_CHART}" \
  --version "${ZIPKIN_VERSION}" \
  --namespace "${OBSERVABILITY_NAMESPACE}" \
  --create-namespace \
  --set service.type=ClusterIP \
  --set service.port=9411 \
  --wait \
  --timeout 5m || echo "⚠️  Zipkin deployment had issues, continuing..."

# Deploy Prometheus and Grafana Stack
echo -e "${BLUE}📊 Deploying Prometheus and Grafana...${NC}"
helm upgrade --install "${PROMETHEUS_STACK_RELEASE}" "${PROMETHEUS_STACK_CHART}" \
  --version "${PROMETHEUS_STACK_VERSION}" \
  --namespace "${OBSERVABILITY_NAMESPACE}" \
  --create-namespace \
  --set prometheus.prometheusSpec.serviceMonitorSelectorNilUsesHelmValues=false \
  --set grafana.service.type=ClusterIP \
  --set grafana.adminPassword=admin \
  --wait \
  --timeout 10m || echo "⚠️  Prometheus/Grafana deployment had issues, continuing..."

# Deploy Elasticsearch (lightweight single-node setup for Minikube)
echo -e "${BLUE}📝 Deploying Elasticsearch and Kibana (lightweight single-node)...${NC}"
cd "$SCRIPT_DIR"
helm upgrade --install "${ELASTICSEARCH_RELEASE}" "${ELASTICSEARCH_CHART}" \
  --namespace "${OBSERVABILITY_NAMESPACE}" \
  --create-namespace \
  --wait \
  --timeout 10m || echo "⚠️  Elasticsearch/Kibana deployment had issues, continuing..."
cd "$SCRIPT_DIR"

echo -e "${GREEN}✅ Observability stack deployment initiated${NC}"

# 4. Build Java Services
echo -e "${BLUE}🔨 Building Java services with Maven...${NC}"
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven is not installed. Please install Maven first."
    exit 1
fi
mvn clean package -DskipTests

# 5. Build Docker Images
echo -e "${BLUE}🐳 Building Docker images...${NC}"
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed. Please install Docker first."
    exit 1
fi

# Services with 'latest' tag
LATEST_SERVICES=(
  "accounts-service"
  "blocker-service"
  "cash-service"
  "exchange-service"
  "notifications-service"
  "transfer-service"
)

# Services with 'fixed' tag
FIXED_SERVICES=(
  "auth-server"
  "exchange-generator-service"
  "front-ui"
)

echo "Building services with 'latest' tag..."
for service in "${LATEST_SERVICES[@]}"; do
  echo "Building ${service}:latest..."
  docker build -t "${service}:latest" -f "${service}/Dockerfile" .
done

echo "Building services with 'fixed' tag..."
for service in "${FIXED_SERVICES[@]}"; do
  echo "Building ${service}:fixed..."
  docker build -t "${service}:fixed" -f "${service}/Dockerfile" .
done

# 6. Load Images into Minikube
echo -e "${BLUE}📦 Loading images into Minikube...${NC}"
echo "Loading 'latest' tag images..."
for service in "${LATEST_SERVICES[@]}"; do
  echo "Loading ${service}:latest..."
  minikube image load "${service}:latest"
done

echo "Loading 'fixed' tag images..."
for service in "${FIXED_SERVICES[@]}"; do
  echo "Loading ${service}:fixed..."
  minikube image load "${service}:fixed"
done

# 7. Deploy with Helm
echo -e "${BLUE}☸️  Deploying Helm Charts...${NC}"

cd "$SCRIPT_DIR/helm"

# Update dependencies for the umbrella chart
echo "📥 Updating dependencies..."
helm dependency update my-bank-app

# Install or Upgrade the release
echo "🚀 Installing/Upgrading 'my-bank-app' release..."
helm upgrade --install my-bank-app ./my-bank-app \
  --set kafka.enabled=false \
  --set global.kafka.bootstrapServers=kafka.kafka.svc.cluster.local:9092 \
  --wait \
  --timeout 15m

echo -e "${GREEN}✅ Deployment commands executed successfully!${NC}"
echo ""
echo "--------------------------------------------------------"
echo "📝 Next Steps:"
echo "1. Watch the pods status:"
echo "   kubectl get pods -w"
echo ""
echo "2. Once pods are running, access the UI:"
echo "   - If NOT using minikube tunnel: add \"$(minikube ip) bank.local\" to /etc/hosts and open http://bank.local/"
echo "   - If using minikube tunnel: add '127.0.0.1 bank.local' to /etc/hosts and open http://bank.local/"
echo "   - Or port-forward: kubectl port-forward svc/front-ui 8086:8086 and open http://localhost:8086"
echo ""
echo "3. Access Observability Dashboards:"
echo "   - Zipkin (Tracing):"
echo "     kubectl port-forward -n ${OBSERVABILITY_NAMESPACE} svc/${ZIPKIN_RELEASE} 9411:9411"
echo "     Then open http://localhost:9411"
echo ""
echo "   - Grafana (Metrics):"
echo "     kubectl port-forward -n ${OBSERVABILITY_NAMESPACE} svc/${PROMETHEUS_STACK_RELEASE}-grafana 3000:80"
echo "     Then open http://localhost:3000 (admin/admin)"
echo ""
echo "   - Prometheus (Metrics Query):"
echo "     kubectl port-forward -n ${OBSERVABILITY_NAMESPACE} svc/${PROMETHEUS_STACK_RELEASE}-prometheus 9090:9090"
echo "     Then open http://localhost:9090"
echo ""
echo "   - Kibana (Logs):"
echo "     kubectl port-forward -n ${OBSERVABILITY_NAMESPACE} svc/${ELASTICSEARCH_RELEASE}-elasticsearch-simple-kibana 5601:5601"
echo "     Then open http://localhost:5601"
echo "--------------------------------------------------------"

