package alumno.gaboemi.cloudevaluacion.controller;

import alumno.gaboemi.cloudevaluacion.client.AwsLambdaClient;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/student")
@PreAuthorize("hasRole('Student')")
public class StudentController {

    private final AwsLambdaClient lambda;

    public StudentController(AwsLambdaClient lambda) {
        this.lambda = lambda;
    }

    @GetMapping("/courses")
    public ResponseEntity<String> myCourses(@AuthenticationPrincipal Jwt jwt) {
        return lambda.invoke("lambda-enrollment-service", Map.of("action", "STUDENT_GET_COURSES", "studentId", TeacherCourseController.identity(jwt)));
    }

    @GetMapping("/courses/available")
    public ResponseEntity<String> available(@AuthenticationPrincipal Jwt jwt) {
        return lambda.invoke("lambda-course-service", Map.of("action", "STUDENT_GET_AVAILABLE", "studentId", TeacherCourseController.identity(jwt)));
    }

    @PostMapping("/courses/{id}/enroll")
    public ResponseEntity<String> enroll(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return lambda.invoke("lambda-enrollment-service", Map.of("action", "STUDENT_ENROLL", "courseId", id, "studentId", TeacherCourseController.identity(jwt)));
    }

    @GetMapping("/courses/{id}/tasks")
    public ResponseEntity<String> tasksOfCourse(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return lambda.invoke("lambda-task-service", Map.of("action", "STUDENT_GET_COURSE_TASKS", "courseId", id, "studentId", TeacherCourseController.identity(jwt)));
    }

    @GetMapping("/tasks")
    public ResponseEntity<String> myTasks(@AuthenticationPrincipal Jwt jwt) {
        return lambda.invoke("lambda-task-service", Map.of("action", "STUDENT_GET_TASKS", "studentId", TeacherCourseController.identity(jwt)));
    }
}