package alumno.gaboemi.cloudevaluacion.controller;

import alumno.gaboemi.cloudevaluacion.client.KafkaEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/reports")
@PreAuthorize("hasRole('Admin')")
public class ReportController {

    private final KafkaEventPublisher events;

    public ReportController(KafkaEventPublisher events) {
        this.events = events;
    }

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<String> generateReport(@AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email == null) email = jwt.getSubject();

        events.publishEvent("GENERATE_REPORT", Map.of("requestedBy", email));

        return ResponseEntity.accepted().body("{\"message\":\"Report generation initiated\"}");
    }
}