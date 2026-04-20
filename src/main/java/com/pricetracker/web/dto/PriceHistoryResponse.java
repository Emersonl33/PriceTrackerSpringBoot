package com.pricetracker.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(name = "PriceHistoryResponse", description = "DTO para resposta de histórico de preço")
public record PriceHistoryResponse(
        @Schema(description = "ID do produto", example = "550e8400-e29b-41d4-a716-446655440000")
        String productId,

        @Schema(description = "Timestamp do registro em milliseconds", example = "1713607800000")
        Long timestamp,

        @Schema(description = "Preço registrado", example = "1500.00")
        BigDecimal price,

        @Schema(description = "ID do tenant", example = "org-123")
        String tenantId,

        @Schema(description = "Data e hora de criação", example = "2024-04-19T10:30:00Z")
        OffsetDateTime createdAt
) {}
