package com.miniuber.core.auth.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

/**
 * Publisher for user registration events to Kafka
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegistrationEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String USER_REGISTERED_TOPIC = "user.registered";
    private static final String DRIVER_REGISTERED_TOPIC = "driver.registered";

    /**
     * Publish rider registration event
     */
    public void publishRiderRegistration(Long userId, String email, String name) {
        UserRegistrationEvent event = new UserRegistrationEvent(
                userId,
                email,
                name,
                "RIDER",
                LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
        );

        publishEvent(USER_REGISTERED_TOPIC, event);
    }

    /**
     * Publish driver registration event
     */
    public void publishDriverRegistration(Long userId, String email, String name) {
        UserRegistrationEvent event = new UserRegistrationEvent(
                userId,
                email,
                name,
                "DRIVER",
                LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
        );

        publishEvent(DRIVER_REGISTERED_TOPIC, event);
    }

    /**
     * Internal method to publish event to Kafka
     */
    private void publishEvent(String topic, UserRegistrationEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            
            CompletableFuture<SendResult<String, String>> future = 
                    kafkaTemplate.send(topic, event.getUserId().toString(), eventJson);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Published {} registration event: userId={}, partition={}, offset={}", 
                            event.getUserType(), 
                            event.getUserId(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to publish {} registration event: userId={}, error={}", 
                            event.getUserType(), 
                            event.getUserId(), 
                            ex.getMessage());
                }
            });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event: {}", e.getMessage());
        }
    }
}
