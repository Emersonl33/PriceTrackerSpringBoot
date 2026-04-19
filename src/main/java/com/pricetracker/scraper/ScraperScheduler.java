package com.pricetracker.scraper;

import com.pricetracker.domain.model.PriceSnapshot;
import com.pricetracker.domain.model.Product;
import com.pricetracker.domain.repository.ProductRepository;
import com.pricetracker.domain.repository.SnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;

@Component
public class ScraperScheduler {

    private static final Logger log = LoggerFactory.getLogger(ScraperScheduler.class);

    private final ProductRepository productRepo;
    private final SnapshotRepository snapshotRepo;
    private final PriceScraper scraper;
    private final ExecutorService threadPool = Executors.newFixedThreadPool(5);

    public ScraperScheduler(ProductRepository productRepo,
                            SnapshotRepository snapshotRepo,
                            PriceScraper scraper) {
        this.productRepo = productRepo;
        this.snapshotRepo = snapshotRepo;
        this.scraper = scraper;
    }

    @Scheduled(fixedDelayString = "${scraper.interval.ms}")
    public void runScraping() {
        try {
            List<Product> all = productRepo.findAllActive();
            log.info("Scheduler iniciado — {} produtos para verificar", all.size());

            if (all.isEmpty()) {
                log.info("Nenhum produto ativo para scraping");
                return;
            }

            List<CompletableFuture<Void>> futures = all.stream()
                    .map(p -> CompletableFuture.runAsync(() -> scrapeProduct(p), threadPool))
                    .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            log.info("Scheduler finalizado com sucesso");
        } catch (Exception e) {
            log.error("Erro fatal no scheduler de scraping", e);
        }
    }

    private void scrapeProduct(Product product) {
        try {
            log.debug("Scraping: {} ({})", product.getName(), product.getUrl());

            var priceOptional = scraper.scrape(product.getUrl());

            if (priceOptional.isPresent()) {
                var price = priceOptional.get();

                // 1. salva snapshot no histórico
                PriceSnapshot snapshot = PriceSnapshot.builder()
                        .productId(product.getProductId())
                        .capturedAt(Instant.now().toString())
                        .price(price)
                        .build();
                snapshotRepo.save(snapshot);

                // 2. atualiza currentPrice no produto para consulta rápida
                product.setCurrentPrice(price);
                product.setUpdatedAt(Instant.now().toString());
                productRepo.save(product);

                log.info("✓ Preço capturado: {} → R$ {}", product.getName(), price);
            } else {
                log.warn("✗ Não foi possível extrair preço de: {} ({})", product.getName(), product.getUrl());
            }
        } catch (Exception e) {
            log.error("✗ Erro ao fazer scraping de {} ({}): {}",
                    product.getName(), product.getUrl(), e.getMessage(), e);
        }
    }
}

