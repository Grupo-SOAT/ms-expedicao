package br.com.fiap.postech.msexpedicao.adapter.input;

import br.com.fiap.postech.msexpedicao.domain.ExecutionJob;
import br.com.fiap.postech.msexpedicao.domain.ExecutionStatus;
import br.com.fiap.postech.msexpedicao.domain.usecase.ExecutionQueueUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/execution-queue")
public class ExecutionQueueController {

    private final ExecutionQueueUseCase useCase;

    public ExecutionQueueController(ExecutionQueueUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    public ResponseEntity<ExecutionJobResponse> enqueue(@RequestBody EnqueueRequest request) {
        ExecutionJob job = useCase.enqueue(request.serviceOrderId(), request.branchId(), request.priority());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(job));
    }

    @GetMapping
    public ResponseEntity<List<ExecutionJobResponse>> queue(
            @RequestParam Long branchId,
            @RequestParam ExecutionStatus status
    ) {
        return ResponseEntity.ok(useCase.queue(branchId, status).stream()
                .map(ExecutionQueueController::toResponse)
                .toList());
    }

    @PostMapping("/{serviceOrderId}/start")
    public ResponseEntity<ExecutionJobResponse> start(
            @PathVariable Long serviceOrderId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId
    ) {
        return ResponseEntity.ok(toResponse(useCase.start(serviceOrderId, correlationId)));
    }

    @PostMapping("/{serviceOrderId}/complete")
    public ResponseEntity<ExecutionJobResponse> complete(
            @PathVariable Long serviceOrderId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId
    ) {
        return ResponseEntity.ok(toResponse(useCase.complete(serviceOrderId, correlationId)));
    }

    static ExecutionJobResponse toResponse(ExecutionJob job) {
        return new ExecutionJobResponse(job.getServiceOrderId(), job.getBranchId(), job.getPriority(),
                job.getStatus().name(), job.getCreatedAt(), job.getStartedAt(), job.getCompletedAt());
    }

    public record EnqueueRequest(Long serviceOrderId, Long branchId, int priority) {
    }

    public record ExecutionJobResponse(
            Long serviceOrderId,
            Long branchId,
            int priority,
            String status,
            Instant createdAt,
            Instant startedAt,
            Instant completedAt
    ) {
    }
}
