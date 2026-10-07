package alumno.gaboemi.cloudevaluacion.controller;

import alumno.gaboemi.cloudevaluacion.dto.MeResponse;
import alumno.gaboemi.cloudevaluacion.dto.MessageDto;
import alumno.gaboemi.cloudevaluacion.dto.ReportMessage;
import alumno.gaboemi.cloudevaluacion.dto.ReportRequestDto;
import alumno.gaboemi.cloudevaluacion.service.ReportPublisherService;
import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api")
public class ApiController {

	private final ReportPublisherService reportPublisherService;

	public ApiController(ReportPublisherService reportPublisherService) {
		this.reportPublisherService = reportPublisherService;
	}
	
	@GetMapping("/health")
	public ResponseEntity<MessageDto> getHealth() {
		try {
			return ResponseEntity.ok(new MessageDto("API is UP"));
		} catch (Exception e) {
			MessageDto errorResponse = new MessageDto("DOWN", e.getMessage());
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
		}
	}

	@PostMapping("/generar-informe")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public Map<String, Object> generarInforme(@Valid @RequestBody ReportRequestDto body) {
		ReportMessage msg = reportPublisherService.publish(body.email());
		return Map.of(
			"status", "accepted",
			"requestId", msg.requestId(),
			"email", msg.email(),
			"requestedAt", msg.requestedAt()
		);
	}
	

	@GetMapping("/me")
	public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
		List<String> roles = jwt.getClaimAsStringList("cognito:groups");
		return new MeResponse(
				jwt.getSubject(),
				jwt.getClaimAsString("email"),
				roles == null ? List.of() : roles
		);
	}
}
