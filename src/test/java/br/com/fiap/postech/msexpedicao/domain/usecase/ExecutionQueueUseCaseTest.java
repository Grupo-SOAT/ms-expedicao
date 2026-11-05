package br.com.fiap.postech.msexpedicao.domain.usecase;

import br.com.fiap.postech.msexpedicao.domain.ExecutionJob;
import br.com.fiap.postech.msexpedicao.domain.ExecutionStatus;
import br.com.fiap.postech.msexpedicao.domain.ExecutionEvent;
import br.com.fiap.postech.msexpedicao.port.ExecutionEventPublisherPort;
import br.com.fiap.postech.msexpedicao.port.ExecutionJobRepositoryPort;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExecutionQueueUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private final ExecutionJobRepositoryPort repository = mock(ExecutionJobRepositoryPort.class);
    private final ExecutionEventPublisherPort publisher = mock(ExecutionEventPublisherPort.class);
    private final ExecutionQueueUseCase useCase =
            new ExecutionQueueUseCase(repository, publisher, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void should_enqueue_new_job() {
        when(repository.findByServiceOrderId(1L)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ExecutionJob job = useCase.enqueue(1L, 2L, 3);

        assertThat(job)
                .usingRecursiveComparison()
                .isEqualTo(ExecutionJob.enqueue(1L, 2L, 3, NOW));
    }

    @Test
    void should_reuse_existing_job() {
        ExecutionJob existing = ExecutionJob.enqueue(1L, 2L, 3, NOW);
        when(repository.findByServiceOrderId(1L)).thenReturn(Optional.of(existing));

        ExecutionJob job = useCase.enqueue(1L, 2L, 3);

        assertThat(job).isSameAs(existing);
        verify(repository, never()).save(any());
    }

    @Test
    void should_start_job() {
        ExecutionJob job = ExecutionJob.enqueue(1L, 2L, 3, NOW);
        when(repository.findByServiceOrderId(1L)).thenReturn(Optional.of(job));
        when(repository.save(job)).thenReturn(job);

        ExecutionJob started = useCase.start(1L, "corr-1");

        assertThat(started.getStatus()).isEqualTo(ExecutionStatus.IN_PROGRESS);
        assertThat(started.getStartedAt()).isEqualTo(NOW);
        verify(publisher).publish(org.mockito.ArgumentMatchers.argThat(
                event -> ExecutionEvent.EXECUTION_STARTED.equals(event.type())
                        && "corr-1".equals(event.correlationId())));
    }

    @Test
    void should_complete_job() {
        ExecutionJob job = ExecutionJob.restore(1L, 2L, 3, NOW, ExecutionStatus.IN_PROGRESS, NOW, null);
        when(repository.findByServiceOrderId(1L)).thenReturn(Optional.of(job));
        when(repository.save(job)).thenReturn(job);

        ExecutionJob completed = useCase.complete(1L, "corr-1");

        assertThat(completed.getStatus()).isEqualTo(ExecutionStatus.COMPLETED);
        assertThat(completed.getCompletedAt()).isEqualTo(NOW);
        verify(publisher).publish(org.mockito.ArgumentMatchers.argThat(
                event -> ExecutionEvent.EXECUTION_COMPLETED.equals(event.type())));
    }

    @Test
    void should_reject_start_when_job_is_missing() {
        when(repository.findByServiceOrderId(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.start(9L, "corr"))
                .isInstanceOf(ExecutionJobNotFoundException.class);
    }

    @Test
    void should_reject_complete_when_job_is_missing() {
        when(repository.findByServiceOrderId(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.complete(9L, "corr"))
                .isInstanceOf(ExecutionJobNotFoundException.class);
    }

    @Test
    void should_query_queue_by_branch_and_status() {
        List<ExecutionJob> jobs = List.of(ExecutionJob.enqueue(1L, 2L, 0, NOW));
        when(repository.findQueue(2L, ExecutionStatus.QUEUED)).thenReturn(jobs);

        assertThat(useCase.queue(2L, ExecutionStatus.QUEUED)).isSameAs(jobs);
    }

    @Test
    void should_cancel_queued_job() {
        ExecutionJob job = ExecutionJob.enqueue(1L, 2L, 3, NOW);
        when(repository.findByServiceOrderId(1L)).thenReturn(Optional.of(job));
        when(repository.save(job)).thenReturn(job);

        useCase.cancel(1L);

        assertThat(job.getStatus()).isEqualTo(ExecutionStatus.CANCELLED);
        verify(repository).save(job);
    }

    @Test
    void should_ignore_cancel_when_job_is_missing() {
        when(repository.findByServiceOrderId(9L)).thenReturn(Optional.empty());

        useCase.cancel(9L);

        verify(repository, never()).save(any());
    }

    @Test
    void should_not_cancel_job_already_in_progress() {
        ExecutionJob job = ExecutionJob.restore(1L, 2L, 3, NOW, ExecutionStatus.IN_PROGRESS, NOW, null);
        when(repository.findByServiceOrderId(1L)).thenReturn(Optional.of(job));

        useCase.cancel(1L);

        assertThat(job.getStatus()).isEqualTo(ExecutionStatus.IN_PROGRESS);
        verify(repository, never()).save(any());
    }
}
