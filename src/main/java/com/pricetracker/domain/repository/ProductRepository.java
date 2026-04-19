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

    public List<Product> findByTenantId(String tenantId) {
        QueryConditional condition = QueryConditional
                .keyEqualTo(Key.builder().partitionValue(tenantId).build());

        return table.query(condition)
                .items()
                .stream()
                .toList();
    }

    public Product findById(String tenantId, String productId) {
        return table.getItem(Key.builder()
                .partitionValue(tenantId)
                .sortValue(productId)
                .build());
    }

    // usado pelo scheduler: busca todos os produtos de todos os tenants
    public List<Product> findAll() {
        return table.scan().items().stream().toList();
    }

    public List<Product> findAllActive() {
        // busca todos os produtos de todos os tenants (para o scheduler processar)
        return table.scan().items().stream()
                .filter(p -> p.getUrl() != null && !p.getUrl().isBlank())
                .toList();
    }

    public void delete(String tenantId, String productId) {
        table.deleteItem(Key.builder()
                .partitionValue(tenantId)
                .sortValue(productId)
                .build());
    }
}