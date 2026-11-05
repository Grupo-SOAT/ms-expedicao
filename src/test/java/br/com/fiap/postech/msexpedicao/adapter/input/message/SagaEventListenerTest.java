package br.com.fiap.postech.msexpedicao.adapter.input.message;

import br.com.fiap.postech.msexpedicao.domain.usecase.ExecutionQueueUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class SagaEventListenerTest {

    private final ExecutionQueueUseCase useCase = mock(ExecutionQueueUseCase.class);
    private final SagaEventListener listener = new SagaEventListener(new ObjectMapper(), useCase);

    @Test
    void should_enqueue_when_order_is_approved() {
        listener.onServiceOrderEvent("""
                {"eventId":"evt-1","type":"SERVICE_ORDER_STATUS_CHANGED","serviceOrderId":7,
                 "branchId":3,"status":"APPROVED","correlationId":"corr-1"}
                """);

        verify(useCase).enqueue(7L, 3L, 0);
    }

    @Test
    void should_enqueue_when_order_is_in_progress() {
        listener.onServiceOrderEvent("""
                {"eventId":"evt-2","type":"SERVICE_ORDER_STATUS_CHANGED","serviceOrderId":8,
                 "branchId":4,"status":"IN_PROGRESS"}
                """);

        verify(useCase).enqueue(8L, 4L, 0);
    }

    @Test
    void should_cancel_when_order_is_cancelled() {
        listener.onServiceOrderEvent("""
                {"eventId":"evt-3","type":"SERVICE_ORDER_STATUS_CHANGED","serviceOrderId":9,
                 "branchId":5,"status":"CANCELLED"}
                """);

        verify(useCase).cancel(9L);
    }

    @Test
    void should_ignore_other_statuses() {
        listener.onServiceOrderEvent("""
                {"eventId":"evt-4","type":"SERVICE_ORDER_STATUS_CHANGED","serviceOrderId":9,
                 "branchId":5,"status":"PENDING"}
                """);

        verify(useCase, never()).enqueue(any(), any(), anyInt());
        verify(useCase, never()).cancel(any());
    }

    @Test
    void should_ignore_other_service_order_event_types() {
        listener.onServiceOrderEvent("""
                {"eventId":"evt-5","type":"SERVICE_ORDER_CREATED","serviceOrderId":9,"branchId":5}
                """);

        verify(useCase, never()).enqueue(any(), any(), anyInt());
    }

    @Test
    void should_cancel_on_payment_rejected_and_timeout() {
        listener.onPaymentEvent("""
                {"eventId":"evt-6","type":"PAYMENT_REJECTED","serviceOrderId":7}
                """);
        listener.onPaymentEvent("""
                {"eventId":"evt-7","type":"PAYMENT_TIMEOUT","serviceOrderId":8}
                """);

        verify(useCase).cancel(7L);
        verify(useCase).cancel(8L);
    }

    @Test
    void should_ignore_payment_approved() {
        listener.onPaymentEvent("""
                {"eventId":"evt-8","type":"PAYMENT_APPROVED","serviceOrderId":7}
                """);

        verify(useCase, never()).cancel(any());
    }

    @Test
    void should_reject_malformed_payloads() {
        assertThatThrownBy(() -> listener.onServiceOrderEvent("nope"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> listener.onPaymentEvent("nope"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
