package com.codemind.repo_service.kafka;

import com.codemind.repo_service.event.RepoIngestionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RepoEventProducer {

    private static final String TOPIC = "repo-ingestion";

    private final KafkaTemplate<String, RepoIngestionEvent> kafkaTemplate;

    public void publishIngestionEvent(RepoIngestionEvent event) {
        log.info("Publishing ingestion event for repo: {}", event.getRepoId());
        kafkaTemplate.send(TOPIC, event.getRepoId(), event);
    }
}