package alumno.gaboemi.cloudevaluacion.dto;

import java.time.Instant;
import java.time.LocalDate;

import alumno.gaboemi.cloudevaluacion.model.Task;

public record TaskResponse(Long id, Long courseId, String title, String description,
                           LocalDate dueDate, Instant createdAt) {
    public static TaskResponse from(Task t) {
        return new TaskResponse(t.getId(), t.getCourse().getId(), t.getTitle(),
                t.getDescription(), t.getDueDate(), t.getCreatedAt());
    }
}