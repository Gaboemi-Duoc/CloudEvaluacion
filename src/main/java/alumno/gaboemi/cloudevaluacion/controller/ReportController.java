package alumno.gaboemi.cloudevaluacion.controller;

import alumno.gaboemi.cloudevaluacion.client.AwsSnsClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/reports")
@PreAuthorize("hasRole('Admin')")
public class ReportController {

    private final AwsSnsClient sns;

    public ReportController(AwsSnsClient sns) {
        this.sns = sns;
    }

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<String> generateReport() {
        sns.publishEvent("GENERATE_REPORT", Map.of());
        return ResponseEntity.accepted().body("{\"message\":\"Report generation initiated\"}");
    }
}