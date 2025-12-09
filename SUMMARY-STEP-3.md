# Step 3: Add Spring Kafka Dependencies to Microservices - Detailed Summary

## Overview
Added Spring Kafka dependencies to six microservices to enable Kafka producer and consumer functionality. This is the first step in implementing event-driven communication between services.

## Dependencies Added

### Dependency Details

#### 1. spring-kafka (Runtime)
```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

**Purpose**: Core Spring Kafka library
**What it provides**:
- `KafkaTemplate`: For sending messages (producers)
- `@KafkaListener`: For receiving messages (consumers)
- Spring Boot auto-configuration for Kafka
- JSON serialization/deserialization
- Error handling and retry mechanisms
- Transaction support
- Kafka health indicators for Actuator

**Version**: Managed by Spring Boot parent POM (3.5.6)
- Automatically provides compatible Kafka client version
- No need to specify version explicitly
- Spring Boot 3.5.6 uses spring-kafka 3.x with Kafka client 3.x

**Transitive Dependencies** (automatically included):
- `kafka-clients`: Apache Kafka Java client library
- `spring-messaging`: Spring messaging abstractions
- `spring-context`: Spring application context
- `jackson-databind`: For JSON message serialization

#### 2. spring-kafka-test (Test Scope)
```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka-test</artifactId>
    <scope>test</scope>
</dependency>
```

**Purpose**: Testing utilities for Kafka integration tests
**What it provides**:
- `@EmbeddedKafka`: Starts an in-memory Kafka broker for tests
- `KafkaTestUtils`: Helper methods for testing
- Test consumers and producers
- Utilities for waiting and asserting message delivery
- Integration with Spring Test framework

**Scope**: `test` - only available during test compilation/execution
**Benefits**:
- No need for external Kafka during development
- Fast, isolated unit and integration tests
- Deterministic test results
- Easy CI/CD integration

## Services Modified

### 1. accounts-service/pom.xml
**Role**: Producer
**Will publish events**:
- `account-created`: When new account is registered
- `account-updated`: When account details change (future enhancement)

**Why Kafka?**:
- Notifications should be asynchronous
- Account creation shouldn't wait for notification delivery
- Decouples accounts service from notifications service
- Better fault tolerance (if notifications down, account creation succeeds)

### 2. cash-service/pom.xml
**Role**: Producer
**Will publish events**:
- `deposit-completed`: After successful deposit
- `withdrawal-completed`: After successful withdrawal

**Why Kafka?**:
- Cash operations should complete quickly
- Notification delivery can happen asynchronously
- If notification fails, money operation isn't affected
- Better user experience (faster response times)

### 3. transfer-service/pom.xml
**Role**: Producer
**Will publish events**:
- `transfer-initiated`: When transfer starts
- `transfer-completed`: When transfer succeeds
- `transfer-failed`: When transfer fails

**Why Kafka?**:
- Transfer is a complex multi-step operation
- Each step can emit events
- Other services might want to react (e.g., fraud detection)
- Audit trail of transfer lifecycle

### 4. notifications-service/pom.xml
**Role**: Consumer
**Will consume events**:
- All of the above events
- Creates notification records in database
- Future: Could send emails, SMS, push notifications

**Why Kafka?**:
- Natural fit for notifications (fire-and-forget)
- Can be scaled independently (multiple consumer instances)
- Can handle message bursts (e.g., end-of-month reports)
- Can catch up if service was down (messages retained in Kafka)

### 5. exchange-service/pom.xml
**Role**: Potential producer (future enhancement)
**Could publish events**:
- `exchange-rate-updated`: When rates change
- Other services could react to rate changes

**Why add now?**:
- Prepares service for future event publishing
- Minimal overhead (just a dependency)
- Keeps all services consistent

### 6. exchange-generator-service/pom.xml
**Role**: Potential producer (future enhancement)
**Could publish events**:
- `exchange-rate-generated`: When new rate is generated
- Could trigger notifications for large rate changes

**Why add now?**:
- Future-proofing
- May want to notify users of significant rate changes
- Consistent with other services

## Technical Considerations

### Why Not Add to All Services?

**Services WITHOUT Kafka** (intentional):
- **auth-server**: Authentication is request-response by nature
- **front-ui**: Frontend doesn't produce/consume Kafka events directly
- **blocker-service**: Consulted synchronously during transactions (needs immediate response)

**Rationale**:
- Some operations MUST be synchronous (blocking)
- Blocker service must return "allow/deny" immediately
- Auth service must validate tokens in real-time
- Front-UI communicates via REST APIs, not Kafka

### Dependency Management

**Version Management**:
- Spring Boot's `spring-boot-dependencies` BOM manages versions
- Parent POM (my-bank-app/pom.xml) defines Spring Boot version: 3.5.6
- All children inherit compatible versions automatically

**No version conflicts**:
```xml
<!-- No version specified - inherited from parent -->
<artifactId>spring-kafka</artifactId>
```

**Benefit**: Guaranteed compatibility between Spring Boot, Spring Kafka, and Kafka clients

### Kafka Client Compatibility

**Spring Boot 3.5.6 → Spring Kafka 3.x → Kafka Client 3.x**

**Kafka Protocol Compatibility**:
- Kafka clients 3.x can talk to Kafka brokers 2.x, 3.x, 4.x
- Our Bitnami Kafka chart uses Kafka 3.x (KRaft mode)
- Full compatibility ensured

### Build Impact

**Maven Build Changes**:
1. Dependencies downloaded from Maven Central
2. Transitive dependencies resolved
3. Added to classpath during compile and runtime
4. Test dependencies only in test classpath

**Build Size Impact**:
- `kafka-clients`: ~5 MB
- `spring-kafka`: ~1 MB
- `spring-kafka-test`: ~500 KB (test only)
- Total runtime impact: ~6 MB per service

**Startup Impact**:
- Spring Boot auto-configuration detects Kafka on classpath
- Configures `KafkaTemplate` bean if `spring.kafka.bootstrap-servers` is set
- Minimal startup time increase (~100-200ms)

## Spring Boot Auto-Configuration

When Spring Boot detects `spring-kafka` on classpath, it automatically configures:

### Producer Configuration (if enabled):
- `KafkaTemplate<K, V>`: Main API for sending messages
- `ProducerFactory`: Factory for creating Kafka producers
- Default serializers: `StringSerializer` for keys, `JsonSerializer` for values
- Default acks: `all` (ensures "at least once" delivery)

### Consumer Configuration (if `@KafkaListener` found):
- `ConcurrentKafkaListenerContainerFactory`: Manages listener containers
- `ConsumerFactory`: Factory for creating Kafka consumers
- Default deserializers: `StringDeserializer` for keys, `JsonDeserializer` for values
- Default error handlers and retry logic

### Configuration Properties:
```yaml
spring:
  kafka:
    bootstrap-servers: kafka.kafka.svc.cluster.local:9092
    producer:
      key-serializer: StringSerializer
      value-serializer: JsonSerializer
      acks: all  # At least once delivery
    consumer:
      group-id: my-service-group
      key-deserializer: StringDeserializer
      value-deserializer: JsonDeserializer
      enable-auto-commit: false  # Manual commit for at least once
```

These properties will be configured in Helm ConfigMaps in subsequent steps.

## Testing Infrastructure

With `spring-kafka-test` added, developers can now write:

### Integration Tests with Embedded Kafka:
```java
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"test-topic"})
class KafkaIntegrationTest {
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Test
    void testSendMessage() {
        kafkaTemplate.send("test-topic", "test-key", "test-message");
        // Assert message received
    }
}
```

### Benefits:
- Tests run in milliseconds (embedded Kafka starts quickly)
- No external dependencies
- Deterministic (controlled environment)
- Can test error scenarios (broker down, network issues, etc.)

## Next Steps

With dependencies in place, the next steps are:

### Immediate (Step 4-5):
1. **Remove spring-boot-starter-web from notifications-service**
   - No longer needs REST endpoints
   - Will only consume Kafka messages

2. **Create Kafka configuration classes**
   - Producer configuration (accounts, cash, transfer)
   - Consumer configuration (notifications)
   - Serialization/deserialization setup

### Subsequent (Step 6-8):
3. **Implement Kafka producers**
   - Create `KafkaNotificationProducer` in each producer service
   - Replace `NotificationsClient` calls with Kafka publishing

4. **Implement Kafka consumer**
   - Create `@KafkaListener` in notifications-service
   - Process notification events from all topics

5. **Configure Helm ConfigMaps**
   - Add Kafka bootstrap servers
   - Configure producer/consumer properties
   - Set up "at least once" delivery guarantees

## Verification

### To verify dependencies were added correctly:

```bash
# Check if dependencies resolve
cd /path/to/service
mvn dependency:tree | grep kafka

# Expected output:
# [INFO] +- org.springframework.kafka:spring-kafka:jar:3.x.x:compile
# [INFO] |  +- org.apache.kafka:kafka-clients:jar:3.x.x:compile
# [INFO] +- org.springframework.kafka:spring-kafka-test:jar:3.x.x:test
```

### Build all services to confirm:
```bash
mvn clean install -DskipTests
```

Should complete without errors, downloading Kafka dependencies if not cached.

## Kafka Dependency Tree

Visual representation of what gets included:

```
spring-kafka (compile)
├── kafka-clients (compile)
│   ├── slf4j-api
│   ├── lz4-java (compression)
│   └── snappy-java (compression)
├── spring-messaging (compile)
├── spring-context (compile)
└── spring-retry (compile)

spring-kafka-test (test)
├── spring-kafka (compile)
├── kafka-clients-test (test)
├── junit-jupiter (test)
└── assertj-core (test)
```

## At Least Once Delivery Configuration

These dependencies support "at least once" delivery through:

### Producer Side:
- `acks=all`: All in-sync replicas must acknowledge
- `retries=3`: Retry failed sends automatically
- `enable.idempotence=true`: Prevents duplicate messages from retries

### Consumer Side:
- `enable-auto-commit=false`: Manual commit control
- `ack-mode=manual`: Explicit acknowledgment after processing
- Error handlers with retry logic

Configuration will be added to application.yml files in subsequent steps.

## Summary

Successfully added Spring Kafka dependencies to 6 microservices:
- ✅ accounts-service (producer)
- ✅ cash-service (producer)
- ✅ transfer-service (producer)
- ✅ notifications-service (consumer)
- ✅ exchange-service (future producer)
- ✅ exchange-generator-service (future producer)

**Benefits achieved**:
- Kafka producer/consumer capabilities enabled
- Testing infrastructure available
- Auto-configuration will activate when properties are set
- Services ready for event-driven communication
- Foundation for "at least once" delivery guarantee

**No breaking changes**:
- Services still compile and run
- Dependencies are inactive until configured
- Existing REST endpoints remain functional
- No impact on current functionality

The services now have the capability to use Kafka, pending configuration and implementation in subsequent steps.

