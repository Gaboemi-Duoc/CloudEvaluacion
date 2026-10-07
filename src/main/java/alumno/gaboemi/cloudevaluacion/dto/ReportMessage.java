package alumno.gaboemi.cloudevaluacion.dto;

public record ReportMessage(
	String requestId,
	String email,
	String requestedAt
) {}