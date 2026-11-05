package br.com.fiap.postech.msexpedicao.adapter.input;

import br.com.fiap.postech.msexpedicao.domain.ExecutionJob;
import br.com.fiap.postech.msexpedicao.domain.InvalidExecutionTransitionException;
import br.com.fiap.postech.msexpedicao.domain.ExecutionStatus;
import br.com.fiap.postech.msexpedicao.domain.usecase.ExecutionJobNotFoundException;
import br.com.fiap.postech.msexpedicao.domain.usecase.ExecutionQueueUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExecutionQueueControllerTest {

    private final ExecutionQueueUseCase useCase = mock(ExecutionQueueUseCase.class);
    private MockMvc mockMvc;
    private ExecutionJob job;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ExecutionQueueController(useCase))
                .setControllerAdvice(new ExecutionExceptionHandler())
                .build();
        job = ExecutionJob.enqueue(1L, 2L, 3, Instant.parse("2026-01-01T10:00:00Z"));
    }

    @Test
    void should_enqueue_job() throws Exception {
        when(useCase.enqueue(1L, 2L, 3)).thenReturn(job);

        mockMvc.perform(post("/execution-queue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serviceOrderId\":1,\"branchId\":2,\"priority\":3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.serviceOrderId").value(1));
    }

    @Test
    void should_list_queue() throws Exception {
        when(useCase.queue(2L, ExecutionStatus.QUEUED)).thenReturn(List.of(job));

        mockMvc.perform(get("/execution-queue").param("branchId", "2").param("status", "QUEUED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].branchId").value(2));
    }

    @Test
    void should_start_and_complete_job() throws Exception {
        when(useCase.start(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any())).thenReturn(job);
        when(useCase.complete(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any())).thenReturn(job);

        mockMvc.perform(post("/execution-queue/1/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceOrderId").value(1));
        mockMvc.perform(post("/execution-queue/1/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceOrderId").value(1));
    }

    @Test
    void should_return_not_found_for_missing_job() throws Exception {
        when(useCase.start(org.mockito.ArgumentMatchers.eq(9L), org.mockito.ArgumentMatchers.any())).thenThrow(new ExecutionJobNotFoundException(9L));

        mockMvc.perform(post("/execution-queue/9/start")).andExpect(status().isNotFound());
    }

    @Test
    void should_return_conflict_for_invalid_transition() throws Exception {
        when(useCase.start(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any())).thenThrow(new InvalidExecutionTransitionException(
                ExecutionStatus.IN_PROGRESS, ExecutionStatus.IN_PROGRESS));

        mockMvc.perform(post("/execution-queue/1/start")).andExpect(status().isConflict());
    }

    @Test
    void should_return_bad_request_for_invalid_priority() throws Exception {
        when(useCase.enqueue(any(), any(), anyInt()))
                .thenThrow(new IllegalArgumentException("priority must not be negative"));

        mockMvc.perform(post("/execution-queue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serviceOrderId\":1,\"branchId\":2,\"priority\":-1}"))
                .andExpect(status().isBadRequest());
    }
}
