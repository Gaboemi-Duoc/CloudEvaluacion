package alumno.gaboemi.cloudevaluacion.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCourseRequest(
        @NotBlank String name,
        String description,
        @NotBlank String teacherId
) {}