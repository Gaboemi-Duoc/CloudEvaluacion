package alumno.gaboemi.cloudevaluacion.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class KafkaEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;
    private final String commandsTopic;

    public KafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                               JsonMapper jsonMapper,
                               @Value("${app.kafka.topic.commands}") String commandsTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.jsonMapper = jsonMapper;
        this.commandsTopic = commandsTopic;
    }

    /**
     * Publishes an async command event to Kafka.
     *
     * Uses `action` as the message key, mirroring the old SNS filter-attribute:
     *  - All events of the same action land on the same partition (ordering preserved)
     *  - Consumers can filter by key, or you can add per-action topics later
     */
    public void publishEvent(String action, Object data) {
        try {
            String message = jsonMapper.writeValueAsString(
                    Map.of("action", action, "data", data)
            );

            // Blocking wait to preserve the old SNS synchronous semantics.
            kafkaTemplate.send(commandsTopic, action, message)
                    .get(5, TimeUnit.SECONDS);

        } catch (Exception e) {
            throw new RuntimeException("Failed to publish to Kafka topic '" + commandsTopic + "'", e);
        }
    }
}