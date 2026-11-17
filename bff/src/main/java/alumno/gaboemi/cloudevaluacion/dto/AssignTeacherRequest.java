package alumno.gaboemi.cloudevaluacion.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignTeacherRequest(@NotBlank String teacherId) {}