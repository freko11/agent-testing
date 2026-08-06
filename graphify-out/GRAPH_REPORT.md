# Graph Report - agent-adece954ae016e328  (2026-08-06)

## Corpus Check
- 329 files · ~146,596 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 2748 nodes · 7533 edges · 134 communities (118 shown, 16 thin omitted)
- Extraction: 86% EXTRACTED · 14% INFERRED · 0% AMBIGUOUS · INFERRED: 1046 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `348af4a7`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- BrokerOrderRequest
- BinanceFuturesTradingAdapter
- AlpacaTradingAdapter
- Order
- .switchTo
- BrokerAdapterContractTest
- AlpacaMarketDataClient
- .calculate
- SignalCallEntry
- OrderSide
- IndicatorSnapshot
- devDependencies
- Regime
- BrokerCredential
- .calculate
- TradingModeServiceTest
- MarketDataExceptionHandler
- RiskLimitService
- .calculate
- BinanceMarketDataClient
- TradeForm.tsx
- SignalRuleId
- TradingModeEvent
- SecurityConfig.java
- .run
- .resolveOrRegister
- order/api.ts
- PriceChart.tsx
- FakeBinanceFuturesTradingServer
- DashboardPage.tsx
- compilerOptions
- Notification
- SignalController.java
- BacktestHarness.java
- RiskConsentEvent
- .getChartData
- IndicatorService
- .submitOrder
- JpaRepository
- compilerOptions
- Broker
- .export
- OrderService
- run skill (project override)
- NotificationServiceTest
- security-review skill
- apiFetch
- MarketDataControllerTest
- WatchlistEntry
- Checkpoint
- general-purpose agent (implementation)
- CredentialEncryptionService
- Candle
- .findFirstCrossing
- .getPriceHistory
- adapter-contract-check skill
- WatchlistController
- TradingModeController
- BrokerAdapterConfig.java
- BinanceFuturesAdapterConfig.java
- LiveSignalDriftService.java
- ApiErrorResponse
- OrderExceptionHandler
- NotificationType
- BrokerOrderResult
- WatchlistControllerTest
- RiskLimitConfig.java
- TradingMode
- CoreDataModelIntegrationTest.java
- OrderAuditEntry
- .run
- .calculate
- watchlist/api.ts
- NotificationController
- OrderControllerTest
- WatchlistSignalPollerTest
- NotificationControllerTest
- SignalDriftControllerIntegrationTest
- TickerMetrics.tsx
- LiveSignalDriftServiceTest.java
- LiveSignalDriftServiceTest
- mvnw
- Changelog
- OrderStatus
- AuthController.java
- NotificationExceptionHandler.java
- .computeSignal
- TradingModeExceptionHandler.java
- plugins
- BacktestHarnessTest.java
- .handleUnavailable
- signal-rule-review skill
- dataviz skill
- NotificationService
- ClockConfig.java
- TradingModeService
- BackendApplicationTests.java
- OrderQueryControllerTest
- TickerController
- WatchlistService
- BackendApplication
- SchedulingConfig.java
- F5.3 Order status & history
- E7 — Observability & Hardening
- CheckpointStats
- MarketHoursServiceTest
- .calculate
- BacktestReport
- TickerControllerTest
- tsconfig.json
- Bluesky Icon (SVG symbol)
- Discord Icon (SVG symbol)
- Documentation Icon (SVG symbol)
- App Favicon (Purple Lightning-Bolt Glyph)
- com.autotrade.dashboard:backend
- killswitch/api.ts
- SignalDriftController
- MarketDataController
- .findRegistered
- .getId
- TradingModeBanner.tsx
- OrderRepository
- TickerService
- Ticker
- Notification system + WatchlistSignalPoller
- SignalService
- .switchTo_live_belowThreshold_throwsPaperTradeThresholdNotMetException_noHistoryPersisted
- BacktestConfig
- Backing up and restoring the Oracle instance (E7-F3-S1)
- db-backup.sh
- db-restore.sh
- .handleRateLimited

## God Nodes (most connected - your core abstractions)
1. `TradingMode` - 124 edges
2. `Broker` - 105 edges
3. `Ticker` - 100 edges
4. `Order` - 93 edges
5. `Candle` - 91 edges
6. `AssetType` - 84 edges
7. `Changelog` - 65 edges
8. `IndicatorSnapshot` - 61 edges
9. `BinanceFuturesTradingAdapter` - 58 edges
10. `BrokerCredential` - 57 edges

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

## Communities (134 total, 16 thin omitted)

### Community 0 - "BrokerOrderRequest"
Cohesion: 0.06
Nodes (22): AssetBalance, BrokerAccountStatus, BrokerAdapterRetryPolicy, BrokerOrderRequest, Logger, Override, RetryingBrokerAdapter, Override (+14 more)

### Community 1 - "BinanceFuturesTradingAdapter"
Cohesion: 0.06
Nodes (23): Credentials, BinanceAccountAsset, BinanceAccountResponse, BinanceAlgoOrderResponse, BinanceErrorResponse, BinanceFuturesTradingAdapter, BinanceOrderResponse, BinancePositionResponse (+15 more)

### Community 2 - "AlpacaTradingAdapter"
Cohesion: 0.08
Nodes (23): AlpacaAccountResponse, AlpacaBracketLeg, AlpacaErrorResponse, AlpacaOrderRequestBody, AlpacaOrderResponse, AlpacaPositionResponse, AlpacaStopLeg, AlpacaTradingAdapter (+15 more)

### Community 3 - "Order"
Cohesion: 0.06
Nodes (8): Entity, Override, PrePersist, PreUpdate, Table, Order, Test, BeforeEach

### Community 4 - ".switchTo"
Cohesion: 0.25
Nodes (6): TradingModeResponse, AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, TradingModeControllerTest

### Community 5 - "BrokerAdapterContractTest"
Cohesion: 0.14
Nodes (10): AlpacaTradingAdapterContractTest, ExtendWith, Override, BinanceFuturesTradingAdapterContractTest, ExtendWith, Override, BrokerAdapterContractTest, Test (+2 more)

### Community 6 - "AlpacaMarketDataClient"
Cohesion: 0.09
Nodes (23): AlpacaBar, AlpacaBarsResponse, AlpacaMarketDataClient, Candle, Component, JsonIgnoreProperties, Override, RestClient (+15 more)

### Community 7 - ".calculate"
Cohesion: 0.09
Nodes (17): HoldTermRule, MODERATE_HIGH, MODERATE_LOW, MODERATE_MEDIUM, STRONG_HIGH, STRONG_LOW, STRONG_MEDIUM, match() (+9 more)

### Community 8 - "SignalCallEntry"
Cohesion: 0.10
Nodes (11): IndicatorComputation, SignalCall, BUY, HOLD, SELL, Entity, Override, PrePersist (+3 more)

### Community 9 - "OrderSide"
Cohesion: 0.25
Nodes (7): EntryOrderType, LIMIT, MARKET, OrderSide, BUY, SELL, HttpMethod

### Community 10 - "IndicatorSnapshot"
Cohesion: 0.08
Nodes (5): IndicatorSnapshot, Entity, Override, PrePersist, Table

### Community 11 - "devDependencies"
Cohesion: 0.05
Nodes (36): dependencies, lightweight-charts, react, react-dom, react-router-dom, devDependencies, oxlint, @types/node (+28 more)

### Community 12 - "Regime"
Cohesion: 0.14
Nodes (8): Regime, RANGING, TRENDING, RegimeGatedRuleEngine, Test, RegimeClassifierTest, Test, RegimeGatedRuleEngineTest

### Community 13 - "BrokerCredential"
Cohesion: 0.07
Nodes (20): Autowired, BrokerCredential, Entity, Override, PrePersist, PreUpdate, Table, BrokerCredentialRepository (+12 more)

### Community 14 - ".calculate"
Cohesion: 0.24
Nodes (5): AdxCalculator, MathContext, AdxCalculatorTest, Candle, Test

### Community 15 - "TradingModeServiceTest"
Cohesion: 0.21
Nodes (5): SpringBootTest, Test, TestPropertySource, Transactional, TradingModeServiceTest

### Community 16 - "MarketDataExceptionHandler"
Cohesion: 0.13
Nodes (11): InsufficientPriceHistoryException, InvalidIndicatorRequestException, InvalidPriceHistoryRequestException, ExceptionHandler, Logger, ResponseEntity, RestControllerAdvice, MarketDataExceptionHandler (+3 more)

### Community 17 - "RiskLimitService"
Cohesion: 0.20
Nodes (7): Logger, Service, RiskLimitService, ConfigurationProperties, RiskLimitsProperties, Test, RiskLimitServiceTest

### Community 18 - ".calculate"
Cohesion: 0.24
Nodes (5): MathContext, MovingAverageResult, MovingAverageCrossoverCalculator, Test, MovingAverageCrossoverCalculatorTest

### Community 19 - "BinanceMarketDataClient"
Cohesion: 0.11
Nodes (12): BinanceMarketDataClient, Candle, Component, Override, RestClient, TooManyRequests, Logger, RetryHelper (+4 more)

### Community 20 - "TradeForm.tsx"
Cohesion: 0.10
Nodes (24): TickerSummary, Broker, HoldTerm, IndicatorResponse, MacdResult, MovingAverageRelation, MovingAverageResult, SignalCall (+16 more)

### Community 21 - "SignalRuleId"
Cohesion: 0.11
Nodes (17): RuleThresholds, SignalRuleId, BEARISH_MAJORITY, BEARISH_UNANIMOUS, BULLISH_MAJORITY, BULLISH_UNANIMOUS, CONFLICTING_SIGNALS, NO_STRONG_SIGNAL (+9 more)

### Community 22 - "TradingModeEvent"
Cohesion: 0.17
Nodes (6): Entity, Override, PrePersist, Table, TradingModeEvent, TradingModeEventRepository

### Community 23 - "SecurityConfig.java"
Cohesion: 0.11
Nodes (22): CsrfCookieWriteFilter, Bean, Configuration, Logger, Override, PasswordEncoder, SecurityConfig, SpaCsrfTokenRequestHandler (+14 more)

### Community 24 - ".run"
Cohesion: 0.11
Nodes (17): ApplicationRunner, AlpacaTradingCredentialBootstrap, ApplicationArguments, Component, Logger, Override, BinanceTradingCredentialBootstrap, ApplicationArguments (+9 more)

### Community 25 - ".resolveOrRegister"
Cohesion: 0.25
Nodes (6): Transactional, WatchlistEntry, SpringBootTest, Test, Transactional, WatchlistServiceTest

### Community 26 - "order/api.ts"
Cohesion: 0.14
Nodes (18): RFC-6266, MarketDataError, EntryOrderType, exportOrdersCsv(), fetchOrders(), filenameFromContentDisposition(), OrderSummary, refreshOrder() (+10 more)

### Community 27 - "PriceChart.tsx"
Cohesion: 0.18
Nodes (18): Broker, ChartDataResponse, ChartIndicatorPoint, CandlestickPoint, LinePoint, toBusinessDay(), toCandlestickSeries(), toMaLongSeries() (+10 more)

### Community 28 - "FakeBinanceFuturesTradingServer"
Cohesion: 0.08
Nodes (26): DecryptedCredential, Override, BeforeEach, BeforeEach, FakeAlpacaTradingServer, FakeOrder, ClientHttpRequest, ClientHttpResponse (+18 more)

### Community 29 - "DashboardPage.tsx"
Cohesion: 0.12
Nodes (21): App(), AuthContext, AuthContextValue, AuthProvider(), useAuth(), RequireAuth(), TabItem, Tabs() (+13 more)

### Community 30 - "compilerOptions"
Cohesion: 0.08
Nodes (23): compilerOptions, allowArbitraryExtensions, allowImportingTsExtensions, erasableSyntaxOnly, jsx, lib, module, moduleDetection (+15 more)

### Community 31 - "Notification"
Cohesion: 0.14
Nodes (5): Entity, Override, PrePersist, Table, Notification

### Community 32 - "SignalController.java"
Cohesion: 0.43
Nodes (4): GetMapping, RequestMapping, RestController, SignalController

### Community 33 - "BacktestHarness.java"
Cohesion: 0.06
Nodes (24): BigDecimalIndicators, MacdResult, MovingAverageRelation, EQUAL, SHORT_ABOVE_LONG, SHORT_BELOW_LONG, MovingAverageResult, MathContext (+16 more)

### Community 34 - "RiskConsentEvent"
Cohesion: 0.24
Nodes (5): Entity, Override, PrePersist, Table, RiskConsentEvent

### Community 35 - ".getChartData"
Cohesion: 0.16
Nodes (8): ChartDataResponse, ChartIndicatorPoint, ChartDataResponse, IndicatorControllerTest, AutoConfigureMockMvc, MockMvc, Test, WebMvcTest

### Community 36 - "IndicatorService"
Cohesion: 0.16
Nodes (8): IndicatorController, GetMapping, RequestMapping, RestController, IndicatorService, Service, IndicatorSnapshotRepository, BeforeEach

### Community 37 - ".submitOrder"
Cohesion: 0.31
Nodes (5): PlaceOrderRequest, SignalComputation, ExtendWith, Test, OrderServiceTest

### Community 38 - "JpaRepository"
Cohesion: 0.05
Nodes (32): EngageKillSwitchResponse, GetMapping, PostMapping, RequestMapping, RestController, KillSwitchController, Entity, Override (+24 more)

### Community 39 - "compilerOptions"
Cohesion: 0.10
Nodes (19): compilerOptions, allowImportingTsExtensions, erasableSyntaxOnly, lib, module, moduleDetection, noEmit, noFallthroughCasesInSwitch (+11 more)

### Community 40 - "Broker"
Cohesion: 0.12
Nodes (13): Broker, ALPACA, BINANCE, BrokerAdapterAmbiguousOrderException, BrokerAdapterException, BrokerAdapterRateLimitedException, BrokerAdapterTransientException, BrokerAdapterUnavailableException (+5 more)

### Community 41 - ".export"
Cohesion: 0.23
Nodes (3): OrderCsvExporter, Test, OrderCsvExporterTest

### Community 42 - "OrderService"
Cohesion: 0.12
Nodes (16): PostMapping, RequestMapping, ResponseEntity, RestController, OrderController, GetMapping, PostMapping, RequestMapping (+8 more)

### Community 43 - "run skill (project override)"
Cohesion: 0.14
Nodes (19): run skill (project override), MarketHoursService (hardcoded NYSE/NASDAQ calendar), oracle-xe Docker Compose service, F1.1 Local dev environment, E1-F1-S1 Docker Compose file for Oracle XE, E1-F1-S2 Spring Boot backend skeleton, E1-F1-S3 React app skeleton with routing, E1-F1-S4 CI pipeline builds/tests both apps (+11 more)

### Community 44 - "NotificationServiceTest"
Cohesion: 0.31
Nodes (3): ExtendWith, Test, NotificationServiceTest

### Community 45 - "security-review skill"
Cohesion: 0.21
Nodes (17): guardrail-check skill, security-review skill, simplify skill, F1.3 Secrets & config management, E1-F3-S1 Broker API keys encrypted at rest, E1-F3-S2 Dashboard requires login, F4.2 Alpaca adapter (stocks), F6.1 Paper/live mode toggle (+9 more)

### Community 46 - "apiFetch"
Cohesion: 0.20
Nodes (18): apiFetch(), readCookie(), fetchChartData(), AssetType, fetchPriceHistory(), MarketDataErrorCode, parseMarketDataError(), PriceHistoryResponse (+10 more)

### Community 47 - "MarketDataControllerTest"
Cohesion: 0.29
Nodes (5): AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, MarketDataControllerTest

### Community 48 - "WatchlistEntry"
Cohesion: 0.17
Nodes (5): Entity, Override, PrePersist, Table, WatchlistEntry

### Community 49 - "Checkpoint"
Cohesion: 0.08
Nodes (16): Checkpoint, MAX, MID, MIN, DirectionalAccumulator, DirectionalOutcome, LOSS, WASH (+8 more)

### Community 50 - "general-purpose agent (implementation)"
Cohesion: 0.24
Nodes (16): Explore agent (research), general-purpose agent (implementation), Plan agent (design gate), Mandatory per-story workflow (update CLAUDE.md, commit, push), CLAUDE.md project status & architecture log, E1 — Platform Foundation, F1.2 Core data model, F1.4 Testing strategy (+8 more)

### Community 51 - "CredentialEncryptionService"
Cohesion: 0.18
Nodes (7): CredentialEncryptionService, Component, Logger, CredentialEncryptionServiceTest, Test, Broker-credential encryption key rotation procedure, SecretKeySpec

### Community 52 - "Candle"
Cohesion: 0.09
Nodes (12): MacdResult, MathContext, MacdCalculator, Candle, E2ECandleFixtures, Candle, IndicatorTestFixtures, Candle (+4 more)

### Community 53 - ".findFirstCrossing"
Cohesion: 0.15
Nodes (11): DirectionalScoreResult, ExitReason, HORIZON_EXPIRED, SL_HIT, TP_HIT, CrossingEvent, WalkForwardScorer, IndicatorAccumulator (+3 more)

### Community 54 - ".getPriceHistory"
Cohesion: 0.26
Nodes (5): MarketClosedException, PriceHistoryResult, IndicatorServiceTest, ExtendWith, Test

### Community 55 - "adapter-contract-check skill"
Cohesion: 0.18
Nodes (15): adapter-contract-check skill, BrokerAdapter interface (E4-F1-S1), OrderService.submitOrder (bracket order construction), RetryingBrokerAdapter decorator (retry/backoff/outage), TradingModeService (paper/live switch, append-only), F4.1 Adapter interface, E4-F1-S1 BrokerAdapter interface + mock + contract suite, E4-F1-S2 Rate-limit/retry/backoff in adapter contract (+7 more)

### Community 56 - "WatchlistController"
Cohesion: 0.25
Nodes (8): GetMapping, PostMapping, RequestMapping, ResponseEntity, RestController, WatchlistController, WatchlistEntryResponse, DeleteMapping

### Community 57 - "TradingModeController"
Cohesion: 0.33
Nodes (5): GetMapping, PostMapping, RequestMapping, RestController, TradingModeController

### Community 58 - "BrokerAdapterConfig.java"
Cohesion: 0.27
Nodes (8): AlpacaTradingProperties, ConfigurationProperties, BrokerAdapterConfig, Bean, Configuration, EnableConfigurationProperties, RestClient, SimpleClientHttpRequestFactory

### Community 59 - "BinanceFuturesAdapterConfig.java"
Cohesion: 0.27
Nodes (8): BinanceFuturesAdapterConfig, Bean, Configuration, EnableConfigurationProperties, RestClient, SimpleClientHttpRequestFactory, BinanceFuturesTradingProperties, ConfigurationProperties

### Community 60 - "LiveSignalDriftService.java"
Cohesion: 0.20
Nodes (10): Service, MarketDataService, Component, ConditionalOnProperty, Logger, Scheduled, LiveSignalDriftService, Query (+2 more)

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
Cohesion: 0.16
Nodes (7): BrokerAdapter, BrokerAdapterRouter, Service, BrokerOrderResult, KillSwitchCancelSummary, BrokerAdapterRouterTest, Test

### Community 65 - "WatchlistControllerTest"
Cohesion: 0.25
Nodes (7): AddWatchlistEntryRequest, AutoConfigureMockMvc, MockMvc, ObjectMapper, Test, WebMvcTest, WatchlistControllerTest

### Community 66 - "RiskLimitConfig.java"
Cohesion: 0.83
Nodes (3): Configuration, EnableConfigurationProperties, RiskLimitConfig

### Community 67 - "TradingMode"
Cohesion: 0.17
Nodes (7): TradingMode, LIVE, PAPER, BrokerCredentialNotConfiguredException, JsonInclude, OrderResponse, TradingModeChangeRequest

### Community 68 - "CoreDataModelIntegrationTest.java"
Cohesion: 0.31
Nodes (5): CoreDataModelIntegrationTest, SpringBootTest, Test, Transactional, EntityManager

### Community 69 - "OrderAuditEntry"
Cohesion: 0.14
Nodes (5): Entity, Override, PrePersist, Table, OrderAuditEntry

### Community 70 - ".run"
Cohesion: 0.21
Nodes (7): BacktestHarness, HoldGateAccumulator, RuleEvaluator, HoldGateOutcome, LARGE_MOVE, STABLE, FunctionalInterface

### Community 72 - "watchlist/api.ts"
Cohesion: 0.46
Nodes (6): fetchWatchlist(), removeFromWatchlist(), WatchlistEntry, describeError(), Watchlist(), WatchlistProps

### Community 73 - "NotificationController"
Cohesion: 0.29
Nodes (6): GetMapping, PostMapping, RequestMapping, ResponseEntity, RestController, NotificationController

### Community 74 - "OrderControllerTest"
Cohesion: 0.18
Nodes (8): KillSwitchEngagedException, RiskLimitExceededException, AutoConfigureMockMvc, MockMvc, ObjectMapper, Test, WebMvcTest, OrderControllerTest

### Community 75 - "WatchlistSignalPollerTest"
Cohesion: 0.42
Nodes (3): ExtendWith, Test, WatchlistSignalPollerTest

### Community 76 - "NotificationControllerTest"
Cohesion: 0.20
Nodes (5): AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, NotificationControllerTest

### Community 77 - "SignalDriftControllerIntegrationTest"
Cohesion: 0.14
Nodes (14): AutoConfigureMockMvc, Cookie, MockMvc, SpringBootTest, Test, TestPropertySource, SignalDriftControllerIntegrationTest, AutoConfigureMockMvc (+6 more)

### Community 78 - "TickerMetrics.tsx"
Cohesion: 0.18
Nodes (10): AddToWatchlistButton(), ERROR_MESSAGES, formatOrDash(), relationLabel(), SIGNAL_GLYPH, StatTileProps, StatTileTone, TickerMetricsProps (+2 more)

### Community 80 - "LiveSignalDriftServiceTest"
Cohesion: 0.44
Nodes (4): Candle, ExtendWith, Test, LiveSignalDriftServiceTest

### Community 81 - "mvnw"
Cohesion: 0.33
Nodes (6): mvnw script, clean(), die(), exec_maven(), set_java_home(), verbose()

### Community 82 - "Changelog"
Cohesion: 0.03
Nodes (65): Changelog, Dark-first premium visual pass, E1-F1-S1 — local Oracle XE via Docker Compose, E1-F1-S2 — Spring Boot backend skeleton, E1-F1-S3 — React app skeleton, E1-F1-S4 — CI pipeline, E1-F1-S5 — env/config profiles, E1-F2 — core data model (+57 more)

### Community 83 - "OrderStatus"
Cohesion: 0.14
Nodes (13): OrderStatus, CANCELLED, FAILED, FILLED, PARTIALLY_FILLED, PARTIALLY_PROTECTED, PENDING, REJECTED (+5 more)

### Community 84 - "AuthController.java"
Cohesion: 0.39
Nodes (6): Authentication, AuthController, GetMapping, RequestMapping, ResponseEntity, RestController

### Community 85 - "NotificationExceptionHandler.java"
Cohesion: 0.29
Nodes (6): InvalidNotificationRequestException, ExceptionHandler, Logger, ResponseEntity, RestControllerAdvice, NotificationExceptionHandler

### Community 86 - ".computeSignal"
Cohesion: 0.22
Nodes (6): AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, SignalControllerTest, Test

### Community 87 - "TradingModeExceptionHandler.java"
Cohesion: 0.29
Nodes (6): RiskConsentNotGivenException, ExceptionHandler, Logger, ResponseEntity, RestControllerAdvice, TradingModeExceptionHandler

### Community 88 - "plugins"
Cohesion: 0.22
Nodes (8): plugins, rules, react/only-export-components, react/rules-of-hooks, $schema, oxc, typescript, warn

### Community 89 - "BacktestHarnessTest.java"
Cohesion: 0.36
Nodes (3): Candle, BacktestHarnessTest, Test

### Community 91 - "signal-rule-review skill"
Cohesion: 0.43
Nodes (8): signal-rule-review skill, BacktestHarness (walk-forward JUnit validation), HoldTermCalculator (trend strength x volatility band), SignalRuleEngine (Buy/Sell/Hold rule table), F2.3 Buy/Sell/Hold signal & hold-term, E2-F3-S2 Suggested hold-term alongside the call, F2.4 Backtesting, E2-F4-S1 Backtest rule table against historical data

### Community 92 - "dataviz skill"
Cohesion: 0.38
Nodes (7): dataviz skill, SignalBadge colorblind-safe teal/orange/slate palette, F3.1 Ticker lookup & metrics display, E3-F1-S1 Ticker lookup + stat-tile metrics, E3-F1-S2 Buy/Sell/Hold badge color-coded, F3.2 Metric visualization, E3-F2-S1 Price chart with MA/RSI overlays

### Community 93 - "NotificationService"
Cohesion: 0.21
Nodes (6): Pageable, NotificationRepository, Logger, Service, NotificationService, BeforeEach

### Community 94 - "ClockConfig.java"
Cohesion: 0.60
Nodes (3): ClockConfig, Bean, Configuration

### Community 95 - "TradingModeService"
Cohesion: 0.36
Nodes (3): RiskConsentEventRepository, Service, TradingModeService

### Community 96 - "BackendApplicationTests.java"
Cohesion: 0.60
Nodes (3): BackendApplicationTests, SpringBootTest, Test

### Community 97 - "OrderQueryControllerTest"
Cohesion: 0.15
Nodes (5): AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, OrderQueryControllerTest

### Community 98 - "TickerController"
Cohesion: 0.26
Nodes (6): PostMapping, RequestMapping, ResponseEntity, RestController, TickerController, TickerResponse

### Community 99 - "WatchlistService"
Cohesion: 0.25
Nodes (4): Query, WatchlistEntryRepository, Service, WatchlistService

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
Cohesion: 0.17
Nodes (9): CheckpointStats, IndicatorId, MA_CROSSOVER, MACD, RSI, CheckpointStatsTest, Test, IndicatorExpectancyCalibrationTest (+1 more)

### Community 106 - ".calculate"
Cohesion: 0.26
Nodes (4): MathContext, VolatilityCalculator, Test, VolatilityCalculatorTest

### Community 107 - "BacktestReport"
Cohesion: 0.13
Nodes (8): DirectionalOutcomeStats, BacktestReport, HoldGateStats, Test, RegimeCalibrationTest, RegimeSplitStats, Test, WeightedVoteBacktestTest

### Community 108 - "TickerControllerTest"
Cohesion: 0.32
Nodes (7): RegisterTickerRequest, AutoConfigureMockMvc, MockMvc, ObjectMapper, Test, WebMvcTest, TickerControllerTest

### Community 117 - "killswitch/api.ts"
Cohesion: 0.32
Nodes (10): clearKillSwitch(), engageKillSwitch(), EngageKillSwitchResponse, fetchKillSwitchState(), KillSwitchCancelSummary, KillSwitchResponse, KillSwitchState, describeError() (+2 more)

### Community 118 - "SignalDriftController"
Cohesion: 0.29
Nodes (6): ConditionalOnProperty, GetMapping, RequestMapping, RestController, SignalDriftController, SignalDriftReport

### Community 119 - "MarketDataController"
Cohesion: 0.43
Nodes (4): GetMapping, RequestMapping, RestController, MarketDataController

### Community 120 - ".findRegistered"
Cohesion: 0.17
Nodes (7): MarketDataClient, Component, MarketHoursService, BeforeEach, ExtendWith, Test, MarketDataServiceTest

### Community 121 - ".getId"
Cohesion: 0.33
Nodes (4): SpringBootTest, Test, Transactional, TickerServiceTest

### Community 122 - "TradingModeBanner.tsx"
Cohesion: 0.42
Nodes (8): fetchTradingMode(), giveRiskConsent(), switchTradingMode(), TradingMode, TradingModeState, describeError(), otherMode(), TradingModeBanner()

### Community 123 - "OrderRepository"
Cohesion: 0.31
Nodes (3): Pageable, Query, OrderRepository

### Community 124 - "TickerService"
Cohesion: 0.28
Nodes (4): TickerRepository, Service, Transactional, TickerService

### Community 125 - "Ticker"
Cohesion: 0.10
Nodes (10): BrokerPosition, AssetType, CRYPTO, STOCK, Entity, Override, PrePersist, Table (+2 more)

### Community 126 - "Notification system + WatchlistSignalPoller"
Cohesion: 0.40
Nodes (5): Notification system + WatchlistSignalPoller, Watchlist feature (watchlist_entries), F3.3 Watchlist (stretch), E3-F3-S1 Watchlist persisted in Oracle DB, F5.4 Notifications

### Community 127 - "SignalService"
Cohesion: 0.17
Nodes (12): Component, ConditionalOnProperty, Logger, Scheduled, WatchlistSignalPoller, SignalCallEntryRepository, Service, SignalService (+4 more)

### Community 132 - "Backing up and restoring the Oracle instance (E7-F3-S1)"
Cohesion: 0.40
Nodes (4): Backing up and restoring the Oracle instance (E7-F3-S1), Backup procedure, Notes, Restore-test procedure

## Knowledge Gaps
- **261 isolated node(s):** `com.autotrade.dashboard:backend`, `MIN`, `MID`, `MAX`, `WIN` (+256 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **16 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `TradingMode` connect `TradingMode` to `BrokerOrderRequest`, `BinanceFuturesTradingAdapter`, `AlpacaTradingAdapter`, `Order`, `.switchTo`, `BrokerAdapterContractTest`, `OrderSide`, `BrokerCredential`, `TradingModeServiceTest`, `TradingModeEvent`, `.run`, `FakeBinanceFuturesTradingServer`, `.submitOrder`, `Broker`, `OrderService`, `BrokerAdapterConfig.java`, `BinanceFuturesAdapterConfig.java`, `BrokerOrderResult`, `CoreDataModelIntegrationTest.java`, `LiveSignalDriftServiceTest.java`, `OrderStatus`, `TradingModeService`, `OrderQueryControllerTest`, `OrderRepository`, `Ticker`?**
  _High betweenness centrality (0.099) - this node is a cross-community bridge._
- **Why does `BrokerCredentialService` connect `BrokerCredential` to `BrokerOrderRequest`, `BinanceFuturesTradingAdapter`, `AlpacaTradingAdapter`, `CoreDataModelIntegrationTest.java`, `.submitOrder`, `BrokerAdapterContractTest`, `Broker`, `OrderSide`, `OrderService`, `CredentialEncryptionService`, `OrderStatus`, `.run`, `BrokerAdapterConfig.java`, `BinanceFuturesAdapterConfig.java`, `FakeBinanceFuturesTradingServer`, `SignalService`?**
  _High betweenness centrality (0.081) - this node is a cross-community bridge._
- **Why does `Candle` connect `Candle` to `AlpacaMarketDataClient`, `SignalCallEntry`, `.calculate`, `.calculate`, `BinanceMarketDataClient`, `SignalRuleId`, `BacktestHarness.java`, `.getChartData`, `IndicatorService`, `Broker`, `Checkpoint`, `.findFirstCrossing`, `.getPriceHistory`, `LiveSignalDriftService.java`, `.run`, `.calculate`, `LiveSignalDriftServiceTest.java`, `LiveSignalDriftServiceTest`, `BacktestHarnessTest.java`, `CheckpointStats`, `.calculate`, `BacktestReport`, `.findRegistered`, `Ticker`?**
  _High betweenness centrality (0.075) - this node is a cross-community bridge._
- **Are the 6 inferred relationships involving `Candle` (e.g. with `.chartData_marketClosedWithCachedFallback_returns200Stale()` and `.chartData_registeredTicker_returns200WithCandlesAndIndicators()`) actually correct?**
  _`Candle` has 6 INFERRED edges - model-reasoned connections that need verification._
- **What connects `com.autotrade.dashboard:backend`, `MIN`, `MID` to the rest of the system?**
  _261 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `BrokerOrderRequest` be split into smaller, more focused modules?**
  _Cohesion score 0.055043859649122805 - nodes in this community are weakly interconnected._
- **Should `BinanceFuturesTradingAdapter` be split into smaller, more focused modules?**
  _Cohesion score 0.0629076372817168 - nodes in this community are weakly interconnected._