package br.com.fiap.postech.msexpedicao.adapter.output.persistence;

import br.com.fiap.postech.msexpedicao.domain.ExecutionJob;
import br.com.fiap.postech.msexpedicao.domain.ExecutionStatus;
import br.com.fiap.postech.msexpedicao.port.ExecutionJobRepositoryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
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

/**
 * DynamoDB repository for the execution queue.
 *
 * <p>Base table key: {@code PK = SO#<serviceOrderId>}, {@code SK = JOB}. Queue access uses
 * GSI1: {@code GSI1PK = BRANCH#<branchId>#STATUS#<status>} and
 * {@code GSI1SK = <priority>#<createdAt>#<serviceOrderId>}.</p>
 */
@Component
public class DynamoExecutionJobRepository implements ExecutionJobRepositoryPort {

    static final String PK = "PK";
    static final String SK = "SK";
    static final String GSI1PK = "GSI1PK";
    static final String GSI1SK = "GSI1SK";

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;
    private final String queueIndexName;

    public DynamoExecutionJobRepository(
            DynamoDbClient dynamoDbClient,
            @Value("${app.dynamodb.table}") String tableName,
            @Value("${app.dynamodb.queue-index}") String queueIndexName
    ) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
        this.queueIndexName = queueIndexName;
    }

    @Override
    public ExecutionJob save(ExecutionJob job) {
        dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(tableName)
                .item(toItem(job))
                .build());
        return job;
    }

    @Override
    public Optional<ExecutionJob> findByServiceOrderId(Long serviceOrderId) {
        GetItemResponse response = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of(
                        PK, string("SO#" + serviceOrderId),
                        SK, string("JOB")))
                .build());
        return response.hasItem() && !response.item().isEmpty()
                ? Optional.of(toDomain(response.item()))
                : Optional.empty();
    }

    @Override
    public List<ExecutionJob> findQueue(Long branchId, ExecutionStatus status) {
        QueryResponse response = dynamoDbClient.query(QueryRequest.builder()
                .tableName(tableName)
                .indexName(queueIndexName)
                .keyConditionExpression(GSI1PK + " = :pk")
                .expressionAttributeValues(Map.of(
                        ":pk", string(queuePartition(branchId, status))))
                .build());
        return response.items().stream().map(DynamoExecutionJobRepository::toDomain).toList();
    }

    static Map<String, AttributeValue> toItem(ExecutionJob job) {
        var item = new java.util.HashMap<String, AttributeValue>();
        item.put(PK, string("SO#" + job.getServiceOrderId()));
        item.put(SK, string("JOB"));
        item.put(GSI1PK, string(queuePartition(job.getBranchId(), job.getStatus())));
        item.put(GSI1SK, string(sortKey(job)));
        item.put("serviceOrderId", number(job.getServiceOrderId()));
        item.put("branchId", number(job.getBranchId()));
        item.put("priority", number((long) job.getPriority()));
        item.put("status", string(job.getStatus().name()));
        item.put("createdAt", string(job.getCreatedAt().toString()));
        if (job.getStartedAt() != null) {
            item.put("startedAt", string(job.getStartedAt().toString()));
        }
        if (job.getCompletedAt() != null) {
            item.put("completedAt", string(job.getCompletedAt().toString()));
        }
        return item;
    }

    static ExecutionJob toDomain(Map<String, AttributeValue> item) {
        return ExecutionJob.restore(
                Long.valueOf(item.get("serviceOrderId").n()),
                Long.valueOf(item.get("branchId").n()),
                Integer.parseInt(item.get("priority").n()),
                Instant.parse(item.get("createdAt").s()),
                ExecutionStatus.valueOf(item.get("status").s()),
                item.containsKey("startedAt") ? Instant.parse(item.get("startedAt").s()) : null,
                item.containsKey("completedAt") ? Instant.parse(item.get("completedAt").s()) : null
        );
    }

    private static String queuePartition(Long branchId, ExecutionStatus status) {
        return "BRANCH#" + branchId + "#STATUS#" + status.name();
    }

    private static String sortKey(ExecutionJob job) {
        return job.getPriority() + "#" + job.getCreatedAt() + "#" + job.getServiceOrderId();
    }

    private static AttributeValue string(String value) {
        return AttributeValue.builder().s(value).build();
    }

    private static AttributeValue number(Long value) {
        return AttributeValue.builder().n(String.valueOf(value)).build();
    }
}
