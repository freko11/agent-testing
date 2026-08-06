# Graph Report - agent testing  (2026-08-06)

## Corpus Check
- 317 files · ~138,893 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 2668 nodes · 7243 edges · 129 communities (108 shown, 21 thin omitted)
- Extraction: 86% EXTRACTED · 14% INFERRED · 0% AMBIGUOUS · INFERRED: 1007 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `beed2315`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- MockBrokerAdapter
- BinanceFuturesTradingAdapter
- AlpacaTradingAdapter
- Order
- .switchTo
- BrokerAdapterContractTest
- AlpacaMarketDataClient
- HoldTermRule
- SignalCallEntry
- TradingMode
- IndicatorSnapshot
- devDependencies
- .applyGate
- SignalRuleId
- .calculate
- TradingModeServiceTest
- MarketDataExceptionHandler
- RiskLimitService
- .calculate
- BinanceMarketDataClient
- TradeForm.tsx
- ThresholdCalibrationTest
- TradingModeEvent
- SecurityConfig.java
- .store
- .add
- order/api.ts
- PriceChart.tsx
- .readDecrypted
- DashboardPage.tsx
- compilerOptions
- Notification
- SignalController.java
- MovingAverageResult
- RiskConsentEvent
- .getPriceHistory
- IndicatorController
- .submitOrder
- KillSwitchService
- compilerOptions
- Broker
- .export
- OrderService
- run skill (project override)
- NotificationService
- security-review skill
- apiFetch
- MarketDataControllerTest.java
- WatchlistEntry
- .calculate
- general-purpose agent (implementation)
- IndicatorId
- Candle
- .findFirstCrossing
- Checkpoint
- adapter-contract-check skill
- WatchlistController
- TradingModeResponse
- BrokerAdapterConfig.java
- BinanceFuturesAdapterConfig.java
- .calculate
- ApiErrorResponse
- OrderExceptionHandler
- NotificationType
- BrokerOrderResult
- WatchlistControllerTest
- RiskLimitConfig.java
- .calculate
- CoreDataModelIntegrationTest.java
- OrderAuditEntry
- .run
- .calculate
- watchlist/api.ts
- NotificationController
- OrderControllerTest
- WatchlistSignalPollerTest
- NotificationControllerTest
- SecurityConfigTest
- TickerMetrics.tsx
- HoldTerm
- .classify
- mvnw
- Changelog
- OrderStatus
- AuthController.java
- NotificationExceptionHandler.java
- RegimeCalibrationTest
- TradingModeExceptionHandler.java
- plugins
- BacktestHarnessTest.java
- .handleUnavailable
- signal-rule-review skill
- dataviz skill
- .bullishCandles
- ClockConfig.java
- TradingModeService
- BackendApplicationTests.java
- OrderQueryControllerTest
- WatchlistService
- BackendApplication
- SchedulingConfig.java
- F5.3 Order status & history
- E7 — Observability & Hardening
- CheckpointStats
- MarketHoursServiceTest
- .calculate
- BacktestReport
- BacktestConfig
- tsconfig.json
- Bluesky Icon (SVG symbol)
- Discord Icon (SVG symbol)
- Documentation Icon (SVG symbol)
- App Favicon (Purple Lightning-Bolt Glyph)
- com.autotrade.dashboard:backend
- killswitch/api.ts
- MarketDataController
- TickerService
- TradingModeBanner.tsx
- Ticker
- Notification system + WatchlistSignalPoller
- SignalService
- BacktestHarness.java
- Backing up and restoring the Oracle instance (E7-F3-S1)
- db-backup.sh
- db-restore.sh
- OrderController
- .handleRateLimited

## God Nodes (most connected - your core abstractions)
1. `TradingMode` - 123 edges
2. `Broker` - 104 edges
3. `Ticker` - 97 edges
4. `Order` - 92 edges
5. `Candle` - 83 edges
6. `AssetType` - 83 edges
7. `Changelog` - 64 edges
8. `IndicatorSnapshot` - 59 edges
9. `BinanceFuturesTradingAdapter` - 58 edges
10. `BrokerCredential` - 56 edges

## Surprising Connections (you probably didn't know these)
- `Broker-credential encryption key rotation procedure` --references--> `BrokerCredentialService`  [EXTRACTED]
  docs/runbooks/credential-key-rotation.md → backend/src/main/java/com/autotrade/dashboard/broker/BrokerCredentialService.java
- `Broker-credential encryption key rotation procedure` --references--> `CredentialEncryptionService`  [EXTRACTED]
  docs/runbooks/credential-key-rotation.md → backend/src/main/java/com/autotrade/dashboard/broker/CredentialEncryptionService.java
- `run skill (project override)` --references--> `E5-F3-S1 Order status/history page`  [EXTRACTED]
  .claude/skills/run/SKILL.md → docs/agile-plan.md
- `CLAUDE.md project status & architecture log` --references--> `oracle-xe Docker Compose service`  [EXTRACTED]
  CLAUDE.md → docker-compose.yml
- `general-purpose agent (implementation)` --references--> `F1.3 Secrets & config management`  [EXTRACTED]
  .claude/agents/general-purpose.md → docs/agile-plan.md

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **E6 risk-guardrail verification flow (caps, kill switch, exposure cap, audit, gating)** — docs_agile_plan_e6_f1_s1, docs_agile_plan_e6_f2_s1, docs_agile_plan_e6_f2_s2, docs_agile_plan_e6_f2_s3, docs_agile_plan_e6_f3_s1, docs_agile_plan_e6_f3_s2, claude_skills_guardrail_check_skill_guardrail_check, claude_skills_security_review_skill_security_review [INFERRED 0.85]
- **Solo-build role mapping: Plan/Explore/general-purpose agents plus run/simplify skills** — claude_agents_plan_plan, claude_agents_explore_explore, claude_agents_general_purpose_general_purpose, claude_skills_run_skill_run, claude_skills_simplify_skill_simplify [EXTRACTED 1.00]
- **BrokerAdapter contract group: interface, retry decorator, verification checklist, and its origin stories** — concept_broker_adapter_interface, concept_retrying_broker_adapter, claude_skills_adapter_contract_check_skill_adapter_contract_check, docs_agile_plan_e4_f1_s1, docs_agile_plan_e4_f1_s2, docs_agile_plan_e4_f1_s3 [INFERRED 0.85]

## Communities (129 total, 21 thin omitted)

### Community 0 - "MockBrokerAdapter"
Cohesion: 0.06
Nodes (16): AssetBalance, BrokerAccountStatus, BrokerAdapterRetryPolicy, Logger, Override, RetryingBrokerAdapter, Override, MockBrokerAdapter (+8 more)

### Community 1 - "BinanceFuturesTradingAdapter"
Cohesion: 0.06
Nodes (24): Credentials, BinanceAccountAsset, BinanceAccountResponse, BinanceAlgoOrderResponse, BinanceErrorResponse, BinanceFuturesTradingAdapter, BinanceOrderResponse, BinancePositionResponse (+16 more)

### Community 2 - "AlpacaTradingAdapter"
Cohesion: 0.08
Nodes (22): AlpacaAccountResponse, AlpacaBracketLeg, AlpacaErrorResponse, AlpacaOrderRequestBody, AlpacaOrderResponse, AlpacaPositionResponse, AlpacaStopLeg, AlpacaTradingAdapter (+14 more)

### Community 3 - "Order"
Cohesion: 0.06
Nodes (7): Entity, Override, PrePersist, PreUpdate, Table, Order, Test

### Community 4 - ".switchTo"
Cohesion: 0.33
Nodes (5): AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, TradingModeControllerTest

### Community 5 - "BrokerAdapterContractTest"
Cohesion: 0.09
Nodes (15): AlpacaTradingAdapterContractTest, ExtendWith, Override, BrokerAdapterContractTest, Test, Override, MockBrokerAdapterContractTest, ExtendWith (+7 more)

### Community 6 - "AlpacaMarketDataClient"
Cohesion: 0.08
Nodes (23): AlpacaBar, AlpacaBarsResponse, AlpacaMarketDataClient, Candle, Component, JsonIgnoreProperties, Override, RestClient (+15 more)

### Community 7 - "HoldTermRule"
Cohesion: 0.10
Nodes (15): HoldTermRule, MODERATE_HIGH, MODERATE_LOW, MODERATE_MEDIUM, STRONG_HIGH, STRONG_LOW, STRONG_MEDIUM, match() (+7 more)

### Community 8 - "SignalCallEntry"
Cohesion: 0.10
Nodes (13): SignalCall, BUY, HOLD, SELL, Entity, Override, PrePersist, Table (+5 more)

### Community 9 - "TradingMode"
Cohesion: 0.09
Nodes (28): BrokerCredential, Entity, Override, PrePersist, PreUpdate, Table, BrokerCredentialRepository, BrokerCredentialService (+20 more)

### Community 10 - "IndicatorSnapshot"
Cohesion: 0.08
Nodes (6): IndicatorComputation, IndicatorSnapshot, Entity, Override, PrePersist, Table

### Community 11 - "devDependencies"
Cohesion: 0.05
Nodes (36): dependencies, lightweight-charts, react, react-dom, react-router-dom, devDependencies, oxlint, @types/node (+28 more)

### Community 12 - ".applyGate"
Cohesion: 0.30
Nodes (3): RegimeGatedRuleEngine, Test, RegimeGatedRuleEngineTest

### Community 13 - "SignalRuleId"
Cohesion: 0.15
Nodes (12): SignalRuleId, BEARISH_MAJORITY, BEARISH_UNANIMOUS, BULLISH_MAJORITY, BULLISH_UNANIMOUS, CONFLICTING_SIGNALS, NO_STRONG_SIGNAL, NO_VOLUME_DATA (+4 more)

### Community 14 - ".calculate"
Cohesion: 0.24
Nodes (5): AdxCalculator, MathContext, AdxCalculatorTest, Candle, Test

### Community 15 - "TradingModeServiceTest"
Cohesion: 0.17
Nodes (6): PaperTradeThresholdNotMetException, SpringBootTest, Test, Transactional, TradingModeServiceTest, TestPropertySource

### Community 16 - "MarketDataExceptionHandler"
Cohesion: 0.15
Nodes (10): InsufficientPriceHistoryException, InvalidPriceHistoryRequestException, ExceptionHandler, Logger, ResponseEntity, RestControllerAdvice, MarketDataExceptionHandler, NoPriceDataException (+2 more)

### Community 17 - "RiskLimitService"
Cohesion: 0.20
Nodes (7): Logger, Service, RiskLimitService, ConfigurationProperties, RiskLimitsProperties, Test, RiskLimitServiceTest

### Community 18 - ".calculate"
Cohesion: 0.22
Nodes (5): MathContext, MovingAverageResult, MovingAverageCrossoverCalculator, Test, MovingAverageCrossoverCalculatorTest

### Community 19 - "BinanceMarketDataClient"
Cohesion: 0.11
Nodes (12): BinanceMarketDataClient, Candle, Component, Override, RestClient, TooManyRequests, Logger, RetryHelper (+4 more)

### Community 20 - "TradeForm.tsx"
Cohesion: 0.10
Nodes (24): TickerSummary, Broker, HoldTerm, IndicatorResponse, MacdResult, MovingAverageRelation, MovingAverageResult, SignalCall (+16 more)

### Community 21 - "ThresholdCalibrationTest"
Cohesion: 0.35
Nodes (4): RuleThresholds, Test, NamedCandidate, ThresholdCalibrationTest

### Community 22 - "TradingModeEvent"
Cohesion: 0.20
Nodes (5): Entity, Override, PrePersist, Table, TradingModeEvent

### Community 23 - "SecurityConfig.java"
Cohesion: 0.11
Nodes (22): CsrfCookieWriteFilter, Bean, Configuration, Logger, Override, PasswordEncoder, SecurityConfig, SpaCsrfTokenRequestHandler (+14 more)

### Community 24 - ".store"
Cohesion: 0.05
Nodes (32): ApplicationRunner, AlpacaTradingCredentialBootstrap, ApplicationArguments, Component, Logger, Override, BinanceTradingCredentialBootstrap, ApplicationArguments (+24 more)

### Community 25 - ".add"
Cohesion: 0.24
Nodes (6): Transactional, WatchlistEntry, SpringBootTest, Test, Transactional, WatchlistServiceTest

### Community 26 - "order/api.ts"
Cohesion: 0.14
Nodes (18): RFC-6266, MarketDataError, EntryOrderType, exportOrdersCsv(), fetchOrders(), filenameFromContentDisposition(), OrderSummary, refreshOrder() (+10 more)

### Community 27 - "PriceChart.tsx"
Cohesion: 0.18
Nodes (18): Broker, ChartDataResponse, ChartIndicatorPoint, CandlestickPoint, LinePoint, toBusinessDay(), toCandlestickSeries(), toMaLongSeries() (+10 more)

### Community 28 - ".readDecrypted"
Cohesion: 0.07
Nodes (28): DecryptedCredential, Override, BeforeEach, BeforeEach, BinanceFuturesTradingAdapterContractTest, BeforeEach, ExtendWith, Override (+20 more)

### Community 29 - "DashboardPage.tsx"
Cohesion: 0.12
Nodes (21): App(), AuthContext, AuthContextValue, AuthProvider(), useAuth(), RequireAuth(), TabItem, Tabs() (+13 more)

### Community 30 - "compilerOptions"
Cohesion: 0.08
Nodes (23): compilerOptions, allowArbitraryExtensions, allowImportingTsExtensions, erasableSyntaxOnly, jsx, lib, module, moduleDetection (+15 more)

### Community 31 - "Notification"
Cohesion: 0.15
Nodes (5): Entity, Override, PrePersist, Table, Notification

### Community 32 - "SignalController.java"
Cohesion: 0.25
Nodes (5): InvalidIndicatorRequestException, GetMapping, RequestMapping, RestController, SignalController

### Community 33 - "MovingAverageResult"
Cohesion: 0.06
Nodes (20): BigDecimalIndicators, MacdResult, MovingAverageResult, IndicatorVotes, SignalRuleEngine, IndicatorWeights, WeightedVoteRuleEngine, AutoConfigureMockMvc (+12 more)

### Community 34 - "RiskConsentEvent"
Cohesion: 0.24
Nodes (5): Entity, Override, PrePersist, Table, RiskConsentEvent

### Community 35 - ".getPriceHistory"
Cohesion: 0.09
Nodes (20): ChartDataResponse, ChartIndicatorPoint, IndicatorService, ChartDataResponse, Service, IndicatorSnapshotRepository, MarketClosedException, Service (+12 more)

### Community 36 - "IndicatorController"
Cohesion: 0.39
Nodes (4): IndicatorController, GetMapping, RequestMapping, RestController

### Community 37 - ".submitOrder"
Cohesion: 0.31
Nodes (5): PlaceOrderRequest, SignalComputation, ExtendWith, Test, OrderServiceTest

### Community 38 - "KillSwitchService"
Cohesion: 0.06
Nodes (31): EngageKillSwitchResponse, GetMapping, PostMapping, RequestMapping, RestController, KillSwitchController, Entity, Override (+23 more)

### Community 39 - "compilerOptions"
Cohesion: 0.10
Nodes (19): compilerOptions, allowImportingTsExtensions, erasableSyntaxOnly, lib, module, moduleDetection, noEmit, noFallthroughCasesInSwitch (+11 more)

### Community 40 - "Broker"
Cohesion: 0.08
Nodes (18): Broker, ALPACA, BINANCE, BrokerAdapterAmbiguousOrderException, BrokerAdapterException, BrokerAdapterRateLimitedException, BrokerAdapterTransientException, BrokerAdapterUnavailableException (+10 more)

### Community 41 - ".export"
Cohesion: 0.25
Nodes (3): OrderCsvExporter, Test, OrderCsvExporterTest

### Community 42 - "OrderService"
Cohesion: 0.13
Nodes (13): BrokerAdapter, BrokerAdapterRouter, Service, OrderAuditEntryRepository, GetMapping, PostMapping, RequestMapping, ResponseEntity (+5 more)

### Community 43 - "run skill (project override)"
Cohesion: 0.14
Nodes (19): run skill (project override), MarketHoursService (hardcoded NYSE/NASDAQ calendar), oracle-xe Docker Compose service, F1.1 Local dev environment, E1-F1-S1 Docker Compose file for Oracle XE, E1-F1-S2 Spring Boot backend skeleton, E1-F1-S3 React app skeleton with routing, E1-F1-S4 CI pipeline builds/tests both apps (+11 more)

### Community 44 - "NotificationService"
Cohesion: 0.13
Nodes (9): Pageable, NotificationRepository, Logger, Service, NotificationService, BeforeEach, ExtendWith, Test (+1 more)

### Community 45 - "security-review skill"
Cohesion: 0.19
Nodes (18): guardrail-check skill, security-review skill, simplify skill, F1.3 Secrets & config management, E1-F3-S1 Broker API keys encrypted at rest, E1-F3-S2 Dashboard requires login, F4.2 Alpaca adapter (stocks), F6.1 Paper/live mode toggle (+10 more)

### Community 46 - "apiFetch"
Cohesion: 0.20
Nodes (18): apiFetch(), readCookie(), fetchChartData(), AssetType, fetchPriceHistory(), MarketDataErrorCode, parseMarketDataError(), PriceHistoryResponse (+10 more)

### Community 47 - "MarketDataControllerTest.java"
Cohesion: 0.30
Nodes (5): AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, MarketDataControllerTest

### Community 48 - "WatchlistEntry"
Cohesion: 0.14
Nodes (7): Entity, Override, PrePersist, Table, WatchlistEntry, Query, WatchlistEntryRepository

### Community 50 - "general-purpose agent (implementation)"
Cohesion: 0.24
Nodes (16): Explore agent (research), general-purpose agent (implementation), Plan agent (design gate), Mandatory per-story workflow (update CLAUDE.md, commit, push), CLAUDE.md project status & architecture log, E1 — Platform Foundation, F1.2 Core data model, F1.4 Testing strategy (+8 more)

### Community 51 - "IndicatorId"
Cohesion: 0.19
Nodes (7): IndicatorId, MA_CROSSOVER, MACD, RSI, Test, Test, OutOfSampleValidationTest

### Community 52 - "Candle"
Cohesion: 0.19
Nodes (5): Candle, BacktestCandleCsvLoader, Candle, IndicatorTestFixtures, Candle

### Community 53 - ".findFirstCrossing"
Cohesion: 0.38
Nodes (4): CrossingEvent, BacktestHarnessTpSlTest, Candle, Test

### Community 54 - "Checkpoint"
Cohesion: 0.11
Nodes (15): DirectionalAccumulator, IndicatorAccumulator, Checkpoint, MAX, MID, MIN, DirectionalOutcome, LOSS (+7 more)

### Community 55 - "adapter-contract-check skill"
Cohesion: 0.18
Nodes (15): adapter-contract-check skill, BrokerAdapter interface (E4-F1-S1), OrderService.submitOrder (bracket order construction), RetryingBrokerAdapter decorator (retry/backoff/outage), TradingModeService (paper/live switch, append-only), F4.1 Adapter interface, E4-F1-S1 BrokerAdapter interface + mock + contract suite, E4-F1-S2 Rate-limit/retry/backoff in adapter contract (+7 more)

### Community 56 - "WatchlistController"
Cohesion: 0.25
Nodes (8): GetMapping, PostMapping, RequestMapping, ResponseEntity, RestController, WatchlistController, WatchlistEntryResponse, DeleteMapping

### Community 57 - "TradingModeResponse"
Cohesion: 0.16
Nodes (7): TradingModeChangeRequest, GetMapping, PostMapping, RequestMapping, RestController, TradingModeController, TradingModeResponse

### Community 58 - "BrokerAdapterConfig.java"
Cohesion: 0.27
Nodes (8): AlpacaTradingProperties, ConfigurationProperties, BrokerAdapterConfig, Bean, Configuration, EnableConfigurationProperties, RestClient, SimpleClientHttpRequestFactory

### Community 59 - "BinanceFuturesAdapterConfig.java"
Cohesion: 0.27
Nodes (8): BinanceFuturesAdapterConfig, Bean, Configuration, EnableConfigurationProperties, RestClient, SimpleClientHttpRequestFactory, BinanceFuturesTradingProperties, ConfigurationProperties

### Community 60 - ".calculate"
Cohesion: 0.23
Nodes (5): MacdResult, MathContext, MacdCalculator, Test, MacdCalculatorTest

### Community 61 - "ApiErrorResponse"
Cohesion: 0.35
Nodes (7): ApiErrorResponse, JsonInclude, ExceptionHandler, Logger, ResponseEntity, RestControllerAdvice, RiskExceptionHandler

### Community 62 - "OrderExceptionHandler"
Cohesion: 0.13
Nodes (9): InvalidTradeRequestException, ExceptionHandler, Logger, ResponseEntity, RestControllerAdvice, OrderExceptionHandler, OrderNotFoundException, OrderRefreshUnavailableException (+1 more)

### Community 63 - "NotificationType"
Cohesion: 0.17
Nodes (11): JsonInclude, NotificationResponse, NotificationType, ORDER_CANCELLED, ORDER_FAILED, ORDER_FILLED, ORDER_PARTIALLY_FILLED, ORDER_PARTIALLY_PROTECTED (+3 more)

### Community 64 - "BrokerOrderResult"
Cohesion: 0.21
Nodes (4): BrokerOrderResult, KillSwitchCancelSummary, BrokerAdapterRouterTest, Test

### Community 65 - "WatchlistControllerTest"
Cohesion: 0.23
Nodes (7): AddWatchlistEntryRequest, AutoConfigureMockMvc, MockMvc, ObjectMapper, Test, WebMvcTest, WatchlistControllerTest

### Community 66 - "RiskLimitConfig.java"
Cohesion: 0.83
Nodes (3): Configuration, EnableConfigurationProperties, RiskLimitConfig

### Community 68 - "CoreDataModelIntegrationTest.java"
Cohesion: 0.31
Nodes (5): CoreDataModelIntegrationTest, SpringBootTest, Test, Transactional, EntityManager

### Community 69 - "OrderAuditEntry"
Cohesion: 0.15
Nodes (5): Entity, Override, PrePersist, Table, OrderAuditEntry

### Community 70 - ".run"
Cohesion: 0.19
Nodes (7): BacktestHarness, HoldGateAccumulator, RuleEvaluator, HoldGateOutcome, LARGE_MOVE, STABLE, FunctionalInterface

### Community 72 - "watchlist/api.ts"
Cohesion: 0.46
Nodes (6): fetchWatchlist(), removeFromWatchlist(), WatchlistEntry, describeError(), Watchlist(), WatchlistProps

### Community 73 - "NotificationController"
Cohesion: 0.29
Nodes (6): GetMapping, PostMapping, RequestMapping, ResponseEntity, RestController, NotificationController

### Community 74 - "OrderControllerTest"
Cohesion: 0.15
Nodes (10): JsonInclude, TradeOrderResponse, KillSwitchEngagedException, RiskLimitExceededException, AutoConfigureMockMvc, MockMvc, ObjectMapper, Test (+2 more)

### Community 75 - "WatchlistSignalPollerTest"
Cohesion: 0.42
Nodes (3): ExtendWith, Test, WatchlistSignalPollerTest

### Community 76 - "NotificationControllerTest"
Cohesion: 0.26
Nodes (5): AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, NotificationControllerTest

### Community 77 - "SecurityConfigTest"
Cohesion: 0.32
Nodes (6): AutoConfigureMockMvc, MockMvc, SpringBootTest, Test, SecurityConfigTest, Cookie

### Community 78 - "TickerMetrics.tsx"
Cohesion: 0.18
Nodes (10): AddToWatchlistButton(), ERROR_MESSAGES, formatOrDash(), relationLabel(), SIGNAL_GLYPH, StatTileProps, StatTileTone, TickerMetricsProps (+2 more)

### Community 79 - "HoldTerm"
Cohesion: 0.24
Nodes (5): HoldTerm, Regime, RANGING, TRENDING, BacktestDecisionPoint

### Community 81 - "mvnw"
Cohesion: 0.33
Nodes (6): mvnw script, clean(), die(), exec_maven(), set_java_home(), verbose()

### Community 82 - "Changelog"
Cohesion: 0.03
Nodes (64): Changelog, Dark-first premium visual pass, E1-F1-S1 — local Oracle XE via Docker Compose, E1-F1-S2 — Spring Boot backend skeleton, E1-F1-S3 — React app skeleton, E1-F1-S4 — CI pipeline, E1-F1-S5 — env/config profiles, E1-F2 — core data model (+56 more)

### Community 83 - "OrderStatus"
Cohesion: 0.12
Nodes (14): Pageable, Query, OrderRepository, OrderStatus, CANCELLED, FAILED, FILLED, PARTIALLY_FILLED (+6 more)

### Community 84 - "AuthController.java"
Cohesion: 0.39
Nodes (6): Authentication, AuthController, GetMapping, RequestMapping, ResponseEntity, RestController

### Community 85 - "NotificationExceptionHandler.java"
Cohesion: 0.29
Nodes (6): InvalidNotificationRequestException, ExceptionHandler, Logger, ResponseEntity, RestControllerAdvice, NotificationExceptionHandler

### Community 87 - "TradingModeExceptionHandler.java"
Cohesion: 0.29
Nodes (6): RiskConsentNotGivenException, ExceptionHandler, Logger, ResponseEntity, RestControllerAdvice, TradingModeExceptionHandler

### Community 88 - "plugins"
Cohesion: 0.22
Nodes (8): plugins, rules, react/only-export-components, react/rules-of-hooks, $schema, oxc, typescript, warn

### Community 91 - "signal-rule-review skill"
Cohesion: 0.43
Nodes (8): signal-rule-review skill, BacktestHarness (walk-forward JUnit validation), HoldTermCalculator (trend strength x volatility band), SignalRuleEngine (Buy/Sell/Hold rule table), F2.3 Buy/Sell/Hold signal & hold-term, E2-F3-S2 Suggested hold-term alongside the call, F2.4 Backtesting, E2-F4-S1 Backtest rule table against historical data

### Community 92 - "dataviz skill"
Cohesion: 0.38
Nodes (7): dataviz skill, SignalBadge colorblind-safe teal/orange/slate palette, F3.1 Ticker lookup & metrics display, E3-F1-S1 Ticker lookup + stat-tile metrics, E3-F1-S2 Buy/Sell/Hold badge color-coded, F3.2 Metric visualization, E3-F2-S1 Price chart with MA/RSI overlays

### Community 94 - "ClockConfig.java"
Cohesion: 0.60
Nodes (3): ClockConfig, Bean, Configuration

### Community 95 - "TradingModeService"
Cohesion: 0.16
Nodes (5): RiskConsentEventRepository, TradingModeEventRepository, Service, TradingModeService, JpaRepository

### Community 96 - "BackendApplicationTests.java"
Cohesion: 0.60
Nodes (3): BackendApplicationTests, SpringBootTest, Test

### Community 97 - "OrderQueryControllerTest"
Cohesion: 0.15
Nodes (5): AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, OrderQueryControllerTest

### Community 101 - "SchedulingConfig.java"
Cohesion: 0.83
Nodes (3): Configuration, SchedulingConfig, EnableScheduling

### Community 102 - "F5.3 Order status & history"
Cohesion: 0.50
Nodes (4): OrderCsvExporter (RFC 4180 trade-history export), F5.3 Order status & history, E5-F3-S1 Order status/history page, E5-F3-S2 Export trade history to CSV

### Community 103 - "E7 — Observability & Hardening"
Cohesion: 0.50
Nodes (4): E7 — Observability & Hardening, E7-F1 Structured logging, E7-F2 Security review gate, E7-F3 Backup/restore

### Community 104 - "CheckpointStats"
Cohesion: 0.29
Nodes (4): CheckpointStats, CheckpointStatsTest, Test, IndicatorExpectancyCalibrationTest

### Community 106 - ".calculate"
Cohesion: 0.26
Nodes (4): MathContext, VolatilityCalculator, Test, VolatilityCalculatorTest

### Community 107 - "BacktestReport"
Cohesion: 0.24
Nodes (4): BacktestReport, DirectionalOutcomeStats, HoldGateStats, RegimeSplitStats

### Community 117 - "killswitch/api.ts"
Cohesion: 0.32
Nodes (10): clearKillSwitch(), engageKillSwitch(), EngageKillSwitchResponse, fetchKillSwitchState(), KillSwitchCancelSummary, KillSwitchResponse, KillSwitchState, describeError() (+2 more)

### Community 119 - "MarketDataController"
Cohesion: 0.43
Nodes (4): GetMapping, RequestMapping, RestController, MarketDataController

### Community 120 - "TickerService"
Cohesion: 0.07
Nodes (29): Autowired, MarketDataClient, Component, MarketHoursService, PostMapping, RequestMapping, ResponseEntity, RestController (+21 more)

### Community 122 - "TradingModeBanner.tsx"
Cohesion: 0.42
Nodes (8): fetchTradingMode(), giveRiskConsent(), switchTradingMode(), TradingMode, TradingModeState, describeError(), otherMode(), TradingModeBanner()

### Community 125 - "Ticker"
Cohesion: 0.13
Nodes (5): Entity, Override, PrePersist, Table, Ticker

### Community 126 - "Notification system + WatchlistSignalPoller"
Cohesion: 0.40
Nodes (5): Notification system + WatchlistSignalPoller, Watchlist feature (watchlist_entries), F3.3 Watchlist (stretch), E3-F3-S1 Watchlist persisted in Oracle DB, F5.4 Notifications

### Community 127 - "SignalService"
Cohesion: 0.20
Nodes (10): Component, Logger, WatchlistSignalPoller, SignalCallEntryRepository, Service, SignalService, BeforeEach, BeforeEach (+2 more)

### Community 130 - "BacktestHarness.java"
Cohesion: 0.20
Nodes (5): MathContext, RsiCalculator, VolumeTrendCalculator, HoldTermCalculator, RegimeClassifier

### Community 132 - "Backing up and restoring the Oracle instance (E7-F3-S1)"
Cohesion: 0.40
Nodes (4): Backing up and restoring the Oracle instance (E7-F3-S1), Backup procedure, Notes, Restore-test procedure

### Community 140 - "OrderController"
Cohesion: 0.39
Nodes (5): PostMapping, RequestMapping, ResponseEntity, RestController, OrderController

## Knowledge Gaps
- **260 isolated node(s):** `com.autotrade.dashboard:backend`, `ALPACA`, `BINANCE`, `PAPER`, `LIVE` (+255 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **21 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `BrokerCredentialService` connect `TradingMode` to `BinanceFuturesTradingAdapter`, `AlpacaTradingAdapter`, `CoreDataModelIntegrationTest.java`, `.submitOrder`, `BrokerAdapterContractTest`, `Broker`, `OrderService`, `security-review skill`, `.store`, `BrokerAdapterConfig.java`, `BinanceFuturesAdapterConfig.java`, `.readDecrypted`?**
  _High betweenness centrality (0.104) - this node is a cross-community bridge._
- **Why does `TradingMode` connect `TradingMode` to `MockBrokerAdapter`, `BinanceFuturesTradingAdapter`, `AlpacaTradingAdapter`, `Order`, `.switchTo`, `BrokerAdapterContractTest`, `TradingModeServiceTest`, `TradingModeEvent`, `.store`, `.readDecrypted`, `.submitOrder`, `Broker`, `OrderService`, `TradingModeResponse`, `BrokerAdapterConfig.java`, `BinanceFuturesAdapterConfig.java`, `BrokerOrderResult`, `CoreDataModelIntegrationTest.java`, `OrderStatus`, `TradingModeService`, `OrderQueryControllerTest`?**
  _High betweenness centrality (0.094) - this node is a cross-community bridge._
- **Why does `Broker` connect `Broker` to `MockBrokerAdapter`, `BinanceFuturesTradingAdapter`, `AlpacaTradingAdapter`, `Order`, `AlpacaMarketDataClient`, `TradingMode`, `IndicatorSnapshot`, `.handleRateLimited`, `BinanceMarketDataClient`, `.store`, `.getPriceHistory`, `.submitOrder`, `MarketDataControllerTest.java`, `CoreDataModelIntegrationTest.java`, `OrderControllerTest`, `.handleUnavailable`, `OrderQueryControllerTest`, `TickerService`, `Ticker`?**
  _High betweenness centrality (0.079) - this node is a cross-community bridge._
- **Are the 6 inferred relationships involving `Candle` (e.g. with `.chartData_marketClosedWithCachedFallback_returns200Stale()` and `.chartData_registeredTicker_returns200WithCandlesAndIndicators()`) actually correct?**
  _`Candle` has 6 INFERRED edges - model-reasoned connections that need verification._
- **What connects `com.autotrade.dashboard:backend`, `ALPACA`, `BINANCE` to the rest of the system?**
  _260 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `MockBrokerAdapter` be split into smaller, more focused modules?**
  _Cohesion score 0.06368330464716007 - nodes in this community are weakly interconnected._
- **Should `BinanceFuturesTradingAdapter` be split into smaller, more focused modules?**
  _Cohesion score 0.06363636363636363 - nodes in this community are weakly interconnected._