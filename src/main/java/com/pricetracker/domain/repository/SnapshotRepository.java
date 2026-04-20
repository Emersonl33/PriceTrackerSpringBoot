package com.pricetracker.domain.repository;

import com.pricetracker.domain.model.PriceSnapshot;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.*;

import java.util.List;

@Repository
public class SnapshotRepository {

    private final DynamoDbTable<PriceSnapshot> table;

    public SnapshotRepository(DynamoDbTable<PriceSnapshot> table) {
        this.table = table;
    }

    public void save(PriceSnapshot snapshot) {
        table.putItem(snapshot);
    }

    public List<PriceSnapshot> findByProductId(String productId) {
        QueryConditional condition = QueryConditional
                .keyEqualTo(Key.builder().partitionValue(productId).build());

        return table.query(QueryEnhancedRequest.builder()
                        .queryConditional(condition)
                        .scanIndexForward(false) // ordem reversa (mais recente primeiro)
                        .build())
                .items()
                .stream()
                .toList();
    }
}
