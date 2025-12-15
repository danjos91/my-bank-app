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

# 3. Deploy with Helm
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
echo "--------------------------------------------------------"
echo "📝 Next Steps:"
echo "1. Watch the pods status:"
echo "   kubectl get pods -w"
echo ""
echo "2. Once pods are running, access the UI:"
echo "   - If NOT using minikube tunnel: add \"$(minikube ip) bank.local\" to /etc/hosts and open http://bank.local/"
echo "   - If using minikube tunnel: add '127.0.0.1 bank.local' to /etc/hosts and open http://bank.local/"
echo "   - Or port-forward: kubectl port-forward svc/front-ui 8086:8086 and open http://localhost:8086"
echo "--------------------------------------------------------"


