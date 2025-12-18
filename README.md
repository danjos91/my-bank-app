# MyBank App - Microservices Banking Application (v4.0)

![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Apache Kafka](https://img.shields.io/badge/Apache_Kafka-231F20?style=for-the-badge&logo=apachekafka&logoColor=white)
![Kubernetes](https://img.shields.io/badge/kubernetes-%23326ce5.svg?style=for-the-badge&logo=kubernetes&logoColor=white)
![Helm](https://img.shields.io/badge/Helm-0F1689?style=for-the-badge&logo=helm&logoColor=white)
![Jenkins](https://img.shields.io/badge/jenkins-%232C5263.svg?style=for-the-badge&logo=jenkins&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)

An event-driven microservices banking platform built with Spring Boot, Apache Kafka, PostgreSQL, and Kubernetes. It supports user management, multi-currency accounts, money transfers with currency conversion, real-time notifications, and exchange rate management. Deployment is handled with **Helm Charts** and **Jenkins CI/CD**, including automated Kafka provisioning.

**Features:**
- ☁️ **Kubernetes Native**: Uses K8s Services, ConfigMaps, and Secrets (no Eureka/Config Server).
- 📦 **Helm Charts**: Dedicated charts for each microservice plus an umbrella chart.
- 📡 **Event-Driven with Kafka**: Idempotent producers and manual-ack consumers for reliable messaging.
- 💾 **StatefulSets**: Databases deployed as StatefulSets with persistent storage.
- 🚀 **CI/CD**: Jenkins pipelines for services, umbrella chart, and Kafka (topics included).
- 🔐 **OAuth2**: Auth Server running in Kubernetes.
- 🌐 **Ingress**: Front UI exposed via Ingress Controller.
- 💱 **Multi-Currency**: RUB, USD, and CNY with automatic conversion.

## 💱 Multi-Currency & Key Features

This application supports complex banking operations including:

- **Multi-Currency Accounts**: Users can create accounts in **RUB**, **USD**, and **CNY**.
- **Currency Conversion**: Automatic real-time conversion for transfers between different currencies (e.g., USD → RUB → CNY).
- **Exchange Service**: Dedicated microservice for managing exchange rates.
- **Exchange Generator**: Emits `exchange-rates` events every second via Kafka.
- **Blocker Service**: Monitors transactions and blocks suspicious activity based on thresholds (e.g., transactions > 10,000 RUB).
- **Notifications**: Kafka-backed real-time alerts for all account activities.

## 🚀 Quick Start (Kubernetes)

### Fast path
```bash
chmod +x start.sh
./start.sh   # starts Minikube (if needed), installs Kafka + topics, installs umbrella chart
```

### Prerequisites
- **Minikube** (or Kind/Colima)
- **Kubectl**
- **Helm 3+**
- **Docker**
- **Java 21** or higher
- **Maven 3.9+**

### Manual steps
1) **Start Minikube**
```bash
minikube start --cpus 4 --memory 8192
minikube addons enable ingress
```

2) **Deploy Kafka (Bitnami, KRaft)**
```bash
kubectl create namespace kafka --dry-run=client -o yaml | kubectl apply -f -
helm upgrade --install kafka oci://registry-1.docker.io/bitnamicharts/kafka \
  --version 30.1.5 \
  -n kafka --create-namespace \
  -f helm/kafka/values-standalone.yaml
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=kafka -n kafka --timeout=300s
```
- Chart values now use image `docker.io/bitnamilegacy/kafka:4.0.0-debian-12-r10` (KRaft, PLAINTEXT).

3) **Create Kafka topics** (if Jenkins pipeline is not run)
```bash
for topic in account-created account-updated deposit-completed withdrawal-completed \
  transfer-initiated transfer-completed transfer-failed notification-event exchange-rates service-logs; do
  kubectl exec -n kafka $(kubectl get pod -n kafka -l app.kubernetes.io/name=kafka,app.kubernetes.io/instance=kafka -o jsonpath='{.items[0].metadata.name}') -- \
    kafka-topics.sh --create --if-not-exists --bootstrap-server localhost:9092 \
    --topic "$topic" --partitions 3 --replication-factor 1 \
    --config retention.ms=604800000 --config min.insync.replicas=1 || true
done
```

4) **Deploy with Helm (Umbrella Chart)**
This deploys all microservices and databases at once.
```bash
cd helm/
helm dependency update my-bank-app
helm upgrade --install my-bank-app ./my-bank-app \
  --set kafka.enabled=false \
  --set global.kafka.bootstrapServers=kafka.kafka.svc.cluster.local:9092
```
- Note: `kafka.enabled=false` avoids deploying the Kafka subchart when you already installed Kafka separately.

5) **Access the application**
```bash
# If using Minikube Tunnel (requires root):
minikube tunnel
# Add to /etc/hosts: 127.0.0.1 bank.local
# Access at http://bank.local

# OR port-forward the front-end:
kubectl port-forward svc/front-ui 8086:8086
# Access at http://localhost:8086
```

## 📈 Observability Stack

The application includes a complete observability stack for monitoring, tracing, and logging:

- **Grafana** 📊 - Metrics visualization and dashboards
- **Prometheus** 📈 - Metrics collection and alerting
- **Zipkin** 🔍 - Distributed tracing
- **ELK Stack** 📝 - Centralized logging (Elasticsearch + Logstash + Kibana)

### 🚀 Quick Start - Accessing Observability Tools

The observability stack is automatically deployed by `./start.sh`. Access the UIs with these commands:

```bash
# Start port-forwarding in separate terminals
kubectl -n observability port-forward svc/grafana 3000:80 &
kubectl -n observability port-forward svc/zipkin 9411:9411 &
kubectl -n observability port-forward svc/prometheus-server 9090:80 &
kubectl -n observability port-forward svc/kibana 5601:5601 &
```

### 📊 Using Grafana (Metrics & Dashboards)

**Access:** http://localhost:3000  
**Login:** `admin` / `admin123`

#### Importing Dashboards

1. Click **"+"** → **"Import dashboard"** in the left sidebar
2. Enter one of these dashboard IDs:
   - **12900** - Spring Boot 2.x Statistics (⭐ Recommended)
   - **4701** - JVM Micrometer (Memory, GC, Threads)
   - **11378** - Kafka Overview (Topics, Consumer Lag)
3. Click **"Load"**
4. Select **"Prometheus"** as the data source
5. Click **"Import"**

#### What You'll See

- **HTTP Requests:** Response times, error rates, request counts per endpoint
- **JVM Metrics:** Memory usage, garbage collection, thread pools
- **Database:** Connection pool stats, query performance
- **Kafka:** Producer/consumer metrics, message throughput
- **System:** CPU, disk I/O, network traffic

**💡 Tip:** Make some transfers in the banking app (http://bank.local) and watch the metrics update in real-time!

### 🔍 Using Zipkin (Distributed Tracing)

**Access:** http://localhost:9411

#### How to Use

1. Open Zipkin UI
2. Click **"Run Query"** to see recent traces
3. Click on any trace to see the full request flow across services
4. Example trace flow for a transfer:
   ```
   front-ui → accounts-service → transfer-service → cash-service → notifications-service
   ```

**What to Look For:**
- **Latency:** How long each service takes
- **Errors:** Failed spans highlighted in red
- **Dependencies:** Visual service dependency graph

### 📝 Using Kibana (Centralized Logs)

**Access:** http://localhost:5601

#### Setup (First Time Only)

1. Go to **"Management"** → **"Stack Management"** → **"Index Patterns"**
2. Click **"Create index pattern"**
3. Enter pattern: `logstash-*`
4. Select time field: `@timestamp`
5. Click **"Create index pattern"**

#### Viewing Logs

1. Go to **"Discover"** in the left sidebar
2. Select date range (top-right)
3. Use the search bar to filter logs:
   - `level: ERROR` - Show only errors
   - `application: "accounts-service"` - Filter by service
   - `message: "transfer"` - Search in log messages
   - `traceId: "abc123"` - Find logs for a specific trace

**💡 Tip:** Copy a `traceId` from Zipkin and search for it in Kibana to see all logs for that request!

### 📈 Using Prometheus (Raw Metrics)

**Access:** http://localhost:9090

#### Useful Queries

```promql
# HTTP request rate per service
rate(http_server_requests_seconds_count[5m])

# JVM memory usage
jvm_memory_used_bytes{area="heap"}

# Database connection pool
hikaricp_connections_active

# Kafka consumer lag
kafka_consumer_fetch_manager_records_lag_max
```

**⚠️ Note:** Currently, the `/actuator/prometheus` endpoint has a known issue with Spring Boot 3.5.6. Metrics are still available via `/actuator/metrics` (JSON format) on each service.

### 🛠️ Manual Deployment

If not using `./start.sh`, deploy the observability stack manually:

```bash
kubectl create namespace observability --dry-run=client -o yaml | kubectl apply -f -

helm upgrade --install zipkin oci://registry-1.docker.io/bitnamicharts/zipkin \
  --version 1.3.11 -n observability -f helm/observability/values-zipkin.yaml
helm upgrade --install prometheus oci://registry-1.docker.io/bitnamicharts/prometheus \
  --version 2.1.23 -n observability -f helm/observability/values-prometheus.yaml
helm upgrade --install elasticsearch oci://registry-1.docker.io/bitnamicharts/elasticsearch \
  --version 22.1.6 -n observability -f helm/observability/values-elasticsearch.yaml
helm upgrade --install logstash oci://registry-1.docker.io/bitnamicharts/logstash \
  --version 7.0.11 -n observability -f helm/observability/values-logstash.yaml
helm upgrade --install kibana oci://registry-1.docker.io/bitnamicharts/kibana \
  --version 12.1.10 -n observability -f helm/observability/values-kibana.yaml
helm upgrade --install grafana oci://registry-1.docker.io/bitnamicharts/grafana \
  --version 12.1.8 -n observability -f helm/observability/values-grafana.yaml
```

### 🔧 Technical Details

**Service Instrumentation:**
- All services include Spring Boot Actuator with 27+ exposed endpoints
- Micrometer for metrics collection with tags (application, environment)
- Zipkin Brave for distributed tracing with 100% sampling
- Logback Kafka appender for centralized logging

**Metrics Endpoints:**
- Health: `http://<service>:port/actuator/health`
- Metrics (JSON): `http://<service>:port/actuator/metrics`
- Individual metric: `http://<service>:port/actuator/metrics/jvm.memory.used`

**Alert Rules** (configured in Prometheus):
- `HighErrorRate`: > 5% HTTP 5xx errors
- `HighJvmMemory`: > 90% heap usage
- `SlowHttpLatency`: p95 latency > 1s

**Local Development:**
```bash
export MANAGEMENT_ZIPKIN_TRACING_ENDPOINT=http://localhost:9411/api/v2/spans
export LOGS_BOOTSTRAP_SERVERS=localhost:9092
export LOGS_TOPIC=service-logs
```

### 📡 Kafka (Bitnami + Jenkins)
- Kafka is deployed in KRaft mode using `helm/kafka/values.yaml` (test) and `helm/kafka/values-prod.yaml` (prod) via Jenkins pipelines (`helm/kafka/Jenkinsfile` and root `Jenkinsfile`).
- Bootstrap for services: `kafka.kafka.svc.cluster.local:9092`.
- Topics created by pipelines:
  - Test: partitions=3, replication=1, `min.insync.replicas=1` for `account-created`, `account-updated`, `deposit-completed`, `withdrawal-completed`, `transfer-initiated`, `transfer-completed`, `transfer-failed`, `notification-event`, `exchange-rates`, `service-logs`.
  - Prod: partitions=6, replication=3, `min.insync.replicas=2` for the same topics.
- Quick local deploy:
  ```bash
  helm upgrade --install kafka oci://registry-1.docker.io/bitnamicharts/kafka -n kafka --create-namespace -f helm/kafka/values-standalone.yaml
  # Production: add -f helm/kafka/values-standalone-prod.yaml
  ```
- Key producers/consumers:
  - `exchange-generator-service`: idempotent producer (`acks=all`, retries, `enable.idempotence=true`) to `exchange-rates`.
  - `exchange-service`: consumer with `AckMode.MANUAL_IMMEDIATE` for `exchange-rates`.
  - `accounts-service`, `cash-service`, `transfer-service`: produce domain events for notifications and transfers.
  - `notifications-service`: consumes account/transfer topics and `notification-event` with manual ack for at-least-once delivery.
- The umbrella chart includes Kafka as a dependency (alias `kafka`, version `26.1.1` from `oci://registry-1.docker.io/bitnamicharts`). Disable it with `--set kafka.enabled=false` when deploying Kafka separately (as in the quick start above).

## 🔐 Configuration & Secrets Management

### Database Password Configuration

All microservices now support configurable database passwords through Helm values. By default, the password is `bank_app_password`, but you can override it for different environments.

#### Development (using default password)
```bash
helm install my-bank-app ./helm/my-bank-app
```

#### Override Password via Command Line
```bash
# Single service
helm install accounts-service ./helm/accounts-service \
  --set db.password=my_secure_password

# All services via umbrella chart
helm install my-bank-app ./helm/my-bank-app \
  --set accounts-service.db.password=accounts_pass \
  --set blocker-service.db.password=blocker_pass \
  --set cash-service.db.password=cash_pass \
  --set exchange-service.db.password=exchange_pass \
  --set notifications-service.db.password=notifications_pass \
  --set transfer-service.db.password=transfer_pass
```

#### Using Custom Values File
Create a `prod-values.yaml` file:
```yaml
accounts-service:
  db:
    password: "prod_accounts_password"

blocker-service:
  db:
    password: "prod_blocker_password"

cash-service:
  db:
    password: "prod_cash_password"

exchange-service:
  db:
    password: "prod_exchange_password"

notifications-service:
  db:
    password: "prod_notifications_password"

transfer-service:
  db:
    password: "prod_transfer_password"
```

Then deploy:
```bash
helm install my-bank-app ./helm/my-bank-app -f prod-values.yaml
```

#### CI/CD Integration (GitLab, Jenkins, GitHub Actions)
```bash
# Using environment variables from CI/CD secret store
helm install my-bank-app ./helm/my-bank-app \
  --set accounts-service.db.password=$ACCOUNTS_DB_PASSWORD \
  --set blocker-service.db.password=$BLOCKER_DB_PASSWORD \
  --set cash-service.db.password=$CASH_DB_PASSWORD \
  --set exchange-service.db.password=$EXCHANGE_DB_PASSWORD \
  --set notifications-service.db.password=$NOTIFICATIONS_DB_PASSWORD \
  --set transfer-service.db.password=$TRANSFER_DB_PASSWORD
```

### 📂 Project Structure

```
my-bank-app/
├── accounts-service/           # User accounts & balances
├── auth-server/                # OAuth2 Authorization Server
├── blocker-service/            # Suspicious transaction blocker
├── cash-service/               # Deposits & Withdrawals
├── exchange-generator-service/ # Generates random exchange rates
├── exchange-service/           # Handles currency conversion
├── front-ui/                   # Web Interface (Thymeleaf)
├── notifications-service/      # User notifications
├── transfer-service/           # Money transfers logic
├── helm/                       # Helm Charts
│   ├── my-bank-app/            # Umbrella Chart
│   ├── kafka/                  # Kafka values and Jenkinsfile
│   ├── accounts-service/       # Individual Charts...
│   ├── auth-server/
│   ├── blocker-service/
│   ├── cash-service/
│   ├── exchange-generator-service/
│   ├── exchange-service/
│   ├── front-ui/
│   ├── notifications-service/
│   └── transfer-service/
├── start.sh                    # Local helper: Minikube + Kafka + Helm deploy
├── Jenkinsfile                 # Master CI/CD Pipeline
└── README.md                   # Documentation
```

## 🔧 Service Ports

Even though in Kubernetes services communicate via internal cluster IPs, the internal container ports remain the same:

| Service | Port | Description |
|---------|------|-------------|
| **Gateway / Ingress** | 80/443 | Entry point (Front UI exposed here) |
| **Accounts** | 8081 | User & Multi-Currency Account Management |
| **Cash** | 8082 | Cash Operations (Deposit/Withdraw) |
| **Transfer** | 8083 | Money Transfers with Currency Conversion |
| **Notifications** | 8081 | Kafka consumer; health/actuator only |
| **Auth Server** | 8085 | OAuth2 Authentication |
| **Front UI** | 8086 | Web Interface |
| **Exchange** | 8087 | Currency Exchange Rates & Conversion (Kafka consumer) |
| **Exchange Generator** | 8088 | Automated Exchange Rate Generation (Kafka producer) |
| **Blocker** | 8089 | Suspicious Transaction Detection |
| **Kafka (internal)** | 9092 | Broker bootstrap (kafka.kafka.svc.cluster.local) |
| **PostgreSQL** | 5432 | Database (Internal) |

## 🛠️ CI/CD with Jenkins

This project includes `Jenkinsfile` for each microservice, a Kafka-specific pipeline (`helm/kafka/Jenkinsfile`), and a master `Jenkinsfile` for the whole project that:
- Deploys Kafka (Bitnami, KRaft) and creates required topics.
- Builds all services and Docker images.
- Deploys the umbrella Helm chart to test/prod with `global.kafka.bootstrapServers` set to `kafka.kafka.svc.cluster.local:9092`.

### Setting up Jenkins in Minikube

1. **Install Jenkins via Helm:**
   ```bash
   helm repo add jenkins https://charts.jenkins.io
   helm repo update
   kubectl create namespace jenkins
   helm install jenkins jenkins/jenkins --namespace jenkins --set controller.serviceType=NodePort
   ```

2. **Get Admin Password:**
   ```bash
   kubectl exec --namespace jenkins -it svc/jenkins -c jenkins -- /bin/cat /run/secrets/additional/chart-admin-password && echo
   ```

3. **Access Jenkins:**
   ```bash
   minikube service jenkins -n jenkins --url
   ```

4. **Create Pipeline:**
   - New Item -> Pipeline -> Name: `my-bank-app`
   - Definition: Pipeline script from SCM -> Git
   - Repository URL: (Your Git Repo URL)
   - Script Path: `Jenkinsfile` (for the umbrella project) or `accounts-service/Jenkinsfile` (for individual services).

## 👤 Test Users

The following test users are available for testing the application functionality. They are automatically created in the database on startup.

| Username | Password | Role | Initial Balance |
|----------|----------|------|-----------------|
| `admin` | `password123` | User | 200,000 RUB |
| `john` | `password123` | User | 5,000 RUB |
| `jane` | `password123` | User | 7,500 RUB |
| `bob` | `password123` | User | 3,000 RUB |

## 🔍 Troubleshooting

### Prometheus Endpoint Not Available

**Issue:** `/actuator/prometheus` returns 404 error  
**Status:** Known issue with Spring Boot 3.5.6 - the Prometheus text format endpoint is not being auto-configured despite correct dependencies and configuration.

**Workarounds:**
1. **Use JSON Metrics:** Access `http://<service>:port/actuator/metrics` for JSON-formatted metrics
2. **View Individual Metrics:** `http://<service>:port/actuator/metrics/jvm.memory.used`
3. **Check Available Endpoints:** `http://<service>:port/actuator` lists all 27+ available endpoints

**Verified Working:**
- ✅ Metrics collection via Micrometer
- ✅ All actuator endpoints (health, info, metrics)
- ✅ Distributed tracing via Zipkin
- ✅ Centralized logging via ELK
- ❌ Prometheus scrape endpoint (text format)

### Grafana Login Blocked

If you see "too many consecutive incorrect login attempts":
```bash
kubectl rollout restart deployment grafana -n observability
```
Wait 30 seconds, then try again with `admin`/`admin123`

### Services Not Starting

Check pod status:
```bash
kubectl get pods -n default
kubectl logs -n default <pod-name>
```

Common issues:
- **ImagePullBackOff:** Run `eval $(minikube docker-env)` before building images
- **CrashLoopBackOff:** Check logs for database connection errors
- **Pending:** Check if PVCs are bound: `kubectl get pvc`

### Kafka Connection Errors

Verify Kafka is running:
```bash
kubectl get pods -n kafka
kubectl logs -n kafka kafka-controller-0
```

Recreate topics if needed (see Kafka section above).

### Database Connection Issues

Check database pods:
```bash
kubectl get pods | grep db
kubectl logs <service>-db-0
```

Access database directly:
```bash
kubectl exec -it <service>-db-0 -- psql -U bank_app_user -d bank_app_db
```
