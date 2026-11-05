package br.com.fiap.postech.msexpedicao.adapter.input.message;

import br.com.fiap.postech.msexpedicao.adapter.input.message.event.PaymentEvent;
import br.com.fiap.postech.msexpedicao.adapter.input.message.event.ServiceOrderEvent;
import br.com.fiap.postech.msexpedicao.domain.usecase.ExecutionQueueUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Builds the execution queue from the choreographed saga events.
 *
 * <p>An order enters the queue when it reaches {@code APPROVED}; payment rejection or timeout
 * cancels the queued job.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SagaEventListener {

    static final String STATUS_CHANGED = "SERVICE_ORDER_STATUS_CHANGED";
    static final String PAYMENT_REJECTED = "PAYMENT_REJECTED";
    static final String PAYMENT_TIMEOUT = "PAYMENT_TIMEOUT";

    private final ObjectMapper objectMapper;
    private final ExecutionQueueUseCase useCase;

    @KafkaListener(
            topics = "${app.events.topic.service-order}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onServiceOrderEvent(String payload) {
        ServiceOrderEvent event = read(payload, ServiceOrderEvent.class);
        if (!STATUS_CHANGED.equals(event.getType()) || event.getStatus() == null) {
            log.info("Ignoring service order event type={}", event.getType());
            return;
        }
        switch (event.getStatus()) {
            case "APPROVED", "IN_PROGRESS" -> useCase.enqueue(
                    event.getServiceOrderId(), event.getBranchId(), 0);
            case "CANCELLED" -> useCase.cancel(event.getServiceOrderId());
            default -> log.debug("No queue action for status={}", event.getStatus());
        }
    }

    @KafkaListener(
            topics = "${app.events.topic.payment}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onPaymentEvent(String payload) {
        PaymentEvent event = read(payload, PaymentEvent.class);
        if (PAYMENT_REJECTED.equals(event.getType()) || PAYMENT_TIMEOUT.equals(event.getType())) {
            useCase.cancel(event.getServiceOrderId());
            return;
        }
        log.info("Ignoring payment event type={}", event.getType());
    }

    private <T> T read(String payload, Class<T> type) {
        try {
            return objectMapper.readValue(payload, type);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Cannot deserialize saga event", exception);
        }
    }
}
