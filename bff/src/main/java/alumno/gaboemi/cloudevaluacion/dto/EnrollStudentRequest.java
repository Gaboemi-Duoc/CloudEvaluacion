package alumno.gaboemi.cloudevaluacion.dto;

import jakarta.validation.constraints.NotBlank;

public record EnrollStudentRequest(@NotBlank String studentId) {}