package alumno.gaboemi.cloudevaluacion.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.util.Map;

@Component
public class AwsSnsClient {
    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;

    public AwsSnsClient(ObjectMapper objectMapper) {
        this.snsClient = SnsClient.builder().build();
        this.objectMapper = objectMapper;
    }

    public void publishEvent(String action, Object data) {
        try {
            String message = objectMapper.writeValueAsString(
                Map.of("action", action, "data", data)
            );

            // Create the message attribute for the filter policy
            MessageAttributeValue actionAttr = MessageAttributeValue.builder()
                    .dataType("String")
                    .stringValue(action)
                    .build();

            PublishRequest request = PublishRequest.builder()
                    .topicArn(System.getenv("SNS_TOPIC_ARN"))
                    .message(message)
                    .messageAttributes(Map.of("action", actionAttr)) // Add attribute here
                    .build();

            snsClient.publish(request);
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish to SNS", e);
        }
    }
}