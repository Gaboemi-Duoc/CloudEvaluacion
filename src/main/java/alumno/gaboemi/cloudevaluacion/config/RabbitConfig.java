package alumno.gaboemi.cloudevaluacion.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Value("${app.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${app.rabbitmq.queue-informe}")
    private String queueInforme;

    @Value("${app.rabbitmq.queue-db}")
    private String queueDb;

    @Bean
    public FanoutExchange informeExchange() {
        return new FanoutExchange(exchangeName, true, false);
    }

    @Bean
    public Queue informeQueue() {
        return new Queue(queueInforme, true);
    }

    @Bean
    public Queue dbInformeQueue() {
        return new Queue(queueDb, true);
    }

    @Bean
    public Binding bindingInforme(FanoutExchange exchange, Queue informeQueue) {
        return BindingBuilder.bind(informeQueue).to(exchange);
    }

    @Bean
    public Binding bindingDb(FanoutExchange exchange, Queue dbInformeQueue) {
        return BindingBuilder.bind(dbInformeQueue).to(exchange);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf,
                                          Jackson2JsonMessageConverter converter) {
        RabbitTemplate tpl = new RabbitTemplate(cf);
        tpl.setMessageConverter(converter);
        return tpl;
    }
}