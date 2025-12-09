# Step 1: Kafka Jenkinsfile and Helm Configuration - Detailed Summary

## Overview
Created the infrastructure configuration files for deploying Apache Kafka to Kubernetes using Helm and Jenkins CI/CD.

## Files Created

### 1. helm/kafka/Jenkinsfile
**Purpose**: Jenkins pipeline for automated deployment and management of Kafka platform in Kubernetes.

**Key Features**:

#### Stage 1: Add Bitnami Helm Repository
- Adds the official Bitnami Helm chart repository
- Updates the repository cache to get latest chart versions
- Bitnami Kafka chart is the most popular and well-maintained Kafka Helm chart

#### Stage 2: Create Kafka Namespace
- Creates dedicated `kafka` namespace for isolation
- Uses `--dry-run=client -o yaml | kubectl apply -f -` pattern for idempotency
- Separating Kafka in its own namespace provides better resource management and security

#### Stage 3: Deploy Kafka to Test
- Deploys Kafka using Bitnami chart version 30.1.5
- **KRaft mode enabled** (Kafka without Zookeeper) - modern Kafka architecture
- Test environment configuration:
  - 1 broker replica (lightweight for testing)
  - 1 controller replica
  - 3 partitions per topic
  - Replication factor: 1 (acceptable for test)
  - Log retention: 168 hours (7 days)
  - PLAINTEXT protocol (no encryption for simplicity in test)
- `--wait --timeout 10m`: Ensures deployment completes before proceeding

#### Stage 4: Wait for Kafka Ready
- Uses `kubectl wait` to ensure all Kafka pods are in Ready state
- 5-minute timeout to handle slow startups
- Prevents race conditions with subsequent stages

#### Stage 5: Create Kafka Topics
- Creates 8 predefined topics for the banking application:
  1. **account-created**: Published when new account is created
  2. **account-updated**: Published when account is modified
  3. **deposit-completed**: Published by cash-service after successful deposit
  4. **withdrawal-completed**: Published by cash-service after successful withdrawal
  5. **transfer-initiated**: Published when transfer starts
  6. **transfer-completed**: Published when transfer succeeds
  7. **transfer-failed**: Published when transfer fails
  8. **notification-event**: Generic topic for other notification events

- Topic configuration:
  - 3 partitions: Allows parallel processing by multiple consumers
  - Replication factor 1: Test environment setting
  - Retention: 7 days (604800000 ms)
  - min.insync.replicas: 1 (matches replication factor)
  - `--if-not-exists`: Makes creation idempotent
  - `|| true`: Prevents pipeline failure if topic already exists

#### Stage 6: Verify Topics
- Lists all topics to confirm successful creation
- Provides visibility in Jenkins console output
- Useful for debugging and validation

#### Stage 7: Deploy Kafka to Production
- Requires manual approval via Jenkins input step
- **Production configuration** (much more robust):
  - **3 broker replicas**: High availability, survives 1-2 broker failures
  - **3 controller replicas**: KRaft quorum for fault tolerance
  - **6 partitions per topic**: Better parallelism and throughput
  - **Replication factor 3**: Every message stored on 3 brokers
  - **min.insync.replicas: 2**: At least 2 brokers must acknowledge writes
  - **Persistence enabled**: 50Gi per broker
  - **Resource limits**: 2Gi-4Gi RAM, 1-2 CPU cores per broker
  - **deleteTopicEnable: false**: Prevents accidental topic deletion
  - **autoCreateTopicsEnable: false**: Topics must be explicitly created
- Creates production topics with same structure but higher replication

**Technical Decisions**:

1. **KRaft vs Zookeeper**: 
   - Chose KRaft (Kafka 3.x+) to eliminate Zookeeper dependency
   - Simpler architecture, better performance, industry direction

2. **Topic Naming Strategy**:
   - Past tense verbs (e.g., "completed", "created") indicate events that happened
   - Follows event-driven architecture best practices
   - Clear semantic meaning for developers

3. **Partition Strategy**:
   - 3 partitions (test) / 6 partitions (prod)
   - Allows horizontal scaling of consumers
   - Multiple partition groups can process messages in parallel

4. **Replication Strategy**:
   - Test: RF=1, min.insync=1 (fast, but data loss possible)
   - Prod: RF=3, min.insync=2 (guarantees "at least once" delivery)
   - Aligns with user requirement for "at least once" message delivery

5. **Retention Policy**:
   - 7 days retention balances storage costs with audit requirements
   - Can be adjusted per topic if needed

### 2. helm/kafka/values.yaml
**Purpose**: Helm values file for development/test Kafka deployment.

**Structure**:
- **Global settings**: Storage class configuration
- **Kafka broker configuration**: Single broker, KRaft mode
- **Listeners**: PLAINTEXT protocol for all communication
- **Topic defaults**: Auto-create disabled, delete enabled
- **Persistence**: 10Gi storage for development
- **Resources**: Minimal (512Mi-1Gi RAM, 250m-500m CPU)
- **Topics definition**: Documents all 8 topics with configuration

**Key Configuration Choices**:

1. **autoCreateTopicsEnable: false**:
   - Topics must be explicitly created via Jenkins
   - Prevents accidental topic creation from typos
   - Better governance and control

2. **deleteTopicEnable: true** (dev only):
   - Allows cleanup during development
   - Disabled in production

3. **compressionType: producer**:
   - Producer decides compression algorithm
   - Reduces network bandwidth and storage

4. **maxRequestSize & messageMaxBytes: 1MB**:
   - Reasonable limit for banking notification events
   - Prevents oversized messages from impacting cluster

### 3. helm/kafka/values-prod.yaml
**Purpose**: Production overrides for Kafka deployment.

**Production Hardening**:
1. **High Availability**: 3 brokers + 3 controllers
2. **Data Durability**: RF=3, min.insync=2
3. **Resource Scaling**: 2-4Gi RAM, 1-2 CPU cores
4. **Storage**: 50Gi per broker (total 150Gi)
5. **Safety**: deleteTopicEnable=false
6. **Performance**: 6 partitions for better parallelism

**Deployment Command**:
```bash
# Development
helm install kafka bitnami/kafka -f values.yaml --namespace kafka

# Production
helm install kafka bitnami/kafka -f values.yaml -f values-prod.yaml --namespace kafka
```

## Integration with CI/CD

### Jenkins Setup:
1. Create new Jenkins pipeline job: "kafka-deployment"
2. Set pipeline script path: `helm/kafka/Jenkinsfile`
3. Configure git repository credentials
4. Run pipeline to deploy Kafka

### Kubernetes Service DNS:
Once deployed, Kafka will be accessible at:
- **Internal**: `kafka.kafka.svc.cluster.local:9092`
- **From microservices**: Spring Boot will use this bootstrap server URL

## Testing the Deployment

### Verify Kafka is running:
```bash
kubectl get pods -n kafka
kubectl get svc -n kafka
```

### List topics:
```bash
kubectl exec -n kafka kafka-controller-0 -- \
  kafka-topics.sh --list --bootstrap-server localhost:9092
```

### Test producer:
```bash
kubectl exec -n kafka kafka-controller-0 -- \
  kafka-console-producer.sh --bootstrap-server localhost:9092 --topic test-topic
```

### Test consumer:
```bash
kubectl exec -n kafka kafka-controller-0 -- \
  kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic test-topic --from-beginning
```

## Next Steps

This completes the Kafka infrastructure setup. Next steps:
1. Update umbrella Jenkinsfile to include Kafka deployment
2. Add Spring Kafka dependencies to microservices
3. Implement Kafka producers in services (Accounts, Cash, Transfer)
4. Implement Kafka consumer in Notifications service
5. Configure Kafka connection in Helm ConfigMaps

## Security Considerations (Future Enhancements)

Current configuration uses PLAINTEXT for simplicity. For production, consider:
1. **SASL/SCRAM authentication**: User/password authentication
2. **TLS encryption**: Encrypt data in transit
3. **ACLs**: Fine-grained topic access control
4. **Network policies**: Restrict pod-to-pod communication
5. **Secrets management**: Store credentials in Kubernetes Secrets

## At Least Once Delivery Guarantee

The configuration ensures "at least once" delivery through:
1. **Producer**: `acks=all` (configured in Spring Boot, not Helm)
2. **Broker**: `min.insync.replicas=2` (production)
3. **Consumer**: Manual commit mode (to be configured in Spring Boot)
4. **Replication**: RF=3 ensures data survives broker failures

## Summary

Successfully created Kafka infrastructure configuration with:
- ✅ Jenkins pipeline for automated deployment
- ✅ Helm values for development and production
- ✅ 8 predefined topics for banking events
- ✅ KRaft mode (no Zookeeper dependency)
- ✅ Separation of test and production configurations
- ✅ At least once delivery guarantee setup
- ✅ High availability for production (3 brokers)

The foundation is now ready for microservices integration.

