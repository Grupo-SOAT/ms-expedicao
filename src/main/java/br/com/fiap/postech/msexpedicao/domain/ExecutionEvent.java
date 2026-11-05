package br.com.fiap.postech.msexpedicao.domain;

import java.time.Instant;
import java.util.UUID;

public record ExecutionEvent(
        String eventId,
        String type,
        Instant occurredAt,
        String correlationId,
        Long serviceOrderId,
        Long branchId
) {

    public static final String EXECUTION_STARTED = "EXECUTION_STARTED";
    public static final String EXECUTION_COMPLETED = "EXECUTION_COMPLETED";

    public static ExecutionEvent of(String type, ExecutionJob job, Instant now, String correlationId) {
        return new ExecutionEvent(UUID.randomUUID().toString(), type, now, correlationId,
                job.getServiceOrderId(), job.getBranchId());
    }
}
