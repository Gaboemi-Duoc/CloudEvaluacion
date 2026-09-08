package alumno.gaboemi.cloudevaluacion.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import alumno.gaboemi.cloudevaluacion.dto.MessageDto;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


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
	
}
