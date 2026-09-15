package alumno.gaboemi.cloudevaluacion.controller;

import alumno.gaboemi.cloudevaluacion.dto.*;
import alumno.gaboemi.cloudevaluacion.service.CourseService;
import alumno.gaboemi.cloudevaluacion.service.EnrollmentService;
import alumno.gaboemi.cloudevaluacion.service.TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student")
@PreAuthorize("hasRole('Student')")
public class StudentController {

    private final CourseService courseService;
    private final EnrollmentService enrollmentService;
    private final TaskService taskService;

    public StudentController(CourseService courseService,
                             EnrollmentService enrollmentService,
                             TaskService taskService) {
        this.courseService = courseService;
        this.enrollmentService = enrollmentService;
        this.taskService = taskService;
    }

    @GetMapping("/courses")
    public List<CourseResponse> myCourses(@AuthenticationPrincipal Jwt jwt) {
        return enrollmentService.findByStudent(TeacherCourseController.identity(jwt)).stream()
                .map(e -> CourseResponse.from(e.getCourse()))
                .toList();
    }

    @GetMapping("/courses/available")
    public List<CourseResponse> available(@AuthenticationPrincipal Jwt jwt) {
        var enrolledIds = enrollmentService.findByStudent(TeacherCourseController.identity(jwt))
                .stream().map(e -> e.getCourse().getId()).toList();
        return courseService.findAll().stream()
                .filter(c -> !enrolledIds.contains(c.getId()))
                .map(CourseResponse::from)
                .toList();
    }

    @PostMapping("/courses/{id}/enroll")
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentResponse enroll(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return EnrollmentResponse.from(
                enrollmentService.enroll(id, TeacherCourseController.identity(jwt)));
    }

    @GetMapping("/courses/{id}/tasks")
    public List<TaskResponse> tasksOfCourse(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        if (!enrollmentService.isEnrolled(id, TeacherCourseController.identity(jwt))) {
            throw new AccessDeniedException("You are not enrolled in this course");
        }
        return taskService.findByCourse(id).stream().map(TaskResponse::from).toList();
    }

    @GetMapping("/tasks")
    public List<TaskResponse> myTasks(@AuthenticationPrincipal Jwt jwt) {
        var ids = enrollmentService.findByStudent(TeacherCourseController.identity(jwt))
                .stream().map(e -> e.getCourse().getId()).toList();
        return taskService.findByCourseIds(ids).stream().map(TaskResponse::from).toList();
    }
}