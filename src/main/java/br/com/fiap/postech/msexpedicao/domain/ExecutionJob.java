package br.com.fiap.postech.msexpedicao.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * A job in the execution queue. The queue is partitioned by branch and status.
 */
public class ExecutionJob {

    private final Long serviceOrderId;
    private final Long branchId;
    private final int priority;
    private final Instant createdAt;
    private ExecutionStatus status;
    private Instant startedAt;
    private Instant completedAt;

    private ExecutionJob(
            Long serviceOrderId,
            Long branchId,
            int priority,
            Instant createdAt,
            ExecutionStatus status,
            Instant startedAt,
            Instant completedAt
    ) {
        this.serviceOrderId = serviceOrderId;
        this.branchId = branchId;
        this.priority = priority;
        this.createdAt = createdAt;
        this.status = status;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    public static ExecutionJob enqueue(Long serviceOrderId, Long branchId, int priority, Instant now) {
        Objects.requireNonNull(serviceOrderId, "serviceOrderId is required");
        Objects.requireNonNull(branchId, "branchId is required");
        if (priority < 0) {
            throw new IllegalArgumentException("priority must not be negative");
        }
        return new ExecutionJob(serviceOrderId, branchId, priority, now, ExecutionStatus.QUEUED, null, null);
    }

    public static ExecutionJob restore(
            Long serviceOrderId,
            Long branchId,
            int priority,
            Instant createdAt,
            ExecutionStatus status,
            Instant startedAt,
            Instant completedAt
    ) {
        Objects.requireNonNull(serviceOrderId, "serviceOrderId is required");
        Objects.requireNonNull(branchId, "branchId is required");
        Objects.requireNonNull(status, "status is required");
        return new ExecutionJob(serviceOrderId, branchId, priority, createdAt, status, startedAt, completedAt);
    }

    public void start(Instant now) {
        if (status != ExecutionStatus.QUEUED) {
            throw new InvalidExecutionTransitionException(status, ExecutionStatus.IN_PROGRESS);
        }
        this.status = ExecutionStatus.IN_PROGRESS;
        this.startedAt = now;
    }

    public void cancel(Instant now) {
        if (status != ExecutionStatus.QUEUED) {
            throw new InvalidExecutionTransitionException(status, ExecutionStatus.CANCELLED);
        }
        this.status = ExecutionStatus.CANCELLED;
        this.completedAt = now;
    }

    public void complete(Instant now) {
        if (status != ExecutionStatus.IN_PROGRESS) {
            throw new InvalidExecutionTransitionException(status, ExecutionStatus.COMPLETED);
        }
        this.status = ExecutionStatus.COMPLETED;
        this.completedAt = now;
    }

    public Long getServiceOrderId() {
        return serviceOrderId;
    }

    public Long getBranchId() {
        return branchId;
    }

    public int getPriority() {
        return priority;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
