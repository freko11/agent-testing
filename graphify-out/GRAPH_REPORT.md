# Graph Report - .  (2026-08-02)

## Corpus Check
- 265 files · ~85,606 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 2132 nodes · 5750 edges · 117 communities (99 shown, 18 thin omitted)
- Extraction: 86% EXTRACTED · 14% INFERRED · 0% AMBIGUOUS · INFERRED: 792 edges (avg confidence: 0.8)
- Token cost: 396,592 input · 0 output

## Community Hubs (Navigation)
- Broker Adapter Retry & Mock Test Infra
- Binance Futures Trading Adapter
- Alpaca Trading Adapter
- Order Entity & Persistence
- Trading Mode Switch (Paper/Live)
- Broker Credential Decryption
- Alpaca Market Data Client
- Hold-Term Calculator
- Alpaca Adapter Contract Tests
- Broker & Credential Core Types
- Indicator Snapshot Computation
- Frontend Dependencies (package.json)
- Broker Credential Encryption Service
- Broker Enum & Credential Core
- Indicator Response DTOs
- Signal Call Entry & Notification Recording
- Indicator Request Exceptions
- Order Submission Flow
- Hold Term & Signal Call Enum
- Binance Market Data Client
- Frontend Signal API Types
- Broker Adapter Exceptions
- Order Repository
- Security Config (CSRF)
- Alpaca Trading Credential Bootstrap
- Watchlist Entry & Ticker Resolution
- Frontend Auth & Order API
- Frontend Chart API
- Market Data Service Routing
- Frontend App Shell & Auth Context
- Frontend TS Config (App)
- Notification Entity
- Watchlist Signal Poller
- Signal Rule Engine Tests
- Broker Credential Entity
- Market Closed Exception & Indicator Tests
- Broker Credential Repository
- BrokerAdapter Interface & Result Types
- Ticker Entity
- Frontend TS Config (Node)
- Indicator Service
- Chart Data & Price History
- Order CSV Exporter
- Docs: E1.1 Local Dev Environment (Oracle/Docker)
- Notification Service Tests
- Docs: F1.3 Secrets & Config Management
- Frontend API Error Helpers
- Indicator Test Fixtures
- Ticker Repository & Service
- Watchlist Entry Persistence
- Docs: Agent Roles & Delivery Workflow
- Volatility Calculator
- Market Data Controller Tests
- Backtest Harness (Walk-Forward Loop)
- Backtest Directional Outcome Stats
- Docs: Broker Adapter & Guardrail Rationale
- Watchlist Controller
- Order Controller Tests
- Alpaca Trading Config
- Binance Futures Adapter Config
- MACD Calculator
- Moving Average Crossover Calculator
- API Error Response Handling
- Notification Response DTOs
- Signal Controller Tests
- Watchlist Controller Tests
- Backtest Report
- Market Hours Service Tests
- Frontend Market Data Error Types
- Broker Order Result & Status Enum
- Chart Data DTOs & Indicator Controller
- RSI Calculator
- Volume Trend Calculator
- Notification Controller
- Order Query Controller
- Ticker Controller Tests
- Notification Controller Tests
- Security Config Tests
- Frontend Ticker Metrics Component
- Notification Repository
- Ticker Controller
- Maven Wrapper Script (mvnw)
- Binance Credential Bootstrap Tests
- Market Data Controller
- Auth Controller
- Notification Exception Handler
- Ticker Service Tests
- Live Mode Guard Exception Handler
- Frontend Lint Config (oxlint)
- Broker Credential Find Tests
- Order Controller
- Docs: Buy/Sell/Hold Signal & Hold-Term
- Docs: Ticker Lookup & Signal Badge
- Market Data Rate-Limited Exception
- Clock Config Bean
- Market Data Unavailable Exception
- Backend Application Context Test
- Backtest Candle CSV Loader
- E2E Test Candle Fixtures
- Docs: Watchlist & Notifications
- Backend Application Entrypoint
- Scheduling Config
- Docs: Order History & CSV Export
- Docs: E7 Observability & Hardening (Planned)
- Invalid Trade Request Exception
- Order Not Found Exception
- Order Refresh Unavailable Exception
- Signal Not Actionable Exception
- Backtest Config Thresholds
- Frontend TS Config (Root)
- Icon Set: Bluesky/X
- Icon Set: Discord/Community
- Icon Set: Docs/GitHub
- App Favicon
- Maven POM (Backend)

## God Nodes (most connected - your core abstractions)
1. `TradingMode` - 119 edges
2. `Broker` - 103 edges
3. `Ticker` - 90 edges
4. `Order` - 85 edges
5. `AssetType` - 79 edges
6. `IndicatorSnapshot` - 59 edges
7. `Candle` - 58 edges
8. `BinanceFuturesTradingAdapter` - 54 edges
9. `BrokerCredential` - 53 edges
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

## Communities (117 total, 18 thin omitted)

### Community 0 - "Broker Adapter Retry & Mock Test Infra"
Cohesion: 0.06
Nodes (19): AssetBalance, BrokerAdapterRetryPolicy, BrokerOrderRequest, Override, RetryingBrokerAdapter, Override, MockBrokerAdapter, MockOrderState (+11 more)

### Community 1 - "Binance Futures Trading Adapter"
Cohesion: 0.07
Nodes (21): Credentials, BinanceAccountAsset, BinanceAccountResponse, BinanceErrorResponse, BinanceFuturesTradingAdapter, BinanceOrderResponse, BinancePositionResponse, Credentials (+13 more)

### Community 2 - "Alpaca Trading Adapter"
Cohesion: 0.08
Nodes (22): AlpacaAccountResponse, AlpacaBracketLeg, AlpacaErrorResponse, AlpacaOrderRequestBody, AlpacaOrderResponse, AlpacaPositionResponse, AlpacaStopLeg, AlpacaTradingAdapter (+14 more)

### Community 3 - "Order Entity & Persistence"
Cohesion: 0.07
Nodes (8): Entity, Override, PrePersist, PreUpdate, Table, Order, Test, BeforeEach

### Community 4 - "Trading Mode Switch (Paper/Live)"
Cohesion: 0.07
Nodes (24): GetMapping, PostMapping, RequestMapping, RestController, TradingModeController, Entity, Override, PrePersist (+16 more)

### Community 5 - "Broker Credential Decryption"
Cohesion: 0.10
Nodes (22): DecryptedCredential, Override, BeforeEach, BeforeEach, FakeAlpacaTradingServer, FakeOrder, ClientHttpRequest, ClientHttpResponse (+14 more)

### Community 6 - "Alpaca Market Data Client"
Cohesion: 0.09
Nodes (23): AlpacaBar, AlpacaBarsResponse, AlpacaMarketDataClient, Candle, Component, JsonIgnoreProperties, Override, RestClient (+15 more)

### Community 7 - "Hold-Term Calculator"
Cohesion: 0.08
Nodes (18): HoldTermCalculator, HoldTermRule, MODERATE_HIGH, MODERATE_LOW, MODERATE_MEDIUM, STRONG_HIGH, STRONG_LOW, STRONG_MEDIUM (+10 more)

### Community 8 - "Alpaca Adapter Contract Tests"
Cohesion: 0.10
Nodes (14): AlpacaTradingAdapterContractTest, ExtendWith, Override, BinanceFuturesTradingAdapterContractTest, ExtendWith, Override, BrokerAdapterContractTest, Test (+6 more)

### Community 9 - "Broker & Credential Core Types"
Cohesion: 0.20
Nodes (11): BrokerPosition, EntryOrderType, LIMIT, MARKET, OrderSide, BUY, SELL, AssetType (+3 more)

### Community 10 - "Indicator Snapshot Computation"
Cohesion: 0.07
Nodes (5): IndicatorSnapshot, Entity, Override, PrePersist, Table

### Community 11 - "Frontend Dependencies (package.json)"
Cohesion: 0.05
Nodes (36): dependencies, lightweight-charts, react, react-dom, react-router-dom, devDependencies, oxlint, @types/node (+28 more)

### Community 12 - "Broker Credential Encryption Service"
Cohesion: 0.12
Nodes (12): Transactional, CredentialEncryptionService, Component, Logger, BrokerCredentialServiceRotationTest, SpringBootTest, Test, Transactional (+4 more)

### Community 13 - "Broker Enum & Credential Core"
Cohesion: 0.09
Nodes (14): Broker, ALPACA, BINANCE, BrokerAccountStatus, BrokerAdapterTransientException, TradingMode, LIVE, PAPER (+6 more)

### Community 14 - "Indicator Response DTOs"
Cohesion: 0.18
Nodes (12): IndicatorResponse, MacdResult, MovingAverageRelation, EQUAL, SHORT_ABOVE_LONG, SHORT_BELOW_LONG, MovingAverageResult, TickerSummary (+4 more)

### Community 15 - "Signal Call Entry & Notification Recording"
Cohesion: 0.12
Nodes (9): IndicatorComputation, Entity, Override, PrePersist, Table, SignalCallEntry, ExtendWith, Test (+1 more)

### Community 16 - "Indicator Request Exceptions"
Cohesion: 0.13
Nodes (10): InsufficientPriceHistoryException, InvalidIndicatorRequestException, InvalidPriceHistoryRequestException, ExceptionHandler, ResponseEntity, RestControllerAdvice, MarketDataExceptionHandler, NoPriceDataException (+2 more)

### Community 17 - "Order Submission Flow"
Cohesion: 0.33
Nodes (4): PlaceOrderRequest, SignalComputation, Test, OrderServiceTest

### Community 18 - "Hold Term & Signal Call Enum"
Cohesion: 0.10
Nodes (19): HoldTerm, SignalCall, BUY, HOLD, SELL, call(), SignalRuleId, BEARISH_MAJORITY (+11 more)

### Community 19 - "Binance Market Data Client"
Cohesion: 0.12
Nodes (11): BinanceMarketDataClient, Candle, Component, Override, RestClient, TooManyRequests, RetryHelper, BinanceMarketDataClientTest (+3 more)

### Community 20 - "Frontend Signal API Types"
Cohesion: 0.11
Nodes (23): TickerSummary, Broker, HoldTerm, IndicatorResponse, MacdResult, MovingAverageRelation, MovingAverageResult, SignalCall (+15 more)

### Community 21 - "Broker Adapter Exceptions"
Cohesion: 0.12
Nodes (12): BrokerAdapterAmbiguousOrderException, BrokerAdapterException, BrokerAdapterRateLimitedException, BrokerAdapterRouter, Service, BrokerAdapterUnavailableException, Logger, Service (+4 more)

### Community 22 - "Order Repository"
Cohesion: 0.12
Nodes (7): Pageable, OrderRepository, AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, OrderQueryControllerTest

### Community 23 - "Security Config (CSRF)"
Cohesion: 0.16
Nodes (19): CsrfCookieWriteFilter, Bean, Configuration, Logger, Override, SecurityConfig, SpaCsrfTokenRequestHandler, CsrfToken (+11 more)

### Community 24 - "Alpaca Trading Credential Bootstrap"
Cohesion: 0.14
Nodes (15): ApplicationRunner, AlpacaTradingCredentialBootstrap, ApplicationArguments, Component, Logger, Override, BinanceTradingCredentialBootstrap, Component (+7 more)

### Community 25 - "Watchlist Entry & Ticker Resolution"
Cohesion: 0.19
Nodes (6): Transactional, WatchlistEntry, SpringBootTest, Test, Transactional, WatchlistServiceTest

### Community 26 - "Frontend Auth & Order API"
Cohesion: 0.14
Nodes (18): RFC-6266, readCookie(), EntryOrderType, exportOrdersCsv(), fetchOrders(), filenameFromContentDisposition(), OrderSummary, refreshOrder() (+10 more)

### Community 27 - "Frontend Chart API"
Cohesion: 0.18
Nodes (18): Broker, ChartDataResponse, ChartIndicatorPoint, CandlestickPoint, LinePoint, toBusinessDay(), toCandlestickSeries(), toMaLongSeries() (+10 more)

### Community 28 - "Market Data Service Routing"
Cohesion: 0.16
Nodes (9): MarketDataClient, Service, MarketDataService, Component, MarketHoursService, BeforeEach, ExtendWith, Test (+1 more)

### Community 29 - "Frontend App Shell & Auth Context"
Cohesion: 0.18
Nodes (16): App(), AuthContext, AuthContextValue, AuthProvider(), useAuth(), RequireAuth(), DashboardPage(), LoginPage() (+8 more)

### Community 30 - "Frontend TS Config (App)"
Cohesion: 0.08
Nodes (23): compilerOptions, allowArbitraryExtensions, allowImportingTsExtensions, erasableSyntaxOnly, jsx, lib, module, moduleDetection (+15 more)

### Community 31 - "Notification Entity"
Cohesion: 0.14
Nodes (5): Entity, Override, PrePersist, Table, Notification

### Community 32 - "Watchlist Signal Poller"
Cohesion: 0.22
Nodes (9): Component, Logger, WatchlistSignalPoller, BeforeEach, ExtendWith, Test, WatchlistSignalPollerTest, ConditionalOnProperty (+1 more)

### Community 33 - "Signal Rule Engine Tests"
Cohesion: 0.23
Nodes (3): MacdResult, Test, SignalRuleEngineTest

### Community 34 - "Broker Credential Entity"
Cohesion: 0.10
Nodes (6): BrokerCredential, Entity, Override, PrePersist, PreUpdate, Table

### Community 35 - "Market Closed Exception & Indicator Tests"
Cohesion: 0.19
Nodes (6): MarketClosedException, IndicatorControllerTest, AutoConfigureMockMvc, MockMvc, Test, WebMvcTest

### Community 36 - "Broker Credential Repository"
Cohesion: 0.14
Nodes (10): BrokerCredentialRepository, IndicatorSnapshotRepository, CoreDataModelIntegrationTest, SpringBootTest, Test, Transactional, SpringBootTest, Transactional (+2 more)

### Community 37 - "BrokerAdapter Interface & Result Types"
Cohesion: 0.25
Nodes (4): BrokerAdapter, BrokerOrderResult, BrokerAdapterRouterTest, Test

### Community 38 - "Ticker Entity"
Cohesion: 0.14
Nodes (6): Entity, Override, PrePersist, Table, Ticker, Query

### Community 39 - "Frontend TS Config (Node)"
Cohesion: 0.10
Nodes (19): compilerOptions, allowImportingTsExtensions, erasableSyntaxOnly, lib, module, moduleDetection, noEmit, noFallthroughCasesInSwitch (+11 more)

### Community 40 - "Indicator Service"
Cohesion: 0.17
Nodes (10): BigDecimalIndicators, IndicatorService, Service, GetMapping, RequestMapping, RestController, SignalController, Service (+2 more)

### Community 41 - "Chart Data & Price History"
Cohesion: 0.25
Nodes (6): ChartDataResponse, PriceHistoryResult, IndicatorServiceTest, BeforeEach, ExtendWith, Test

### Community 42 - "Order CSV Exporter"
Cohesion: 0.23
Nodes (3): OrderCsvExporter, Test, OrderCsvExporterTest

### Community 43 - "Docs: E1.1 Local Dev Environment (Oracle/Docker)"
Cohesion: 0.14
Nodes (19): run skill (project override), MarketHoursService (hardcoded NYSE/NASDAQ calendar), oracle-xe Docker Compose service, F1.1 Local dev environment, E1-F1-S1 Docker Compose file for Oracle XE, E1-F1-S2 Spring Boot backend skeleton, E1-F1-S3 React app skeleton with routing, E1-F1-S4 CI pipeline builds/tests both apps (+11 more)

### Community 44 - "Notification Service Tests"
Cohesion: 0.26
Nodes (4): BeforeEach, ExtendWith, Test, NotificationServiceTest

### Community 45 - "Docs: F1.3 Secrets & Config Management"
Cohesion: 0.21
Nodes (17): guardrail-check skill, security-review skill, simplify skill, F1.3 Secrets & config management, E1-F3-S1 Broker API keys encrypted at rest, E1-F3-S2 Dashboard requires login, F4.2 Alpaca adapter (stocks), F6.1 Paper/live mode toggle (+9 more)

### Community 46 - "Frontend API Error Helpers"
Cohesion: 0.26
Nodes (14): apiFetch(), fetchChartData(), fetchPriceHistory(), parseMarketDataError(), fetchNotifications(), fetchUnreadCount(), markAllNotificationsRead(), markNotificationRead() (+6 more)

### Community 47 - "Indicator Test Fixtures"
Cohesion: 0.28
Nodes (3): Candle, IndicatorTestFixtures, Candle

### Community 48 - "Ticker Repository & Service"
Cohesion: 0.17
Nodes (7): TickerRepository, Service, Transactional, TickerService, WatchlistEntryRepository, Service, WatchlistService

### Community 49 - "Watchlist Entry Persistence"
Cohesion: 0.17
Nodes (5): Entity, Override, PrePersist, Table, WatchlistEntry

### Community 50 - "Docs: Agent Roles & Delivery Workflow"
Cohesion: 0.24
Nodes (16): Explore agent (research), general-purpose agent (implementation), Plan agent (design gate), Mandatory per-story workflow (update CLAUDE.md, commit, push), CLAUDE.md project status & architecture log, E1 — Platform Foundation, F1.2 Core data model, F1.4 Testing strategy (+8 more)

### Community 51 - "Volatility Calculator"
Cohesion: 0.26
Nodes (4): MathContext, VolatilityCalculator, Test, VolatilityCalculatorTest

### Community 52 - "Market Data Controller Tests"
Cohesion: 0.23
Nodes (6): TickerNotRegisteredException, AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, MarketDataControllerTest

### Community 53 - "Backtest Harness (Walk-Forward Loop)"
Cohesion: 0.23
Nodes (5): BacktestHarness, HoldGateAccumulator, HoldGateOutcome, LARGE_MOVE, STABLE

### Community 54 - "Backtest Directional Outcome Stats"
Cohesion: 0.19
Nodes (9): DirectionalAccumulator, Checkpoint, MAX, MID, MIN, DirectionalOutcome, LOSS, WASH (+1 more)

### Community 55 - "Docs: Broker Adapter & Guardrail Rationale"
Cohesion: 0.18
Nodes (15): adapter-contract-check skill, BrokerAdapter interface (E4-F1-S1), OrderService.submitOrder (bracket order construction), RetryingBrokerAdapter decorator (retry/backoff/outage), TradingModeService (paper/live switch, append-only), F4.1 Adapter interface, E4-F1-S1 BrokerAdapter interface + mock + contract suite, E4-F1-S2 Rate-limit/retry/backoff in adapter contract (+7 more)

### Community 56 - "Watchlist Controller"
Cohesion: 0.25
Nodes (8): GetMapping, PostMapping, RequestMapping, ResponseEntity, RestController, WatchlistController, WatchlistEntryResponse, DeleteMapping

### Community 57 - "Order Controller Tests"
Cohesion: 0.31
Nodes (6): AutoConfigureMockMvc, MockMvc, ObjectMapper, Test, WebMvcTest, OrderControllerTest

### Community 58 - "Alpaca Trading Config"
Cohesion: 0.27
Nodes (8): AlpacaTradingProperties, ConfigurationProperties, BrokerAdapterConfig, Bean, Configuration, EnableConfigurationProperties, RestClient, SimpleClientHttpRequestFactory

### Community 59 - "Binance Futures Adapter Config"
Cohesion: 0.27
Nodes (8): BinanceFuturesAdapterConfig, Bean, Configuration, EnableConfigurationProperties, RestClient, SimpleClientHttpRequestFactory, BinanceFuturesTradingProperties, ConfigurationProperties

### Community 60 - "MACD Calculator"
Cohesion: 0.24
Nodes (5): MacdResult, MathContext, MacdCalculator, Test, MacdCalculatorTest

### Community 61 - "Moving Average Crossover Calculator"
Cohesion: 0.24
Nodes (5): MathContext, MovingAverageResult, MovingAverageCrossoverCalculator, Test, MovingAverageCrossoverCalculatorTest

### Community 62 - "API Error Response Handling"
Cohesion: 0.37
Nodes (6): ApiErrorResponse, JsonInclude, ExceptionHandler, ResponseEntity, RestControllerAdvice, OrderExceptionHandler

### Community 63 - "Notification Response DTOs"
Cohesion: 0.17
Nodes (11): JsonInclude, NotificationResponse, NotificationType, ORDER_CANCELLED, ORDER_FAILED, ORDER_FILLED, ORDER_PARTIALLY_FILLED, ORDER_PARTIALLY_PROTECTED (+3 more)

### Community 64 - "Signal Controller Tests"
Cohesion: 0.31
Nodes (5): AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, SignalControllerTest

### Community 65 - "Watchlist Controller Tests"
Cohesion: 0.27
Nodes (7): AddWatchlistEntryRequest, AutoConfigureMockMvc, MockMvc, ObjectMapper, Test, WebMvcTest, WatchlistControllerTest

### Community 66 - "Backtest Report"
Cohesion: 0.21
Nodes (4): BacktestReport, CheckpointStats, DirectionalOutcomeStats, HoldGateStats

### Community 68 - "Frontend Market Data Error Types"
Cohesion: 0.26
Nodes (9): MarketDataError, MarketDataErrorCode, PriceHistoryResponse, fetchWatchlist(), removeFromWatchlist(), WatchlistEntry, describeError(), Watchlist() (+1 more)

### Community 69 - "Broker Order Result & Status Enum"
Cohesion: 0.17
Nodes (10): OrderStatus, CANCELLED, FAILED, FILLED, PARTIALLY_FILLED, PARTIALLY_PROTECTED, PENDING, REJECTED (+2 more)

### Community 70 - "Chart Data DTOs & Indicator Controller"
Cohesion: 0.23
Nodes (6): ChartDataResponse, ChartIndicatorPoint, IndicatorController, GetMapping, RequestMapping, RestController

### Community 71 - "RSI Calculator"
Cohesion: 0.29
Nodes (4): MathContext, RsiCalculator, Test, RsiCalculatorTest

### Community 72 - "Volume Trend Calculator"
Cohesion: 0.30
Nodes (3): VolumeTrendCalculator, Test, VolumeTrendCalculatorTest

### Community 73 - "Notification Controller"
Cohesion: 0.29
Nodes (6): GetMapping, PostMapping, RequestMapping, ResponseEntity, RestController, NotificationController

### Community 74 - "Order Query Controller"
Cohesion: 0.26
Nodes (6): GetMapping, PostMapping, RequestMapping, ResponseEntity, RestController, OrderQueryController

### Community 75 - "Ticker Controller Tests"
Cohesion: 0.32
Nodes (7): RegisterTickerRequest, AutoConfigureMockMvc, MockMvc, ObjectMapper, Test, WebMvcTest, TickerControllerTest

### Community 76 - "Notification Controller Tests"
Cohesion: 0.26
Nodes (5): AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, NotificationControllerTest

### Community 77 - "Security Config Tests"
Cohesion: 0.32
Nodes (6): AutoConfigureMockMvc, MockMvc, SpringBootTest, Test, SecurityConfigTest, Cookie

### Community 78 - "Frontend Ticker Metrics Component"
Cohesion: 0.21
Nodes (8): AddToWatchlistButton(), ERROR_MESSAGES, formatOrDash(), relationLabel(), StatTileProps, TickerMetricsProps, TickerMetricsResult(), addToWatchlist()

### Community 80 - "Ticker Controller"
Cohesion: 0.29
Nodes (6): PostMapping, RequestMapping, ResponseEntity, RestController, TickerController, TickerResponse

### Community 81 - "Maven Wrapper Script (mvnw)"
Cohesion: 0.33
Nodes (6): mvnw script, clean(), die(), exec_maven(), set_java_home(), verbose()

### Community 82 - "Binance Credential Bootstrap Tests"
Cohesion: 0.33
Nodes (5): ApplicationArguments, Override, BinanceTradingCredentialBootstrapTest, ExtendWith, Test

### Community 83 - "Market Data Controller"
Cohesion: 0.27
Nodes (5): GetMapping, RequestMapping, RestController, MarketDataController, PriceHistoryResponse

### Community 84 - "Auth Controller"
Cohesion: 0.39
Nodes (6): Authentication, AuthController, GetMapping, RequestMapping, ResponseEntity, RestController

### Community 85 - "Notification Exception Handler"
Cohesion: 0.31
Nodes (5): InvalidNotificationRequestException, ExceptionHandler, ResponseEntity, RestControllerAdvice, NotificationExceptionHandler

### Community 86 - "Ticker Service Tests"
Cohesion: 0.36
Nodes (4): SpringBootTest, Test, Transactional, TickerServiceTest

### Community 87 - "Live Mode Guard Exception Handler"
Cohesion: 0.31
Nodes (5): LiveModeNotYetAvailableException, ExceptionHandler, ResponseEntity, RestControllerAdvice, TradingModeExceptionHandler

### Community 88 - "Frontend Lint Config (oxlint)"
Cohesion: 0.22
Nodes (8): plugins, rules, react/only-export-components, react/rules-of-hooks, $schema, oxc, typescript, warn

### Community 89 - "Broker Credential Find Tests"
Cohesion: 0.39
Nodes (4): BrokerCredentialServiceFindTest, SpringBootTest, Test, Transactional

### Community 90 - "Order Controller"
Cohesion: 0.39
Nodes (5): PostMapping, RequestMapping, ResponseEntity, RestController, OrderController

### Community 91 - "Docs: Buy/Sell/Hold Signal & Hold-Term"
Cohesion: 0.43
Nodes (8): signal-rule-review skill, BacktestHarness (walk-forward JUnit validation), HoldTermCalculator (trend strength x volatility band), SignalRuleEngine (Buy/Sell/Hold rule table), F2.3 Buy/Sell/Hold signal & hold-term, E2-F3-S2 Suggested hold-term alongside the call, F2.4 Backtesting, E2-F4-S1 Backtest rule table against historical data

### Community 92 - "Docs: Ticker Lookup & Signal Badge"
Cohesion: 0.38
Nodes (7): dataviz skill, SignalBadge colorblind-safe teal/orange/slate palette, F3.1 Ticker lookup & metrics display, E3-F1-S1 Ticker lookup + stat-tile metrics, E3-F1-S2 Buy/Sell/Hold badge color-coded, F3.2 Metric visualization, E3-F2-S1 Price chart with MA/RSI overlays

### Community 94 - "Clock Config Bean"
Cohesion: 0.60
Nodes (3): ClockConfig, Bean, Configuration

### Community 96 - "Backend Application Context Test"
Cohesion: 0.60
Nodes (3): BackendApplicationTests, SpringBootTest, Test

### Community 99 - "Docs: Watchlist & Notifications"
Cohesion: 0.40
Nodes (5): Notification system + WatchlistSignalPoller, Watchlist feature (watchlist_entries), F3.3 Watchlist (stretch), E3-F3-S1 Watchlist persisted in Oracle DB, F5.4 Notifications

### Community 101 - "Scheduling Config"
Cohesion: 0.83
Nodes (3): Configuration, SchedulingConfig, EnableScheduling

### Community 102 - "Docs: Order History & CSV Export"
Cohesion: 0.50
Nodes (4): OrderCsvExporter (RFC 4180 trade-history export), F5.3 Order status & history, E5-F3-S1 Order status/history page, E5-F3-S2 Export trade history to CSV

### Community 103 - "Docs: E7 Observability & Hardening (Planned)"
Cohesion: 0.50
Nodes (4): E7 — Observability & Hardening, E7-F1 Structured logging, E7-F2 Security review gate, E7-F3 Backup/restore

## Knowledge Gaps
- **171 isolated node(s):** `com.autotrade.dashboard:backend`, `ALPACA`, `BINANCE`, `PAPER`, `LIVE` (+166 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **18 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `TradingMode` connect `Broker Enum & Credential Core` to `Broker Adapter Retry & Mock Test Infra`, `Binance Futures Trading Adapter`, `Alpaca Trading Adapter`, `Order Entity & Persistence`, `Trading Mode Switch (Paper/Live)`, `Alpaca Adapter Contract Tests`, `Broker & Credential Core Types`, `Broker Credential Encryption Service`, `Indicator Response DTOs`, `Order Submission Flow`, `Broker Adapter Exceptions`, `Order Repository`, `Alpaca Trading Credential Bootstrap`, `Broker Credential Entity`, `Broker Credential Repository`, `BrokerAdapter Interface & Result Types`, `Alpaca Trading Config`, `Binance Futures Adapter Config`, `Order Query Controller`, `Binance Credential Bootstrap Tests`, `Broker Credential Find Tests`?**
  _High betweenness centrality (0.127) - this node is a cross-community bridge._
- **Why does `Broker` connect `Broker Enum & Credential Core` to `Broker Adapter Retry & Mock Test Infra`, `Binance Futures Trading Adapter`, `Alpaca Trading Adapter`, `Order Entity & Persistence`, `Alpaca Market Data Client`, `Broker & Credential Core Types`, `Indicator Snapshot Computation`, `Broker Credential Encryption Service`, `Indicator Response DTOs`, `Order Submission Flow`, `Binance Market Data Client`, `Broker Adapter Exceptions`, `Market Data Service Routing`, `Broker Credential Entity`, `Market Closed Exception & Indicator Tests`, `Broker Credential Repository`, `Indicator Service`, `Chart Data & Price History`, `Market Data Controller Tests`, `Order Controller Tests`, `Signal Controller Tests`, `Chart Data DTOs & Indicator Controller`, `Market Data Controller`, `Market Data Rate-Limited Exception`, `Market Data Unavailable Exception`?**
  _High betweenness centrality (0.118) - this node is a cross-community bridge._
- **Why does `Ticker` connect `Ticker Entity` to `Order Entity & Persistence`, `Trading Mode Switch (Paper/Live)`, `Broker & Credential Core Types`, `Indicator Snapshot Computation`, `Broker Enum & Credential Core`, `Indicator Response DTOs`, `Signal Call Entry & Notification Recording`, `Order Submission Flow`, `Hold Term & Signal Call Enum`, `Broker Adapter Exceptions`, `Watchlist Entry & Ticker Resolution`, `Market Data Service Routing`, `Notification Entity`, `Watchlist Signal Poller`, `Market Closed Exception & Indicator Tests`, `Broker Credential Repository`, `BrokerAdapter Interface & Result Types`, `Indicator Service`, `Chart Data & Price History`, `Order CSV Exporter`, `Notification Service Tests`, `Ticker Repository & Service`, `Watchlist Entry Persistence`, `Market Data Controller Tests`, `Signal Controller Tests`, `Watchlist Controller Tests`, `Ticker Controller`, `Ticker Service Tests`?**
  _High betweenness centrality (0.099) - this node is a cross-community bridge._
- **What connects `com.autotrade.dashboard:backend`, `ALPACA`, `BINANCE` to the rest of the system?**
  _171 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Broker Adapter Retry & Mock Test Infra` be split into smaller, more focused modules?**
  _Cohesion score 0.05934065934065934 - nodes in this community are weakly interconnected._
- **Should `Binance Futures Trading Adapter` be split into smaller, more focused modules?**
  _Cohesion score 0.06639427987742594 - nodes in this community are weakly interconnected._
- **Should `Alpaca Trading Adapter` be split into smaller, more focused modules?**
  _Cohesion score 0.08125 - nodes in this community are weakly interconnected._