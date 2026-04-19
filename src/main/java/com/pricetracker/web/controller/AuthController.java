package com.pricetracker.web.controller;

import com.pricetracker.service.AuthService;
import com.pricetracker.web.dto.LoginRequest;
import com.pricetracker.web.dto.LoginResponse;
import com.pricetracker.web.dto.RegisterAdminRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/users")
    public ResponseEntity<?> listUsers() {
        try {
            return ResponseEntity.ok(authService.listUsers());
        } catch (Exception e) {
            log.error("Erro ao listar usuários", e);
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody LoginRequest req) {
        log.info("Register attempt for email: {}", req.email());
        try {
            authService.register(req.email(), req.password());
            String token = authService.login(req.email(), req.password());
            log.info("Registration successful for email: {}", req.email());
            return ResponseEntity.ok(new LoginResponse(token));
        } catch (IllegalArgumentException e) {
            log.warn("Registration failed for email: {} - {}", req.email(), e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/register/admin")
    public ResponseEntity<?> registerAdmin(@RequestBody RegisterAdminRequest req) {
        try {
            authService.registerAdmin(req.email(), req.password(), req.adminSecret());
            String token = authService.login(req.email(), req.password());
            return ResponseEntity.ok(new LoginResponse(token));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        log.info("Login attempt for email: {}", req.email());
        try {
            String token = authService.login(req.email(), req.password());
            log.info("Login successful for email: {}", req.email());
            return ResponseEntity.ok(new LoginResponse(token));
        } catch (IllegalArgumentException e) {
            log.warn("Login failed for email: {} - {}", req.email(), e.getMessage());
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }
}