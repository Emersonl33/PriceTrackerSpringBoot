package com.pricetracker.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@DynamoDbBean
public class Product {

    private String userId;      // PK — era tenantId
    private String productId;   // SK
    private String url;
    private String name;
    private BigDecimal currentPrice;
    private String createdAt;
    private String updatedAt;

    @DynamoDbPartitionKey
    public String getUserId() { return userId; }

    @DynamoDbSortKey
    public String getProductId() { return productId; }
}