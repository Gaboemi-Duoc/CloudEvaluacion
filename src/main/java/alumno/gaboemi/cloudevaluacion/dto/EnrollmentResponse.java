package alumno.gaboemi.cloudevaluacion.dto;

import java.time.Instant;

import alumno.gaboemi.cloudevaluacion.model.Enrollment;

public record EnrollmentResponse(Long id, Long courseId, String courseName,
                                 String studentId, Instant createdAt) {
    public static EnrollmentResponse from(Enrollment e) {
        return new EnrollmentResponse(e.getId(), e.getCourse().getId(),
                e.getCourse().getName(), e.getStudentId(), e.getCreatedAt());
    }
}