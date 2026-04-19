package com.pricetracker.config;

import com.pricetracker.domain.model.PriceSnapshot;
import com.pricetracker.domain.model.Product;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.net.URI;

@Configuration
public class DynamoDbConfig {

    @Value("${aws.dynamodb.endpoint}")
    private String endpoint;

    @Value("${aws.dynamodb.region}")
    private String region;

    @Value("${aws.dynamodb.accessKey}")
    private String accessKey;

    @Value("${aws.dynamodb.secretKey}")
    private String secretKey;

    @Bean
    public DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }

    @Bean
    public DynamoDbEnhancedClient enhancedClient(DynamoDbClient client) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(client)
                .build();
    }

    @Bean
    public DynamoDbTable<Product> productTable(DynamoDbEnhancedClient enhanced) {
        DynamoDbTable<Product> table = enhanced.table("products",
                TableSchema.fromBean(Product.class));
        createTableIfNotExists(table);
        return table;
    }

    @Bean
    public DynamoDbTable<PriceSnapshot> snapshotTable(DynamoDbEnhancedClient enhanced) {
        DynamoDbTable<PriceSnapshot> table = enhanced.table("price_history",
                TableSchema.fromBean(PriceSnapshot.class));
        createTableIfNotExists(table);
        return table;
    }

    private <T> void createTableIfNotExists(DynamoDbTable<T> table) {
        try {
            table.createTable();
        } catch (ResourceInUseException ignored) {
            // tabela já existe — normal no restart
        }
    }
}