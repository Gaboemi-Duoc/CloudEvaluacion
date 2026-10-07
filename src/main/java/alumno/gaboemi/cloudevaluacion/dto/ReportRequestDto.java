package alumno.gaboemi.cloudevaluacion.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ReportRequestDto(
	@NotBlank @Email String email
) {}