# Observability Implementation Guide

This document describes the complete observability implementation in the MyBank application, including distributed tracing, metrics collection, logging, and monitoring.

## Overview

The application implements a full observability stack with the following components:

1. **Distributed Tracing** - Zipkin with automatic trace propagation
2. **Metrics Collection** - Prometheus with alerting
3. **Centralized Logging** - ELK Stack (Elasticsearch, Logstash, Kibana) with Kafka
4. **Monitoring Dashboards** - Grafana with custom dashboards

## 1. Distributed Tracing with Zipkin

### Implementation

All microservices automatically send traces to Zipkin using:
- **Micrometer Tracing Bridge** (`micrometer-tracing-bridge-brave`)
- **Zipkin Reporter** (`zipkin-reporter-brave`)

### Trace Propagation

#### HTTP Requests
- **Front UI**: Generates new trace IDs for incoming user requests
- **Microservices**: Extract trace/span IDs from incoming HTTP headers
- **RestTemplate**: Automatically propagates trace context using `RestTemplateBuilder`
- Headers used: `X-B3-TraceId`, `X-B3-SpanId`, `X-B3-ParentSpanId`, `X-B3-Sampled`

#### Database Operations
- JPA/Hibernate queries are automatically instrumented
- Each database operation creates a child span within the current trace
- Connection pooling operations are also traced

#### Kafka Operations
- Kafka producers automatically add trace context to message headers
- Kafka consumers extract trace context and continue the trace
- Message processing creates child spans

### Configuration

All services have the following configuration in `application.yml`:

```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # 100% sampling for development
  zipkin:
    tracing:
      endpoint: ${MANAGEMENT_ZIPKIN_TRACING_ENDPOINT:http://zipkin.observability.svc.cluster.local:9411/api/v2/spans}
```

### Accessing Zipkin

```bash
# Port-forward to access Zipkin UI
kubectl -n observability port-forward svc/zipkin 9411:9411

# Open browser
http://localhost:9411
```

## 2. Metrics Collection with Prometheus

### Exposed Metrics

Each microservice exposes metrics at `/actuator/prometheus`:

#### HTTP Metrics
- `http_server_requests_seconds_count` - Total request count
- `http_server_requests_seconds_sum` - Total request duration
- `http_server_requests_seconds_bucket` - Request duration histogram
- Labels: `method`, `uri`, `status`, `exception`

#### JVM Metrics
- `jvm_memory_used_bytes` - Memory usage by area (heap/non-heap)
- `jvm_memory_max_bytes` - Maximum memory
- `jvm_gc_pause_seconds` - Garbage collection pause times
- `jvm_threads_live_threads` - Active thread count
- `jvm_classes_loaded_classes` - Loaded class count

#### Custom Metrics
- `spring_kafka_producer_*` - Kafka producer metrics
- `spring_kafka_consumer_*` - Kafka consumer metrics
- `hikaricp_connections_*` - Database connection pool metrics

### Prometheus Configuration

Services are automatically discovered via Kubernetes pod annotations:

```yaml
annotations:
  prometheus.io/scrape: "true"
  prometheus.io/path: "/actuator/prometheus"
  prometheus.io/port: "8081"
```

### Alert Rules

Three critical alerts are configured:

#### 1. HighErrorRate
- **Condition**: More than 10% of requests return 5xx errors
- **Duration**: 2 minutes
- **Severity**: Critical
- **Expression**: `rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.1`

#### 2. HighJvmMemory
- **Condition**: Heap usage above 80%
- **Duration**: 2 minutes
- **Severity**: Warning
- **Expression**: `(jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) > 0.8`

#### 3. SlowHttpLatency
- **Condition**: P95 latency above 2 seconds
- **Duration**: 2 minutes
- **Severity**: Warning
- **Expression**: `histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])) > 2`

### Accessing Prometheus

```bash
# Port-forward to access Prometheus UI
kubectl -n observability port-forward svc/prometheus-server 9090:80

# Open browser
http://localhost:9090
```

## 3. Centralized Logging with ELK Stack

### Log Flow

```
Microservices → Logback → Kafka (topic: service-logs) → Logstash → Elasticsearch → Kibana
```

### Logback Configuration

All services use `logback-spring.xml` with two appenders:

#### Console Appender
Shows logs with trace context:
```
2024-12-15T10:30:45.123 INFO  [http-nio-8081-exec-1] [a1b2c3d4e5f6g7h8,i9j0k1l2m3n4o5p6] c.e.service.AccountService - Account created
                                                        ^traceId         ^spanId
```

#### Kafka Appender
Sends structured logs to Kafka topic `service-logs`:
```xml
<appender name="KAFKA" class="com.github.danielwegener.logback.kafka.KafkaAppender">
    <topic>service-logs</topic>
    <producerConfig>bootstrap.servers=kafka.kafka.svc.cluster.local:9092</producerConfig>
    <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
</appender>
```

### Log Fields

Each log entry includes:
- `@timestamp` - Log timestamp
- `message` - Log message
- `logger_name` - Logger name
- `level` - Log level (INFO, WARN, ERROR)
- `thread_name` - Thread name
- `traceId` - Distributed trace ID
- `spanId` - Current span ID
- `application_name` - Service name
- `stack_trace` - Exception stack trace (if present)

### Logstash Pipeline

Logstash consumes from Kafka and indexes to Elasticsearch:

```
input {
  kafka {
    bootstrap_servers => "kafka.kafka.svc.cluster.local:9092"
    topics => ["service-logs"]
    group_id => "logstash-service-logs"
  }
}
filter {
  json { source => "message" }
}
output {
  elasticsearch {
    hosts => ["http://elasticsearch-master.observability.svc.cluster.local:9200"]
    index => "service-logs-%{+YYYY.MM.dd}"
  }
}
```

### Accessing Kibana

```bash
# Port-forward to access Kibana
kubectl -n observability port-forward svc/kibana 5601:5601

# Open browser
http://localhost:5601
```

#### Creating Index Pattern
1. Go to Management → Stack Management → Index Patterns
2. Create pattern: `service-logs-*`
3. Select time field: `@timestamp`
4. Click "Create index pattern"

#### Searching Logs by Trace ID
In Kibana Discover:
```
traceId: "a1b2c3d4e5f6g7h8"
```

This shows all logs from all services involved in that trace.

## 4. Monitoring Dashboards in Grafana

### Pre-configured Dashboards

#### Spring Boot Overview Dashboard
- HTTP 5xx error rate
- HTTP P95 latency
- Active requests
- Request rate by endpoint

#### JVM Metrics Dashboard
- Heap memory usage (used/max)
- GC pause time (P95)
- Thread count
- Class loader metrics

### Accessing Grafana

```bash
# Port-forward to access Grafana
kubectl -n observability port-forward svc/grafana 3000:80

# Open browser
http://localhost:3000
# Login: admin / admin123
```

### Viewing Alerts

1. Go to Alerting → Alert Rules
2. View active alerts and their states
3. Configure notification channels (optional)

## 5. Deployment

### Deploy Observability Stack

```bash
# Create namespace
kubectl create namespace observability

# Deploy Zipkin
helm upgrade --install zipkin oci://registry-1.docker.io/bitnamicharts/zipkin \
  --version 5.0.4 -n observability -f helm/observability/values-zipkin.yaml

# Deploy Prometheus with AlertManager
helm upgrade --install prometheus oci://registry-1.docker.io/bitnamicharts/prometheus \
  --version 24.6.0 -n observability -f helm/observability/values-prometheus.yaml

# Deploy Elasticsearch
helm upgrade --install elasticsearch oci://registry-1.docker.io/bitnamicharts/elasticsearch \
  --version 21.2.8 -n observability -f helm/observability/values-elasticsearch.yaml

# Deploy Logstash
helm upgrade --install logstash oci://registry-1.docker.io/bitnamicharts/logstash \
  --version 8.4.2 -n observability -f helm/observability/values-logstash.yaml

# Deploy Kibana
helm upgrade --install kibana oci://registry-1.docker.io/bitnamicharts/kibana \
  --version 16.5.5 -n observability -f helm/observability/values-kibana.yaml

# Deploy Grafana
helm upgrade --install grafana oci://registry-1.docker.io/bitnamicharts/grafana \
  --version 8.5.8 -n observability -f helm/observability/values-grafana.yaml
```

### Verify Deployment

```bash
# Check all pods are running
kubectl get pods -n observability

# Expected output:
# NAME                                    READY   STATUS    RESTARTS   AGE
# zipkin-xxxxx                            1/1     Running   0          5m
# prometheus-server-xxxxx                 1/1     Running   0          5m
# prometheus-alertmanager-xxxxx           1/1     Running   0          5m
# elasticsearch-master-0                  1/1     Running   0          5m
# logstash-xxxxx                          1/1     Running   0          5m
# kibana-xxxxx                            1/1     Running   0          5m
# grafana-xxxxx                           1/1     Running   0          5m
```

## 6. Testing the Observability Stack

### 1. Generate Some Traffic

```bash
# Access the application
kubectl port-forward svc/front-ui 8086:8086

# Open browser and perform some operations:
# - Login
# - Create accounts
# - Make transfers
# - Generate some errors (try invalid operations)
```

### 2. View Traces in Zipkin

1. Open Zipkin: http://localhost:9411
2. Click "Find Traces"
3. Select a trace to see the full request flow across services
4. Observe:
   - HTTP calls between services
   - Database queries
   - Kafka message sends/receives
   - Timing of each operation

### 3. View Metrics in Prometheus

1. Open Prometheus: http://localhost:9090
2. Try these queries:
   ```promql
   # HTTP request rate by service
   rate(http_server_requests_seconds_count[5m])
   
   # P95 latency by endpoint
   histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))
   
   # Memory usage
   jvm_memory_used_bytes{area="heap"}
   
   # Error rate
   rate(http_server_requests_seconds_count{status=~"5.."}[5m])
   ```

### 4. View Logs in Kibana

1. Open Kibana: http://localhost:5601
2. Go to Analytics → Discover
3. Select index pattern `service-logs-*`
4. Search examples:
   ```
   # All logs from accounts-service
   application_name:"accounts-service"
   
   # All ERROR logs
   level:"ERROR"
   
   # Logs from specific trace
   traceId:"a1b2c3d4e5f6g7h8"
   
   # Logs containing "transfer"
   message:*transfer*
   ```

### 5. View Dashboards in Grafana

1. Open Grafana: http://localhost:3000
2. Go to Dashboards
3. Open "Spring Boot Overview"
4. Observe metrics updating in real-time

### 6. Test Alerts

#### Trigger High Error Rate Alert
```bash
# Make requests that cause 500 errors
curl http://localhost:8086/invalid-endpoint

# Check Prometheus alerts
# Open: http://localhost:9090/alerts
# You should see HighErrorRate firing after 2 minutes
```

#### Trigger High Memory Alert
```bash
# Simulate memory pressure by creating many large objects
# (This would require custom endpoint in the services)
```

## 7. Troubleshooting

### Traces Not Appearing in Zipkin

1. Check Zipkin endpoint configuration:
   ```bash
   kubectl get pods -n default -o yaml | grep ZIPKIN
   ```

2. Check service can reach Zipkin:
   ```bash
   kubectl exec -it <pod-name> -- curl -v http://zipkin.observability.svc.cluster.local:9411/health
   ```

### Metrics Not Being Scraped

1. Check Prometheus targets:
   ```bash
   # Open Prometheus UI
   # Go to Status → Targets
   # Look for pods with state "DOWN"
   ```

2. Verify pod annotations:
   ```bash
   kubectl get pods -o yaml | grep prometheus.io
   ```

### Logs Not Appearing in Kibana

1. Check Kafka topic has messages:
   ```bash
   kubectl exec -it -n kafka kafka-0 -- kafka-console-consumer.sh \
     --bootstrap-server localhost:9092 \
     --topic service-logs \
     --max-messages 5
   ```

2. Check Logstash is consuming:
   ```bash
   kubectl logs -n observability <logstash-pod>
   ```

3. Verify Elasticsearch indices:
   ```bash
   kubectl exec -it -n observability elasticsearch-master-0 -- \
     curl localhost:9200/_cat/indices?v
   ```

## 8. Production Considerations

### Sampling Rate

For production, reduce trace sampling:
```yaml
management:
  tracing:
    sampling:
      probability: 0.1  # 10% sampling
```

### Log Retention

Configure Elasticsearch index lifecycle:
- Delete indices older than 30 days
- Move older data to warm/cold storage

### Resource Limits

Adjust resource limits based on load:
- Increase Prometheus retention and storage
- Scale Elasticsearch cluster
- Increase Logstash replicas for high log volume

### Alert Notification

Configure AlertManager to send notifications:
- Email
- Slack
- PagerDuty
- Webhook

## Summary

This observability implementation provides:

✅ **Distributed Tracing**: Full request flow visibility across all services, databases, and Kafka
✅ **Metrics Collection**: Comprehensive HTTP, JVM, and custom metrics with alerting
✅ **Centralized Logging**: Structured logs with trace correlation in Elasticsearch
✅ **Monitoring Dashboards**: Pre-configured Grafana dashboards for key metrics
✅ **Alerting**: Automated alerts for critical conditions (errors, memory, latency)

All features are production-ready and follow industry best practices.

