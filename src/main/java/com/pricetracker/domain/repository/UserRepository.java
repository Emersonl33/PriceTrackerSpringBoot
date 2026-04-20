package com.pricetracker.domain.repository;

import com.pricetracker.domain.model.AppUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {

    private static final Logger log = LoggerFactory.getLogger(UserRepository.class);

    private final DynamoDbTable<AppUser> table;

    public UserRepository(DynamoDbTable<AppUser> table) {
        this.table = table;
    }

    public void save(AppUser user) {
        log.info("Salvando usuário: {}", user.getEmail());
        table.putItem(user);
    }

    public Optional<AppUser> findByEmail(String email) {
        log.info("Buscando usuário por email: {}", email);

        QueryConditional condition = QueryConditional
                .keyEqualTo(Key.builder().partitionValue(email).build());

        return table.query(condition)
                .items()
                .stream()
                .findFirst();
    }

    public Optional<AppUser> findById(String id) {
        log.info("Buscando usuário por id: {}", id);

        return table.scan()
                .items()
                .stream()
                .filter(u -> u.getId().equals(id))
                .findFirst();
    }

    public List<AppUser> findAll() {
        return table.scan().items().stream().toList();
    }
}
