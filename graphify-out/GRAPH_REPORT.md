# Graph Report - agent testing  (2026-08-02)

## Corpus Check
- 262 files · ~88,371 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 2184 nodes · 5858 edges · 123 communities (102 shown, 21 thin omitted)
- Extraction: 86% EXTRACTED · 14% INFERRED · 0% AMBIGUOUS · INFERRED: 804 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `aff227f7`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- MockBrokerAdapter
- BinanceFuturesTradingAdapter
- AlpacaTradingAdapter
- Order
- TradingModeServiceTest
- DecryptedCredential
- AlpacaMarketDataClient
- HoldTermRule
- BrokerAdapterContractTest
- TradingMode
- IndicatorSnapshot
- devDependencies
- .readDecrypted
- Broker
- WatchlistSignalPollerTest.java
- SignalCallEntry
- MarketDataExceptionHandler
- .submitOrder
- SignalRuleId
- BinanceMarketDataClient
- TradeForm.tsx
- OrderServiceTest.java
- OrderQueryControllerTest
- SecurityConfig.java
- .run
- .resolveOrRegister
- order/api.ts
- PriceChart.tsx
- .findRegistered
- DashboardPage.tsx
- compilerOptions
- Notification
- WatchlistSignalPollerTest
- .evaluate
- Override
- TickerNotRegisteredException
- BrokerCredentialService
- OrderServiceTest
- Ticker
- compilerOptions
- SignalController.java
- .getPriceHistory
- .order
- run skill (project override)
- NotificationServiceTest
- security-review skill
- apiFetch
- Candle
- TickerService
- WatchlistEntry
- general-purpose agent (implementation)
- .calculate
- MarketDataControllerTest.java
- .run
- Checkpoint
- adapter-contract-check skill
- WatchlistController
- OrderControllerTest
- BrokerAdapterConfig.java
- BinanceFuturesAdapterConfig.java
- .calculate
- .calculate
- OrderExceptionHandler
- NotificationType
- .computeSignal
- WatchlistControllerTest
- BacktestReport
- MarketHoursServiceTest
- marketdata/api.ts
- OrderStatus
- ChartDataResponse
- .calculate
- .calculate
- NotificationController
- OrderQueryController
- TickerControllerTest
- NotificationControllerTest
- SecurityConfigTest
- TickerMetrics.tsx
- NotificationRepository
- TickerController
- mvnw
- Changelog
- MarketDataController
- AuthController.java
- NotificationExceptionHandler.java
- TickerServiceTest
- ApiErrorResponse
- plugins
- BrokerCredentialServiceFindTest.java
- OrderController
- signal-rule-review skill
- dataviz skill
- .handleRateLimited
- ClockConfig.java
- .calculate
- BackendApplicationTests.java
- .load
- FakeAlpacaTradingServer
- Notification system + WatchlistSignalPoller
- BackendApplication
- SchedulingConfig.java
- F5.3 Order status & history
- E7 — Observability & Hardening
- FakeBinanceFuturesTradingServer
- .encrypt
- IndicatorService
- TradingModeBanner.tsx
- BacktestConfig
- tsconfig.json
- Bluesky Icon (SVG symbol)
- Discord Icon (SVG symbol)
- Documentation Icon (SVG symbol)
- App Favicon (Purple Lightning-Bolt Glyph)
- com.autotrade.dashboard:backend
- CredentialEncryptionService
- .export
- HoldTermCalculator
- Order.java
- RetryingBinanceFuturesTradingAdapterContractTest
- BrokerAdapterRouterTest

## God Nodes (most connected - your core abstractions)
1. `TradingMode` - 122 edges
2. `Broker` - 104 edges
3. `Ticker` - 93 edges
4. `Order` - 86 edges
5. `AssetType` - 80 edges
6. `IndicatorSnapshot` - 59 edges
7. `Candle` - 58 edges
8. `BrokerCredential` - 56 edges
9. `BinanceFuturesTradingAdapter` - 54 edges
10. `BrokerCredentialService` - 50 edges

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

## Communities (123 total, 21 thin omitted)

### Community 0 - "MockBrokerAdapter"
Cohesion: 0.07
Nodes (14): AssetBalance, BrokerAdapterRetryPolicy, Override, RetryingBrokerAdapter, Override, MockBrokerAdapter, MockOrderState, PositionState (+6 more)

### Community 1 - "BinanceFuturesTradingAdapter"
Cohesion: 0.07
Nodes (21): Credentials, BinanceAccountAsset, BinanceAccountResponse, BinanceErrorResponse, BinanceFuturesTradingAdapter, BinanceOrderResponse, BinancePositionResponse, Credentials (+13 more)

### Community 2 - "AlpacaTradingAdapter"
Cohesion: 0.07
Nodes (26): AlpacaAccountResponse, AlpacaBracketLeg, AlpacaErrorResponse, AlpacaOrderRequestBody, AlpacaOrderResponse, AlpacaPositionResponse, AlpacaStopLeg, AlpacaTradingAdapter (+18 more)

### Community 4 - "TradingModeServiceTest"
Cohesion: 0.06
Nodes (22): PaperTradeThresholdNotMetException, GetMapping, PostMapping, RequestMapping, RestController, TradingModeController, Entity, Override (+14 more)

### Community 5 - "DecryptedCredential"
Cohesion: 0.15
Nodes (8): DecryptedCredential, Override, BeforeEach, BeforeEach, RestClient, RestClient, BeforeEach, BeforeEach

### Community 6 - "AlpacaMarketDataClient"
Cohesion: 0.09
Nodes (23): AlpacaBar, AlpacaBarsResponse, AlpacaMarketDataClient, Candle, Component, JsonIgnoreProperties, Override, RestClient (+15 more)

### Community 7 - "HoldTermRule"
Cohesion: 0.10
Nodes (15): HoldTermRule, MODERATE_HIGH, MODERATE_LOW, MODERATE_MEDIUM, STRONG_HIGH, STRONG_LOW, STRONG_MEDIUM, match() (+7 more)

### Community 8 - "BrokerAdapterContractTest"
Cohesion: 0.14
Nodes (9): BrokerAdapterContractTest, Test, Override, MockBrokerAdapterContractTest, ExtendWith, Override, RetryingAlpacaTradingAdapterContractTest, Override (+1 more)

### Community 9 - "TradingMode"
Cohesion: 0.15
Nodes (16): BrokerCredential, Entity, PrePersist, PreUpdate, Table, TradingMode, LIVE, PAPER (+8 more)

### Community 10 - "IndicatorSnapshot"
Cohesion: 0.07
Nodes (5): IndicatorSnapshot, Entity, Override, PrePersist, Table

### Community 11 - "devDependencies"
Cohesion: 0.05
Nodes (36): dependencies, lightweight-charts, react, react-dom, react-router-dom, devDependencies, oxlint, @types/node (+28 more)

### Community 12 - ".readDecrypted"
Cohesion: 0.22
Nodes (5): Transactional, BrokerCredentialServiceRotationTest, SpringBootTest, Test, Transactional

### Community 13 - "Broker"
Cohesion: 0.09
Nodes (14): Broker, ALPACA, BINANCE, BrokerAccountStatus, BrokerAdapterTransientException, MarketDataUnavailableException, BrokerCredentialNotConfiguredException, JsonInclude (+6 more)

### Community 14 - "WatchlistSignalPollerTest.java"
Cohesion: 0.21
Nodes (12): IndicatorResponse, BigDecimalIndicators, MacdResult, MovingAverageRelation, EQUAL, SHORT_ABOVE_LONG, SHORT_BELOW_LONG, MovingAverageResult (+4 more)

### Community 15 - "SignalCallEntry"
Cohesion: 0.13
Nodes (6): IndicatorComputation, Entity, Override, PrePersist, Table, SignalCallEntry

### Community 16 - "MarketDataExceptionHandler"
Cohesion: 0.14
Nodes (9): InsufficientPriceHistoryException, InvalidPriceHistoryRequestException, ExceptionHandler, ResponseEntity, RestControllerAdvice, MarketDataExceptionHandler, NoPriceDataException, TickerAssetTypeConflictException (+1 more)

### Community 18 - "SignalRuleId"
Cohesion: 0.11
Nodes (19): HoldTerm, SignalCall, BUY, HOLD, SELL, call(), SignalRuleId, BEARISH_MAJORITY (+11 more)

### Community 19 - "BinanceMarketDataClient"
Cohesion: 0.12
Nodes (11): BinanceMarketDataClient, Candle, Component, Override, RestClient, TooManyRequests, RetryHelper, BinanceMarketDataClientTest (+3 more)

### Community 20 - "TradeForm.tsx"
Cohesion: 0.11
Nodes (23): TickerSummary, Broker, HoldTerm, IndicatorResponse, MacdResult, MovingAverageRelation, MovingAverageResult, SignalCall (+15 more)

### Community 21 - "OrderServiceTest.java"
Cohesion: 0.09
Nodes (20): BrokerAdapter, BrokerAdapterAmbiguousOrderException, BrokerAdapterException, BrokerAdapterRateLimitedException, BrokerAdapterRouter, Service, BrokerAdapterUnavailableException, Logger (+12 more)

### Community 22 - "OrderQueryControllerTest"
Cohesion: 0.24
Nodes (5): AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, OrderQueryControllerTest

### Community 23 - "SecurityConfig.java"
Cohesion: 0.16
Nodes (19): CsrfCookieWriteFilter, Bean, Configuration, Logger, Override, SecurityConfig, SpaCsrfTokenRequestHandler, CsrfToken (+11 more)

### Community 24 - ".run"
Cohesion: 0.11
Nodes (17): ApplicationRunner, AlpacaTradingCredentialBootstrap, ApplicationArguments, Component, Logger, Override, BinanceTradingCredentialBootstrap, ApplicationArguments (+9 more)

### Community 25 - ".resolveOrRegister"
Cohesion: 0.18
Nodes (7): AddWatchlistEntryRequest, Transactional, WatchlistEntry, SpringBootTest, Test, Transactional, WatchlistServiceTest

### Community 26 - "order/api.ts"
Cohesion: 0.14
Nodes (18): RFC-6266, readCookie(), EntryOrderType, exportOrdersCsv(), fetchOrders(), filenameFromContentDisposition(), OrderSummary, refreshOrder() (+10 more)

### Community 27 - "PriceChart.tsx"
Cohesion: 0.18
Nodes (18): Broker, ChartDataResponse, ChartIndicatorPoint, CandlestickPoint, LinePoint, toBusinessDay(), toCandlestickSeries(), toMaLongSeries() (+10 more)

### Community 28 - ".findRegistered"
Cohesion: 0.24
Nodes (5): MarketDataClient, BeforeEach, ExtendWith, Test, MarketDataServiceTest

### Community 29 - "DashboardPage.tsx"
Cohesion: 0.30
Nodes (9): App(), AuthContext, AuthContextValue, AuthProvider(), useAuth(), RequireAuth(), DashboardPage(), LoginPage() (+1 more)

### Community 30 - "compilerOptions"
Cohesion: 0.08
Nodes (23): compilerOptions, allowArbitraryExtensions, allowImportingTsExtensions, erasableSyntaxOnly, jsx, lib, module, moduleDetection (+15 more)

### Community 31 - "Notification"
Cohesion: 0.14
Nodes (5): Entity, Override, PrePersist, Table, Notification

### Community 32 - "WatchlistSignalPollerTest"
Cohesion: 0.22
Nodes (9): Component, Logger, WatchlistSignalPoller, BeforeEach, ExtendWith, Test, WatchlistSignalPollerTest, ConditionalOnProperty (+1 more)

### Community 33 - ".evaluate"
Cohesion: 0.23
Nodes (3): MacdResult, Test, SignalRuleEngineTest

### Community 35 - "TickerNotRegisteredException"
Cohesion: 0.17
Nodes (7): MarketClosedException, TickerNotRegisteredException, IndicatorControllerTest, AutoConfigureMockMvc, MockMvc, Test, WebMvcTest

### Community 36 - "BrokerCredentialService"
Cohesion: 0.12
Nodes (12): BrokerCredentialRepository, BrokerCredentialService, Logger, Service, BinanceFuturesTradingAdapterContractTest, ExtendWith, Override, CoreDataModelIntegrationTest (+4 more)

### Community 37 - "OrderServiceTest"
Cohesion: 0.25
Nodes (3): BrokerOrderResult, Test, OrderServiceTest

### Community 38 - "Ticker"
Cohesion: 0.10
Nodes (10): IndicatorSnapshotRepository, Entity, Override, PrePersist, Table, Ticker, TickerRepository, TradingModeEventRepository (+2 more)

### Community 39 - "compilerOptions"
Cohesion: 0.10
Nodes (19): compilerOptions, allowImportingTsExtensions, erasableSyntaxOnly, lib, module, moduleDetection, noEmit, noFallthroughCasesInSwitch (+11 more)

### Community 40 - "SignalController.java"
Cohesion: 0.25
Nodes (5): InvalidIndicatorRequestException, GetMapping, RequestMapping, RestController, SignalController

### Community 41 - ".getPriceHistory"
Cohesion: 0.27
Nodes (6): ChartDataResponse, PriceHistoryResult, IndicatorServiceTest, BeforeEach, ExtendWith, Test

### Community 43 - "run skill (project override)"
Cohesion: 0.14
Nodes (19): run skill (project override), MarketHoursService (hardcoded NYSE/NASDAQ calendar), oracle-xe Docker Compose service, F1.1 Local dev environment, E1-F1-S1 Docker Compose file for Oracle XE, E1-F1-S2 Spring Boot backend skeleton, E1-F1-S3 React app skeleton with routing, E1-F1-S4 CI pipeline builds/tests both apps (+11 more)

### Community 44 - "NotificationServiceTest"
Cohesion: 0.26
Nodes (4): BeforeEach, ExtendWith, Test, NotificationServiceTest

### Community 45 - "security-review skill"
Cohesion: 0.19
Nodes (18): guardrail-check skill, security-review skill, simplify skill, F1.3 Secrets & config management, E1-F3-S1 Broker API keys encrypted at rest, E1-F3-S2 Dashboard requires login, F4.2 Alpaca adapter (stocks), F6.1 Paper/live mode toggle (+10 more)

### Community 46 - "apiFetch"
Cohesion: 0.26
Nodes (14): apiFetch(), fetchChartData(), fetchPriceHistory(), parseMarketDataError(), fetchNotifications(), fetchUnreadCount(), markAllNotificationsRead(), markNotificationRead() (+6 more)

### Community 47 - "Candle"
Cohesion: 0.18
Nodes (5): Candle, E2ECandleFixtures, Candle, IndicatorTestFixtures, Candle

### Community 48 - "TickerService"
Cohesion: 0.23
Nodes (6): Service, Transactional, TickerService, WatchlistEntryRepository, Service, WatchlistService

### Community 49 - "WatchlistEntry"
Cohesion: 0.17
Nodes (5): Entity, Override, PrePersist, Table, WatchlistEntry

### Community 50 - "general-purpose agent (implementation)"
Cohesion: 0.24
Nodes (16): Explore agent (research), general-purpose agent (implementation), Plan agent (design gate), Mandatory per-story workflow (update CLAUDE.md, commit, push), CLAUDE.md project status & architecture log, E1 — Platform Foundation, F1.2 Core data model, F1.4 Testing strategy (+8 more)

### Community 51 - ".calculate"
Cohesion: 0.26
Nodes (4): MathContext, VolatilityCalculator, Test, VolatilityCalculatorTest

### Community 52 - "MarketDataControllerTest.java"
Cohesion: 0.30
Nodes (5): AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, MarketDataControllerTest

### Community 53 - ".run"
Cohesion: 0.23
Nodes (5): BacktestHarness, HoldGateAccumulator, HoldGateOutcome, LARGE_MOVE, STABLE

### Community 54 - "Checkpoint"
Cohesion: 0.15
Nodes (10): DirectionalAccumulator, Checkpoint, MAX, MID, MIN, CheckpointStats, DirectionalOutcome, LOSS (+2 more)

### Community 55 - "adapter-contract-check skill"
Cohesion: 0.18
Nodes (15): adapter-contract-check skill, BrokerAdapter interface (E4-F1-S1), OrderService.submitOrder (bracket order construction), RetryingBrokerAdapter decorator (retry/backoff/outage), TradingModeService (paper/live switch, append-only), F4.1 Adapter interface, E4-F1-S1 BrokerAdapter interface + mock + contract suite, E4-F1-S2 Rate-limit/retry/backoff in adapter contract (+7 more)

### Community 56 - "WatchlistController"
Cohesion: 0.25
Nodes (8): GetMapping, PostMapping, RequestMapping, ResponseEntity, RestController, WatchlistController, WatchlistEntryResponse, DeleteMapping

### Community 57 - "OrderControllerTest"
Cohesion: 0.31
Nodes (6): AutoConfigureMockMvc, MockMvc, ObjectMapper, Test, WebMvcTest, OrderControllerTest

### Community 58 - "BrokerAdapterConfig.java"
Cohesion: 0.27
Nodes (8): AlpacaTradingProperties, ConfigurationProperties, BrokerAdapterConfig, Bean, Configuration, EnableConfigurationProperties, RestClient, SimpleClientHttpRequestFactory

### Community 59 - "BinanceFuturesAdapterConfig.java"
Cohesion: 0.27
Nodes (8): BinanceFuturesAdapterConfig, Bean, Configuration, EnableConfigurationProperties, RestClient, SimpleClientHttpRequestFactory, BinanceFuturesTradingProperties, ConfigurationProperties

### Community 60 - ".calculate"
Cohesion: 0.21
Nodes (5): MacdResult, MathContext, MacdCalculator, Test, MacdCalculatorTest

### Community 61 - ".calculate"
Cohesion: 0.22
Nodes (5): MathContext, MovingAverageResult, MovingAverageCrossoverCalculator, Test, MovingAverageCrossoverCalculatorTest

### Community 62 - "OrderExceptionHandler"
Cohesion: 0.14
Nodes (8): InvalidTradeRequestException, ExceptionHandler, ResponseEntity, RestControllerAdvice, OrderExceptionHandler, OrderNotFoundException, OrderRefreshUnavailableException, SignalNotActionableException

### Community 63 - "NotificationType"
Cohesion: 0.17
Nodes (11): JsonInclude, NotificationResponse, NotificationType, ORDER_CANCELLED, ORDER_FAILED, ORDER_FILLED, ORDER_PARTIALLY_FILLED, ORDER_PARTIALLY_PROTECTED (+3 more)

### Community 64 - ".computeSignal"
Cohesion: 0.16
Nodes (9): AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, SignalControllerTest, BeforeEach, ExtendWith, Test (+1 more)

### Community 65 - "WatchlistControllerTest"
Cohesion: 0.33
Nodes (6): AutoConfigureMockMvc, MockMvc, ObjectMapper, Test, WebMvcTest, WatchlistControllerTest

### Community 66 - "BacktestReport"
Cohesion: 0.31
Nodes (3): BacktestReport, DirectionalOutcomeStats, HoldGateStats

### Community 68 - "marketdata/api.ts"
Cohesion: 0.26
Nodes (9): MarketDataError, MarketDataErrorCode, PriceHistoryResponse, fetchWatchlist(), removeFromWatchlist(), WatchlistEntry, describeError(), Watchlist() (+1 more)

### Community 69 - "OrderStatus"
Cohesion: 0.11
Nodes (15): BrokerPosition, OrderStatus, CANCELLED, FAILED, FILLED, PARTIALLY_FILLED, PARTIALLY_PROTECTED, PENDING (+7 more)

### Community 70 - "ChartDataResponse"
Cohesion: 0.22
Nodes (6): ChartDataResponse, ChartIndicatorPoint, IndicatorController, GetMapping, RequestMapping, RestController

### Community 71 - ".calculate"
Cohesion: 0.29
Nodes (4): MathContext, RsiCalculator, Test, RsiCalculatorTest

### Community 72 - ".calculate"
Cohesion: 0.30
Nodes (3): VolumeTrendCalculator, Test, VolumeTrendCalculatorTest

### Community 73 - "NotificationController"
Cohesion: 0.29
Nodes (6): GetMapping, PostMapping, RequestMapping, ResponseEntity, RestController, NotificationController

### Community 74 - "OrderQueryController"
Cohesion: 0.26
Nodes (6): GetMapping, PostMapping, RequestMapping, ResponseEntity, RestController, OrderQueryController

### Community 75 - "TickerControllerTest"
Cohesion: 0.32
Nodes (7): RegisterTickerRequest, AutoConfigureMockMvc, MockMvc, ObjectMapper, Test, WebMvcTest, TickerControllerTest

### Community 76 - "NotificationControllerTest"
Cohesion: 0.26
Nodes (5): AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, NotificationControllerTest

### Community 77 - "SecurityConfigTest"
Cohesion: 0.32
Nodes (6): AutoConfigureMockMvc, MockMvc, SpringBootTest, Test, SecurityConfigTest, Cookie

### Community 78 - "TickerMetrics.tsx"
Cohesion: 0.21
Nodes (8): AddToWatchlistButton(), ERROR_MESSAGES, formatOrDash(), relationLabel(), StatTileProps, TickerMetricsProps, TickerMetricsResult(), addToWatchlist()

### Community 80 - "TickerController"
Cohesion: 0.26
Nodes (6): PostMapping, RequestMapping, ResponseEntity, RestController, TickerController, TickerResponse

### Community 81 - "mvnw"
Cohesion: 0.33
Nodes (6): mvnw script, clean(), die(), exec_maven(), set_java_home(), verbose()

### Community 82 - "Changelog"
Cohesion: 0.05
Nodes (39): Changelog, E1-F1-S1 — local Oracle XE via Docker Compose, E1-F1-S2 — Spring Boot backend skeleton, E1-F1-S3 — React app skeleton, E1-F1-S4 — CI pipeline, E1-F1-S5 — env/config profiles, E1-F2 — core data model, E1-F3-S1 — broker-credential key rotation (+31 more)

### Community 83 - "MarketDataController"
Cohesion: 0.27
Nodes (5): GetMapping, RequestMapping, RestController, MarketDataController, PriceHistoryResponse

### Community 84 - "AuthController.java"
Cohesion: 0.39
Nodes (6): Authentication, AuthController, GetMapping, RequestMapping, ResponseEntity, RestController

### Community 85 - "NotificationExceptionHandler.java"
Cohesion: 0.31
Nodes (5): InvalidNotificationRequestException, ExceptionHandler, ResponseEntity, RestControllerAdvice, NotificationExceptionHandler

### Community 86 - "TickerServiceTest"
Cohesion: 0.43
Nodes (4): SpringBootTest, Test, Transactional, TickerServiceTest

### Community 87 - "ApiErrorResponse"
Cohesion: 0.36
Nodes (6): ApiErrorResponse, JsonInclude, ExceptionHandler, ResponseEntity, RestControllerAdvice, TradingModeExceptionHandler

### Community 88 - "plugins"
Cohesion: 0.22
Nodes (8): plugins, rules, react/only-export-components, react/rules-of-hooks, $schema, oxc, typescript, warn

### Community 89 - "BrokerCredentialServiceFindTest.java"
Cohesion: 0.39
Nodes (4): BrokerCredentialServiceFindTest, SpringBootTest, Test, Transactional

### Community 90 - "OrderController"
Cohesion: 0.39
Nodes (5): PostMapping, RequestMapping, ResponseEntity, RestController, OrderController

### Community 91 - "signal-rule-review skill"
Cohesion: 0.43
Nodes (8): signal-rule-review skill, BacktestHarness (walk-forward JUnit validation), HoldTermCalculator (trend strength x volatility band), SignalRuleEngine (Buy/Sell/Hold rule table), F2.3 Buy/Sell/Hold signal & hold-term, E2-F3-S2 Suggested hold-term alongside the call, F2.4 Backtesting, E2-F4-S1 Backtest rule table against historical data

### Community 92 - "dataviz skill"
Cohesion: 0.38
Nodes (7): dataviz skill, SignalBadge colorblind-safe teal/orange/slate palette, F3.1 Ticker lookup & metrics display, E3-F1-S1 Ticker lookup + stat-tile metrics, E3-F1-S2 Buy/Sell/Hold badge color-coded, F3.2 Metric visualization, E3-F2-S1 Price chart with MA/RSI overlays

### Community 94 - "ClockConfig.java"
Cohesion: 0.60
Nodes (3): ClockConfig, Bean, Configuration

### Community 96 - "BackendApplicationTests.java"
Cohesion: 0.60
Nodes (3): BackendApplicationTests, SpringBootTest, Test

### Community 98 - "FakeAlpacaTradingServer"
Cohesion: 0.32
Nodes (7): FakeAlpacaTradingServer, FakeOrder, ClientHttpRequest, ClientHttpResponse, HttpStatus, ObjectMapper, MockClientHttpRequest

### Community 99 - "Notification system + WatchlistSignalPoller"
Cohesion: 0.40
Nodes (5): Notification system + WatchlistSignalPoller, Watchlist feature (watchlist_entries), F3.3 Watchlist (stretch), E3-F3-S1 Watchlist persisted in Oracle DB, F5.4 Notifications

### Community 101 - "SchedulingConfig.java"
Cohesion: 0.83
Nodes (3): Configuration, SchedulingConfig, EnableScheduling

### Community 102 - "F5.3 Order status & history"
Cohesion: 0.50
Nodes (4): OrderCsvExporter (RFC 4180 trade-history export), F5.3 Order status & history, E5-F3-S1 Order status/history page, E5-F3-S2 Export trade history to CSV

### Community 103 - "E7 — Observability & Hardening"
Cohesion: 0.50
Nodes (4): E7 — Observability & Hardening, E7-F1 Structured logging, E7-F2 Security review gate, E7-F3 Backup/restore

### Community 104 - "FakeBinanceFuturesTradingServer"
Cohesion: 0.33
Nodes (7): FakeBinanceFuturesTradingServer, ClientHttpRequest, ClientHttpResponse, HttpStatus, StoredOrder, MultiValueMap, URI

### Community 106 - "IndicatorService"
Cohesion: 0.26
Nodes (6): IndicatorService, Service, Service, MarketDataService, Component, MarketHoursService

### Community 107 - "TradingModeBanner.tsx"
Cohesion: 0.44
Nodes (7): fetchTradingMode(), switchTradingMode(), TradingMode, TradingModeState, describeError(), otherMode(), TradingModeBanner()

### Community 117 - "CredentialEncryptionService"
Cohesion: 0.43
Nodes (4): CredentialEncryptionService, Component, Logger, SecretKeySpec

### Community 120 - "Order.java"
Cohesion: 0.29
Nodes (4): Entity, PrePersist, PreUpdate, Table

### Community 121 - "RetryingBinanceFuturesTradingAdapterContractTest"
Cohesion: 0.48
Nodes (3): ExtendWith, Override, RetryingBinanceFuturesTradingAdapterContractTest

## Knowledge Gaps
- **209 isolated node(s):** `com.autotrade.dashboard:backend`, `ALPACA`, `BINANCE`, `PAPER`, `LIVE` (+204 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **21 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `TradingMode` connect `TradingMode` to `MockBrokerAdapter`, `BinanceFuturesTradingAdapter`, `AlpacaTradingAdapter`, `Order`, `TradingModeServiceTest`, `BrokerAdapterContractTest`, `.readDecrypted`, `Broker`, `.submitOrder`, `OrderServiceTest.java`, `OrderQueryControllerTest`, `.run`, `BrokerCredentialService`, `OrderServiceTest`, `BrokerAdapterConfig.java`, `BinanceFuturesAdapterConfig.java`, `OrderStatus`, `OrderQueryController`, `BrokerCredentialServiceFindTest.java`, `Order.java`, `RetryingBinanceFuturesTradingAdapterContractTest`?**
  _High betweenness centrality (0.119) - this node is a cross-community bridge._
- **Why does `Broker` connect `Broker` to `MockBrokerAdapter`, `BinanceFuturesTradingAdapter`, `AlpacaTradingAdapter`, `Order`, `AlpacaMarketDataClient`, `TradingMode`, `IndicatorSnapshot`, `.readDecrypted`, `WatchlistSignalPollerTest.java`, `MarketDataExceptionHandler`, `.submitOrder`, `BinanceMarketDataClient`, `OrderServiceTest.java`, `OrderQueryControllerTest`, `.findRegistered`, `TickerNotRegisteredException`, `BrokerCredentialService`, `.getPriceHistory`, `Candle`, `MarketDataControllerTest.java`, `OrderControllerTest`, `OrderStatus`, `ChartDataResponse`, `MarketDataController`, `.handleRateLimited`, `Order.java`?**
  _High betweenness centrality (0.118) - this node is a cross-community bridge._
- **Why does `Ticker` connect `Ticker` to `Order`, `TradingModeServiceTest`, `TradingMode`, `IndicatorSnapshot`, `Broker`, `WatchlistSignalPollerTest.java`, `SignalCallEntry`, `.submitOrder`, `SignalRuleId`, `OrderServiceTest.java`, `.resolveOrRegister`, `.findRegistered`, `Notification`, `WatchlistSignalPollerTest`, `TickerNotRegisteredException`, `BrokerCredentialService`, `OrderServiceTest`, `.getPriceHistory`, `.order`, `NotificationServiceTest`, `Candle`, `TickerService`, `WatchlistEntry`, `MarketDataControllerTest.java`, `WatchlistControllerTest`, `OrderStatus`, `TickerController`, `IndicatorService`, `Order.java`?**
  _High betweenness centrality (0.092) - this node is a cross-community bridge._
- **What connects `com.autotrade.dashboard:backend`, `ALPACA`, `BINANCE` to the rest of the system?**
  _209 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `MockBrokerAdapter` be split into smaller, more focused modules?**
  _Cohesion score 0.07059607059607059 - nodes in this community are weakly interconnected._
- **Should `BinanceFuturesTradingAdapter` be split into smaller, more focused modules?**
  _Cohesion score 0.06639427987742594 - nodes in this community are weakly interconnected._
- **Should `AlpacaTradingAdapter` be split into smaller, more focused modules?**
  _Cohesion score 0.07115677321156773 - nodes in this community are weakly interconnected._