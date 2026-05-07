package com.devhunt.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class CurrencyService {

    // Бесплатный API без ключей (базовая валюта - USD)
    private static final String API_URL = "https://open.er-api.com/v6/latest/USD";
    private final RestClient restClient = RestClient.create();

    // Потокобезопасный кэш для хранения курсов (Валюта -> Сколько единиц в 1 долларе)
    private final Map<String, BigDecimal> rates = new ConcurrentHashMap<>();

    // Выкачиваем курсы при старте приложения и обновляем каждые 12 часов (43 200 000 мс)
    @PostConstruct
    @Scheduled(fixedRate = 43200000)
    public void updateRates() {
        log.info("🔄 Обновление курсов валют из внешнего API...");
        try {
            ExchangeRateResponse response = restClient.get()
                    .uri(API_URL)
                    .retrieve()
                    .body(ExchangeRateResponse.class);

            if (response != null && response.rates() != null) {
                response.rates().forEach((currency, rate) ->
                        rates.put(currency, BigDecimal.valueOf(rate)));
                log.info("✅ Курсы обновлены. Текущий курс RUB/USD: {}", rates.get("RUB"));
            }
        } catch (Exception e) {
            log.error("❌ Ошибка при загрузке курсов валют. Используем fallback-значения.", e.getMessage());
            // Запасной план, если нет интернета или API недоступен
            if (rates.isEmpty()) {
                rates.put("RUB", BigDecimal.valueOf(90.0));
                rates.put("KZT", BigDecimal.valueOf(450.0));
                rates.put("EUR", BigDecimal.valueOf(0.92));
                rates.put("USD", BigDecimal.valueOf(1.0));
            }
        }
    }

    // Из любой валюты в USD (для базы данных)
    public BigDecimal convertToUsd(BigDecimal amount, String currency) {
        if (amount == null || currency == null) return null;
        String cur = currency.toUpperCase();

        if (cur.equals("USD")) return amount;

        BigDecimal rate = rates.get(cur);
        if (rate == null) {
            log.warn("⚠️ Неизвестная валюта: {}. Оставляем оригинальное значение.", cur);
            return amount;
        }

        // API выдает курс: 1 USD = 92 RUB. Чтобы найти доллары, делим рубли на курс.
        return amount.divide(rate, 2, RoundingMode.HALF_UP);
    }

    // Из USD обратно в Рубли (для красивой статистики)
    public BigDecimal convertToRub(BigDecimal usdAmount) {
        if (usdAmount == null) return BigDecimal.ZERO;

        // Берем свежий курс рубля, если его нет — берем 90 как дефолт
        BigDecimal rubRate = rates.getOrDefault("RUB", BigDecimal.valueOf(90.0));
        return usdAmount.multiply(rubRate).setScale(0, RoundingMode.HALF_UP);
    }

    // Вспомогательный рекорд для маппинга JSON-ответа от API
    private record ExchangeRateResponse(String result, Map<String, Double> rates) {}
}