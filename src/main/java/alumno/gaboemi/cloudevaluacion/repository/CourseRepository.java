package alumno.gaboemi.cloudevaluacion.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import alumno.gaboemi.cloudevaluacion.model.Course;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByTeacherId(String teacherId);
}