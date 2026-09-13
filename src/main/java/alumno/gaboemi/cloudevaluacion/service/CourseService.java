package alumno.gaboemi.cloudevaluacion.service;

import alumno.gaboemi.cloudevaluacion.model.Course;
import alumno.gaboemi.cloudevaluacion.dto.CreateCourseRequest;
import alumno.gaboemi.cloudevaluacion.exception.NotFoundException;
import alumno.gaboemi.cloudevaluacion.repository.CourseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CourseService {

    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    public List<Course> findAll() { return courseRepository.findAll(); }

    public List<Course> findForTeacher(String teacherId) {
        return courseRepository.findByTeacherId(teacherId);
    }

    public Course findById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Course not found: " + id));
    }

    @Transactional
    public Course create(CreateCourseRequest req) {
        Course c = new Course();
        c.setName(req.name());
        c.setDescription(req.description());
        c.setTeacherId(req.teacherId());
        return courseRepository.save(c);
    }

    @Transactional
    public Course assignTeacher(Long id, String teacherId) {
        Course c = findById(id);
        c.setTeacherId(teacherId);
        return courseRepository.save(c);
    }

    @Transactional
    public void delete(Long id) {
        courseRepository.delete(findById(id));
    }
}