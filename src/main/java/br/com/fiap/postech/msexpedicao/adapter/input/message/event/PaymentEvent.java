package br.com.fiap.postech.msexpedicao.adapter.input.message.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class PaymentEvent {

    private String eventId;
    private String type;
    private Instant occurredAt;
    private String correlationId;
    private Long serviceOrderId;
    private String paymentId;
}
