package com.pricetracker.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(name = "ProductResponse", description = "DTO para resposta de produtos")
public record ProductResponse(
        @Schema(description = "ID do tenant", example = "org-123")
        String tenantId,

        @Schema(description = "ID único do produto", example = "prod-uuid-123")
        String productId,

        @Schema(description = "Nome do produto", example = "Notebook Dell")
        String name,

        @Schema(description = "Preço do produto", example = "1500.00")
        BigDecimal price,

        @Schema(description = "Data e hora de criação", example = "2024-04-19T10:30:00Z")
        OffsetDateTime createdAt
) {}
