package br.com.fiap.postech.msexpedicao.adapter.input;

import br.com.fiap.postech.msexpedicao.domain.InvalidExecutionTransitionException;
import br.com.fiap.postech.msexpedicao.domain.usecase.ExecutionJobNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class ExecutionExceptionHandler {

    @ExceptionHandler(ExecutionJobNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ExecutionJobNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(InvalidExecutionTransitionException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidTransition(InvalidExecutionTransitionException exception) {
        return error(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidArgument(IllegalArgumentException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "message", String.valueOf(message)
        ));
    }
}
