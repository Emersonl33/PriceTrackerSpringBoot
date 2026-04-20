package com.pricetracker.scraper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class NuuvemPriceFetcher implements GamePriceFetcher {

    private static final Logger log = LoggerFactory.getLogger(NuuvemPriceFetcher.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

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

            return extractFromDataPrice(doc, url);

        } catch (Exception e) {
            log.error("Erro ao buscar preço na Nuuvem para {}: {}", url, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<BigDecimal> extractFromDataPrice(Document doc, String url) {
        try {
            // Procura por qualquer elemento com atributo data-price
            Element element = doc.selectFirst("[data-price]");
            
            if (element == null) {
                log.warn("Nenhum elemento com data-price encontrado em: {}", url);
                return Optional.empty();
            }

            String dataPriceJson = element.attr("data-price");
            log.debug("data-price encontrado: {}", dataPriceJson);

            // Parse do JSON
            JsonNode jsonNode = objectMapper.readTree(dataPriceJson);
            
            // Extrai o valor 'v' (preço em centavos)
            if (!jsonNode.has("v") || jsonNode.get("v").isNull()) {
                log.warn("Campo 'v' nao encontrado em data-price para: {}", url);
                return Optional.empty();
            }

            long centavos = jsonNode.get("v").asLong();
            
            // Converte centavos para BigDecimal (centavos / 100)
            BigDecimal price = BigDecimal.valueOf(centavos).divide(BigDecimal.valueOf(100));
            
            log.info("Nuuvem: R$ {} em {}", price, url);
            return Optional.of(price);

        } catch (Exception e) {
            log.error("Erro ao parsear data-price da Nuuvem para {}: {}", url, e.getMessage());
            return Optional.empty();
        }
    }
}