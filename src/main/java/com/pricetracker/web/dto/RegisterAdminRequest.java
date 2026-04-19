package com.pricetracker.web.dto;

public record RegisterAdminRequest(String email, String password, String adminSecret) {}
