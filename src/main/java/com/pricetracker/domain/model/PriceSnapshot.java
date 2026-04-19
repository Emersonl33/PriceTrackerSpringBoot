package com.pricetracker.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@DynamoDbBean
public class PriceSnapshot {
    private String productId;
    private String capturedAt;
    private BigDecimal price;

    @DynamoDbPartitionKey
    public String getProductId() { return productId; }

    @DynamoDbSortKey
    public String getCapturedAt() { return capturedAt; }
}
