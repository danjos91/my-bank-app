# Observability Setup Guide

## Overview

The MyBank App now includes a comprehensive observability stack with:
- **Zipkin**: Distributed tracing
- **Prometheus**: Metrics collection
- **Grafana**: Metrics visualization
- **ELK Stack**: Centralized logging (Elasticsearch, Logstash, Kibana)

## Current Status

✅ **Deployed Components:**
- Zipkin: Running in `observability` namespace
- Prometheus: Running in `observability` namespace  
- Grafana: Running in `observability` namespace
- Alertmanager: Running in `observability` namespace

⚠️ **Pending:**
- ELK Stack: Elasticsearch deployment in progress (may take time)
- Microservices: Need rebuild with observability dependencies

## Quick Access

### Zipkin (Tracing)
```bash
kubectl port-forward -n observability svc/zipkin 9411:9411
# Open http://localhost:9411
```

### Grafana (Dashboards)
```bash
kubectl port-forward -n observability svc/kube-prometheus-stack-grafana 3000:80
# Open http://localhost:3000
# Login: admin / admin
```

### Prometheus (Query)
```bash
kubectl port-forward -n observability svc/kube-prometheus-stack-prometheus 9090:9090
# Open http://localhost:9090
```

### Kibana (Logs)
```bash
kubectl port-forward -n observability svc/elasticsearch-elasticsearch-simple-kibana 5601:5601
# Open http://localhost:5601
```

## Next Steps

### 1. Rebuild Microservices with Observability

The current running services are using old images without observability dependencies. To enable full observability:

```bash
# Build with observability dependencies
mvn clean package -DskipTests

# Rebuild Docker images
./start.sh  # This will rebuild images and redeploy
```

### 2. Configure Prometheus ServiceMonitor

To automatically scrape metrics from microservices, create ServiceMonitor resources:

```bash
# Example ServiceMonitor for accounts-service
kubectl apply -f - <<EOF
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: accounts-service
  namespace: default
spec:
  selector:
    matchLabels:
      app.kubernetes.io/name: accounts-service
  endpoints:
  - port: http
    path: /actuator/prometheus
EOF
```

### 3. Import Grafana Dashboards

```bash
./setup-grafana-dashboards.sh
```

Or manually import from `helm/grafana/dashboards/`:
- `spring-boot-metrics.json`
- `jvm-metrics.json`
- `business-metrics.json`
- `system-metrics.json`

### 4. Configure Prometheus Alerts

```bash
./setup-prometheus-alerts.sh
```

Alert rules are in `helm/prometheus/alerts/`:
- `application-alerts.yaml`: HTTP errors, response times, memory/CPU
- `business-alerts.yaml`: Transaction failures, transfer errors

### 5. Verify Integration

```bash
./verify-observability.sh
```

## Configuration Files

### Microservice Configuration

All microservices have been configured with:
- **Zipkin Tracing**: `spring.zipkin.base-url=http://zipkin.observability.svc.cluster.local:9411`
- **Prometheus Metrics**: `management.endpoints.web.exposure.include=prometheus`
- **JSON Logging**: Logback configured for ELK ingestion
- **Kafka Logging**: Logs sent to `application-logs` topic

### Environment Variables

Helm charts inject observability configuration via:
- `SPRING_ZIPKIN_BASE_URL`
- `MANAGEMENT_TRACING_SAMPLING_PROBABILITY`
- `MANAGEMENT_METRICS_EXPORT_PROMETHEUS_ENABLED`
- `LOGGING_KAFKA_BOOTSTRAP_SERVERS`

## Custom Metrics

The following custom metrics are implemented:

### Cash Service
- `cash.deposits.total`
- `cash.withdrawals.total`
- `cash.deposits.failed.total`
- `cash.withdrawals.failed.total`
- `cash.deposits.duration`
- `cash.withdrawals.duration`

### Transfer Service
- `transfer.initiated.total`
- `transfer.completed.total`
- `transfer.failed.total`
- `transfer.cancelled.total`
- `transfer.duration`

### Accounts Service
- `accounts.created.total`
- `accounts.deleted.total`
- `accounts.balance.added.total`
- `accounts.balance.subtracted.total`
- `accounts.active.count` (gauge)

## Troubleshooting

### Prometheus endpoint not available
- Services need rebuild with `micrometer-registry-prometheus` dependency
- Check actuator endpoints: `curl http://service:port/actuator`

### No traces in Zipkin
- Verify `spring.zipkin.base-url` is set correctly
- Check `management.tracing.sampling.probability` (should be > 0)
- Ensure services are making HTTP calls (traces only appear on request)

### No logs in Kibana
- Verify Kafka topic `application-logs` exists
- Check Logstash is configured to consume from Kafka
- Verify logback-spring.xml has Kafka appender enabled

### Grafana dashboards empty
- Ensure Prometheus is scraping microservice endpoints
- Check ServiceMonitor resources are created
- Verify metrics are being exposed at `/actuator/prometheus`

## CI/CD Integration

The observability stack is integrated into Jenkins pipelines:
- `helm/kafka/Jenkinsfile`: Deploys observability components
- Root `Jenkinsfile`: Includes observability deployment stages

## Documentation

See `README.md` for detailed observability documentation and access instructions.

