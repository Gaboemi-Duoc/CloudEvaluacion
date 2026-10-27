package alumno.gaboemi.cloudevaluacion.controller;

import alumno.gaboemi.cloudevaluacion.client.AwsLambdaClient;
import alumno.gaboemi.cloudevaluacion.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher/courses")
@PreAuthorize("hasRole('Teacher')")
public class TeacherCourseController {

    private final AwsLambdaClient lambda;

    public TeacherCourseController(AwsLambdaClient lambda) {
        this.lambda = lambda;
    }

    @GetMapping
    public ResponseEntity<String> myCourses(@AuthenticationPrincipal Jwt jwt) {
        return lambda.invoke("lambda-course-service", Map.of("action", "TEACHER_GET_COURSES", "teacherId", identity(jwt)));
    }

    @GetMapping("/{id}/students")
    public ResponseEntity<String> students(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return lambda.invoke("lambda-enrollment-service", Map.of("action", "TEACHER_GET_STUDENTS", "courseId", id, "teacherId", identity(jwt)));
    }

    @PostMapping("/{id}/enrollments")
    public ResponseEntity<String> enroll(@PathVariable Long id, @Valid @RequestBody EnrollStudentRequest req, @AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "TEACHER_ENROLL_STUDENT");
        payload.put("courseId", id);
        payload.put("studentId", req.studentId());
        payload.put("teacherId", identity(jwt));
        return lambda.invoke("lambda-enrollment-service", payload);
    }

    @GetMapping("/{id}/tasks")
    public ResponseEntity<String> tasks(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return lambda.invoke("lambda-task-service", Map.of("action", "TEACHER_GET_TASKS", "courseId", id, "teacherId", identity(jwt)));
    }

    @PostMapping("/{id}/tasks")
    public ResponseEntity<String> createTask(@PathVariable Long id, @Valid @RequestBody CreateTaskRequest req, @AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "TEACHER_CREATE_TASK");
        payload.put("courseId", id);
        payload.put("data", req);
        payload.put("teacherId", identity(jwt));
        return lambda.invoke("lambda-task-service", payload);
    }

    static String identity(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        return email != null ? email : jwt.getSubject();
    }
}