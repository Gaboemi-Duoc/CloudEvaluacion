package alumno.gaboemi.cloudevaluacion.dto;

public record MessageDto(String message, String error) {
	public MessageDto(String status) {
		this(status, null);
	}
}
