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
public class MercadoLivrePriceFetcher implements GamePriceFetcher {

    private static final Logger log = LoggerFactory.getLogger(MercadoLivrePriceFetcher.class);

    // Captura "809 reais" ou "1.299 reais com 90 centavos" do aria-label
    private static final Pattern PRICE_PATTERN =
            Pattern.compile("(\\d{1,3}(?:\\.\\d{3})*(?:\\s+reais)?(?:\\s+com\\s+\\d{1,2}\\s+centavos)?)");

    @Override
    public boolean supports(String url) {
        return url != null && url.contains("mercadolivre.com");
    }

    @Override
    public String sourceName() {
        return "Mercado Livre";
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

            return extractFromAriaLabel(doc, url);

        } catch (Exception e) {
            log.error("Erro ao buscar preco no Mercado Livre para {}: {}", url, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<BigDecimal> extractFromAriaLabel(Document doc, String url) {
        try {
            // Procura pelo span com data-andes-money-amount (indicador de preco ML)
            Element priceElement = doc.selectFirst("[data-andes-money-amount='true']");

            if (priceElement == null) {
                log.warn("Nenhum elemento com data-andes-money-amount encontrado em: {}", url);
                return tryAlternativeSelectorML(doc, url);
            }

            // Extrai o aria-label que contem o preco em texto (ex: "809 reais")
            String ariaLabel = priceElement.attr("aria-label");
            log.debug("aria-label encontrado: {}", ariaLabel);

            if (ariaLabel == null || ariaLabel.isBlank()) {
                log.warn("aria-label vazio ou nao encontrado em: {}", url);
                return tryAlternativeSelectorML(doc, url);
            }

            return parseAriaLabel(ariaLabel, url);

        } catch (Exception e) {
            log.error("Erro ao parsear aria-label do Mercado Livre para {}: {}", url, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<BigDecimal> tryAlternativeSelectorML(Document doc, String url) {
        try {
            // Seletor alternativo: classe andes-money-amount
            Element element = doc.selectFirst(".andes-money-amount");
            if (element != null) {
                String text = element.text();
                log.debug("Tentando seletor alternativo .andes-money-amount: {}", text);
                return parsePrice(text, url);
            }

            // Outro seletor alternativo
            element = doc.selectFirst("[itemprop=price]");
            if (element != null) {
                String content = element.attr("content");
                log.debug("Tentando meta tag itemprop=price: {}", content);
                return parsePrice(content, url);
            }

            log.warn("Nenhum seletor alternativo de preco encontrado para: {}", url);
            return Optional.empty();

        } catch (Exception e) {
            log.error("Erro ao tentar seletores alternativos: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<BigDecimal> parseAriaLabel(String ariaLabel, String url) {
        try {
            // aria-label pode ter formatos como:
            // "809 reais"
            // "1.299 reais com 90 centavos"
            // "809,90 reais"

            // Remove "reais" e "centavos"
            String cleaned = ariaLabel
                    .replaceAll("\\s+reais.*", "")  // Remove "reais" e tudo depois
                    .replaceAll("\\s+com.*", "")    // Remove "com X centavos"
                    .trim();

            // Normaliza formato brasileiro (1.299 ou 1.299,90)
            String normalized = cleaned
                    .replaceAll("\\.", "")           // Remove separador de milhar
                    .replace(",", ".");              // Troca virgula por ponto

            BigDecimal price = new BigDecimal(normalized);
            log.info("Mercado Livre: R$ {} em {}", price, url);
            return Optional.of(price);

        } catch (NumberFormatException e) {
            log.warn("Nao foi possivel converter aria-label '{}' para BigDecimal", ariaLabel);
            return Optional.empty();
        }
    }

    private Optional<BigDecimal> parsePrice(String raw, String url) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }

        try {
            Matcher matcher = PRICE_PATTERN.matcher(raw);
            if (!matcher.find()) {
                log.warn("Padrao de preco nao encontrado em '{}' para {}", raw, url);
                return Optional.empty();
            }

            String extracted = matcher.group(1);
            String normalized = extracted
                    .replaceAll("[^\\d,.]", "")      // Remove palavras
                    .replaceAll("\\.", "")           // Remove separador de milhar
                    .replace(",", ".");              // Troca virgula por ponto

            BigDecimal price = new BigDecimal(normalized);
            log.info("Mercado Livre (alternativo): R$ {} em {}", price, url);
            return Optional.of(price);

        } catch (NumberFormatException e) {
            log.warn("Nao foi possivel converter '{}' para BigDecimal", raw);
            return Optional.empty();
        }
    }
}

