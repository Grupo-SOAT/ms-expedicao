package br.com.fiap.postech.msexpedicao.domain.usecase;

import br.com.fiap.postech.msexpedicao.domain.ExecutionJob;
import br.com.fiap.postech.msexpedicao.domain.ExecutionStatus;
import br.com.fiap.postech.msexpedicao.domain.ExecutionEvent;
import br.com.fiap.postech.msexpedicao.port.ExecutionEventPublisherPort;
import br.com.fiap.postech.msexpedicao.port.ExecutionJobRepositoryPort;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Drives the execution queue: enqueue, start, complete and query by branch.
 */
public class ExecutionQueueUseCase {

    private final ExecutionJobRepositoryPort repositoryPort;
    private final ExecutionEventPublisherPort eventPublisherPort;
    private final Clock clock;

    public ExecutionQueueUseCase(ExecutionJobRepositoryPort repositoryPort,
                                 ExecutionEventPublisherPort eventPublisherPort, Clock clock) {
        this.repositoryPort = repositoryPort;
        this.eventPublisherPort = eventPublisherPort;
        this.clock = clock;
    }

    public ExecutionJob enqueue(Long serviceOrderId, Long branchId, int priority) {
        return repositoryPort.findByServiceOrderId(serviceOrderId)
                .orElseGet(() -> repositoryPort.save(
                        ExecutionJob.enqueue(serviceOrderId, branchId, priority, Instant.now(clock))));
    }

    public ExecutionJob start(Long serviceOrderId, String correlationId) {
        ExecutionJob job = find(serviceOrderId);
        job.start(Instant.now(clock));
        ExecutionJob saved = repositoryPort.save(job);
        eventPublisherPort.publish(ExecutionEvent.of(
                ExecutionEvent.EXECUTION_STARTED, saved, Instant.now(clock), correlationId));
        return saved;
    }

    public ExecutionJob complete(Long serviceOrderId, String correlationId) {
        ExecutionJob job = find(serviceOrderId);
        job.complete(Instant.now(clock));
        ExecutionJob saved = repositoryPort.save(job);
        eventPublisherPort.publish(ExecutionEvent.of(
                ExecutionEvent.EXECUTION_COMPLETED, saved, Instant.now(clock), correlationId));
        return saved;
    }

    public void cancel(Long serviceOrderId) {
        repositoryPort.findByServiceOrderId(serviceOrderId).ifPresent(job -> {
            if (job.getStatus() == ExecutionStatus.QUEUED) {
                job.cancel(Instant.now(clock));
                repositoryPort.save(job);
            }
        });
    }

    public List<ExecutionJob> queue(Long branchId, ExecutionStatus status) {
        return repositoryPort.findQueue(branchId, status);
    }

    private ExecutionJob find(Long serviceOrderId) {
        return repositoryPort.findByServiceOrderId(serviceOrderId)
                .orElseThrow(() -> new ExecutionJobNotFoundException(serviceOrderId));
    }
}
