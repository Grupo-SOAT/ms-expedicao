package br.com.fiap.postech.msexpedicao.port;

import br.com.fiap.postech.msexpedicao.domain.ExecutionEvent;

public interface ExecutionEventPublisherPort {

    void publish(ExecutionEvent event);
}
