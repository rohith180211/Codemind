package com.codemind.repo_service.config;

import com.codemind.repo_service.event.RepoIngestionEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public ProducerFactory<String, RepoIngestionEvent> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        Serializer<RepoIngestionEvent> valueSerializer = (topic, data) -> {
            try {
                return objectMapper().writeValueAsBytes(data);
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize event", e);
            }
        };

        return new DefaultKafkaProducerFactory<>(
                config,
                new StringSerializer(),
                valueSerializer
        );
    }

    @Bean
    public KafkaTemplate<String, RepoIngestionEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}