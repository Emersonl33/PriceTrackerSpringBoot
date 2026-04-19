package com.pricetracker.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
public class AppUser {
    private String id;
    private String email;
    private String passwordHash;
}
