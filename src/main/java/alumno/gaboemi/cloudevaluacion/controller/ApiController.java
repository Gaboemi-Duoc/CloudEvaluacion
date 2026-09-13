package alumno.gaboemi.cloudevaluacion.controller;

import alumno.gaboemi.cloudevaluacion.dto.MeResponse;
import alumno.gaboemi.cloudevaluacion.dto.MessageDto;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api")
public class ApiController {
	@GetMapping("/health")
	public ResponseEntity<MessageDto> getHealth() {
		try {
			return ResponseEntity.ok(new MessageDto("API is UP"));
		} catch (Exception e) {
			MessageDto errorResponse = new MessageDto("DOWN", e.getMessage());
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
		}
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
