package com.pricetracker.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class AppUser {

    private String email;
    private String id;
    private String passwordHash;
    private String role; // "ADMIN" ou "USER"

    @DynamoDbPartitionKey
    public String getEmail() { return email; }
}
