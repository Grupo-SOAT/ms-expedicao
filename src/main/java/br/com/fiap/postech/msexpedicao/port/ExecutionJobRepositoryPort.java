package br.com.fiap.postech.msexpedicao.port;

import br.com.fiap.postech.msexpedicao.domain.ExecutionJob;
import br.com.fiap.postech.msexpedicao.domain.ExecutionStatus;

import java.util.List;
import java.util.Optional;

public interface ExecutionJobRepositoryPort {

    ExecutionJob save(ExecutionJob job);

    Optional<ExecutionJob> findByServiceOrderId(Long serviceOrderId);

    List<ExecutionJob> findQueue(Long branchId, ExecutionStatus status);
}
