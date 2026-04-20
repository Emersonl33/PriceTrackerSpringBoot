package com.pricetracker.web.controller;

import com.pricetracker.domain.model.PriceSnapshot;
import com.pricetracker.domain.model.Product;
import com.pricetracker.domain.service.ProductService;
import com.pricetracker.web.dto.AddProductRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/products")
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<?> add(@RequestBody AddProductRequest req) {
        log.info("Received request to add product: url={}, name={}", req.url(), req.name());
        try {
            Product product = productService.addProduct(req.url(), req.name());
            log.info("Product added successfully: {}", product);
            return ResponseEntity.ok(product);
        } catch (IllegalArgumentException e) {
            log.warn("Duplicate product name: {}", e.getMessage());
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<Product>> list() {
        log.info("Received request to list products");
        List<Product> products = productService.listProducts();
        log.info("Returning {} products", products.size());
        return ResponseEntity.ok(products);
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<?> delete(@PathVariable String productId) {
        log.info("Received request to delete product with id: {}", productId);
        try {
            productService.deleteProduct(productId);
            log.info("Product deleted successfully: {}", productId);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            log.warn("Product not found for id: {}", productId);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{productId}/history")
    public ResponseEntity<?> history(@PathVariable String productId) {
        log.info("Received request to get history for product id: {}", productId);
        try {
            List<PriceSnapshot> history = productService.getHistory(productId);
            log.info("Returning history with {} snapshots for product id: {}", history.size(), productId);
            return ResponseEntity.ok(history);
        } catch (NoSuchElementException e) {
            log.warn("Product not found for id: {}", productId);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/history/search")
    public ResponseEntity<?> historyByName(@RequestParam String name) {
        try {
            log.info("Received request to search history by name: {}", name);

            List<Product> products = productService.findByName(name);

            if (products.isEmpty()) {
                log.warn("No products found with name: {}", name);
                return ResponseEntity.notFound().build();
            }

            products.forEach(p ->
                    log.info("Found product: id={}, name={}", p.getProductId(), p.getName())
            );

            Map<String, List<PriceSnapshot>> result = products.stream()
                    .collect(Collectors.groupingBy(
                            Product::getName,
                            Collectors.flatMapping(
                                    p -> productService.getHistory(p.getProductId()).stream(),
                                    Collectors.toList()
                            )
                    ));

            log.info("Returning history for {} products", result.size());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error searching history by name '{}': {}", name, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Erro interno ao buscar histórico");
        }
    }

    @GetMapping("/admin/all")
    public ResponseEntity<?> listAll() {
        try {
            return ResponseEntity.ok(productService.listAllProducts());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(403).body("Acesso negado");
        } catch (Exception e) {
            log.error("Erro ao listar todos os produtos", e);
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @GetMapping("/admin/history")
    public ResponseEntity<?> listAllHistory() {
        try {
            return ResponseEntity.ok(productService.listAllProductsWithHistory());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(403).body("Acesso negado");
        } catch (Exception e) {
            log.error("Erro ao listar histórico completo", e);
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}