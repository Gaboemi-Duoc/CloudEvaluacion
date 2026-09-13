package alumno.gaboemi.cloudevaluacion.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import alumno.gaboemi.cloudevaluacion.model.Task;

import java.util.Collection;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByCourseId(Long courseId);
    List<Task> findByCourseIdIn(Collection<Long> courseIds);
}