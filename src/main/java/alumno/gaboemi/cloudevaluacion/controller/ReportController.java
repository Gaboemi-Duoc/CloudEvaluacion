package alumno.gaboemi.cloudevaluacion.controller;

import alumno.gaboemi.cloudevaluacion.client.AwsLambdaClient;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/reports")
@PreAuthorize("hasRole('Admin')")
public class ReportController {

    private final AwsLambdaClient lambda;

    public ReportController(AwsLambdaClient lambda) {
        this.lambda = lambda;
    }

    @PostMapping("/generate")
    public ResponseEntity<String> generateReport() {
        return lambda.invoke("lambda-report-generator", Map.of("action", "GENERATE_REPORT"));
    }
}