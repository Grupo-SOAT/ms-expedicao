package br.com.fiap.postech.msexpedicao.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExecutionJobTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    @Test
    void should_enqueue_job() {
        ExecutionJob job = ExecutionJob.enqueue(1L, 2L, 3, NOW);

        assertThat(job.getServiceOrderId()).isEqualTo(1L);
        assertThat(job.getBranchId()).isEqualTo(2L);
        assertThat(job.getPriority()).isEqualTo(3);
        assertThat(job.getCreatedAt()).isEqualTo(NOW);
        assertThat(job.getStatus()).isEqualTo(ExecutionStatus.QUEUED);
    }

    @Test
    void should_reject_invalid_enqueue_arguments() {
        assertThatThrownBy(() -> ExecutionJob.enqueue(null, 2L, 0, NOW))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ExecutionJob.enqueue(1L, null, 0, NOW))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ExecutionJob.enqueue(1L, 2L, -1, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_start_and_complete_job() {
        ExecutionJob job = ExecutionJob.enqueue(1L, 2L, 0, NOW);

        job.start(NOW);
        assertThat(job.getStatus()).isEqualTo(ExecutionStatus.IN_PROGRESS);
        assertThat(job.getStartedAt()).isEqualTo(NOW);

        job.complete(NOW);
        assertThat(job.getStatus()).isEqualTo(ExecutionStatus.COMPLETED);
        assertThat(job.getCompletedAt()).isEqualTo(NOW);
    }

    @Test
    void should_reject_start_when_not_queued() {
        ExecutionJob job = ExecutionJob.enqueue(1L, 2L, 0, NOW);
        job.start(NOW);

        assertThatThrownBy(() -> job.start(NOW))
                .isInstanceOfSatisfying(InvalidExecutionTransitionException.class, exception -> {
                    assertThat(exception.getSource()).isEqualTo(ExecutionStatus.IN_PROGRESS);
                    assertThat(exception.getTarget()).isEqualTo(ExecutionStatus.IN_PROGRESS);
                });
    }

    @Test
    void should_reject_complete_when_not_in_progress() {
        ExecutionJob job = ExecutionJob.enqueue(1L, 2L, 0, NOW);

        assertThatThrownBy(() -> job.complete(NOW))
                .isInstanceOf(InvalidExecutionTransitionException.class);
    }

    @Test
    void should_restore_job() {
        ExecutionJob job = ExecutionJob.restore(1L, 2L, 4, NOW, ExecutionStatus.COMPLETED, NOW, NOW);

        assertThat(job.getStatus()).isEqualTo(ExecutionStatus.COMPLETED);
        assertThat(job.getPriority()).isEqualTo(4);
        assertThat(job.getStartedAt()).isEqualTo(NOW);
        assertThat(job.getCompletedAt()).isEqualTo(NOW);
    }

    @Test
    void should_cancel_queued_job() {
        ExecutionJob job = ExecutionJob.enqueue(1L, 2L, 0, NOW);

        job.cancel(NOW);

        assertThat(job.getStatus()).isEqualTo(ExecutionStatus.CANCELLED);
        assertThat(job.getCompletedAt()).isEqualTo(NOW);
    }

    @Test
    void should_reject_cancel_when_not_queued() {
        ExecutionJob job = ExecutionJob.enqueue(1L, 2L, 0, NOW);
        job.start(NOW);

        assertThatThrownBy(() -> job.cancel(NOW))
                .isInstanceOf(InvalidExecutionTransitionException.class);
    }
}
