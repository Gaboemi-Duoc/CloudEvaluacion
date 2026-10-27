package alumno.gaboemi.cloudevaluacion.controller;

import alumno.gaboemi.cloudevaluacion.client.AwsLambdaClient;
import alumno.gaboemi.cloudevaluacion.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/courses")
@PreAuthorize("hasRole('Admin')")
public class AdminCourseController {

    private final AwsLambdaClient lambda;

    public AdminCourseController(AwsLambdaClient lambda) {
        this.lambda = lambda;
    }

    @GetMapping
    public ResponseEntity<String> list() {
        return lambda.invoke("lambda-course-service", Map.of("action", "ADMIN_LIST_COURSES"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<String> get(@PathVariable Long id) {
        return lambda.invoke("lambda-course-service", Map.of("action", "ADMIN_GET_COURSE", "courseId", id));
    }

    @PostMapping
    public ResponseEntity<String> create(@Valid @RequestBody CreateCourseRequest req) {
        return lambda.invoke("lambda-course-service", Map.of("action", "ADMIN_CREATE_COURSE", "data", req));
    }

    @PutMapping("/{id}/teacher")
    public ResponseEntity<String> assignTeacher(@PathVariable Long id, @Valid @RequestBody AssignTeacherRequest req) {
        return lambda.invoke("lambda-course-service", Map.of("action", "ADMIN_ASSIGN_TEACHER", "courseId", id, "teacherId", req.teacherId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        return lambda.invoke("lambda-course-service", Map.of("action", "ADMIN_DELETE_COURSE", "courseId", id));
    }

    @PostMapping("/{id}/enrollments")
    public ResponseEntity<String> enroll(@PathVariable Long id, @Valid @RequestBody EnrollStudentRequest req) {
        return lambda.invoke("lambda-enrollment-service", Map.of("action", "ADMIN_ENROLL_STUDENT", "courseId", id, "studentId", req.studentId()));
    }

    @GetMapping("/{id}/students")
    public ResponseEntity<String> students(@PathVariable Long id) {
        return lambda.invoke("lambda-enrollment-service", Map.of("action", "ADMIN_GET_STUDENTS", "courseId", id));
    }
}