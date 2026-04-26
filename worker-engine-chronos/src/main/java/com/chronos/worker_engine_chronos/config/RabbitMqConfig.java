package com.chronos.worker_engine_chronos.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String EXCHANGE_NAME = "jobs.exchange";
    public static final String ROUTING_KEY = "jobs.new";
    public static final String QUEUE_NAME = "jobs.queue";

    @Bean
    public DirectExchange jobsExchange() {
        return new DirectExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public Queue jobsQueue() {
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    public Binding jobsBinding(Queue jobsQueue, DirectExchange jobsExchange) {
        return BindingBuilder.bind(jobsQueue).to(jobsExchange).with(ROUTING_KEY);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new SimpleMessageConverter());
        return template;
    }
}