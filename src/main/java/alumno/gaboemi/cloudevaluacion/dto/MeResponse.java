package alumno.gaboemi.cloudevaluacion.dto;

import java.util.List;

public record MeResponse(String subject, String email, List<String> roles) {}