package br.com.fiap.postech.msexpedicao.domain.usecase;

public class ExecutionJobNotFoundException extends RuntimeException {

    public ExecutionJobNotFoundException(Long serviceOrderId) {
        super("Execution job not found for service order: " + serviceOrderId);
    }
}
