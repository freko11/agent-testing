package com.autotrade.dashboard.backtest;

import com.autotrade.dashboard.marketdata.Candle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads a checked-in {@code timestamp,open,high,low,close,volume} CSV fixture — real historical
 * daily candles, fetched once from Binance's public klines endpoint, under
 * {@code backend/src/test/resources/backtest/} — into an ascending-by-timestamp {@link Candle}
 * list, the same ordering contract every {@link com.autotrade.dashboard.marketdata.MarketDataClient}
 * implementation guarantees in production.
 */
public final class BacktestCandleCsvLoader {

    private BacktestCandleCsvLoader() {
    }

    public static List<Candle> load(String classpathResource) {
        List<Candle> candles = new ArrayList<>();
        try (InputStream in = BacktestCandleCsvLoader.class.getClassLoader().getResourceAsStream(classpathResource)) {
            if (in == null) {
                throw new IllegalArgumentException("Missing backtest fixture on classpath: " + classpathResource);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            reader.readLine(); // header
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] parts = line.split(",");
                candles.add(new Candle(Instant.parse(parts[0]), new BigDecimal(parts[1]), new BigDecimal(parts[2]),
                        new BigDecimal(parts[3]), new BigDecimal(parts[4]), new BigDecimal(parts[5])));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        for (int i = 1; i < candles.size(); i++) {
            if (!candles.get(i).timestamp().isAfter(candles.get(i - 1).timestamp())) {
                throw new IllegalStateException(
                        classpathResource + " is not strictly ascending by timestamp at row " + (i + 1));
            }
        }
        return candles;
    }
}
