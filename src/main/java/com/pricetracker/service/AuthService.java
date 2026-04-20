package com.pricetracker.service;


import com.pricetracker.domain.model.AppUser;
import com.pricetracker.domain.repository.UserRepository;
import com.pricetracker.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public AuthService(JwtService jwtService,
                       PasswordEncoder passwordEncoder,
                       UserRepository userRepository) {
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    @Value("${admin.secret}")
    private String adminSecret;

    public AppUser registerAdmin(String email, String rawPassword, String secret) {
        if (!adminSecret.equals(secret)) {
            throw new IllegalArgumentException("Chave admin inválida");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("E-mail já cadastrado");
        }
        AppUser user = AppUser.builder()
                .id(UUID.randomUUID().toString())
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role("ADMIN")
                .build();
        userRepository.save(user);
        return user;
    }

    public AppUser register(String email, String rawPassword) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("E-mail já cadastrado");
        }
        AppUser user = AppUser.builder()
                .id(UUID.randomUUID().toString())
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .build();
        userRepository.save(user);
        return user;
    }

    public List<AppUser> listUsers() {
        return userRepository.findAll();
    }

    public String login(String email, String rawPassword) {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Credenciais inválidas"));

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Credenciais inválidas");
        }

        return jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());
    }
}
