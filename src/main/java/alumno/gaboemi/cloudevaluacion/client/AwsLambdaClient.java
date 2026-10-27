package alumno.gaboemi.cloudevaluacion.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

@Component
public class AwsLambdaClient {

    private final LambdaClient lambdaClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AwsLambdaClient() {
        this.lambdaClient = LambdaClient.builder().build();
    }

    public ResponseEntity<String> invoke(String functionName, Object payload) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            
            InvokeRequest request = InvokeRequest.builder()
                    .functionName(functionName)
                    .payload(SdkBytes.fromUtf8String(jsonPayload))
                    .build();

            InvokeResponse response = lambdaClient.invoke(request);
            String responseBody = response.payload().asUtf8String();
            
            JsonNode node = objectMapper.readTree(responseBody);
            int statusCode = node.has("statusCode") ? node.get("statusCode").asInt() : 200;
            String body = node.has("body") ? node.get("body").asText() : responseBody;

            return ResponseEntity.status(statusCode).body(body);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}