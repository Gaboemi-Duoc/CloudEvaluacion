package alumno.gaboemi.cloudevaluacion.service;

import alumno.gaboemi.cloudevaluacion.model.Course;
import alumno.gaboemi.cloudevaluacion.model.Enrollment;
import alumno.gaboemi.cloudevaluacion.exception.NotFoundException;
import alumno.gaboemi.cloudevaluacion.repository.EnrollmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseService courseService;

    public EnrollmentService(EnrollmentRepository enrollmentRepository, CourseService courseService) {
        this.enrollmentRepository = enrollmentRepository;
        this.courseService = courseService;
    }

    public List<Enrollment> findByStudent(String studentId) {
        return enrollmentRepository.findByStudentIdFetchCourse(studentId);
    }

    public List<Enrollment> findByCourse(Long courseId) {
        return enrollmentRepository.findByCourseIdFetchCourse(courseId);
    }

    public boolean isEnrolled(Long courseId, String studentId) {
        return enrollmentRepository.existsByCourseIdAndStudentId(courseId, studentId);
    }

    @Transactional
    public Enrollment enroll(Long courseId, String studentId) {
        if (enrollmentRepository.existsByCourseIdAndStudentId(courseId, studentId)) {
            return enrollmentRepository.findByCourseId(courseId).stream()
                    .filter(e -> e.getStudentId().equals(studentId))
                    .findFirst()
                    .orElseThrow(() -> new NotFoundException("Enrollment not found"));
        }
        Course course = courseService.findById(courseId);
        Enrollment e = new Enrollment();
        e.setCourse(course);
        e.setStudentId(studentId);
        return enrollmentRepository.save(e);
    }
}