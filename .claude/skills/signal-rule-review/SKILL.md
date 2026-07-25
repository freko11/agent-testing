---
name: signal-rule-review
description: Checklist for reviewing the Buy/Sell/Hold rule table and its backtest (E2.3, E2.4) before trusting it with paper or real money. Use after implementing or changing the rule table, before it ships.
---

# What to verify

- **Determinism**: the same indicator inputs always produce the same call — no
  hidden state, no randomness, no reliance on wall-clock time inside the rule
  logic itself (time only enters via market-hours handling, E2-F1-S3, which is
  a separate concern).
- **Threshold documentation**: every branch of the rule table maps to a
  written, human-readable threshold (e.g. "RSI > 70 and MACD bearish crossover
  → Sell") — not a bare numeric formula no one can audit later.
- **Branch coverage**: each branch in the table has its own unit test — not
  just a handful of end-to-end examples that happen to hit a few branches.
- **Hold-term derivation (E2-F3-S2)**: confirm the hold-term range is actually
  derived from volatility/trend strength per ticker, not a hardcoded constant.
- **Backtest evidence (E2-F4-S1)**: before trusting a rule change, run it
  through the backtest harness against historical data and look at the
  win/loss stats — don't ship a rule-table change on unit tests alone, since
  unit tests only prove the code matches the intended thresholds, not that
  the thresholds are good ones.
- **Rule-table versioning (E6-F3-S2)**: confirm a rule-table change bumps the
  version the audit log records, so past orders' logged rationale isn't
  silently reinterpreted under the new rules.
