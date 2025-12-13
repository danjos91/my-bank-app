package io.github.danjos.mybankapp.accounts.config;

import io.github.danjos.mybankapp.accounts.kafka.NotificationEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka producer configuration for accounts-service.
 * Implements "at least once" delivery semantics with acks=all and retries.
 */
@Configuration
public class KafkaProducerConfig {
    
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    /**
     * Producer factory for creating Kafka producers.
     */
    @Bean
    public ProducerFactory<String, NotificationEvent> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        
        // Kafka broker configuration
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        
        // Serialization configuration
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        // "At least once" delivery configuration
        config.put(ProducerConfig.ACKS_CONFIG, "all"); // Wait for all in-sync replicas
        config.put(ProducerConfig.RETRIES_CONFIG, 3); // Retry failed sends
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true); // Prevent duplicates from retries
        
        // Performance tuning
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384); // 16KB batches
        config.put(ProducerConfig.LINGER_MS_CONFIG, 10); // Wait 10ms to batch messages
        config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy"); // Compress messages
        config.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432); // 32MB buffer
        
        // Timeout configuration
        config.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 60000); // 60 seconds
        config.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000); // 30 seconds
        config.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000); // 120 seconds
        
        return new DefaultKafkaProducerFactory<>(config);
    }
    
    /**
     * Kafka template for sending messages.
     */
    @Bean
    public KafkaTemplate<String, NotificationEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}

