package alumno.gaboemi.cloudevaluacion.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic commandsTopic(@Value("${app.kafka.topic.commands}") String name) {
        // 3 partitions => up to 3 concurrent Lambda invocations for this topic
        return TopicBuilder.name(name)
                .partitions(3)
                .replicas(1)
                .build();
    }
}