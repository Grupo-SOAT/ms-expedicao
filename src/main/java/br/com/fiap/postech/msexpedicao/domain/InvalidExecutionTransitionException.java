package br.com.fiap.postech.msexpedicao.domain;

public class InvalidExecutionTransitionException extends RuntimeException {

    private final ExecutionStatus source;
    private final ExecutionStatus target;

    public InvalidExecutionTransitionException(ExecutionStatus source, ExecutionStatus target) {
        super("Execution transition not allowed: " + source + " -> " + target);
        this.source = source;
        this.target = target;
    }

    public ExecutionStatus getSource() {
        return source;
    }

    public ExecutionStatus getTarget() {
        return target;
    }
}
