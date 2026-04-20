package com.pricetracker.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Schema(name = "ProductRequest", description = "DTO para criação/atualização de produtos")
public record ProductRequest(
        @Schema(description = "Nome do produto", example = "Notebook Dell", maxLength = 120)
        @NotBlank @Size(max = 120) String name,

        @Schema(description = "Preço do produto", example = "1500.00")
        @NotNull @DecimalMin(value = "0.00") BigDecimal price
) {}
