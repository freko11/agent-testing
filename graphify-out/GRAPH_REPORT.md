# Graph Report - agent testing  (2026-08-02)

## Corpus Check
- 289 files · ~99,089 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 2388 nodes · 6450 edges · 133 communities (112 shown, 21 thin omitted)
- Extraction: 85% EXTRACTED · 15% INFERRED · 0% AMBIGUOUS · INFERRED: 937 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `6a640684`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- BrokerOrderRequest
- BinanceFuturesTradingAdapter
- AlpacaTradingAdapter
- Order
- TradingModeResponse
- FakeAlpacaTradingServer
- AlpacaMarketDataClient
- .calculate
- BrokerAdapterContractTest
- TradingMode
- IndicatorSnapshot
- devDependencies
- BrokerCredential
- Broker
- OrderServiceTest.java
- TradingModeServiceTest
- MarketDataExceptionHandler
- RiskLimitService
- DecryptedCredential
- BinanceMarketDataClient
- TradeForm.tsx
- TradingModeService
- TradingModeEvent
- SecurityConfig.java
- .run
- .resolveOrRegister
- order/api.ts
- PriceChart.tsx
- TickerService
- DashboardPage.tsx
- compilerOptions
- Notification
- WatchlistSignalPollerTest
- .evaluate
- RiskConsentEvent
- TickerNotRegisteredException
- BrokerCredentialService
- .submitOrder
- KillSwitchService
- compilerOptions
- SignalService
- .getChartData
- OrderQueryController
- run skill (project override)
- NotificationServiceTest
- security-review skill
- apiFetch
- .bullishCandles
- WatchlistEntry
- Ticker
- general-purpose agent (implementation)
- .calculate
- .getPriceHistory
- .run
- Checkpoint
- adapter-contract-check skill
- WatchlistController
- TradingModeController
- BrokerAdapterConfig.java
- BinanceFuturesAdapterConfig.java
- .calculate
- RiskExceptionHandler.java
- ApiErrorResponse
- NotificationType
- BrokerOrderResult
- WatchlistControllerTest
- RiskLimitConfig.java
- InvalidTradeRequestException
- marketdata/api.ts
- OrderStatus
- OrderNotFoundException
- OrderRefreshUnavailableException
- SignalNotActionableException
- NotificationController
- OrderControllerTest
- TickerControllerTest
- NotificationControllerTest
- SecurityConfigTest
- TickerMetrics.tsx
- NotificationService
- TickerServiceTest
- mvnw
- Changelog
- SignalCallEntry
- AuthController.java
- NotificationExceptionHandler.java
- .export
- TradingModeExceptionHandler.java
- plugins
- BrokerCredentialServiceFindTest.java
- .encrypt
- signal-rule-review skill
- dataviz skill
- .handleRateLimited
- ClockConfig.java
- FakeBinanceFuturesTradingServer
- BackendApplicationTests.java
- OrderQueryControllerTest
- .calculate
- Notification system + WatchlistSignalPoller
- BackendApplication
- SchedulingConfig.java
- F5.3 Order status & history
- E7 — Observability & Hardening
- BacktestReport
- MarketHoursServiceTest
- Candle
- IndicatorTestFixtures
- BacktestConfig
- tsconfig.json
- Bluesky Icon (SVG symbol)
- Discord Icon (SVG symbol)
- Documentation Icon (SVG symbol)
- App Favicon (Purple Lightning-Bolt Glyph)
- com.autotrade.dashboard:backend
- killswitch/api.ts
- OrderRepository
- MarketDataController
- .readDecrypted
- TickerController
- BacktestHarnessTest.java
- MarketDataControllerTest.java
- SignalRuleId
- CredentialEncryptionService
- WatchlistService
- PaperTradeThresholdNotMetException
- OrderController
- OrderCsvExporter
- AlpacaTradingAdapterContractTest
- RetryingBinanceFuturesTradingAdapterContractTest
- .handleUnavailable

## God Nodes (most connected - your core abstractions)
1. `TradingMode` - 123 edges
2. `Broker` - 104 edges
3. `Ticker` - 97 edges
4. `Order` - 92 edges
5. `AssetType` - 83 edges
6. `IndicatorSnapshot` - 59 edges
7. `Candle` - 58 edges
8. `BrokerCredential` - 56 edges
9. `BinanceFuturesTradingAdapter` - 54 edges
10. `OrderServiceTest` - 54 edges

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

## Communities (133 total, 21 thin omitted)

### Community 0 - "BrokerOrderRequest"
Cohesion: 0.05
Nodes (23): AssetBalance, BrokerAccountStatus, BrokerAdapterRetryPolicy, BrokerOrderRequest, Override, RetryingBrokerAdapter, BrokerAdapterRouterTest, Test (+15 more)

### Community 1 - "BinanceFuturesTradingAdapter"
Cohesion: 0.07
Nodes (22): Credentials, BinanceAccountAsset, BinanceAccountResponse, BinanceErrorResponse, BinanceFuturesTradingAdapter, BinanceOrderResponse, BinancePositionResponse, Credentials (+14 more)

### Community 2 - "AlpacaTradingAdapter"
Cohesion: 0.08
Nodes (22): AlpacaAccountResponse, AlpacaBracketLeg, AlpacaErrorResponse, AlpacaOrderRequestBody, AlpacaOrderResponse, AlpacaPositionResponse, AlpacaStopLeg, AlpacaTradingAdapter (+14 more)

### Community 3 - "Order"
Cohesion: 0.06
Nodes (7): Entity, Override, PrePersist, PreUpdate, Table, Order, Test

### Community 4 - "TradingModeResponse"
Cohesion: 0.20
Nodes (6): TradingModeResponse, AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, TradingModeControllerTest

### Community 5 - "FakeAlpacaTradingServer"
Cohesion: 0.32
Nodes (7): FakeAlpacaTradingServer, FakeOrder, ClientHttpRequest, ClientHttpResponse, HttpStatus, ObjectMapper, MockClientHttpRequest

### Community 6 - "AlpacaMarketDataClient"
Cohesion: 0.09
Nodes (23): AlpacaBar, AlpacaBarsResponse, AlpacaMarketDataClient, Candle, Component, JsonIgnoreProperties, Override, RestClient (+15 more)

### Community 7 - ".calculate"
Cohesion: 0.08
Nodes (18): HoldTermCalculator, HoldTermRule, MODERATE_HIGH, MODERATE_LOW, MODERATE_MEDIUM, STRONG_HIGH, STRONG_LOW, STRONG_MEDIUM (+10 more)

### Community 8 - "BrokerAdapterContractTest"
Cohesion: 0.18
Nodes (7): BinanceFuturesTradingAdapterContractTest, ExtendWith, Override, BrokerAdapterContractTest, Test, Override, RetryingMockBrokerAdapterContractTest

### Community 9 - "TradingMode"
Cohesion: 0.17
Nodes (15): BrokerPosition, TradingMode, LIVE, PAPER, EntryOrderType, LIMIT, MARKET, JsonInclude (+7 more)

### Community 10 - "IndicatorSnapshot"
Cohesion: 0.08
Nodes (5): IndicatorSnapshot, Entity, Override, PrePersist, Table

### Community 11 - "devDependencies"
Cohesion: 0.05
Nodes (36): dependencies, lightweight-charts, react, react-dom, react-router-dom, devDependencies, oxlint, @types/node (+28 more)

### Community 12 - "BrokerCredential"
Cohesion: 0.12
Nodes (6): BrokerCredential, Entity, Override, PrePersist, PreUpdate, Table

### Community 13 - "Broker"
Cohesion: 0.12
Nodes (9): Broker, ALPACA, BINANCE, BrokerAdapterAmbiguousOrderException, BrokerAdapterException, BrokerAdapterRateLimitedException, BrokerAdapterTransientException, BrokerAdapterUnavailableException (+1 more)

### Community 14 - "OrderServiceTest.java"
Cohesion: 0.14
Nodes (17): IndicatorResponse, BigDecimalIndicators, MacdResult, MovingAverageRelation, EQUAL, SHORT_ABOVE_LONG, SHORT_BELOW_LONG, MovingAverageResult (+9 more)

### Community 15 - "TradingModeServiceTest"
Cohesion: 0.26
Nodes (5): SpringBootTest, Test, Transactional, TradingModeServiceTest, TestPropertySource

### Community 16 - "MarketDataExceptionHandler"
Cohesion: 0.13
Nodes (10): InsufficientPriceHistoryException, InvalidIndicatorRequestException, InvalidPriceHistoryRequestException, ExceptionHandler, ResponseEntity, RestControllerAdvice, MarketDataExceptionHandler, NoPriceDataException (+2 more)

### Community 17 - "RiskLimitService"
Cohesion: 0.21
Nodes (6): Service, RiskLimitService, ConfigurationProperties, RiskLimitsProperties, Test, RiskLimitServiceTest

### Community 18 - "DecryptedCredential"
Cohesion: 0.15
Nodes (8): DecryptedCredential, Override, BeforeEach, BeforeEach, RestClient, RestClient, BeforeEach, BeforeEach

### Community 19 - "BinanceMarketDataClient"
Cohesion: 0.12
Nodes (11): BinanceMarketDataClient, Candle, Component, Override, RestClient, TooManyRequests, RetryHelper, BinanceMarketDataClientTest (+3 more)

### Community 20 - "TradeForm.tsx"
Cohesion: 0.11
Nodes (23): TickerSummary, Broker, HoldTerm, IndicatorResponse, MacdResult, MovingAverageRelation, MovingAverageResult, SignalCall (+15 more)

### Community 22 - "TradingModeEvent"
Cohesion: 0.16
Nodes (6): Entity, Override, PrePersist, Table, TradingModeEvent, TradingModeEventRepository

### Community 23 - "SecurityConfig.java"
Cohesion: 0.16
Nodes (19): CsrfCookieWriteFilter, Bean, Configuration, Logger, Override, SecurityConfig, SpaCsrfTokenRequestHandler, CsrfToken (+11 more)

### Community 24 - ".run"
Cohesion: 0.11
Nodes (17): ApplicationRunner, AlpacaTradingCredentialBootstrap, ApplicationArguments, Component, Logger, Override, BinanceTradingCredentialBootstrap, ApplicationArguments (+9 more)

### Community 25 - ".resolveOrRegister"
Cohesion: 0.21
Nodes (6): Transactional, WatchlistEntry, SpringBootTest, Test, Transactional, WatchlistServiceTest

### Community 26 - "order/api.ts"
Cohesion: 0.14
Nodes (18): RFC-6266, readCookie(), EntryOrderType, exportOrdersCsv(), fetchOrders(), filenameFromContentDisposition(), OrderSummary, refreshOrder() (+10 more)

### Community 27 - "PriceChart.tsx"
Cohesion: 0.18
Nodes (18): Broker, ChartDataResponse, ChartIndicatorPoint, CandlestickPoint, LinePoint, toBusinessDay(), toCandlestickSeries(), toMaLongSeries() (+10 more)

### Community 28 - "TickerService"
Cohesion: 0.14
Nodes (12): MarketDataClient, Service, MarketDataService, Component, MarketHoursService, Service, Transactional, TickerService (+4 more)

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
Cohesion: 0.19
Nodes (4): SignalRuleEngine, MacdResult, Test, SignalRuleEngineTest

### Community 34 - "RiskConsentEvent"
Cohesion: 0.24
Nodes (5): Entity, Override, PrePersist, Table, RiskConsentEvent

### Community 35 - "TickerNotRegisteredException"
Cohesion: 0.17
Nodes (6): TickerNotRegisteredException, IndicatorControllerTest, AutoConfigureMockMvc, MockMvc, Test, WebMvcTest

### Community 36 - "BrokerCredentialService"
Cohesion: 0.12
Nodes (13): BrokerCredentialRepository, BrokerCredentialService, Logger, Service, IndicatorSnapshotRepository, CoreDataModelIntegrationTest, SpringBootTest, Test (+5 more)

### Community 37 - ".submitOrder"
Cohesion: 0.27
Nodes (5): PlaceOrderRequest, SignalComputation, ExtendWith, Test, OrderServiceTest

### Community 38 - "KillSwitchService"
Cohesion: 0.06
Nodes (30): EngageKillSwitchResponse, GetMapping, PostMapping, RequestMapping, RestController, KillSwitchController, Entity, Override (+22 more)

### Community 39 - "compilerOptions"
Cohesion: 0.10
Nodes (19): compilerOptions, allowImportingTsExtensions, erasableSyntaxOnly, lib, module, moduleDetection, noEmit, noFallthroughCasesInSwitch (+11 more)

### Community 40 - "SignalService"
Cohesion: 0.10
Nodes (19): IndicatorComputation, IndicatorService, Service, SignalCallEntryRepository, GetMapping, RequestMapping, RestController, SignalController (+11 more)

### Community 41 - ".getChartData"
Cohesion: 0.23
Nodes (7): ChartDataResponse, ChartIndicatorPoint, IndicatorController, GetMapping, RequestMapping, RestController, ChartDataResponse

### Community 42 - "OrderQueryController"
Cohesion: 0.29
Nodes (6): GetMapping, PostMapping, RequestMapping, ResponseEntity, RestController, OrderQueryController

### Community 43 - "run skill (project override)"
Cohesion: 0.14
Nodes (19): run skill (project override), MarketHoursService (hardcoded NYSE/NASDAQ calendar), oracle-xe Docker Compose service, F1.1 Local dev environment, E1-F1-S1 Docker Compose file for Oracle XE, E1-F1-S2 Spring Boot backend skeleton, E1-F1-S3 React app skeleton with routing, E1-F1-S4 CI pipeline builds/tests both apps (+11 more)

### Community 44 - "NotificationServiceTest"
Cohesion: 0.31
Nodes (3): ExtendWith, Test, NotificationServiceTest

### Community 45 - "security-review skill"
Cohesion: 0.19
Nodes (18): guardrail-check skill, security-review skill, simplify skill, F1.3 Secrets & config management, E1-F3-S1 Broker API keys encrypted at rest, E1-F3-S2 Dashboard requires login, F4.2 Alpaca adapter (stocks), F6.1 Paper/live mode toggle (+10 more)

### Community 46 - "apiFetch"
Cohesion: 0.21
Nodes (19): apiFetch(), fetchPriceHistory(), parseMarketDataError(), fetchNotifications(), fetchUnreadCount(), markAllNotificationsRead(), markNotificationRead(), NotificationEventType (+11 more)

### Community 48 - "WatchlistEntry"
Cohesion: 0.17
Nodes (5): Entity, Override, PrePersist, Table, WatchlistEntry

### Community 49 - "Ticker"
Cohesion: 0.16
Nodes (5): Entity, Override, PrePersist, Table, Ticker

### Community 50 - "general-purpose agent (implementation)"
Cohesion: 0.24
Nodes (16): Explore agent (research), general-purpose agent (implementation), Plan agent (design gate), Mandatory per-story workflow (update CLAUDE.md, commit, push), CLAUDE.md project status & architecture log, E1 — Platform Foundation, F1.2 Core data model, F1.4 Testing strategy (+8 more)

### Community 51 - ".calculate"
Cohesion: 0.26
Nodes (4): MathContext, VolatilityCalculator, Test, VolatilityCalculatorTest

### Community 52 - ".getPriceHistory"
Cohesion: 0.29
Nodes (5): PriceHistoryResult, IndicatorServiceTest, BeforeEach, ExtendWith, Test

### Community 53 - ".run"
Cohesion: 0.23
Nodes (5): BacktestHarness, HoldGateAccumulator, HoldGateOutcome, LARGE_MOVE, STABLE

### Community 54 - "Checkpoint"
Cohesion: 0.19
Nodes (9): DirectionalAccumulator, Checkpoint, MAX, MID, MIN, DirectionalOutcome, LOSS, WASH (+1 more)

### Community 55 - "adapter-contract-check skill"
Cohesion: 0.18
Nodes (15): adapter-contract-check skill, BrokerAdapter interface (E4-F1-S1), OrderService.submitOrder (bracket order construction), RetryingBrokerAdapter decorator (retry/backoff/outage), TradingModeService (paper/live switch, append-only), F4.1 Adapter interface, E4-F1-S1 BrokerAdapter interface + mock + contract suite, E4-F1-S2 Rate-limit/retry/backoff in adapter contract (+7 more)

### Community 56 - "WatchlistController"
Cohesion: 0.25
Nodes (8): GetMapping, PostMapping, RequestMapping, ResponseEntity, RestController, WatchlistController, WatchlistEntryResponse, DeleteMapping

### Community 57 - "TradingModeController"
Cohesion: 0.24
Nodes (6): TradingModeChangeRequest, GetMapping, PostMapping, RequestMapping, RestController, TradingModeController

### Community 58 - "BrokerAdapterConfig.java"
Cohesion: 0.27
Nodes (8): AlpacaTradingProperties, ConfigurationProperties, BrokerAdapterConfig, Bean, Configuration, EnableConfigurationProperties, RestClient, SimpleClientHttpRequestFactory

### Community 59 - "BinanceFuturesAdapterConfig.java"
Cohesion: 0.27
Nodes (8): BinanceFuturesAdapterConfig, Bean, Configuration, EnableConfigurationProperties, RestClient, SimpleClientHttpRequestFactory, BinanceFuturesTradingProperties, ConfigurationProperties

### Community 60 - ".calculate"
Cohesion: 0.23
Nodes (5): MacdResult, MathContext, MacdCalculator, Test, MacdCalculatorTest

### Community 61 - "RiskExceptionHandler.java"
Cohesion: 0.52
Nodes (4): ExceptionHandler, ResponseEntity, RestControllerAdvice, RiskExceptionHandler

### Community 62 - "ApiErrorResponse"
Cohesion: 0.37
Nodes (6): ApiErrorResponse, JsonInclude, ExceptionHandler, ResponseEntity, RestControllerAdvice, OrderExceptionHandler

### Community 63 - "NotificationType"
Cohesion: 0.17
Nodes (11): JsonInclude, NotificationResponse, NotificationType, ORDER_CANCELLED, ORDER_FAILED, ORDER_FILLED, ORDER_PARTIALLY_FILLED, ORDER_PARTIALLY_PROTECTED (+3 more)

### Community 64 - "BrokerOrderResult"
Cohesion: 0.20
Nodes (7): BrokerAdapter, BrokerAdapterRouter, Service, BrokerOrderResult, KillSwitchCancelSummary, Service, OrderService

### Community 65 - "WatchlistControllerTest"
Cohesion: 0.28
Nodes (7): AddWatchlistEntryRequest, AutoConfigureMockMvc, MockMvc, ObjectMapper, Test, WebMvcTest, WatchlistControllerTest

### Community 66 - "RiskLimitConfig.java"
Cohesion: 0.83
Nodes (3): Configuration, EnableConfigurationProperties, RiskLimitConfig

### Community 68 - "marketdata/api.ts"
Cohesion: 0.26
Nodes (9): MarketDataError, MarketDataErrorCode, PriceHistoryResponse, fetchWatchlist(), removeFromWatchlist(), WatchlistEntry, describeError(), Watchlist() (+1 more)

### Community 69 - "OrderStatus"
Cohesion: 0.07
Nodes (16): Entity, Override, PrePersist, Table, OrderAuditEntry, OrderStatus, CANCELLED, FAILED (+8 more)

### Community 73 - "NotificationController"
Cohesion: 0.29
Nodes (6): GetMapping, PostMapping, RequestMapping, ResponseEntity, RestController, NotificationController

### Community 74 - "OrderControllerTest"
Cohesion: 0.15
Nodes (10): JsonInclude, TradeOrderResponse, KillSwitchEngagedException, RiskLimitExceededException, AutoConfigureMockMvc, MockMvc, ObjectMapper, Test (+2 more)

### Community 75 - "TickerControllerTest"
Cohesion: 0.32
Nodes (7): RegisterTickerRequest, AutoConfigureMockMvc, MockMvc, ObjectMapper, Test, WebMvcTest, TickerControllerTest

### Community 76 - "NotificationControllerTest"
Cohesion: 0.20
Nodes (5): AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, NotificationControllerTest

### Community 77 - "SecurityConfigTest"
Cohesion: 0.32
Nodes (6): AutoConfigureMockMvc, MockMvc, SpringBootTest, Test, SecurityConfigTest, Cookie

### Community 78 - "TickerMetrics.tsx"
Cohesion: 0.18
Nodes (11): fetchChartData(), fetchSignal(), AddToWatchlistButton(), ERROR_MESSAGES, formatOrDash(), relationLabel(), StatTileProps, TickerMetrics() (+3 more)

### Community 79 - "NotificationService"
Cohesion: 0.21
Nodes (6): Pageable, NotificationRepository, Logger, Service, NotificationService, BeforeEach

### Community 80 - "TickerServiceTest"
Cohesion: 0.43
Nodes (4): SpringBootTest, Test, Transactional, TickerServiceTest

### Community 81 - "mvnw"
Cohesion: 0.33
Nodes (6): mvnw script, clean(), die(), exec_maven(), set_java_home(), verbose()

### Community 82 - "Changelog"
Cohesion: 0.04
Nodes (44): Changelog, E1-F1-S1 — local Oracle XE via Docker Compose, E1-F1-S2 — Spring Boot backend skeleton, E1-F1-S3 — React app skeleton, E1-F1-S4 — CI pipeline, E1-F1-S5 — env/config profiles, E1-F2 — core data model, E1-F3-S1 — broker-credential key rotation (+36 more)

### Community 83 - "SignalCallEntry"
Cohesion: 0.15
Nodes (5): Entity, Override, PrePersist, Table, SignalCallEntry

### Community 84 - "AuthController.java"
Cohesion: 0.39
Nodes (6): Authentication, AuthController, GetMapping, RequestMapping, ResponseEntity, RestController

### Community 85 - "NotificationExceptionHandler.java"
Cohesion: 0.31
Nodes (5): InvalidNotificationRequestException, ExceptionHandler, ResponseEntity, RestControllerAdvice, NotificationExceptionHandler

### Community 87 - "TradingModeExceptionHandler.java"
Cohesion: 0.31
Nodes (5): RiskConsentNotGivenException, ExceptionHandler, ResponseEntity, RestControllerAdvice, TradingModeExceptionHandler

### Community 88 - "plugins"
Cohesion: 0.22
Nodes (8): plugins, rules, react/only-export-components, react/rules-of-hooks, $schema, oxc, typescript, warn

### Community 89 - "BrokerCredentialServiceFindTest.java"
Cohesion: 0.39
Nodes (4): BrokerCredentialServiceFindTest, SpringBootTest, Test, Transactional

### Community 91 - "signal-rule-review skill"
Cohesion: 0.43
Nodes (8): signal-rule-review skill, BacktestHarness (walk-forward JUnit validation), HoldTermCalculator (trend strength x volatility band), SignalRuleEngine (Buy/Sell/Hold rule table), F2.3 Buy/Sell/Hold signal & hold-term, E2-F3-S2 Suggested hold-term alongside the call, F2.4 Backtesting, E2-F4-S1 Backtest rule table against historical data

### Community 92 - "dataviz skill"
Cohesion: 0.38
Nodes (7): dataviz skill, SignalBadge colorblind-safe teal/orange/slate palette, F3.1 Ticker lookup & metrics display, E3-F1-S1 Ticker lookup + stat-tile metrics, E3-F1-S2 Buy/Sell/Hold badge color-coded, F3.2 Metric visualization, E3-F2-S1 Price chart with MA/RSI overlays

### Community 94 - "ClockConfig.java"
Cohesion: 0.60
Nodes (3): ClockConfig, Bean, Configuration

### Community 95 - "FakeBinanceFuturesTradingServer"
Cohesion: 0.33
Nodes (7): FakeBinanceFuturesTradingServer, ClientHttpRequest, ClientHttpResponse, HttpStatus, StoredOrder, MultiValueMap, URI

### Community 96 - "BackendApplicationTests.java"
Cohesion: 0.60
Nodes (3): BackendApplicationTests, SpringBootTest, Test

### Community 97 - "OrderQueryControllerTest"
Cohesion: 0.16
Nodes (5): AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, OrderQueryControllerTest

### Community 98 - ".calculate"
Cohesion: 0.24
Nodes (5): MathContext, MovingAverageResult, MovingAverageCrossoverCalculator, Test, MovingAverageCrossoverCalculatorTest

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

### Community 104 - "BacktestReport"
Cohesion: 0.21
Nodes (4): BacktestReport, CheckpointStats, DirectionalOutcomeStats, HoldGateStats

### Community 106 - "Candle"
Cohesion: 0.18
Nodes (5): MathContext, RsiCalculator, Candle, Test, RsiCalculatorTest

### Community 107 - "IndicatorTestFixtures"
Cohesion: 0.17
Nodes (5): VolumeTrendCalculator, IndicatorTestFixtures, Candle, Test, VolumeTrendCalculatorTest

### Community 117 - "killswitch/api.ts"
Cohesion: 0.32
Nodes (10): clearKillSwitch(), engageKillSwitch(), EngageKillSwitchResponse, fetchKillSwitchState(), KillSwitchCancelSummary, KillSwitchResponse, KillSwitchState, describeError() (+2 more)

### Community 118 - "OrderRepository"
Cohesion: 0.16
Nodes (7): OrderAuditEntryRepository, Pageable, Query, OrderRepository, TickerRepository, RiskConsentEventRepository, JpaRepository

### Community 119 - "MarketDataController"
Cohesion: 0.27
Nodes (5): GetMapping, RequestMapping, RestController, MarketDataController, PriceHistoryResponse

### Community 120 - ".readDecrypted"
Cohesion: 0.24
Nodes (5): Transactional, BrokerCredentialServiceRotationTest, SpringBootTest, Test, Transactional

### Community 121 - "TickerController"
Cohesion: 0.36
Nodes (6): PostMapping, RequestMapping, ResponseEntity, RestController, TickerController, TickerResponse

### Community 122 - "BacktestHarnessTest.java"
Cohesion: 0.25
Nodes (4): BacktestCandleCsvLoader, Candle, BacktestHarnessTest, Test

### Community 123 - "MarketDataControllerTest.java"
Cohesion: 0.30
Nodes (5): AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, MarketDataControllerTest

### Community 124 - "SignalRuleId"
Cohesion: 0.14
Nodes (13): HoldTerm, call(), SignalRuleId, BEARISH_MAJORITY, BEARISH_UNANIMOUS, BULLISH_MAJORITY, BULLISH_UNANIMOUS, CONFLICTING_SIGNALS (+5 more)

### Community 125 - "CredentialEncryptionService"
Cohesion: 0.43
Nodes (4): CredentialEncryptionService, Component, Logger, SecretKeySpec

### Community 126 - "WatchlistService"
Cohesion: 0.25
Nodes (4): Query, WatchlistEntryRepository, Service, WatchlistService

### Community 128 - "OrderController"
Cohesion: 0.39
Nodes (5): PostMapping, RequestMapping, ResponseEntity, RestController, OrderController

### Community 130 - "AlpacaTradingAdapterContractTest"
Cohesion: 0.48
Nodes (3): AlpacaTradingAdapterContractTest, ExtendWith, Override

### Community 131 - "RetryingBinanceFuturesTradingAdapterContractTest"
Cohesion: 0.48
Nodes (3): ExtendWith, Override, RetryingBinanceFuturesTradingAdapterContractTest

## Knowledge Gaps
- **218 isolated node(s):** `com.autotrade.dashboard:backend`, `ALPACA`, `BINANCE`, `PAPER`, `LIVE` (+213 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **21 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `TradingMode` connect `TradingMode` to `BrokerOrderRequest`, `BinanceFuturesTradingAdapter`, `AlpacaTradingAdapter`, `Order`, `TradingModeResponse`, `AlpacaTradingAdapterContractTest`, `RetryingBinanceFuturesTradingAdapterContractTest`, `BrokerAdapterContractTest`, `BrokerCredential`, `Broker`, `OrderServiceTest.java`, `TradingModeServiceTest`, `TradingModeService`, `TradingModeEvent`, `.run`, `BrokerCredentialService`, `.submitOrder`, `OrderQueryController`, `TradingModeController`, `BrokerAdapterConfig.java`, `BinanceFuturesAdapterConfig.java`, `BrokerOrderResult`, `OrderStatus`, `BrokerCredentialServiceFindTest.java`, `OrderQueryControllerTest`, `OrderRepository`, `.readDecrypted`, `SignalRuleId`?**
  _High betweenness centrality (0.118) - this node is a cross-community bridge._
- **Why does `BrokerCredentialService` connect `BrokerCredentialService` to `BrokerOrderRequest`, `BinanceFuturesTradingAdapter`, `AlpacaTradingAdapter`, `AlpacaTradingAdapterContractTest`, `RetryingBinanceFuturesTradingAdapterContractTest`, `BrokerAdapterContractTest`, `TradingMode`, `Broker`, `OrderServiceTest.java`, `DecryptedCredential`, `.run`, `.submitOrder`, `security-review skill`, `BrokerAdapterConfig.java`, `BinanceFuturesAdapterConfig.java`, `BrokerOrderResult`, `BrokerCredentialServiceFindTest.java`, `OrderRepository`, `.readDecrypted`, `CredentialEncryptionService`?**
  _High betweenness centrality (0.093) - this node is a cross-community bridge._
- **Why does `Broker` connect `Broker` to `BrokerOrderRequest`, `BinanceFuturesTradingAdapter`, `AlpacaTradingAdapter`, `Order`, `.handleUnavailable`, `AlpacaMarketDataClient`, `TradingMode`, `IndicatorSnapshot`, `BrokerCredential`, `OrderServiceTest.java`, `BinanceMarketDataClient`, `TickerService`, `BrokerCredentialService`, `.submitOrder`, `.getChartData`, `.getPriceHistory`, `OrderControllerTest`, `.handleRateLimited`, `OrderQueryControllerTest`, `MarketDataController`, `.readDecrypted`, `MarketDataControllerTest.java`, `SignalRuleId`?**
  _High betweenness centrality (0.086) - this node is a cross-community bridge._
- **What connects `com.autotrade.dashboard:backend`, `ALPACA`, `BINANCE` to the rest of the system?**
  _218 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `BrokerOrderRequest` be split into smaller, more focused modules?**
  _Cohesion score 0.05256648113790971 - nodes in this community are weakly interconnected._
- **Should `BinanceFuturesTradingAdapter` be split into smaller, more focused modules?**
  _Cohesion score 0.06666666666666667 - nodes in this community are weakly interconnected._
- **Should `AlpacaTradingAdapter` be split into smaller, more focused modules?**
  _Cohesion score 0.08125 - nodes in this community are weakly interconnected._