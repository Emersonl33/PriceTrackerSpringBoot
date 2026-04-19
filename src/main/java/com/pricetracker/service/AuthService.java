package com.pricetracker.service;


import com.pricetracker.domain.model.AppUser;
import com.pricetracker.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    // MVP: usuários em memória. Na Fase 2, troque por tabela DynamoDB.
    private final Map<String, AppUser> users = new ConcurrentHashMap<>();

    public AuthService(JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    public AppUser register(String email, String rawPassword) {
        if (users.values().stream().anyMatch(u -> u.getEmail().equals(email))) {
            throw new IllegalArgumentException("E-mail já cadastrado");
        }
        AppUser user = new AppUser(
                UUID.randomUUID().toString(),
                email,
                passwordEncoder.encode(rawPassword)
        );
        users.put(user.getId(), user);
        return user;
    }

    public String login(String email, String rawPassword) {
        AppUser user = users.values().stream()
                .filter(u -> u.getEmail().equals(email))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Credenciais inválidas"));

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Credenciais inválidas");
        }

        return jwtService.generateToken(user.getId(), user.getEmail());
    }
}
