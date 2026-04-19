package com.pricetracker.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Schema(name = "PriceHistoryRequest", description = "DTO para registrar histórico de preço")
public record PriceHistoryRequest(
        @Schema(description = "Preço do produto no momento", example = "1500.00")
        @NotNull @DecimalMin(value = "0.00") BigDecimal price
) {}
