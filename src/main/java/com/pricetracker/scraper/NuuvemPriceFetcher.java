package com.pricetracker.scraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class NuuvemPriceFetcher implements GamePriceFetcher {

    private static final Logger log = LoggerFactory.getLogger(NuuvemPriceFetcher.class);

    // Captura valores como: 49,90 / 149,99 / 9,90
    private static final Pattern PRICE_PATTERN =
            Pattern.compile("(\\d{1,3}(?:\\.\\d{3})*,\\d{2})");

    @Override
    public boolean supports(String url) {
        return url != null && url.contains("nuuvem.com");
    }

    @Override
    public String sourceName() {
        return "Nuuvem";
    }

    @Override
    public Optional<BigDecimal> fetchPrice(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                            "AppleWebKit/537.36 (KHTML, like Gecko) " +
                            "Chrome/120.0.0.0 Safari/537.36")
                    .timeout(10_000)
                    .get();

            return trySelectors(doc, url);

        } catch (Exception e) {
            log.error("Erro ao buscar preço na Nuuvem para {}: {}", url, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<BigDecimal> trySelectors(Document doc, String url) {
        // Seletor principal da Nuuvem
        String price = text(doc, ".product-price--val");
        if (price != null) return parsePrice(price, url);

        // Seletor alternativo (promoção)
        price = text(doc, ".product-discount--val");
        if (price != null) return parsePrice(price, url);

        // Seletor genérico de preço
        price = text(doc, "[class*='price']");
        if (price != null) return parsePrice(price, url);

        // meta tag og:price
        Element meta = doc.selectFirst("meta[property=product:price:amount]");
        if (meta != null) return parsePrice(meta.attr("content"), url);

        log.warn("Nenhum seletor encontrou preço na Nuuvem para: {}", url);
        return Optional.empty();
    }

    private Optional<BigDecimal> parsePrice(String raw, String url) {
        if (raw == null || raw.isBlank()) return Optional.empty();

        Matcher matcher = PRICE_PATTERN.matcher(raw);
        if (!matcher.find()) {
            log.warn("Padrão de preço não encontrado em '{}' para {}", raw, url);
            return Optional.empty();
        }

        try {
            String normalized = matcher.group(1)
                    .replaceAll("\\.", "")   // remove separador de milhar
                    .replace(",", ".");       // troca vírgula decimal por ponto

            BigDecimal price = new BigDecimal(normalized);
            log.info("Nuuvem preço: R$ {} em {}", price, url);
            return Optional.of(price);

        } catch (NumberFormatException e) {
            log.warn("Não foi possível converter '{}' para BigDecimal", raw);
            return Optional.empty();
        }
    }

    private String text(Document doc, String selector) {
        Element el = doc.selectFirst(selector);
        return el != null ? el.text() : null;
    }
}