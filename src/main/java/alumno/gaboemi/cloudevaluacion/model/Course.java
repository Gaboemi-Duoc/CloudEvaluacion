package alumno.gaboemi.cloudevaluacion.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "courses")
@Getter
@Setter
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    /** Cognito email (or sub) of the assigned teacher. */
    @Column(name = "teacher_id", nullable = false)
    private String teacherId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}