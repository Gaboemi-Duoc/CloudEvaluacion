package alumno.gaboemi.cloudevaluacion.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import alumno.gaboemi.cloudevaluacion.model.Enrollment;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByStudentId(String studentId);
    List<Enrollment> findByCourseId(Long courseId);
    boolean existsByCourseIdAndStudentId(Long courseId, String studentId);

    @Query("select e from Enrollment e join fetch e.course where e.course.id = :courseId")
    List<Enrollment> findByCourseIdFetchCourse(@Param("courseId") Long courseId);

    @Query("select e from Enrollment e join fetch e.course where e.studentId = :studentId")
    List<Enrollment> findByStudentIdFetchCourse(@Param("studentId") String studentId);
}