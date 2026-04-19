package com.pricetracker.scraper;

import java.math.BigDecimal;
import java.util.Optional;

public interface GamePriceFetcher {

    /** Retorna true se este fetcher sabe lidar com a URL informada */
    boolean supports(String url);

    /** Busca o preço atual do jogo na URL informada */
    Optional<BigDecimal> fetchPrice(String url);

    /** Nome da fonte — usado nos logs */
    String sourceName();
}
