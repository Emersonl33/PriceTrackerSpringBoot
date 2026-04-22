package com.pricetracker.domain.service;

import com.pricetracker.domain.model.PriceSnapshot;
import com.pricetracker.domain.model.Product;
import com.pricetracker.domain.repository.ProductRepository;
import com.pricetracker.domain.repository.SnapshotRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepo;
    private final SnapshotRepository snapshotRepo;
    private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(BRAZIL_ZONE);

    public ProductService(ProductRepository productRepo,
                          SnapshotRepository snapshotRepo) {
        this.productRepo = productRepo;
        this.snapshotRepo = snapshotRepo;
    }

    private String currentUserId() {
        return (String) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
    }

    private String getNowBRL() {
        return FORMATTER.format(Instant.now());
    }

    public Product addProduct(String url, String name) {
        String userId = currentUserId();

        boolean nameExists = productRepo.findByUserId(userId)
                .stream()
                .anyMatch(p -> p.getName().equalsIgnoreCase(name));

        if (nameExists) {
            throw new IllegalArgumentException("Você já tem um jogo cadastrado com o nome: " + name);
        }

        Product product = Product.builder()
                .userId(userId)
                .productId(UUID.randomUUID().toString())
                .url(url)
                .name(name)
                .createdAt(getNowBRL())
                .updatedAt(getNowBRL())
                .build();
        productRepo.save(product);
        return product;
    }

    public List<Product> listProducts() {
        return productRepo.findByUserId(currentUserId());
    }

    public List<Product> findByName(String name) {
        return productRepo.findByUserIdAndName(currentUserId(), name);
    }

    public void deleteProduct(String productId) {
        productRepo.delete(currentUserId(), productId);
    }

    public List<PriceSnapshot> getHistory(String productId) {
        Product product = productRepo.findById(currentUserId(), productId);
        if (product == null) {
            throw new NoSuchElementException("Produto não encontrado");
        }
        return snapshotRepo.findByProductId(productId);
    }

    public Map<String, List<PriceSnapshot>> listAllProductsWithHistory() {
        checkAdmin();
        return productRepo.findAll().stream()
                .collect(Collectors.toMap(
                        p -> p.getName() + " (user: " + p.getUserId() + ")",
                        p -> snapshotRepo.findByProductId(p.getProductId())
                ));
    }

    public List<Product> listAllProducts() {
        checkAdmin();
        return productRepo.findAll();
    }

    public void saveSnapshot(String productId, BigDecimal price) {
        PriceSnapshot snapshot = PriceSnapshot.builder()
                .productId(productId)
                .capturedAt(getNowBRL())
                .price(price)
                .build();
        snapshotRepo.save(snapshot);
    }

    public void updateCurrentPrice(Product product, BigDecimal price) {
        product.setCurrentPrice(price);
        product.setUpdatedAt(getNowBRL());
        productRepo.save(product);
    }

    public List<Product> findAllProducts() {
        return productRepo.findAll();
    }

    private void checkAdmin() {
        String role = SecurityContextHolder.getContext()
                .getAuthentication().getAuthorities()
                .stream()
                .findFirst()
                .map(a -> a.getAuthority())
                .orElse("");

        if (!role.equals("ROLE_ADMIN")) {
            throw new AccessDeniedException("Acesso negado");
        }
    }
}