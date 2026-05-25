package com.miniuber.core.auth.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration for Auth Service
 * Publishes user registration events for notifications
 * 
 * Topics:
 * - user.registered: Published when a new rider registers
 * - driver.registered: Published when a new driver registers
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String kafkaBootstrapServers;

    /**
     * Producer Factory for Kafka messages
     */
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Kafka Template for sending messages
     */
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * Topic: user.registered - Published when a new rider registers
     */
    @Bean
    public NewTopic userRegisteredTopic() {
        return TopicBuilder.name("user.registered")
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "604800000")  // 7 days
                .build();
    }

    /**
     * Topic: driver.registered - Published when a new driver registers
     */
    @Bean
    public NewTopic driverRegisteredTopic() {
        return TopicBuilder.name("driver.registered")
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "604800000")  // 7 days
                .build();
    }
}
