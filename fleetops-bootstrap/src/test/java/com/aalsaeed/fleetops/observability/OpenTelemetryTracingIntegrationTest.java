package com.aalsaeed.fleetops.observability;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Import(OpenTelemetryTracingIntegrationTest.TestTracingConfiguration.class)
class OpenTelemetryTracingIntegrationTest {

    private static final String TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String PARENT_SPAN_ID = "00f067aa0ba902b7";
    private static final String TRACEPARENT = "00-" + TRACE_ID + "-" + PARENT_SPAN_ID + "-01";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("fleetops_tracing_test")
            .withUsername("fleetops")
            .withPassword("fleetops_test");

    @Container
    @ServiceConnection
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer("rabbitmq:4-management-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Tracer tracer;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Test
    void continuesIncomingW3cTraceAndCorrelatesMdc() throws Exception {
        mockMvc.perform(get("/test/observability/trace").header("traceparent", TRACEPARENT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.traceId").value(TRACE_ID))
                .andExpect(jsonPath("$.spanId").value(matchesPattern("[0-9a-f]{16}")))
                .andExpect(jsonPath("$.mdcTraceId").value(TRACE_ID))
                .andExpect(jsonPath("$.mdcSpanId").value(matchesPattern("[0-9a-f]{16}")));
    }

    @Test
    void propagatesTraceContextThroughRabbitTemplateObservation() {
        Queue queue = QueueBuilder.nonDurable("fleetops.tracing.propagation.test")
                .autoDelete()
                .build();
        amqpAdmin.declareQueue(queue);

        Span producerSpan = tracer.nextSpan().name("rabbit-propagation-test").start();
        try (Tracer.SpanInScope ignored = tracer.withSpan(producerSpan)) {
            rabbitTemplate.convertAndSend("", queue.getName(), "trace-propagation");
        } finally {
            producerSpan.end();
        }

        Message message = rabbitTemplate.receive(queue.getName(), 5_000);
        assertThat(message).isNotNull();

        Object traceparent = message.getMessageProperties().getHeaders().get("traceparent");
        assertThat(traceparent).isNotNull();
        assertThat(traceparent.toString()).contains(producerSpan.context().traceId());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestTracingConfiguration {

        @Bean
        TracingProbeController tracingProbeController(Tracer tracer) {
            return new TracingProbeController(tracer);
        }
    }

    @RestController
    static class TracingProbeController {

        private final Tracer tracer;

        TracingProbeController(Tracer tracer) {
            this.tracer = tracer;
        }

        @GetMapping("/test/observability/trace")
        Map<String, String> currentTrace() {
            Span span = tracer.currentSpan();
            if (span == null) {
                throw new IllegalStateException("No active trace span");
            }
            return Map.of(
                    "traceId", span.context().traceId(),
                    "spanId", span.context().spanId(),
                    "mdcTraceId", valueOrEmpty(MDC.get("traceId")),
                    "mdcSpanId", valueOrEmpty(MDC.get("spanId")));
        }

        private static String valueOrEmpty(String value) {
            return value == null ? "" : value;
        }
    }
}
