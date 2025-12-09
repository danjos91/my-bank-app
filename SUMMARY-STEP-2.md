# Step 2: Update Umbrella Jenkinsfile for Kafka Integration - Detailed Summary

## Overview
Updated the main Jenkinsfile (umbrella project pipeline) to include Apache Kafka deployment as a prerequisite before deploying microservices. This ensures Kafka infrastructure is available before any microservice that depends on it is deployed.

## Changes Made to Jenkinsfile

### 1. New Environment Variables
```groovy
KAFKA_NAMESPACE = 'kafka'
KAFKA_RELEASE_NAME = 'kafka'
```

**Purpose**: 
- Centralized configuration for Kafka deployment
- Makes it easy to change namespace or release name across all stages
- Follows DRY (Don't Repeat Yourself) principle

### 2. New Stage: "Deploy Kafka Platform" (First Stage)
**Position**: Inserted as the FIRST stage, before "Build All"

**Why First?**: 
- Kafka must be running before microservices start
- Microservices will fail health checks if they can't connect to Kafka
- Follows infrastructure-first deployment pattern

**Stage Breakdown**:

#### 2.1. Add Bitnami Helm Repository
```groovy
helm repo add bitnami https://charts.bitnami.com/bitnami || true
helm repo update
```
- Adds Bitnami repository (if not already added)
- `|| true` prevents failure if repo already exists
- Updates repo cache to get latest chart versions

#### 2.2. Create Kafka Namespace
```groovy
kubectl create namespace kafka --dry-run=client -o yaml | kubectl apply -f -
```
- Idempotent namespace creation
- `--dry-run=client -o yaml` generates the namespace YAML
- Piped to `kubectl apply` creates it if missing, does nothing if exists
- Better than `kubectl create namespace` which fails if namespace exists

#### 2.3. Deploy Kafka Using Helm
```groovy
helm upgrade --install kafka bitnami/kafka \
  --version 30.1.5 \
  --namespace kafka \
  --create-namespace \
  -f helm/kafka/values.yaml \
  --wait \
  --timeout 10m
```

**Key Flags**:
- `upgrade --install`: Idempotent - installs if missing, upgrades if exists
- `--version 30.1.5`: Pins specific chart version for reproducibility
- `-f helm/kafka/values.yaml`: Uses our custom configuration
- `--wait`: Blocks until all resources are ready
- `--timeout 10m`: Allows 10 minutes for Kafka startup (disk formatting, controller election, etc.)

**What Gets Deployed**:
- 1 Kafka broker (KRaft mode)
- 1 controller (KRaft quorum)
- Kubernetes Service (kafka.kafka.svc.cluster.local:9092)
- PersistentVolumeClaims (10Gi storage)
- ConfigMaps and Secrets

#### 2.4. Wait for Kafka Pods Ready
```groovy
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=kafka \
  --namespace kafka \
  --timeout=300s || true
```

**Purpose**:
- Double-checks that Kafka pods are in Ready state
- Label selector targets all Kafka pods
- 5-minute timeout
- `|| true`: Don't fail pipeline if wait times out (Helm --wait already waited)
- Extra safety to prevent race conditions

### 3. New Stage: "Create Kafka Topics" (Second Stage)

**Why Separate Stage?**:
- Topic creation requires Kafka to be fully operational
- Separates infrastructure (Kafka) from configuration (topics)
- Easier to troubleshoot failures
- Can be re-run independently

**Topics Created**:
```groovy
def topics = [
    'account-created',       // New account registration
    'account-updated',       // Account modifications
    'deposit-completed',     // Successful deposits
    'withdrawal-completed',  // Successful withdrawals
    'transfer-initiated',    // Transfer started
    'transfer-completed',    // Transfer succeeded
    'transfer-failed',       // Transfer failed
    'notification-event'     // Generic notifications
]
```

**Topic Configuration**:
- **Partitions**: 3 (allows 3 parallel consumers)
- **Replication Factor**: 1 (test/dev environment)
- **Retention**: 604800000 ms (7 days)
- **min.insync.replicas**: 1 (matches replication factor)

**Topic Creation Logic**:
```groovy
sh 'sleep 10'  // Give Kafka extra time to stabilize
```
- Brief pause ensures Kafka broker is fully initialized
- Prevents timing issues where Kafka accepts connections but isn't ready for topic operations

```groovy
kubectl exec -n kafka kafka-controller-0 -- \
  kafka-topics.sh --create \
  --if-not-exists \
  --bootstrap-server localhost:9092 \
  --topic ${topic} \
  ...
  || true
```

**Explanation**:
- `kubectl exec`: Runs command inside Kafka pod
- `kafka-controller-0`: Uses controller pod (any Kafka pod would work)
- `kafka-topics.sh`: Kafka's built-in topic management tool
- `--if-not-exists`: Idempotent - doesn't fail if topic exists
- `localhost:9092`: Inside the pod, Kafka is accessible on localhost
- `|| true`: Don't fail pipeline if topic already exists

**Topic Verification**:
```groovy
kubectl exec -n kafka kafka-controller-0 -- \
  kafka-topics.sh --list --bootstrap-server localhost:9092
```
- Lists all topics after creation
- Output appears in Jenkins console for validation
- Useful for debugging and audit trail

### 4. Updated Stage: "Deploy All to Test"

**Change**:
```groovy
--set global.kafka.bootstrapServers=kafka.kafka.svc.cluster.local:9092
```

**Purpose**:
- Passes Kafka bootstrap server URL to all microservices via Helm
- `kafka.kafka.svc.cluster.local`: Kubernetes internal DNS
  - First "kafka": Service name
  - Second "kafka": Namespace name
  - `svc.cluster.local`: Kubernetes DNS suffix
- Port `9092`: Standard Kafka port
- All microservices will inherit this value via umbrella chart

**How It Works**:
- Umbrella chart defines `global.kafka.bootstrapServers`
- Child charts (accounts, cash, transfer, notifications) reference it
- Injected into ConfigMaps → environment variables → Spring Boot config

### 5. Updated Stage: "Deploy All to Prod"

**Change**:
Same Kafka bootstrap server configuration for production namespace.

**Note**: Uses same Kafka cluster (in kafka namespace) for both test and prod microservices.

**Production Consideration**:
In a real production scenario, you might have:
- Separate Kafka clusters per environment
- Different bootstrap servers:
  - Test: `kafka-test.kafka.svc.cluster.local:9092`
  - Prod: `kafka-prod.kafka.svc.cluster.local:9092`

### 6. New Stage: "Deploy Kafka to Production" (Last Stage)

**Position**: After "Deploy All to Prod"

**Why Separate?**:
- Test and prod apps can share a single Kafka cluster initially
- Upgrading Kafka to production HA mode is a separate decision
- Allows testing application deployment before scaling Kafka

**Production Deployment**:
```groovy
helm upgrade --install kafka bitnami/kafka \
  --version 30.1.5 \
  --namespace kafka \
  -f helm/kafka/values.yaml \
  -f helm/kafka/values-prod.yaml \
  --wait \
  --timeout 15m
```

**Key Differences**:
- Uses BOTH `values.yaml` AND `values-prod.yaml`
- values-prod.yaml overrides development settings
- Longer timeout (15m) for 3-broker cluster startup

**What Changes in Production**:
- **Brokers**: 1 → 3 (high availability)
- **Controllers**: 1 → 3 (quorum for fault tolerance)
- **Partitions**: 3 → 6 (higher parallelism)
- **Replication Factor**: 1 → 3 (data durability)
- **min.insync.replicas**: 1 → 2 (at least once guarantee)
- **Storage**: 10Gi → 50Gi per broker
- **Resources**: Increased CPU/memory limits
- **Topic deletion**: Disabled (safety)

**Production Topics**:
Topics are recreated with production configuration:
- 6 partitions (double the parallelism)
- Replication factor 3 (survives 2 broker failures)
- min.insync.replicas 2 (stronger durability)

**Wait Time**:
```groovy
sh 'sleep 20'
```
- Longer pause for 3-broker cluster to elect leaders
- Ensures all brokers are in sync before topic creation

### 7. New Post Actions
```groovy
post {
    always {
        echo 'Pipeline completed.'
    }
    success {
        echo 'All deployments successful!'
    }
    failure {
        echo 'Pipeline failed! Check logs for details.'
    }
}
```

**Purpose**:
- Provides clear feedback in Jenkins UI
- `always`: Runs regardless of success/failure
- `success`/`failure`: Conditional messages
- Useful for Jenkins notifications (email, Slack, etc.)

## Deployment Flow

### Complete Pipeline Execution Order:
1. **Deploy Kafka Platform** (new)
   - Add Helm repo
   - Create namespace
   - Deploy Kafka
   - Wait for ready
2. **Create Kafka Topics** (new)
   - Wait 10s
   - Create 8 topics
   - Verify creation
3. **Build All**
   - Maven build all microservices
4. **Build Docker Images**
   - Build 9 service images
5. **Deploy All to Test**
   - Deploy umbrella chart with Kafka config
6. **Deploy All to Prod** (requires approval)
   - Deploy to production namespace
7. **Deploy Kafka to Production** (requires approval, new)
   - Upgrade to HA configuration

## Benefits of This Approach

### 1. Infrastructure-First Pattern
- Kafka deployed before apps
- Prevents startup failures
- Clear dependency order

### 2. Idempotency
- Can run pipeline multiple times safely
- `helm upgrade --install` updates if exists
- `--if-not-exists` for topics
- Namespace creation is idempotent

### 3. Environment Separation
- Same pipeline handles test and prod
- Different configurations via Helm values
- Gradual promotion (test → prod)

### 4. Auditability
- All Kafka operations logged in Jenkins
- Topic list printed after creation
- Clear success/failure messages

### 5. Flexibility
- Can deploy Kafka independently (via helm/kafka/Jenkinsfile)
- Can deploy apps without upgrading Kafka
- Separate approval gates for test and prod

## Kubernetes Resources Created

After pipeline execution, the following resources exist in the `kafka` namespace:

### Pods:
- `kafka-controller-0` (StatefulSet pod)
- Potentially `kafka-controller-1` and `kafka-controller-2` in production

### Services:
- `kafka` (ClusterIP): Internal access for microservices
- `kafka-controller-headless` (Headless): For StatefulSet pod discovery

### ConfigMaps:
- `kafka-scripts`: Kafka startup scripts
- `kafka-configuration`: Kafka server.properties

### Secrets:
- `kafka`: Contains sensitive configuration (if any)

### PersistentVolumeClaims:
- `data-kafka-controller-0` (10Gi or 50Gi)
- Additional PVCs for each broker in production

### StatefulSet:
- `kafka-controller`: Manages Kafka pods with stable identities

## Integration with Microservices

Microservices will receive Kafka configuration via Helm values:

```yaml
# In each microservice's ConfigMap (injected by umbrella chart)
spring:
  kafka:
    bootstrap-servers: kafka.kafka.svc.cluster.local:9092
```

This configuration will be added in subsequent steps when we update service ConfigMaps.

## Testing the Integration

### Verify Kafka Deployment:
```bash
# Check Kafka pods
kubectl get pods -n kafka

# Check Kafka service
kubectl get svc -n kafka

# View Kafka logs
kubectl logs -n kafka kafka-controller-0
```

### Verify Topics:
```bash
kubectl exec -n kafka kafka-controller-0 -- \
  kafka-topics.sh --list --bootstrap-server localhost:9092
```

Expected output:
```
account-created
account-updated
deposit-completed
withdrawal-completed
transfer-initiated
transfer-completed
transfer-failed
notification-event
```

### Test Topic Access:
```bash
# Produce a message
kubectl exec -n kafka kafka-controller-0 -- \
  kafka-console-producer.sh --bootstrap-server localhost:9092 \
  --topic notification-event

# Consume messages
kubectl exec -n kafka kafka-controller-0 -- \
  kafka-console-consumer.sh --bootstrap-server localhost:9092 \
  --topic notification-event --from-beginning
```

## Next Steps

With Kafka infrastructure deployed via Jenkins, the next steps are:

1. **Add Spring Kafka dependencies** to microservices (Step 3)
2. **Configure Kafka settings** in Helm ConfigMaps (Step 4)
3. **Implement Kafka producers** in services (Step 5)
4. **Implement Kafka consumer** in notifications-service (Step 6)
5. **Remove REST dependencies** from notifications-service (Step 7)

## Troubleshooting

### If Kafka doesn't start:
```bash
# Check pod events
kubectl describe pod -n kafka kafka-controller-0

# Check logs
kubectl logs -n kafka kafka-controller-0

# Common issues:
# - Insufficient resources (CPU/memory)
# - PersistentVolume not available
# - Network policies blocking communication
```

### If topic creation fails:
```bash
# Check if Kafka is accepting connections
kubectl exec -n kafka kafka-controller-0 -- \
  kafka-broker-api-versions.sh --bootstrap-server localhost:9092

# Manually create topic
kubectl exec -n kafka kafka-controller-0 -- \
  kafka-topics.sh --create --topic test \
  --bootstrap-server localhost:9092 \
  --partitions 1 --replication-factor 1
```

## Summary

Successfully updated the umbrella Jenkinsfile to:
- ✅ Deploy Kafka platform before microservices
- ✅ Create 8 predefined topics automatically
- ✅ Pass Kafka bootstrap server to all microservices
- ✅ Support test and production deployments
- ✅ Include production HA upgrade stage
- ✅ Add proper waiting and verification steps
- ✅ Make deployment fully idempotent
- ✅ Add post-build notifications

The Jenkins pipeline now orchestrates both infrastructure (Kafka) and applications (microservices) in a coordinated, reproducible manner.

