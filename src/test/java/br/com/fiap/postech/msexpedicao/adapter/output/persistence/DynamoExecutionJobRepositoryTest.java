package br.com.fiap.postech.msexpedicao.adapter.output.persistence;

import br.com.fiap.postech.msexpedicao.domain.ExecutionJob;
import br.com.fiap.postech.msexpedicao.domain.ExecutionStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DynamoExecutionJobRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private final DynamoDbClient client = mock(DynamoDbClient.class);
    private final DynamoExecutionJobRepository repository =
            new DynamoExecutionJobRepository(client, "execution", "GSI1");

    @Test
    void should_put_item_with_base_and_queue_keys() {
        ExecutionJob job = ExecutionJob.enqueue(1L, 2L, 3, NOW);

        ExecutionJob saved = repository.save(job);

        assertThat(saved).isSameAs(job);
        ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);
        verify(client).putItem(captor.capture());
        assertThat(captor.getValue().tableName()).isEqualTo("execution");
        assertThat(captor.getValue().item())
                .containsEntry("PK", AttributeValue.builder().s("SO#1").build())
                .containsEntry("SK", AttributeValue.builder().s("JOB").build())
                .containsEntry("GSI1PK", AttributeValue.builder().s("BRANCH#2#STATUS#QUEUED").build())
                .containsEntry("status", AttributeValue.builder().s("QUEUED").build())
                .containsEntry("priority", AttributeValue.builder().n("3").build())
                .containsEntry("GSI1SK", AttributeValue.builder()
                        .s("3#2026-01-01T10:00:00Z#1").build());
    }

    @Test
    void should_find_job_by_service_order_id() {
        when(client.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder()
                .item(item())
                .build());

        Optional<ExecutionJob> found = repository.findByServiceOrderId(1L);

        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(ExecutionStatus.QUEUED);
        assertThat(found.get().getBranchId()).isEqualTo(2L);
    }

    @Test
    void should_return_empty_when_item_does_not_exist() {
        when(client.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder().build());

        assertThat(repository.findByServiceOrderId(1L)).isEmpty();
    }

    @Test
    void should_query_queue_by_branch_and_status() {
        when(client.query(any(QueryRequest.class))).thenReturn(QueryResponse.builder()
                .items(List.of(item()))
                .build());

        List<ExecutionJob> jobs = repository.findQueue(2L, ExecutionStatus.QUEUED);

        ArgumentCaptor<QueryRequest> captor = ArgumentCaptor.forClass(QueryRequest.class);
        verify(client).query(captor.capture());
        assertThat(captor.getValue().indexName()).isEqualTo("GSI1");
        assertThat(captor.getValue().expressionAttributeValues())
                .containsEntry(":pk", AttributeValue.builder().s("BRANCH#2#STATUS#QUEUED").build());
        assertThat(jobs).hasSize(1);
    }

    private Map<String, AttributeValue> item() {
        return Map.of(
                "PK", AttributeValue.builder().s("SO#1").build(),
                "SK", AttributeValue.builder().s("JOB").build(),
                "GSI1PK", AttributeValue.builder().s("BRANCH#2#STATUS#QUEUED").build(),
                "GSI1SK", AttributeValue.builder().s("3#2026-01-01T10:00:00Z#1").build(),
                "serviceOrderId", AttributeValue.builder().n("1").build(),
                "branchId", AttributeValue.builder().n("2").build(),
                "priority", AttributeValue.builder().n("3").build(),
                "status", AttributeValue.builder().s("QUEUED").build(),
                "createdAt", AttributeValue.builder().s("2026-01-01T10:00:00Z").build()
        );
    }
}
