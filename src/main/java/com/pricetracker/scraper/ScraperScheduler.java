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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class ScraperScheduler {

    private static final Logger log = LoggerFactory.getLogger(ScraperScheduler.class);

    private final ProductRepository productRepo;
    private final SnapshotRepository snapshotRepo;
    private final List<GamePriceFetcher> fetchers;
    private final ExecutorService threadPool = Executors.newFixedThreadPool(5);

    public ScraperScheduler(ProductRepository productRepo,
                            SnapshotRepository snapshotRepo,
                            List<GamePriceFetcher> fetchers) {
        this.productRepo = productRepo;
        this.snapshotRepo = snapshotRepo;
        this.fetchers = fetchers;
    }

    @Scheduled(fixedDelayString = "${scraper.interval.ms}")
    public void runScraping() {
        List<Product> all = productRepo.findAll();
        log.info("Scheduler iniciado — {} produtos para verificar", all.size());

        List<CompletableFuture<Void>> futures = all.stream()
                .map(p -> CompletableFuture.runAsync(() -> scrapeProduct(p), threadPool))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        log.info("Scheduler finalizado");
    }

    private void scrapeProduct(Product product) {
        // encontra o fetcher que suporta a URL do produto
        GamePriceFetcher fetcher = fetchers.stream()
                .filter(f -> f.supports(product.getUrl()))
                .findFirst()
                .orElse(null);

        if (fetcher == null) {
            log.warn("Nenhum fetcher suporta a URL: {}", product.getUrl());
            return;
        }

        log.debug("[{}] buscando preço de: {}", fetcher.sourceName(), product.getName());

        Optional<java.math.BigDecimal> price = fetcher.fetchPrice(product.getUrl());

        price.ifPresentOrElse(p -> {
            // salva snapshot no histórico
            PriceSnapshot snapshot = PriceSnapshot.builder()
                    .productId(product.getProductId())
                    .capturedAt(Instant.now().toString())
                    .price(p)
                    .build();
            snapshotRepo.save(snapshot);

            // atualiza currentPrice no produto
            product.setCurrentPrice(p);
            product.setUpdatedAt(Instant.now().toString());
            productRepo.save(product);

            log.info("[{}] {} → R$ {}", fetcher.sourceName(), product.getName(), p);

        }, () -> log.warn("[{}] não retornou preço para: {}",
                fetcher.sourceName(), product.getName()));
    }
}
