package alumno.gaboemi.cloudevaluacion.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "enrollments",
       uniqueConstraints = @UniqueConstraint(columnNames = {"course_id", "student_id"}))
@Getter
@Setter
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    /** Cognito email (or sub) of the enrolled student. */
    @Column(name = "student_id", nullable = false)
    private String studentId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}