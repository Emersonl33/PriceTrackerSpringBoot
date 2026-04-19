package com.pricetracker.scraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.*;

@Component
public class PriceScraper {

    private static final Logger log = LoggerFactory.getLogger(PriceScraper.class);

    // Regex MAIS PRECISO: deve ter R$ ou estar em contexto de preço
    private static final Pattern PRICE_WITH_CURRENCY =
            Pattern.compile("R\\$\\s*([\\d.]+,\\d{2})|R\\$\\s*([\\d,]+\\.\\d{2})");

    // Fallback: números maiores (típico de preços)
    private static final Pattern PRICE_PATTERN =
            Pattern.compile("([1-9]\\d{0,3}[,.]?\\d{0,3}[,.]\\d{2})");

    public Optional<BigDecimal> scrape(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(10_000)
                    .followRedirects(true)
                    .get();

            log.debug("Scraping iniciado para: {}", url);

            String rawPrice = trySelectors(doc, url);

            if (rawPrice == null) {
                log.warn(" Nenhum seletor de preço encontrou valor em: {}", url);
                return Optional.empty();
            }

            log.debug("Preço bruto encontrado: {}", rawPrice);
            return parsePrice(rawPrice);

        } catch (Exception e) {
            log.error(" Erro ao fazer scraping de {}: {}", url, e.getMessage());
            return Optional.empty();
        }
    }

    private String trySelectors(Document doc, String url) {
        // 1. Mercado Livre - preço principal (dentro de span com data-price)
        String price = attr(doc, "[data-price]", "data-price");
        if (isValidPrice(price)) {
            log.debug("✓ ML (data-price): {}", price);
            return price;
        }

        // 2. Mercado Livre - alternativo
        price = text(doc, ".andes-money-amount__fraction");
        if (isValidPrice(price)) {
            log.debug("✓ ML (andes-money): {}", price);
            return price;
        }

        // 3. Amazon - preço inteiro (melhorado)
        price = text(doc, ".a-price-whole");
        if (isValidPrice(price)) {
            log.debug("✓ Amazon (a-price-whole): {}", price);
            return price;
        }

        // 4. Amazon - preço com decimais
        price = text(doc, ".a-price-fraction");
        if (isValidPrice(price)) {
            log.debug("✓ Amazon (a-price-fraction): {}", price);
            return price;
        }

        // 5. Meta tags genéricas (mais confiável)
        price = attr(doc, "meta[property=product:price:amount]", "content");
        if (isValidPrice(price)) {
            log.debug("✓ Meta tag (price:amount): {}", price);
            return price;
        }

        // 6. Schema.org microdata
        price = attr(doc, "[itemprop=price]", "content");
        if (isValidPrice(price)) {
            log.debug(" Schema.org (itemprop): {}", price);
            return price;
        }

        // 7. Última opção: procurar por padrão "R$ XXXX,XX"
        price = extractFromBodyText(doc);
        if (isValidPrice(price)) {
            log.debug("Regex body (R$): {}", price);
            return price;
        }

        return null;
    }

    /**     * Procura por "R$ 1.234,56" no texto do documento     * Mais preciso que antes     */
    private String extractFromBodyText(Document doc) {
        String bodyText = doc.body().text();

        // Primeiro tenta padrão com "R$"
        Matcher matcher = PRICE_WITH_CURRENCY.matcher(bodyText);
        if (matcher.find()) {
            return matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
        }

        // Se não encontrar "R$", tenta padrão genérico
        matcher = PRICE_PATTERN.matcher(bodyText);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    /**     * Valida se a string pode ser um preço     * - Não pode ser vazia     * - Deve conter números     * - Não pode ser muito pequeno (< 0.50)     * - Não pode ser muito grande (> 999.999,99)     */
    private boolean isValidPrice(String price) {
        if (price == null || price.trim().isEmpty()) {
            return false;
        }

        price = price.trim().replaceAll("[^\\d,.]", "");

        try {
            // Tenta fazer parse para verificar
            BigDecimal bd = new BigDecimal(normalizePrice(price));

            // Validações
            BigDecimal min = new BigDecimal("0.50");
            BigDecimal max = new BigDecimal("999999.99");

            return bd.compareTo(min) >= 0 && bd.compareTo(max) <= 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**     * Normaliza a string de preço para formato padrão     * "1.299,90" -> "1299.90"     * "1299.90"  -> "1299.90"     */
    private String normalizePrice(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new NumberFormatException("Preço vazio");
        }

        raw = raw.trim();

        // Se tem vírgula, é formato BR
        if (raw.contains(",")) {
            // Remove todos os pontos (separadores de milhar)
            raw = raw.replaceAll("\\.", "");
            // Troca vírgula por ponto
            raw = raw.replace(",", ".");
        } else if (raw.contains(".")) {
            // Pode ser formato US ou BR
            // Se o ponto é a 3ª posição do final, é separador de milhar (BR)
            int lastDot = raw.lastIndexOf(".");
            if (lastDot > 0 && raw.length() - lastDot == 3) {
                // Formato BR: "1.234.567,89" mas sem vírgula
                // Tira todos os pontos
                raw = raw.replaceAll("\\.", "");
            }
            // Se for "123.45" (US format), deixa como está
        } else {
            // Sem separador, trata como número simples
            // "123" -> "123.00"
            raw = raw + ".00";
        }

        return raw;
    }

    private Optional<BigDecimal> parsePrice(String raw) {
        try {
            String normalized = normalizePrice(raw);
            BigDecimal price = new BigDecimal(normalized);
            log.info("✓ Preço parseado com sucesso: {} → R$ {}", raw, price);
            return Optional.of(price);
        } catch (Exception e) {
            log.warn("❌ Não foi possível parsear o preço: '{}' - {}", raw, e.getMessage());
            return Optional.empty();
        }
    }

    private String text(Document doc, String selector) {
        var el = doc.selectFirst(selector);
        return el != null ? el.text() : null;
    }

    private String attr(Document doc, String selector, String attrName) {
        var el = doc.selectFirst(selector);
        return el != null ? el.attr(attrName) : null;
    }
}
