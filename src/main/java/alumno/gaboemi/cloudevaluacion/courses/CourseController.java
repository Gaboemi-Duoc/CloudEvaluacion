package alumno.gaboemi.cloudevaluacion.courses;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("api/course")
public class CourseController {

    @GetMapping
    public String getCourses() {
        return new String();
    }
    
}
