package br.com.fiap.postech.msexpedicao.adapter.output.message;

import br.com.fiap.postech.msexpedicao.domain.ExecutionEvent;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class KafkaExecutionEventPublisherTest {

    @Test
    void should_publish_with_service_order_key() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        KafkaExecutionEventPublisher publisher = new KafkaExecutionEventPublisher(kafkaTemplate);
        ReflectionTestUtils.setField(publisher, "topic", "execution-events");
        ExecutionEvent event = new ExecutionEvent("evt-1", ExecutionEvent.EXECUTION_STARTED,
                Instant.now(), "corr-1", 7L, 3L);

        publisher.publish(event);

        verify(kafkaTemplate).send("execution-events", "7", event);
    }
}
