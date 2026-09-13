package alumno.gaboemi.cloudevaluacion.controller;

import alumno.gaboemi.cloudevaluacion.model.Course;
import alumno.gaboemi.cloudevaluacion.dto.*;
import alumno.gaboemi.cloudevaluacion.service.CourseService;
import alumno.gaboemi.cloudevaluacion.service.EnrollmentService;
import alumno.gaboemi.cloudevaluacion.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teacher/courses")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherCourseController {

    private final CourseService courseService;
    private final EnrollmentService enrollmentService;
    private final TaskService taskService;

    public TeacherCourseController(CourseService courseService,
                                   EnrollmentService enrollmentService,
                                   TaskService taskService) {
        this.courseService = courseService;
        this.enrollmentService = enrollmentService;
        this.taskService = taskService;
    }

    @GetMapping
    public List<CourseResponse> myCourses(@AuthenticationPrincipal Jwt jwt) {
        return courseService.findForTeacher(identity(jwt)).stream()
                .map(CourseResponse::from).toList();
    }

    @GetMapping("/{id}/students")
    public List<EnrollmentResponse> students(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        assertOwnership(id, jwt);
        return enrollmentService.findByCourse(id).stream().map(EnrollmentResponse::from).toList();
    }

    @PostMapping("/{id}/enrollments")
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentResponse enroll(@PathVariable Long id,
                                     @Valid @RequestBody EnrollStudentRequest req,
                                     @AuthenticationPrincipal Jwt jwt) {
        assertOwnership(id, jwt);
        return EnrollmentResponse.from(enrollmentService.enroll(id, req.studentId()));
    }

    @GetMapping("/{id}/tasks")
    public List<TaskResponse> tasks(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        assertOwnership(id, jwt);
        return taskService.findByCourse(id).stream().map(TaskResponse::from).toList();
    }

    @PostMapping("/{id}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse createTask(@PathVariable Long id,
                                   @Valid @RequestBody CreateTaskRequest req,
                                   @AuthenticationPrincipal Jwt jwt) {
        assertOwnership(id, jwt);
        return TaskResponse.from(taskService.create(id, req));
    }

    private void assertOwnership(Long courseId, Jwt jwt) {
        Course c = courseService.findById(courseId);
        if (!c.getTeacherId().equals(identity(jwt))) {
            throw new AccessDeniedException("You are not the owner of this course");
        }
    }

    /** identity = email claim if present, else the sub. */
    static String identity(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        return email != null ? email : jwt.getSubject();
    }
}