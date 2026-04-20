package com.pricetracker.scraper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SteamPriceFetcher implements GamePriceFetcher {

    private static final Logger log = LoggerFactory.getLogger(SteamPriceFetcher.class);
    private static final Pattern APP_ID_PATTERN = Pattern.compile("/app/(\\d+)");
    private static final String API_URL =
            "https://store.steampowered.com/api/appdetails?appids=%s&cc=br&l=pt";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public boolean supports(String url) {
        return url != null && url.contains("store.steampowered.com/app/");
    }

    @Override
    public String sourceName() {
        return "Steam";
    }

    @Override
    public Optional<BigDecimal> fetchPrice(String url) {
        try {
            String appId = extractAppId(url);
            if (appId == null) {
                log.warn("Não foi possível extrair appId da URL Steam: {}", url);
                return Optional.empty();
            }

            String apiUrl = String.format(API_URL, appId);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("User-Agent", "PriceTracker/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            return parsePrice(appId, response.body());

        } catch (Exception e) {
            log.error("Erro ao buscar preço na Steam para {}: {}", url, e.getMessage());
            return Optional.empty();
        }
    }

    private String extractAppId(String url) {
        Matcher matcher = APP_ID_PATTERN.matcher(url);
        return matcher.find() ? matcher.group(1) : null;
    }

    private Optional<BigDecimal> parsePrice(String appId, String json) {
        try {
            JsonNode root = mapper.readTree(json);
            JsonNode appNode = root.get(appId);

            if (appNode == null || !appNode.path("success").asBoolean()) {
                log.warn("Steam API retornou sucesso=false para appId {}", appId);
                return Optional.empty();
            }

            JsonNode data = appNode.path("data");

            // jogo gratuito
            if (data.path("is_free").asBoolean()) {
                return Optional.of(BigDecimal.ZERO);
            }

            JsonNode priceOverview = data.path("price_overview");
            if (priceOverview.isMissingNode()) {
                log.warn("Sem price_overview para appId {} — pode ser DLC ou bundle", appId);
                return Optional.empty();
            }

            // Steam retorna o preço em centavos: 4999 = R$ 49,99
            int priceInCents = priceOverview.path("final").asInt();
            BigDecimal price = BigDecimal.valueOf(priceInCents).movePointLeft(2);

            log.info("Steam [appId={}] preço: R$ {}", appId, price);
            return Optional.of(price);

        } catch (Exception e) {
            log.error("Erro ao parsear resposta da Steam: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
