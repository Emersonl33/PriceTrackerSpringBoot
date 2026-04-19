package com.pricetracker.domain.repository;

import com.pricetracker.domain.model.Product;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.*;

import java.util.List;

@Repository
public class ProductRepository {

    private final DynamoDbTable<Product> table;

    public ProductRepository(DynamoDbTable<Product> table) {
        this.table = table;
    }

    public void save(Product product) {
        table.putItem(product);
    }

    public List<Product> findByUserId(String userId) {
        QueryConditional condition = QueryConditional
                .keyEqualTo(Key.builder().partitionValue(userId).build());
        return table.query(condition).items().stream().toList();
    }

    public List<Product> findByUserIdAndName(String userId, String name) {
        return findByUserId(userId).stream()
                .filter(p -> p.getName().toLowerCase().contains(name.toLowerCase()))
                .toList();
    }

    public Product findById(String userId, String productId) {
        return table.getItem(Key.builder()
                .partitionValue(userId)
                .sortValue(productId)
                .build());
    }

    public List<Product> findAll() {
        return table.scan().items().stream().toList();
    }

    public void delete(String userId, String productId) {
        table.deleteItem(Key.builder()
                .partitionValue(userId)
                .sortValue(productId)
                .build());
    }
}