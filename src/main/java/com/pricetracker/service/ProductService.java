package com.pricetracker.service;

import com.pricetracker.domain.model.PriceSnapshot;
import com.pricetracker.domain.model.Product;
import com.pricetracker.domain.repository.ProductRepository;
import com.pricetracker.domain.repository.SnapshotRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class ProductService {

    private final ProductRepository productRepo;
    private final SnapshotRepository snapshotRepo;

    public ProductService(ProductRepository productRepo, SnapshotRepository snapshotRepo) {
        this.productRepo = productRepo;
        this.snapshotRepo = snapshotRepo;
    }

    /** Pega o tenantId do JWT — nenhum controller precisa passar isso manualmente */
    private String currentTenantId() {
        return (String) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
    }

    public Product addProduct(String url, String name) {
        Product product = Product.builder()
                .tenantId(currentTenantId())
                .productId(UUID.randomUUID().toString())
                .url(url)
                .name(name)
                .createdAt(Instant.now().toString())
                .updatedAt(Instant.now().toString())
                .build();
        productRepo.save(product);
        return product;
    }

    public List<Product> listProducts() {
        return productRepo.findByTenantId(currentTenantId());
    }

    public void deleteProduct(String productId) {
        productRepo.delete(currentTenantId(), productId);
    }

    public List<PriceSnapshot> getHistory(String productId) {
        // valida que o produto pertence ao tenant antes de retornar histórico
        Product product = productRepo.findById(currentTenantId(), productId);
        if (product == null) {
            throw new NoSuchElementException("Produto não encontrado");
        }
        return snapshotRepo.findByProductId(productId);
    }
}
