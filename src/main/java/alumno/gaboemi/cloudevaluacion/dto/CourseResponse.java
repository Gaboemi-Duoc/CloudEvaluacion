package alumno.gaboemi.cloudevaluacion.dto;

import java.time.Instant;

import alumno.gaboemi.cloudevaluacion.model.Course;

public record CourseResponse(Long id, String name, String description,
                             String teacherId, Instant createdAt) {
    public static CourseResponse from(Course c) {
        return new CourseResponse(c.getId(), c.getName(), c.getDescription(),
                c.getTeacherId(), c.getCreatedAt());
    }
}