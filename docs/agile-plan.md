# Agile Delivery Plan — Auto-Trade Signal Dashboard

## Vision

A personal, paper-trading-first webapp that turns a ticker (stock or crypto) into a
clear Buy/Sell/Hold call with a suggested hold-term, computed from standard technical
indicators — and lets me execute that call as a real broker/exchange order in one
click, with amount, leverage, take-profit and stop-loss set up front. Paper trading
first; live trading only after the flow has proven itself.

## Confirmed scope (from clarifying Q&A)

| Decision | Answer |
|---|---|
| Asset classes | Stocks **and** crypto |
| Execution targets | Broker-agnostic adapter layer; first two reference adapters: **Alpaca** (stocks) + **Binance** (crypto) |
| Signal source | Standard technical indicators (RSI, MACD, MA crossover, volatility/volume) |
| Users | Personal, single-user tool |
| Trading mode | **Paper/simulated first**, live mode gated behind an explicit switch later |
| Team | Solo, building with Claude Code as primary assistant → sequence epics, avoid parallel workstreams |
| Database | Oracle Database, local (e.g. Oracle XE via Docker), managed via Oracle SQL Developer |
| Agent/skill automation | Documented in this plan only — not activated yet |

## Confirmed decisions

- **Backend framework**: **Spring Boot** (REST, scheduling, security, JPA all standard).
  Previously an open assumption in this section; confirmed during the pre-E1 repo-readiness
  audit with no objection raised, so E1-F1-S2 can proceed without re-litigating it.

## Assumptions to confirm as we go (flag before locking in)

- **Indicator math**: plan assumes we either hand-roll RSI/MACD/MA/volatility or use a
  library like `ta4j` — a build-time decision for an Explore agent, not a product decision.
  Deliberately left open until E2-F2 starts; run the `Explore` agent's library comparison
  then, not before.
- **Stock vs. crypto detection**: the dashboard needs a simple rule (ticker format, or an
  explicit asset-type toggle) to know which adapter/data source to call. Treated as a
  small story in the Signal Engine epic, not decided yet — decide when that story is
  picked up, not in advance.

---

## Epics, features, and user stories (INVEST)

Each story is written to be **I**ndependent, **N**egotiable, **V**aluable, **E**stimable,
**S**mall, and **T**estable — sized in story points (Fibonacci) and paired with concrete
acceptance criteria so "done" isn't a judgment call.

### E1 — Platform Foundation
*Get a working skeleton (DB, backend, frontend) that later epics build on.*

**F1.1 Local dev environment**
| ID | Story | Acceptance Criteria | Pts |
|---|---|---|---|
| E1-F1-S1 | As a developer, I want a Docker Compose file for Oracle XE so I can spin up the DB with one command. | `docker compose up` starts Oracle XE; reachable from SQL Developer; connection details documented in CLAUDE.md. | 3 |
| E1-F1-S2 | As a developer, I want a Spring Boot backend skeleton with a health endpoint so later features have a place to plug in. | `/health` returns 200; builds via Maven/Gradle; runs locally with one command. | 2 |
| E1-F1-S3 | As a developer, I want a React app skeleton with routing so dashboard pages can be added incrementally. | `npm run dev` serves a shell app with a placeholder route; clean build, no console errors. | 2 |
| E1-F1-S4 | As a developer, I want a CI pipeline that builds and tests both apps on every push so regressions are caught before they reach `master`. | Push to any branch triggers build+test (e.g. GitHub Actions); failure blocks merge; pipeline documented in CLAUDE.md. | 3 |
| E1-F1-S5 | As a developer, I want environment/config profiles (local / paper / future prod) so config doesn't hardcode one environment. | Spring profiles (or equivalent) switch DB/API base URLs without code changes; documented in CLAUDE.md. | 2 |

**F1.2 Core data model**
| ID | Story | Acceptance Criteria | Pts |
|---|---|---|---|
| E1-F2-S1 | As a developer, I want Oracle tables for tickers, indicator snapshots, orders, and broker credentials so the app has persistent storage. | Schema script creates tables with PK/FK constraints; runs cleanly against the Dockerized DB. | 5 |
| E1-F2-S2 | As a developer, I want a JPA/Hibernate data-access layer mapped to that schema so business logic never hand-writes SQL. | Repository class per table; integration test proves CRUD round-trip. | 5 |
| E1-F2-S3 | As a developer, I want DB migration tooling (Flyway or Liquibase) so schema changes after initial creation are versioned, not manual ALTERs. | Migrations run automatically on startup; a second migration file proves incremental change works. | 3 |

**F1.3 Secrets & config management**
| ID | Story | Acceptance Criteria | Pts |
|---|---|---|---|
| E1-F3-S1 | As a user, I want broker API keys stored encrypted at rest so a leaked config file can't expose live trading credentials. | Keys encrypted (e.g. Jasypt or OS keystore); never appear in logs; rotation process documented; a checked-in `.env.example` (or equivalent Spring `application-*.yml` placeholder) documents every required config key (Alpaca paper key/secret, Binance testnet key/secret, Oracle connection string) without real values. | 5 |
| E1-F3-S2 | As a user, I want the dashboard itself to require login so it isn't wide open even as a single-user tool on my network. | Dashboard requires authentication; session expires; unauthenticated API calls rejected. | 3 |

**F1.4 Testing strategy**
| ID | Story | Acceptance Criteria | Pts |
|---|---|---|---|
| E1-F4-S1 | As a developer, I want an integration/end-to-end test covering ticker → signal → order against a mock broker adapter so the full flow is verified, not just isolated units. | One E2E test exercises the full happy path against a mock adapter and passes in CI. | 5 |
| E1-F4-S2 | As a developer, I want deterministic fixture data for indicator math so RSI/MACD/MA tests aren't each inventing their own numbers. | Fixture dataset checked in; reused by indicator unit tests (ties into E2-F2's reference-value ACs). | 2 |

### E2 — Signal Engine
*Turn a ticker into indicator numbers and a single Buy/Sell/Hold + hold-term call.*

**F2.1 Market data ingestion**
| ID | Story | Acceptance Criteria | Pts |
|---|---|---|---|
| E2-F1-S1 | As a user, I want to enter a ticker and have the backend fetch recent price history so indicators can be computed. | Valid ticker returns last N candles from the right source (Alpaca for stocks, Binance for crypto). | 5 |
| E2-F1-S2 | As a user, I want a clear error for an unrecognized ticker so I'm not left staring at a blank dashboard. | Unknown ticker returns a specific error message, rendered in the UI, not a generic failure. | 2 |
| E2-F1-S3 | As a user, I want stock tickers to reflect market hours (crypto stays 24/7) so I'm not shown stale data as if it were current. | A stock ticker request outside market hours returns a distinct "market closed" state, not stale data presented as current. | 3 |
| E2-F1-S4 | As a user, I want stock market holidays and early-close days reflected in market-hours handling, since E2-F1-S3 explicitly scoped these out of v1 and the gap has stayed open ever since. | `MarketHoursService` gains a hardcoded NYSE/NASDAQ holiday calendar (standard exchange holidays plus known ad hoc closures) and an early-close (1:00pm ET) day list for a bounded near-term year range; a request on a full holiday returns the existing `MARKET_CLOSED` state E2-F1-S3 already built, and a request after an early-close day's 1:00pm cutoff (but before the normal 4:00pm close) also returns that state instead of presenting the day's data as still-live. Unit-tested against a handful of known dates (e.g. a fixed federal holiday, a Thanksgiving-Friday early close). | 3 |

**F2.2 Technical indicator calculation**
| ID | Story | Acceptance Criteria | Pts |
|---|---|---|---|
| E2-F2-S1 | As a user, I want RSI, MACD, and moving-average crossover computed for my ticker so I have objective signal inputs. | Unit tests validate each indicator against known reference values; all returned in one API call. | 8 |
| E2-F2-S2 | As a user, I want a volatility/volume-trend metric included so I can spot dead or erratic tickers before trading. | Metric present in API response; unit-tested against reference data. | 5 |

**F2.3 Buy/Sell/Hold signal & hold-term**
| ID | Story | Acceptance Criteria | Pts |
|---|---|---|---|
| E2-F3-S1 | As a user, I want the indicators combined into a single Buy/Sell/Hold call so I don't have to interpret raw numbers myself. | Deterministic rule table maps indicator combinations → call; thresholds documented; each branch unit-tested. | 8 |
| E2-F3-S2 | As a user, I want a suggested hold-term alongside the call so I know the expected horizon before entering. | Hold-term derived from volatility/trend strength; shown as a labeled range (e.g. "3–10 days"). | 5 |

**F2.4 Backtesting**
| ID | Story | Acceptance Criteria | Pts |
|---|---|---|---|
| E2-F4-S1 | As a user, I want the Buy/Sell/Hold rule table backtested against historical price data so I trust it before risking paper or real money on it. | Given a historical price series, backtest reports the call the rules would have produced at each point plus simple win/loss stats; run as a script or test, not part of the live API. | 5 |
| E2-F4-S2 | As a user, I want the backtest to report per-rule-branch expectancy (average win size vs. average loss size), not just hit rate, so I can tell whether a branch has positive expected value before trusting it with capital — a near-coin-flip win rate can still be profitable if wins run bigger than losses, and today that number isn't measured anywhere. | `BacktestHarness`/`BacktestHarnessTest` extended (not the live rule table — no `SignalRuleEngine` threshold changes) to compute, per `SignalRuleId` branch that produces a BUY/SELL call, avg win return and avg loss return alongside the existing win-rate stat, at the same min/mid/max checkpoints E2-F4-S1 already reports; reuses the existing checked-in BTCUSDT/DOGEUSDT candle fixtures, no new data fetch. Report is read-only diagnostic output (assertions stay structural, per E2-F4-S1's own precedent — the numbers are evidence under review, not a regression target). Findings feed the next decision (confidence-weighted voting, regime filter, threshold re-tune) rather than being acted on in this story. | 3 |

### E3 — Dashboard (Frontend)
*Make the signal legible at a glance.*

**F3.1 Ticker lookup & metrics display**
| ID | Story | Acceptance Criteria | Pts |
|---|---|---|---|
| E3-F1-S1 | As a user, I want to type a ticker and see its key metrics on one screen so I can decide fast. | Search triggers backend call; metrics render as stat tiles within 2s. | 5 |
| E3-F1-S2 | As a user, I want the Buy/Sell/Hold call and hold-term shown prominently and color-coded so the decision is legible at a glance. | Color-coded badge + hold-term text rendered above the metrics grid (built with `dataviz` skill guidance). | 3 |

**F3.2 Metric visualization**
| ID | Story | Acceptance Criteria | Pts |
|---|---|---|---|
| E3-F2-S1 | As a user, I want a price chart with indicator overlays (MA lines, RSI subplot) so I can sanity-check the signal visually. | Chart renders candles + overlays; responsive; accessible color use per `dataviz` skill. | 8 |

**F3.3 Watchlist (stretch)**
| ID | Story | Acceptance Criteria | Pts |
|---|---|---|---|
| E3-F3-S1 | As a user, I want to save tickers I've checked so I can revisit them without retyping. | List persisted in Oracle DB; add/remove works; survives app restart. | 3 |

### E4 — Broker Adapter Layer
*One trading interface, multiple brokers behind it.*

**F4.1 Adapter interface**
| ID | Story | Acceptance Criteria | Pts |
|---|---|---|---|
| E4-F1-S1 | As a developer, I want a `BrokerAdapter` interface (placeOrder, getPosition, cancelOrder, getAccountStatus) so any broker can plug in without touching trading logic. | Interface + contract documented; a mock implementation passes a shared adapter test suite. | 5 |
| E4-F1-S2 | As a user, I want rate-limit/retry/backoff handling built into the adapter contract so both Alpaca and Binance adapters get it uniformly instead of each reimplementing it. | Adapter retries transient failures with backoff; a hard rate-limit error surfaces as a distinct, user-visible state rather than a generic failure. | 3 |
| E4-F1-S3 | As a user, I want a broker/data-provider outage to fail visibly and safely so no order is silently dropped or duplicated. | When a broker is unreachable, the UI shows a clear "broker unavailable" state; retries never duplicate an already-submitted order. | 5 |

**F4.2 Alpaca adapter (stocks)**
| ID | Story | Acceptance Criteria | Pts |
|---|---|---|---|
| E4-F2-S1 | As a user, I want my Alpaca paper account connected so stock orders route through a real simulated broker. | Adapter authenticates with Alpaca paper keys; `getAccountStatus` returns balance. | 5 |
| E4-F2-S2 | As a user, I want to place a market order via Alpaca so the dashboard button has something real to call. | Order submitted, order ID returned, status is pollable. | 5 |

**F4.3 Binance adapter (crypto)**
| ID | Story | Acceptance Criteria | Pts |
|---|---|---|---|
| E4-F3-S1 | As a user, I want my Binance testnet account connected so crypto orders route through a simulated exchange. | Adapter authenticates with testnet keys; `getAccountStatus` returns balances. | 5 |
| E4-F3-S2 | As a user, I want to place a leveraged order via Binance testnet so crypto trades support the same flow as stocks. | Order submitted with leverage param bounded by adapter limits; order ID returned, status pollable. | 8 |
| E4-F3-S3 | As a user, I want a Binance crypto order's stop-loss/take-profit legs to actually place, so a leveraged position is never left unprotected because Binance changed how conditional orders are submitted. | Exit legs (`STOP_MARKET`/`TAKE_PROFIT_MARKET`) are submitted via Binance's Algo Order API (`POST/GET/DELETE /fapi/v1/algoOrder`, `algoType=CONDITIONAL`) instead of the now-rejected conditional-order path on `/fapi/v1/order`; a full bracket order (entry + both exit legs) fills and shows both legs protected end-to-end against the real Binance Futures Testnet, not just mocks. Found live during E4-F3-S2's post-launch verification — see `docs/CHANGELOG.md`'s "E4-F3-S2 follow-up" entry for the root cause and the entry-order precision bug fixed alongside it. | 3 |

### E5 — Auto-Trade Execution
*The button: amount, leverage, TP/SL → a real order.*

**F5.1 Trade input form**
| ID | Story | Acceptance Criteria | Pts |
|---|---|---|---|
| E5-F1-S1 | As a user, I want to enter amount, leverage, take-profit, and stop-loss so I control risk before submitting. | Form validates numeric ranges against broker limits; submit disabled until valid. | 3 |
| E5-F1-S2 | As a user, I want the form to only show fields relevant to the asset type so I can't submit a nonsensical order. | Stock tickers hide/default leverage to 1×; crypto shows a leverage control bounded by the adapter's max. | 3 |

**F5.2 Order construction & submission**
| ID | Story | Acceptance Criteria | Pts |
|---|---|---|---|
| E5-F2-S1 | As a user, I want clicking "Trade" to submit a bracket order (entry + TP + SL) to the correct adapter so the signal becomes a real position in one click. | Payload includes TP/SL; adapter chosen by asset type; confirmation returned to UI. | 8 |
| E5-F2-S2 | As a user, I want an explicit confirmation step before the order fires so a mistyped amount or leverage can't execute instantly. | Modal shows order summary; requires explicit confirm; cancel makes no API call. | 3 |

**F5.3 Order status & history**
| ID | Story | Acceptance Criteria | Pts |
|---|---|---|---|
| E5-F3-S1 | As a user, I want to see the status of orders I've placed so I know what happened after clicking Trade. | Status page polls adapter and/or stores updates in Oracle DB; rejections show the broker's reason. | 5 |
| E5-F3-S2 | As a user, I want to export my trade history to CSV so I have records for my own tracking. | User can export order history for a date range to CSV, including a reference to the signal snapshot that triggered each order. | 2 |

**F5.4 Notifications**
| ID | Story | Acceptance Criteria | Pts |
|---|---|---|---|
| E5-F4-S1 | As a user, I want to be notified when an order fills or is rejected, and (once E3-F3 watchlist exists) when a watchlisted ticker's signal changes, so I don't have to keep the dashboard open. | At least one delivery channel implemented (e.g. email); notification includes ticker, event type, and timestamp; order-event alerts don't block on the watchlist stretch feature. | 5 |

### E6 — Risk & Safety Controls
*Because this moves real money once live mode is on.*

**F6.1 Paper/live mode toggle**
| ID | Story | Acceptance Criteria | Pts |
|---|---|---|---|
| E6-F1-S1 | As a user, I want a global paper/live switch so I can validate the whole flow with fake money first. | Switch changes which API keys/base URLs adapters use; current mode shown in a persistent UI banner. | 3 |
| E6-F1-S2 | As a user, I want live mode blocked until I've completed a minimum number of successful paper trades so I can't skip validation by accident. | Configurable threshold; switch stays disabled with an explanation until met. | 3 |
| E6-F1-S3 | As a user, I want an explicit risk disclaimer/consent step before live mode unlocks, on top of the paper-trade threshold, so switching to real money is a deliberate act. | Live mode requires a one-time explicit acknowledgment, stored with a timestamp. | 2 |

**F6.2 Guardrails**
| ID | Story | Acceptance Criteria | Pts |
|---|---|---|---|
| E6-F2-S1 | As a user, I want a hard server-side cap on leverage and position size so a UI bug or fat-finger can't exceed my risk limits. | Backend rejects any order beyond configured caps, regardless of what the frontend sent. | 5 |
| E6-F2-S2 | As a user, I want a kill switch that cancels all open orders and blocks new submissions so I can stop everything instantly. | One control cancels open orders on both adapters and blocks new trades until manually cleared. | 5 |
| E6-F2-S3 | As a user, I want a portfolio-level aggregate exposure cap, on top of per-order limits, so many individually-small orders can't add up to an outsized risk. | Backend rejects a new order that would push total open exposure beyond a configured aggregate cap, even if the order itself is within per-order limits. | 5 |

**F6.3 Audit log**
| ID | Story | Acceptance Criteria | Pts |
|---|---|---|---|
| E6-F3-S1 | As a user, I want every order and the signal that triggered it logged immutably so I can review my decision trail later. | Append-only Oracle audit table: ticker, signal snapshot, order params, timestamp, result. | 3 |
| E6-F3-S2 | As a user, I want the audit log to record which version of the Buy/Sell/Hold rule table produced a signal so a later rule change doesn't retroactively obscure why a past order fired. | Audit log entries record the rule-table version alongside the signal snapshot. | 3 |
| E6-F3-S3 | As a user, I want a dedicated audit-trail viewer in the dashboard so I can review my decision trail (E6-F3-S1/S2) without exporting CSVs or querying the database directly. | A new UI view lists `OrderAuditEntry` rows (ticker, signal call/rule-table version, order outcome, timestamp), backed by a new paginated read endpoint; reachable from the dashboard's existing tab/nav structure. Backlog story added after E6-F3-S2 shipped the `rule_table_version` column and repeatedly deferred building the viewer itself — found during a general "any overdue findings?" review. | 5 |

### E7 — Observability & Hardening
*Threaded through the whole build, not a phase at the end.*

| ID | Story | Acceptance Criteria | Pts |
|---|---|---|---|
| E7-F1-S1 | As a developer, I want structured logging across backend services so broker/indicator failures are diagnosable. | Consistent log format; broker errors logged with context, never silently swallowed. | 3 |
| E7-F2-S1 | As a user, I want the credential-storage and order-submission code security-reviewed before any live account is connected so leaked keys or injection bugs don't cost real money. | `security-review` run against secrets + adapter code; findings triaged before E6 live-mode gate is closed. | 3 |
| E7-F3-S1 | As a user, I want a tested backup/restore procedure for the Oracle instance so order history and the audit log aren't a single-disk-failure away from gone. | Documented/scripted backup command; a restore has been tested at least once against a fresh Oracle XE instance. | 3 |

### E8 — Signal Quality & Quant Rigor
*Close the gap between "the rule table looks profitable" and "the rule table is
validated, execution-realistic, and monitored" — enhancements identified reviewing
E2's existing rule engine (`SignalRuleEngine`) and backtest harness
(`BacktestHarness`, E2-F4-S1/S2) against professional quant practice. Backlog added
after E2 shipped, same pattern as E4-F3-S3's post-launch addition to E4 — not yet
started.*

**F8.1 Threshold calibration**
| ID | Story | Acceptance Criteria | Pts |
|---|---|---|---|
| E8-F1-S1 | As a user, I want the RSI/volatility/volume-trend thresholds in `SignalRuleEngine` re-tuned against `BacktestHarness`'s per-branch expectancy output (E2-F4-S2), instead of left as the original hand-picked estimates, so the rule table's gates are backed by evidence rather than engineering intuition. | A documented calibration pass (script or test) sweeps threshold candidates against the existing BTCUSDT/DOGEUSDT fixtures, reports expectancy per candidate, and any resulting threshold change is versioned via a `RULE_TABLE_VERSION` bump per `SignalRuleEngine`'s existing versioning convention. | 5 |
| E8-F1-S2 | As a user, I want `rsiOversold` recalibrated independently of `rsiOverbought`, since E8-F4-S1 found E8-F1-S1's symmetric RSI 25/75 shift replicates out-of-sample on the SELL side but not the BUY side, and flagged asymmetric bounds as the likely fix. | `rsiOversold` candidates are swept against a tuning window only (never the out-of-sample surfaces), then the winning candidate(s) are checked against E8-F4-S1's three out-of-sample surfaces (BTCUSDT/DOGEUSDT held-out tails, untouched SOLUSDT) on both BUY and SELL sides before anything ships; any shipped change is versioned via a `RULE_TABLE_VERSION` bump, and a no-ship outcome is an equally valid, documented ending if the evidence doesn't support a change. Found post-launch during E8-F4-S1's validation. | 3 |
| E8-F1-S3 | As a user, I want `rsiOverbought` recalibrated and validated independently against the out-of-sample surfaces, since E8-F1-S2 found `rsiOversold` has no measurable effect on BUY-side classification and traced E8-F1-S1's original BUY-side gain to `rsiOverbought`'s move instead — so the actual lever behind the still-open BUY-side out-of-sample mismatch has never been isolated and tested on its own. | `rsiOverbought` candidates are swept against a tuning window only (never the out-of-sample surfaces), then the winning candidate(s) are checked against E8-F4-S1's three out-of-sample surfaces (BTCUSDT/DOGEUSDT held-out tails, untouched SOLUSDT) on both BUY and SELL sides before anything ships; any shipped change is versioned via a `RULE_TABLE_VERSION` bump, and a no-ship outcome is an equally valid, documented ending if the evidence doesn't support a change (mirrors E8-F1-S2's own no-ship precedent). Found post-launch during E8-F1-S2's investigation. | 3 |
| E8-F1-S4 | As a user, I want `rsiOverbought` calibrated per ticker symbol rather than as one global value, since E8-F1-S3 found the BUY-side optimum genuinely diverges by asset (BTCUSDT/DOGEUSDT/SOLUSDT each prefer a different value in 68-76, with no single value beating the pre-tuning baseline on all three at once) and closed the global-value BUY-side mismatch as understood-but-not-fixable-globally. | `SignalRuleEngine` gains a per-symbol threshold-override mechanism (keyed by normalized ticker symbol, falling back to the existing global `RuleThresholds.DEFAULT` for any uncalibrated or stock symbol) that `SignalService` resolves per order/signal computation without changing `evaluate`'s existing signatures. Each of BTCUSDT/DOGEUSDT/SOLUSDT is swept for its own `rsiOverbought` optimum against its own chronological 70% tuning window only, then validated against that same symbol's own held-out 30% tail before any override ships for that symbol — a per-symbol no-ship is an equally valid, documented outcome if a symbol's own held-out data doesn't confirm its tuning-window winner. `rsiOversold` stays fixed at the global value for every symbol (not independently re-swept), per E8-F1-S2's finding that it has no BUY-side effect. `RULE_TABLE_VERSION` bumps (v2 → v3) as soon as the per-symbol resolution mechanism ships, regardless of how many symbols end up with a non-default override. Audit trail (`OrderAuditEntry`/`SignalCallEntry`) stays code-only for the applied per-symbol value — no new column — consistent with how every other E8 threshold constant is documented (code + CHANGELOG, not a persisted field). Found post-launch during E8-F1-S3's investigation. | 5 |
| E8-F1-S5 | As a user, I want a BUY-side calibration attempt on a non-RSI axis, since E8-F1-S2/S3 closed the E8-F4-S1 BUY-side out-of-sample mismatch as not fixable via either RSI bound alone and named MACD/MA-crossover thresholds as the one untried mechanism short of the per-symbol RSI override E8-F1-S4 already shipped. | `SignalRuleEngine`'s MACD vote gains one new tunable minimum-histogram-magnitude threshold in `RuleThresholds` (default 0, i.e. today's any-nonzero-crossover behavior, so no behavior change until swept). A new calibration test sweeps candidate values against each of BTCUSDT/DOGEUSDT/SOLUSDT's own existing chronological tuning window (reusing the same 70/30 splits E8-F1-S4 already established — no new fixture required), then checks the winning candidate(s) against that same symbol's own held-out tail on the BUY side before anything ships; any shipped change is versioned via a `RULE_TABLE_VERSION` bump, and a no-ship outcome is an equally valid, documented ending, consistent with E8-F1-S2/S3's own precedent. MA-crossover thresholding is explicitly out of scope for this story — a candidate follow-up only if MACD alone doesn't resolve the mismatch, mirroring the rsiOversold-then-rsiOverbought split across E8-F1-S2/S3. | 5 |

**F8.2 Execution-realistic backtesting**
| ID | Story | Acceptance Criteria | Pts |
|---|---|---|---|
| E8-F2-S1 | As a user, I want the backtest to simulate whether a trade's actual take-profit/stop-loss price would be hit before its fixed hold-term checkpoint, so the reported win rate/expectancy reflects what a real bracket order (E5-F2-S1) would have realized, not just the forward price at an arbitrary day count. | `BacktestHarness` walks forward day-by-day within the hold-term window checking for TP/SL price crossing before falling back to the existing min/mid/max endpoint scoring; outcome and exit reason (TP hit / SL hit / horizon expired) recorded per decision point. | 8 |
| E8-F2-S2 | As a user, I want the backtest to account for realistic transaction costs (spread/slippage/fees) so a branch's expectancy isn't reported as positive on paper when it wouldn't survive real execution costs. | A configurable cost-per-trade (bps) is subtracted from every scored outcome; report shows expectancy with and without costs side by side. | 3 |

**F8.3 Adaptive weighting & regime awareness**
| ID | Story | Acceptance Criteria | Pts |
|---|---|---|---|
| E8-F3-S1 | As a user, I want each of RSI/MACD/MA-crossover's votes weighted by its own backtested expectancy rather than counted equally, so the strongest-performing indicator has proportionally more influence on the BUY/SELL call. | Rule engine (or a new scoring layer alongside it) computes a weighted vote using per-indicator expectancy figures sourced from the backtest; existing 2-of-3 unanimous/majority behavior remains available as a fallback/comparison mode so the change is A/B-able against the current table. | 8 |
| E8-F3-S2 | As a user, I want a trend-strength/regime filter so directional signals are trusted less in a choppy/ranging market than in a clear trend, since the same MA-crossover means different things in each. | A regime indicator (e.g. ADX-style or long/short ATR ratio) gates or down-weights the directional vote; backtest shows separate expectancy for trending vs. ranging regimes to prove the filter earns its keep. | 5 |

**F8.4 Validation rigor**
| ID | Story | Acceptance Criteria | Pts |
|---|---|---|---|
| E8-F4-S1 | As a user, I want threshold/weighting changes validated out-of-sample (not tuned and tested on the same fixture) so a re-tune doesn't just fit noise in the two checked-in candle histories. | Calibration runs (E8-F1-S1, E8-F3-S1) use a held-out split or additional untouched fixture symbol/period to confirm expectancy holds outside the tuning set before a change ships. | 5 |

**F8.5 Live signal monitoring**
| ID | Story | Acceptance Criteria | Pts |
|---|---|---|---|
| E8-F5-S1 | As a user, I want the rule table's live performance re-scored periodically against `OrderAuditEntry`'s frozen signal snapshots (E6-F3-S1/E6-F3-S2), so I can detect the rule table's edge decaying in live markets before it costs real money. | A scheduled or on-demand job replays `BacktestHarness`-style scoring against production audit-log entries, grouped by `rule_table_version`, and surfaces expectancy drift versus the original backtest. | 5 |

---

## Suggested build sequence (solo, sequential — not parallel workstreams)

```
E1 (Foundation) → E2 (Signal Engine) → E3.1/E3.2 (Dashboard core)
   → E4 (Adapter interface → Alpaca → Binance, paper/testnet keys only)
   → E5 (Auto-Trade Execution) → E6 (Risk Controls) → live-mode gate
E7 threaded throughout; E3.3 (watchlist) slotted in whenever there's slack.
E8 (Signal Quality & Quant Rigor) is backlog, picked up after E7 — depends on E2's
rule engine/backtest harness and E6-F3's audit log already existing, which they do.
```

Rationale: nothing in E3–E6 is testable without E1+E2 existing first, and the highest-risk
code (real order submission) is deliberately last, behind a paper-mode requirement.

## Definition of Ready / Done (applies to every story above)

- **Ready**: acceptance criteria above are understood; any open assumption it touches
  (framework, indicator library, asset-type detection) has been resolved.
- **Done**: acceptance criteria met; `run` skill used to verify in the actual running app,
  not just unit tests; `simplify` skill applied; CLAUDE.md updated; committed with a
  meaningful message (per this repo's mandatory workflow).

---

## Agent & skill architecture (documented now, not activated — per your choice)

Goal: keep the solo build moving through ~49 stories without you re-prompting for every
one. Mapped by role, not by epic, since the same roles recur every epic:

| Role | Tool | When to use it |
|---|---|---|
| Design gate before coding a non-trivial feature | `Plan` agent | Before E1's schema, E2's indicator/signal rules and backtest harness, E4's adapter interface, E5's bracket-order construction, E6's guardrail logic (portfolio exposure cap, kill switch) — anywhere a wrong first design is expensive to unwind later. |
| Research without burning main-thread context | `Explore` agent | Looking up exact Alpaca/Binance API fields for bracket + leverage orders, comparing `ta4j` vs. hand-rolled indicators, Oracle/JPA quirks. |
| Independent implementation chunks, run in background | `general-purpose` agent, `isolation: "worktree"` | Self-contained stories (one indicator module, the React skeleton, one adapter) that don't need turn-by-turn steering — spawn it, keep working the next story yourself, get notified when it's done instead of babysitting it. |
| Dashboard visuals | `dataviz` skill | Before F3.1's stat tiles and F3.2's chart — consistent, accessible metric visualization instead of ad hoc colors. |
| Verify a feature actually works | `run` skill | End of every story — launches the React + Spring Boot stack and click-throughs the golden path, per this project's own "test in the browser" bar. |
| Keep code lean before it's committed | `simplify` skill | Right before each commit — prevents the backlog's pace from accumulating cruft. |
| Gate on money-handling code | `security-review` skill | Mandatory before F1.3 (secrets) ships, and again before E6.1's live-mode switch is unlocked — this is the code that, if wrong, leaks keys or fires unintended real trades. |
| Gate on the highest-blast-radius code | `/code-review` (consider `ultra` tier) | On the full diff for E4/E5 (adapter + order execution) and E6's guardrails (leverage/position caps, exposure cap, kill switch) before calling those epics done — a bug in either the order path or the checks meant to constrain it costs real money, not just dev time. |
| Backlog auto-advance | `/loop` (dynamic/self-paced) | When you're ready to execute rather than just plan: each iteration takes the next story, implements it (delegating independent chunks per the row above), runs `run` + `simplify`, commits, updates CLAUDE.md, and self-schedules the next iteration — this is the actual fix for "the loop gets interrupted by prompting." Not turned on yet; say the word when you want it live. |
| Scheduled/recurring execution | `CronCreate` / `schedule` skill | **Not used for building.** Flagged only as a possible *future product feature* (e.g., auto-refreshing signals for a watchlist on a timer) — out of scope for v1, since the spec calls for a manual "Trade" button, not an unattended bot. |

### Why this shape

- `Plan` before code prevents relitigating architecture mid-epic.
- `Explore` keeps API-detail lookups from polluting the main conversation with long
  fetched docs.
- Background `general-purpose` agents are the actual throughput lever for a solo
  builder — they let independent stories progress while you're steering the next one.
- The two review gates (`security-review`, `/code-review`) are placed deliberately at
  the two points where a mistake has real financial consequence: secrets handling and
  order execution.
- `/loop` is the mechanism that removes re-prompting entirely once you're ready to
  execute the backlog — everything above it is what `/loop` would actually be calling
  under the hood.
