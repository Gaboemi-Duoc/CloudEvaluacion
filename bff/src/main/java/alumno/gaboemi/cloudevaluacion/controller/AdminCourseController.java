package alumno.gaboemi.cloudevaluacion.controller;

import alumno.gaboemi.cloudevaluacion.client.AwsLambdaClient;
import alumno.gaboemi.cloudevaluacion.client.AwsSnsClient;
import alumno.gaboemi.cloudevaluacion.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/courses")
@PreAuthorize("hasRole('Admin')")
public class AdminCourseController {

    private final AwsLambdaClient lambda;
    private final AwsSnsClient sns;

    public AdminCourseController(AwsLambdaClient lambda, AwsSnsClient sns) {
        this.lambda = lambda;
        this.sns = sns;
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
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<String> create(@Valid @RequestBody CreateCourseRequest req) {
        sns.publishEvent("ADMIN_CREATE_COURSE", req);
        return ResponseEntity.accepted().body("{\"message\":\"Course creation initiated\"}");
    }

    @PutMapping("/{id}/teacher")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<String> assignTeacher(@PathVariable Long id, @Valid @RequestBody AssignTeacherRequest req) {
        sns.publishEvent("ADMIN_ASSIGN_TEACHER", Map.of("courseId", id, "teacherId", req.teacherId()));
        return ResponseEntity.accepted().body("{\"message\":\"Teacher assignment initiated\"}");
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<String> delete(@PathVariable Long id) {
        sns.publishEvent("ADMIN_DELETE_COURSE", Map.of("courseId", id));
        return ResponseEntity.accepted().body("{\"message\":\"Course deletion initiated\"}");
    }

    @PostMapping("/{id}/enrollments")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<String> enroll(@PathVariable Long id, @Valid @RequestBody EnrollStudentRequest req) {
        sns.publishEvent("ADMIN_ENROLL_STUDENT", Map.of("courseId", id, "studentId", req.studentId()));
        return ResponseEntity.accepted().body("{\"message\":\"Enrollment initiated\"}");
    }

    @GetMapping("/{id}/students")
    public ResponseEntity<String> students(@PathVariable Long id) {
        return lambda.invoke("lambda-enrollment-service", Map.of("action", "ADMIN_GET_STUDENTS", "courseId", id));
    }
}