package alumno.gaboemi.cloudevaluacion.controller;

import alumno.gaboemi.cloudevaluacion.dto.*;
import alumno.gaboemi.cloudevaluacion.service.CourseService;
import alumno.gaboemi.cloudevaluacion.service.EnrollmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/courses")
@PreAuthorize("hasRole('Admin')")
public class AdminCourseController {

    private final CourseService courseService;
    private final EnrollmentService enrollmentService;

    public AdminCourseController(CourseService courseService, EnrollmentService enrollmentService) {
        this.courseService = courseService;
        this.enrollmentService = enrollmentService;
    }

    @GetMapping
    public List<CourseResponse> list() {
        return courseService.findAll().stream().map(CourseResponse::from).toList();
    }

    @GetMapping("/{id}")
    public CourseResponse get(@PathVariable Long id) {
        return CourseResponse.from(courseService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseResponse create(@Valid @RequestBody CreateCourseRequest req) {
        return CourseResponse.from(courseService.create(req));
    }

    @PutMapping("/{id}/teacher")
    public CourseResponse assignTeacher(@PathVariable Long id,
                                        @Valid @RequestBody AssignTeacherRequest req) {
        return CourseResponse.from(courseService.assignTeacher(id, req.teacherId()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        courseService.delete(id);
    }

    @PostMapping("/{id}/enrollments")
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentResponse enroll(@PathVariable Long id,
                                     @Valid @RequestBody EnrollStudentRequest req) {
        return EnrollmentResponse.from(enrollmentService.enroll(id, req.studentId()));
    }

    @GetMapping("/{id}/students")
    public List<EnrollmentResponse> students(@PathVariable Long id) {
        return enrollmentService.findByCourse(id).stream().map(EnrollmentResponse::from).toList();
    }
}