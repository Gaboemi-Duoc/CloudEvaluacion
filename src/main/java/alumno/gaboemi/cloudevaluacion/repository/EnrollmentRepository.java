package alumno.gaboemi.cloudevaluacion.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import alumno.gaboemi.cloudevaluacion.model.Enrollment;

import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByStudentId(String studentId);
    List<Enrollment> findByCourseId(Long courseId);
    boolean existsByCourseIdAndStudentId(Long courseId, String studentId);
}