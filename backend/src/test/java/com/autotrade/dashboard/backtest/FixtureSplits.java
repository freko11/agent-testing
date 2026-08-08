package com.autotrade.dashboard.backtest;

import com.autotrade.dashboard.marketdata.Candle;

import java.util.List;

/**
 * Shared BTCUSDT/DOGEUSDT/SOLUSDT fixture loading and the chronological 70/30 tuning/held-out
 * split ({@link #SPLIT_INDEX}) that {@code OutOfSampleValidationTest} (E8-F4-S1) established and
 * every RSI recalibration test since (E8-F1-S2, E8-F1-S3, E8-F1-S4) reused verbatim — the exact
 * same four {@code BTCUSDT}/{@code DOGEUSDT}/{@code SPLIT_INDEX}-derived fields, redeclared in
 * each new test file. Extracted here once a fourth test needed the identical slicing
 * ({@code PerSymbolRsiOverboughtCalibrationTest}, E8-F1-S4), which would otherwise have made it
 * quadruplicated rather than merely triplicated.
 *
 * <p>{@code SOLUSDT_TUNING}/{@code SOLUSDT_HELD_OUT} are new here: every prior calibration test
 * only ever used the full, untouched {@code SOLUSDT} fixture as an out-of-sample validation
 * surface. E8-F1-S4 is the first story to tune against SOLUSDT's own first 700 candles too (see
 * its class Javadoc for why that's a deliberate, confirmed scope decision, not an oversight).
 */
final class FixtureSplits {

    /** ~70% of each 1000-candle fixture (Nov 2023 - Jul 2026) is the tuning window; the
     * remaining ~30% is held out. */
    static final int SPLIT_INDEX = 700;

    static final List<Candle> BTCUSDT = BacktestCandleCsvLoader.load("backtest/btcusdt-daily-history.csv");
    static final List<Candle> DOGEUSDT = BacktestCandleCsvLoader.load("backtest/dogeusdt-daily-history.csv");
    static final List<Candle> SOLUSDT = BacktestCandleCsvLoader.load("backtest/solusdt-daily-history.csv");

    static final List<Candle> BTCUSDT_TUNING = BTCUSDT.subList(0, SPLIT_INDEX);
    static final List<Candle> DOGEUSDT_TUNING = DOGEUSDT.subList(0, SPLIT_INDEX);
    static final List<Candle> SOLUSDT_TUNING = SOLUSDT.subList(0, SPLIT_INDEX);

    static final List<Candle> BTCUSDT_HELD_OUT = BTCUSDT.subList(SPLIT_INDEX, BTCUSDT.size());
    static final List<Candle> DOGEUSDT_HELD_OUT = DOGEUSDT.subList(SPLIT_INDEX, DOGEUSDT.size());
    static final List<Candle> SOLUSDT_HELD_OUT = SOLUSDT.subList(SPLIT_INDEX, SOLUSDT.size());

    private FixtureSplits() {
    }
}
