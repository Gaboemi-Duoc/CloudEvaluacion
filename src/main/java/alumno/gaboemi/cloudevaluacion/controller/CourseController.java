package alumno.gaboemi.cloudevaluacion.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("api/course")
public class CourseController {

    @GetMapping
    public String getMethodName() {
        return new String();
    }
    
    
}
