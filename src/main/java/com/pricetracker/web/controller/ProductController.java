package com.pricetracker.web.controller;

import com.pricetracker.domain.model.PriceSnapshot;
import com.pricetracker.domain.model.Product;
import com.pricetracker.service.ProductService;
import com.pricetracker.web.dto.AddProductRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /** Cadastra um produto para rastrear */
    @PostMapping
    public ResponseEntity<Product> add(@RequestBody AddProductRequest req) {
        return ResponseEntity.ok(productService.addProduct(req.url(), req.name()));
    }

    /** Lista todos os produtos do tenant autenticado */
    @GetMapping
    public ResponseEntity<List<Product>> list() {
        return ResponseEntity.ok(productService.listProducts());
    }

    /** Remove um produto */
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> delete(@PathVariable String productId) {
        productService.deleteProduct(productId);
        return ResponseEntity.noContent().build();
    }

    /** Retorna histórico de preços de um produto */
    @GetMapping("/{productId}/history")
    public ResponseEntity<?> history(@PathVariable String productId) {
        try {
            List<PriceSnapshot> history = productService.getHistory(productId);
            return ResponseEntity.ok(history);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }
}