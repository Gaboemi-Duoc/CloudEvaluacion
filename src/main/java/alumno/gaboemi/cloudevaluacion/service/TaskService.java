package alumno.gaboemi.cloudevaluacion.service;

import alumno.gaboemi.cloudevaluacion.model.Course;
import alumno.gaboemi.cloudevaluacion.model.Task;
import alumno.gaboemi.cloudevaluacion.dto.CreateTaskRequest;
import alumno.gaboemi.cloudevaluacion.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final CourseService courseService;

    public TaskService(TaskRepository taskRepository, CourseService courseService) {
        this.taskRepository = taskRepository;
        this.courseService = courseService;
    }

    public List<Task> findByCourse(Long courseId) {
        return taskRepository.findByCourseId(courseId);
    }

    public List<Task> findByCourseIds(Collection<Long> courseIds) {
        if (courseIds.isEmpty()) return List.of();
        return taskRepository.findByCourseIdIn(courseIds);
    }

    @Transactional
    public Task create(Long courseId, CreateTaskRequest req) {
        Course course = courseService.findById(courseId);
        Task t = new Task();
        t.setCourse(course);
        t.setTitle(req.title());
        t.setDescription(req.description());
        t.setDueDate(req.dueDate());
        return taskRepository.save(t);
    }
}