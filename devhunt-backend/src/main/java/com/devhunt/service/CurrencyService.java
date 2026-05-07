package com.devhunt.service;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class CurrencyService {
    // В MVP хардкодим, в проде - интеграция с API (например, ExchangeRate-API)
    public BigDecimal convertToUsd(BigDecimal amount, String currency) {
        if (amount == null) return null;
        return switch (currency.toUpperCase()) {
            case "RUB" -> amount.divide(BigDecimal.valueOf(90), 2, RoundingMode.HALF_UP);
            case "KZT" -> amount.divide(BigDecimal.valueOf(450), 2, RoundingMode.HALF_UP);
            case "EUR" -> amount.multiply(BigDecimal.valueOf(1.08));
            case "USD" -> amount;
            default -> amount; // Fallback
        };
    }
}