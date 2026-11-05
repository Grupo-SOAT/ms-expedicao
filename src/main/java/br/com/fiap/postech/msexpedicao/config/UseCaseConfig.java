package br.com.fiap.postech.msexpedicao.config;

import br.com.fiap.postech.msexpedicao.domain.usecase.ExecutionQueueUseCase;
import br.com.fiap.postech.msexpedicao.port.ExecutionEventPublisherPort;
import br.com.fiap.postech.msexpedicao.port.ExecutionJobRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class UseCaseConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public ExecutionQueueUseCase executionQueueUseCase(
            ExecutionJobRepositoryPort repositoryPort,
            ExecutionEventPublisherPort eventPublisherPort,
            Clock clock
    ) {
        return new ExecutionQueueUseCase(repositoryPort, eventPublisherPort, clock);
    }
}
