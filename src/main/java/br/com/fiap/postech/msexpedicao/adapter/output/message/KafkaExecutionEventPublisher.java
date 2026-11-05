package br.com.fiap.postech.msexpedicao.adapter.output.message;

import br.com.fiap.postech.msexpedicao.domain.ExecutionEvent;
import br.com.fiap.postech.msexpedicao.port.ExecutionEventPublisherPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaExecutionEventPublisher implements ExecutionEventPublisherPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.events.topic.execution}")
    private String topic;

    @Override
    public void publish(ExecutionEvent event) {
        kafkaTemplate.send(topic, String.valueOf(event.serviceOrderId()), event);
    }
}
