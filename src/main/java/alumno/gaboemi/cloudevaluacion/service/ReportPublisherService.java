package alumno.gaboemi.cloudevaluacion.service;

import alumno.gaboemi.cloudevaluacion.dto.ReportMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class ReportPublisherService {

	private final RabbitTemplate rabbitTemplate;
	private final String exchangeName;

	public ReportPublisherService(RabbitTemplate rabbitTemplate, @Value("${app.rabbitmq.exchange}") String exchangeName) {
		this.rabbitTemplate = rabbitTemplate;
		this.exchangeName = exchangeName;
	}

	public ReportMessage publish(String email) {
		ReportMessage msg = new ReportMessage(
			UUID.randomUUID().toString(),
			email,
			Instant.now().toString()
		);

		// Fanout: routing key vacía, ambas colas reciben el mensaje
		rabbitTemplate.convertAndSend(exchangeName, "", msg);
		return msg;
	}
}