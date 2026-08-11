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
 *
 * <p>{@code AAPL}/{@code AAPL_TUNING}/{@code AAPL_HELD_OUT} (E8-F1-S7) are this repo's first
 * <b>stock</b> fixture — real daily AAPL sessions (2022-08-15 to 2026-08-10, NASDAQ trading days
 * only), fetched once from Yahoo Finance's public chart JSON endpoint the same "fetch once, check
 * in the CSV" way the crypto fixtures were sourced from Binance. Matched to the crypto fixtures by
 * <b>row count</b> (1000), not calendar date range: a stock only trades ~252 days/year, so matching
 * the crypto fixtures' ~2.75-year span would yield roughly 690 candles — too close to a 700-candle
 * tuning window to leave a meaningful held-out tail. Matching row count instead preserves the same
 * 70/30 statistical-power proportions every other {@code FixtureSplits} split relies on, at the
 * cost of AAPL's own date range running longer (~4 years) than BTCUSDT/DOGEUSDT/SOLUSDT's. {@code
 * AAPL_SPLIT_INDEX} is its own constant (not reusing {@link #SPLIT_INDEX}) purely so a future
 * non-1000-row stock fixture doesn't silently inherit a wrong literal — today it happens to equal
 * {@link #SPLIT_INDEX} exactly, since AAPL was also sourced to exactly 1000 rows.
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

    /** E8-F1-S7: this repo's first stock fixture (NASDAQ:AAPL) — see class Javadoc for sourcing
     * and the row-count-vs-date-range tradeoff. */
    static final List<Candle> AAPL = BacktestCandleCsvLoader.load("backtest/aapl-daily-history.csv");

    static final int AAPL_SPLIT_INDEX = (int) Math.round(AAPL.size() * 0.7);

    static final List<Candle> AAPL_TUNING = AAPL.subList(0, AAPL_SPLIT_INDEX);
    static final List<Candle> AAPL_HELD_OUT = AAPL.subList(AAPL_SPLIT_INDEX, AAPL.size());

    private FixtureSplits() {
    }
}
