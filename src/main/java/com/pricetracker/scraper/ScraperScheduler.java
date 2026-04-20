package com.pricetracker.scraper;

import com.pricetracker.domain.model.Product;
import com.pricetracker.domain.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class ScraperScheduler {

    private static final Logger log = LoggerFactory.getLogger(ScraperScheduler.class);

    private final ProductService productService;
    private final List<GamePriceFetcher> fetchers;
    private final ExecutorService threadPool = Executors.newFixedThreadPool(5);

    public ScraperScheduler(ProductService productService,
                            List<GamePriceFetcher> fetchers) {
        this.productService = productService;
        this.fetchers = fetchers;
    }

    @Scheduled(fixedDelayString = "${scraper.interval.ms}")
    public void runScraping() {
        List<Product> all = productService.findAllProducts();
        log.info("Scheduler iniciado — {} produtos para verificar", all.size());

        List<CompletableFuture<Void>> futures = all.stream()
                .map(p -> CompletableFuture.runAsync(() -> scrapeProduct(p), threadPool))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        log.info("Scheduler finalizado");
    }

    private void scrapeProduct(Product product) {
        GamePriceFetcher fetcher = fetchers.stream()
                .filter(f -> f.supports(product.getUrl()))
                .findFirst()
                .orElse(null);

        if (fetcher == null) {
            log.warn("Nenhum fetcher suporta: {}", product.getUrl());
            return;
        }

        log.debug("[{}] buscando: {}", fetcher.sourceName(), product.getName());

        fetcher.fetchPrice(product.getUrl()).ifPresentOrElse(price -> {
            productService.saveSnapshot(product.getProductId(), price);
            productService.updateCurrentPrice(product, price);
            log.info("[{}] {} → R$ {}", fetcher.sourceName(), product.getName(), price);
        }, () -> log.warn("[{}] sem preço para: {}", fetcher.sourceName(), product.getName()));
    }
}
