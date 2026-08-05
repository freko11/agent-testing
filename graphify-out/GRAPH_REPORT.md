# Graph Report - agent testing  (2026-08-05)

## Corpus Check
- 294 files · ~111,434 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 2453 nodes · 6634 edges · 132 communities (111 shown, 21 thin omitted)
- Extraction: 86% EXTRACTED · 14% INFERRED · 0% AMBIGUOUS · INFERRED: 956 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `b8023548`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- BrokerOrderRequest
- BinanceFuturesTradingAdapter
- AlpacaTradingAdapter
- Order
- TradingModeControllerTest
- BrokerAdapterContractTest
- AlpacaMarketDataClient
- .calculate
- BrokerCredentialService
- AssetType
- IndicatorSnapshot
- devDependencies
- SignalCallEntry
- Broker
- WatchlistSignalPollerTest.java
- TradingModeServiceTest
- MarketDataExceptionHandler
- RiskLimitService
- .calculate
- BinanceMarketDataClient
- TradeForm.tsx
- TickerSignalOrderE2ETest.java
- TradingModeEvent
- SecurityConfig.java
- TradingMode.java
- .resolveOrRegister
- order/api.ts
- PriceChart.tsx
- FakeBinanceFuturesTradingServer
- DashboardPage.tsx
- compilerOptions
- Notification
- WatchlistSignalPollerTest
- .evaluate
- RiskConsentEvent
- IndicatorControllerTest.java
- CoreDataModelIntegrationTest.java
- .submitOrder
- KillSwitchService
- compilerOptions
- IndicatorService
- .export
- OrderService
- run skill (project override)
- NotificationServiceTest
- general-purpose agent (implementation)
- apiFetch
- TickerNotRegisteredException
- WatchlistEntry
- Ticker
- CLAUDE.md project status & architecture log
- .calculate
- .getPriceHistory
- .run
- Checkpoint
- adapter-contract-check skill
- WatchlistController
- TradingModeResponse
- BrokerAdapterConfig.java
- BinanceFuturesAdapterConfig.java
- .calculate
- RiskExceptionHandler.java
- ApiErrorResponse
- NotificationType
- Test
- WatchlistControllerTest
- RiskLimitConfig.java
- SignalRuleId
- TradingMode
- OrderAuditEntry
- WatchlistService
- .calculate
- watchlist/api.ts
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
- .tickerToSignalToOrder_happyPath
- AuthController.java
- NotificationExceptionHandler.java
- IndicatorController
- TradingModeExceptionHandler.java
- plugins
- BrokerAdapter
- CredentialEncryptionService
- signal-rule-review skill
- dataviz skill
- BacktestReport
- ClockConfig.java
- TradingModeService
- BackendApplicationTests.java
- OrderQueryControllerTest
- .calculate
- .handleUnavailable
- BackendApplication
- SchedulingConfig.java
- F5.3 Order status & history
- E7 — Observability & Hardening
- PaperTradeThresholdNotMetException
- MarketHoursServiceTest
- Candle
- .handleRateLimited
- BacktestConfig
- tsconfig.json
- Bluesky Icon (SVG symbol)
- Discord Icon (SVG symbol)
- Documentation Icon (SVG symbol)
- App Favicon (Purple Lightning-Bolt Glyph)
- com.autotrade.dashboard:backend
- killswitch/api.ts
- BrokerAdapterRouterTest
- MarketDataController
- OrderQueryController
- InvalidTradeRequestException
- .runAndVerify
- MarketDataService
- OrderNotFoundException
- BrokerCredential
- OrderRefreshUnavailableException
- SignalNotActionableException
- TickerService
- Backing up and restoring the Oracle instance (E7-F3-S1)
- db-backup.sh
- db-restore.sh

## God Nodes (most connected - your core abstractions)
1. `TradingMode` - 123 edges
2. `Broker` - 104 edges
3. `Ticker` - 97 edges
4. `Order` - 92 edges
5. `AssetType` - 83 edges
6. `IndicatorSnapshot` - 59 edges
7. `Candle` - 59 edges
8. `BinanceFuturesTradingAdapter` - 58 edges
9. `BrokerCredential` - 56 edges
10. `OrderServiceTest` - 54 edges

## Surprising Connections (you probably didn't know these)
- `Broker-credential encryption key rotation procedure` --references--> `BrokerCredentialService`  [EXTRACTED]
  docs/runbooks/credential-key-rotation.md → backend/src/main/java/com/autotrade/dashboard/broker/BrokerCredentialService.java
- `Broker-credential encryption key rotation procedure` --references--> `CredentialEncryptionService`  [EXTRACTED]
  docs/runbooks/credential-key-rotation.md → backend/src/main/java/com/autotrade/dashboard/broker/CredentialEncryptionService.java
- `general-purpose agent (implementation)` --references--> `E1-F4-S2 Deterministic indicator fixture data`  [EXTRACTED]
  .claude/agents/general-purpose.md → docs/agile-plan.md
- `run skill (project override)` --references--> `E5-F3-S1 Order status/history page`  [EXTRACTED]
  .claude/skills/run/SKILL.md → docs/agile-plan.md
- `security-review skill` --references--> `F4.2 Alpaca adapter (stocks)`  [EXTRACTED]
  .claude/skills/security-review/SKILL.md → docs/agile-plan.md

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **E6 risk-guardrail verification flow (caps, kill switch, exposure cap, audit, gating)** — docs_agile_plan_e6_f1_s1, docs_agile_plan_e6_f2_s1, docs_agile_plan_e6_f2_s2, docs_agile_plan_e6_f2_s3, docs_agile_plan_e6_f3_s1, docs_agile_plan_e6_f3_s2, claude_skills_guardrail_check_skill_guardrail_check, claude_skills_security_review_skill_security_review [INFERRED 0.85]
- **Solo-build role mapping: Plan/Explore/general-purpose agents plus run/simplify skills** — claude_agents_plan_plan, claude_agents_explore_explore, claude_agents_general_purpose_general_purpose, claude_skills_run_skill_run, claude_skills_simplify_skill_simplify [EXTRACTED 1.00]
- **BrokerAdapter contract group: interface, retry decorator, verification checklist, and its origin stories** — concept_broker_adapter_interface, concept_retrying_broker_adapter, claude_skills_adapter_contract_check_skill_adapter_contract_check, docs_agile_plan_e4_f1_s1, docs_agile_plan_e4_f1_s2, docs_agile_plan_e4_f1_s3 [INFERRED 0.85]

## Communities (132 total, 21 thin omitted)

### Community 0 - "BrokerOrderRequest"
Cohesion: 0.05
Nodes (24): AssetBalance, BrokerAdapterRetryPolicy, BrokerOrderRequest, Logger, Override, RetryingBrokerAdapter, Override, MockBrokerAdapter (+16 more)

### Community 1 - "BinanceFuturesTradingAdapter"
Cohesion: 0.06
Nodes (23): Credentials, BinanceAccountAsset, BinanceAccountResponse, BinanceAlgoOrderResponse, BinanceErrorResponse, BinanceFuturesTradingAdapter, BinanceOrderResponse, BinancePositionResponse (+15 more)

### Community 2 - "AlpacaTradingAdapter"
Cohesion: 0.08
Nodes (23): AlpacaAccountResponse, AlpacaBracketLeg, AlpacaErrorResponse, AlpacaOrderRequestBody, AlpacaOrderResponse, AlpacaPositionResponse, AlpacaStopLeg, AlpacaTradingAdapter (+15 more)

### Community 3 - "Order"
Cohesion: 0.07
Nodes (6): Entity, Override, PrePersist, PreUpdate, Table, Order

### Community 4 - "TradingModeControllerTest"
Cohesion: 0.28
Nodes (5): AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, TradingModeControllerTest

### Community 5 - "BrokerAdapterContractTest"
Cohesion: 0.18
Nodes (7): BinanceFuturesTradingAdapterContractTest, ExtendWith, Override, BrokerAdapterContractTest, Test, Override, MockBrokerAdapterContractTest

### Community 6 - "AlpacaMarketDataClient"
Cohesion: 0.09
Nodes (23): AlpacaBar, AlpacaBarsResponse, AlpacaMarketDataClient, Candle, Component, JsonIgnoreProperties, Override, RestClient (+15 more)

### Community 7 - ".calculate"
Cohesion: 0.08
Nodes (18): HoldTermCalculator, HoldTermRule, MODERATE_HIGH, MODERATE_LOW, MODERATE_MEDIUM, STRONG_HIGH, STRONG_LOW, STRONG_MEDIUM (+10 more)

### Community 8 - "BrokerCredentialService"
Cohesion: 0.16
Nodes (9): BrokerCredentialService, Logger, Service, Transactional, BrokerCredentialServiceFindTest, SpringBootTest, Test, Transactional (+1 more)

### Community 9 - "AssetType"
Cohesion: 0.14
Nodes (20): EntryOrderType, LIMIT, MARKET, OrderSide, BUY, SELL, OrderStatus, CANCELLED (+12 more)

### Community 10 - "IndicatorSnapshot"
Cohesion: 0.08
Nodes (5): IndicatorSnapshot, Entity, Override, PrePersist, Table

### Community 11 - "devDependencies"
Cohesion: 0.05
Nodes (36): dependencies, lightweight-charts, react, react-dom, react-router-dom, devDependencies, oxlint, @types/node (+28 more)

### Community 12 - "SignalCallEntry"
Cohesion: 0.14
Nodes (6): IndicatorComputation, Entity, Override, PrePersist, Table, SignalCallEntry

### Community 13 - "Broker"
Cohesion: 0.12
Nodes (11): Broker, ALPACA, BINANCE, BrokerAdapterAmbiguousOrderException, BrokerAdapterException, BrokerAdapterRateLimitedException, BrokerAdapterTransientException, BrokerAdapterUnavailableException (+3 more)

### Community 14 - "WatchlistSignalPollerTest.java"
Cohesion: 0.16
Nodes (15): IndicatorResponse, MacdResult, MovingAverageRelation, EQUAL, SHORT_ABOVE_LONG, SHORT_BELOW_LONG, MovingAverageResult, TickerSummary (+7 more)

### Community 15 - "TradingModeServiceTest"
Cohesion: 0.23
Nodes (5): SpringBootTest, Test, Transactional, TradingModeServiceTest, TestPropertySource

### Community 16 - "MarketDataExceptionHandler"
Cohesion: 0.13
Nodes (11): InsufficientPriceHistoryException, InvalidIndicatorRequestException, InvalidPriceHistoryRequestException, ExceptionHandler, Logger, ResponseEntity, RestControllerAdvice, MarketDataExceptionHandler (+3 more)

### Community 17 - "RiskLimitService"
Cohesion: 0.20
Nodes (7): Logger, Service, RiskLimitService, ConfigurationProperties, RiskLimitsProperties, Test, RiskLimitServiceTest

### Community 18 - ".calculate"
Cohesion: 0.29
Nodes (4): MathContext, RsiCalculator, Test, RsiCalculatorTest

### Community 19 - "BinanceMarketDataClient"
Cohesion: 0.11
Nodes (12): BinanceMarketDataClient, Candle, Component, Override, RestClient, TooManyRequests, Logger, RetryHelper (+4 more)

### Community 20 - "TradeForm.tsx"
Cohesion: 0.17
Nodes (14): placeOrder(), PlaceOrderPayload, TradeOrderResponse, DEFAULT_VALUES, describeResult(), ResultTone, SUBMIT_ERROR_MESSAGES, SubmitState (+6 more)

### Community 21 - "TickerSignalOrderE2ETest.java"
Cohesion: 0.15
Nodes (9): BrokerPosition, SignalCall, BUY, HOLD, SELL, call(), SpringBootTest, Transactional (+1 more)

### Community 22 - "TradingModeEvent"
Cohesion: 0.20
Nodes (5): Entity, Override, PrePersist, Table, TradingModeEvent

### Community 23 - "SecurityConfig.java"
Cohesion: 0.11
Nodes (22): CsrfCookieWriteFilter, Bean, Configuration, Logger, Override, PasswordEncoder, SecurityConfig, SpaCsrfTokenRequestHandler (+14 more)

### Community 24 - "TradingMode.java"
Cohesion: 0.11
Nodes (17): ApplicationRunner, AlpacaTradingCredentialBootstrap, ApplicationArguments, Component, Logger, Override, BinanceTradingCredentialBootstrap, ApplicationArguments (+9 more)

### Community 25 - ".resolveOrRegister"
Cohesion: 0.25
Nodes (6): Transactional, WatchlistEntry, SpringBootTest, Test, Transactional, WatchlistServiceTest

### Community 26 - "order/api.ts"
Cohesion: 0.13
Nodes (19): RFC-6266, readCookie(), MarketDataError, EntryOrderType, exportOrdersCsv(), fetchOrders(), filenameFromContentDisposition(), OrderSummary (+11 more)

### Community 27 - "PriceChart.tsx"
Cohesion: 0.18
Nodes (18): Broker, ChartDataResponse, ChartIndicatorPoint, CandlestickPoint, LinePoint, toBusinessDay(), toCandlestickSeries(), toMaLongSeries() (+10 more)

### Community 28 - "FakeBinanceFuturesTradingServer"
Cohesion: 0.08
Nodes (26): DecryptedCredential, Override, AlpacaTradingAdapterContractTest, BeforeEach, ExtendWith, Override, BeforeEach, FakeAlpacaTradingServer (+18 more)

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
Cohesion: 0.23
Nodes (9): Component, Logger, WatchlistSignalPoller, BeforeEach, ExtendWith, Test, WatchlistSignalPollerTest, ConditionalOnProperty (+1 more)

### Community 33 - ".evaluate"
Cohesion: 0.19
Nodes (4): SignalRuleEngine, MacdResult, Test, SignalRuleEngineTest

### Community 34 - "RiskConsentEvent"
Cohesion: 0.24
Nodes (5): Entity, Override, PrePersist, Table, RiskConsentEvent

### Community 35 - "IndicatorControllerTest.java"
Cohesion: 0.17
Nodes (7): ChartDataResponse, ChartIndicatorPoint, IndicatorControllerTest, AutoConfigureMockMvc, MockMvc, Test, WebMvcTest

### Community 36 - "CoreDataModelIntegrationTest.java"
Cohesion: 0.16
Nodes (10): Autowired, BrokerCredentialRepository, BrokerCredentialServiceRotationTest, SpringBootTest, Transactional, CoreDataModelIntegrationTest, SpringBootTest, Test (+2 more)

### Community 37 - ".submitOrder"
Cohesion: 0.29
Nodes (5): BrokerOrderResult, PlaceOrderRequest, SignalComputation, ExtendWith, OrderServiceTest

### Community 38 - "KillSwitchService"
Cohesion: 0.06
Nodes (31): EngageKillSwitchResponse, GetMapping, PostMapping, RequestMapping, RestController, KillSwitchController, Entity, Override (+23 more)

### Community 39 - "compilerOptions"
Cohesion: 0.10
Nodes (19): compilerOptions, allowImportingTsExtensions, erasableSyntaxOnly, lib, module, moduleDetection, noEmit, noFallthroughCasesInSwitch (+11 more)

### Community 40 - "IndicatorService"
Cohesion: 0.11
Nodes (14): BigDecimalIndicators, IndicatorService, IndicatorSnapshotRepository, SignalCallEntryRepository, GetMapping, RequestMapping, RestController, SignalController (+6 more)

### Community 41 - ".export"
Cohesion: 0.23
Nodes (3): OrderCsvExporter, Test, OrderCsvExporterTest

### Community 42 - "OrderService"
Cohesion: 0.17
Nodes (9): PostMapping, RequestMapping, ResponseEntity, RestController, OrderController, Logger, Service, OrderService (+1 more)

### Community 43 - "run skill (project override)"
Cohesion: 0.14
Nodes (19): run skill (project override), MarketHoursService (hardcoded NYSE/NASDAQ calendar), oracle-xe Docker Compose service, F1.1 Local dev environment, E1-F1-S1 Docker Compose file for Oracle XE, E1-F1-S2 Spring Boot backend skeleton, E1-F1-S3 React app skeleton with routing, E1-F1-S4 CI pipeline builds/tests both apps (+11 more)

### Community 44 - "NotificationServiceTest"
Cohesion: 0.29
Nodes (3): ExtendWith, Test, NotificationServiceTest

### Community 45 - "general-purpose agent (implementation)"
Cohesion: 0.20
Nodes (20): general-purpose agent (implementation), guardrail-check skill, security-review skill, simplify skill, F1.3 Secrets & config management, E1-F3-S1 Broker API keys encrypted at rest, E1-F3-S2 Dashboard requires login, E5 — Auto-Trade Execution (+12 more)

### Community 46 - "apiFetch"
Cohesion: 0.19
Nodes (21): apiFetch(), AssetType, fetchPriceHistory(), MarketDataErrorCode, parseMarketDataError(), PriceHistoryResponse, fetchNotifications(), fetchUnreadCount() (+13 more)

### Community 47 - "TickerNotRegisteredException"
Cohesion: 0.23
Nodes (6): TickerNotRegisteredException, AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, MarketDataControllerTest

### Community 48 - "WatchlistEntry"
Cohesion: 0.17
Nodes (5): Entity, Override, PrePersist, Table, WatchlistEntry

### Community 49 - "Ticker"
Cohesion: 0.15
Nodes (5): Entity, Override, PrePersist, Table, Ticker

### Community 50 - "CLAUDE.md project status & architecture log"
Cohesion: 0.25
Nodes (11): Explore agent (research), Plan agent (design gate), Mandatory per-story workflow (update CLAUDE.md, commit, push), CLAUDE.md project status & architecture log, E1 — Platform Foundation, F1.2 Core data model, F1.4 Testing strategy, E1-F4-S2 Deterministic indicator fixture data (+3 more)

### Community 51 - ".calculate"
Cohesion: 0.26
Nodes (4): MathContext, VolatilityCalculator, Test, VolatilityCalculatorTest

### Community 52 - ".getPriceHistory"
Cohesion: 0.19
Nodes (8): ChartDataResponse, Service, MarketClosedException, PriceHistoryResult, IndicatorServiceTest, BeforeEach, ExtendWith, Test

### Community 53 - ".run"
Cohesion: 0.23
Nodes (5): BacktestHarness, HoldGateAccumulator, HoldGateOutcome, LARGE_MOVE, STABLE

### Community 54 - "Checkpoint"
Cohesion: 0.19
Nodes (9): DirectionalAccumulator, Checkpoint, MAX, MID, MIN, DirectionalOutcome, LOSS, WASH (+1 more)

### Community 55 - "adapter-contract-check skill"
Cohesion: 0.16
Nodes (17): adapter-contract-check skill, BrokerAdapter interface (E4-F1-S1), OrderService.submitOrder (bracket order construction), RetryingBrokerAdapter decorator (retry/backoff/outage), TradingModeService (paper/live switch, append-only), E4 — Broker Adapter Layer, F4.1 Adapter interface, E4-F1-S1 BrokerAdapter interface + mock + contract suite (+9 more)

### Community 56 - "WatchlistController"
Cohesion: 0.25
Nodes (8): GetMapping, PostMapping, RequestMapping, ResponseEntity, RestController, WatchlistController, WatchlistEntryResponse, DeleteMapping

### Community 57 - "TradingModeResponse"
Cohesion: 0.21
Nodes (7): TradingModeChangeRequest, GetMapping, PostMapping, RequestMapping, RestController, TradingModeController, TradingModeResponse

### Community 58 - "BrokerAdapterConfig.java"
Cohesion: 0.27
Nodes (8): AlpacaTradingProperties, ConfigurationProperties, BrokerAdapterConfig, Bean, Configuration, EnableConfigurationProperties, RestClient, SimpleClientHttpRequestFactory

### Community 59 - "BinanceFuturesAdapterConfig.java"
Cohesion: 0.27
Nodes (8): BinanceFuturesAdapterConfig, Bean, Configuration, EnableConfigurationProperties, RestClient, SimpleClientHttpRequestFactory, BinanceFuturesTradingProperties, ConfigurationProperties

### Community 60 - ".calculate"
Cohesion: 0.24
Nodes (5): MacdResult, MathContext, MacdCalculator, Test, MacdCalculatorTest

### Community 61 - "RiskExceptionHandler.java"
Cohesion: 0.46
Nodes (5): ExceptionHandler, Logger, ResponseEntity, RestControllerAdvice, RiskExceptionHandler

### Community 62 - "ApiErrorResponse"
Cohesion: 0.34
Nodes (7): ApiErrorResponse, JsonInclude, ExceptionHandler, Logger, ResponseEntity, RestControllerAdvice, OrderExceptionHandler

### Community 63 - "NotificationType"
Cohesion: 0.17
Nodes (11): JsonInclude, NotificationResponse, NotificationType, ORDER_CANCELLED, ORDER_FAILED, ORDER_FILLED, ORDER_PARTIALLY_FILLED, ORDER_PARTIALLY_PROTECTED (+3 more)

### Community 65 - "WatchlistControllerTest"
Cohesion: 0.25
Nodes (7): AddWatchlistEntryRequest, AutoConfigureMockMvc, MockMvc, ObjectMapper, Test, WebMvcTest, WatchlistControllerTest

### Community 66 - "RiskLimitConfig.java"
Cohesion: 0.83
Nodes (3): Configuration, EnableConfigurationProperties, RiskLimitConfig

### Community 67 - "SignalRuleId"
Cohesion: 0.14
Nodes (12): HoldTerm, SignalRuleId, BEARISH_MAJORITY, BEARISH_UNANIMOUS, BULLISH_MAJORITY, BULLISH_UNANIMOUS, CONFLICTING_SIGNALS, NO_STRONG_SIGNAL (+4 more)

### Community 68 - "TradingMode"
Cohesion: 0.20
Nodes (7): BrokerAccountStatus, TradingMode, LIVE, PAPER, BrokerCredentialNotConfiguredException, JsonInclude, OrderResponse

### Community 69 - "OrderAuditEntry"
Cohesion: 0.15
Nodes (5): Entity, Override, PrePersist, Table, OrderAuditEntry

### Community 70 - "WatchlistService"
Cohesion: 0.25
Nodes (4): Query, WatchlistEntryRepository, Service, WatchlistService

### Community 71 - ".calculate"
Cohesion: 0.30
Nodes (3): VolumeTrendCalculator, Test, VolumeTrendCalculatorTest

### Community 72 - "watchlist/api.ts"
Cohesion: 0.46
Nodes (6): fetchWatchlist(), removeFromWatchlist(), WatchlistEntry, describeError(), Watchlist(), WatchlistProps

### Community 73 - "NotificationController"
Cohesion: 0.29
Nodes (6): GetMapping, PostMapping, RequestMapping, ResponseEntity, RestController, NotificationController

### Community 74 - "OrderControllerTest"
Cohesion: 0.18
Nodes (8): KillSwitchEngagedException, RiskLimitExceededException, AutoConfigureMockMvc, MockMvc, ObjectMapper, Test, WebMvcTest, OrderControllerTest

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
Cohesion: 0.11
Nodes (22): fetchChartData(), registerTicker(), TickerSummary, Broker, fetchSignal(), HoldTerm, IndicatorResponse, MacdResult (+14 more)

### Community 79 - "NotificationService"
Cohesion: 0.18
Nodes (6): Pageable, NotificationRepository, Logger, Service, NotificationService, BeforeEach

### Community 80 - "TickerServiceTest"
Cohesion: 0.39
Nodes (4): SpringBootTest, Test, Transactional, TickerServiceTest

### Community 81 - "mvnw"
Cohesion: 0.33
Nodes (6): mvnw script, clean(), die(), exec_maven(), set_java_home(), verbose()

### Community 82 - "Changelog"
Cohesion: 0.04
Nodes (53): Changelog, E1-F1-S1 — local Oracle XE via Docker Compose, E1-F1-S2 — Spring Boot backend skeleton, E1-F1-S3 — React app skeleton, E1-F1-S4 — CI pipeline, E1-F1-S5 — env/config profiles, E1-F2 — core data model, E1-F3-S1 — broker-credential key rotation (+45 more)

### Community 84 - "AuthController.java"
Cohesion: 0.39
Nodes (6): Authentication, AuthController, GetMapping, RequestMapping, ResponseEntity, RestController

### Community 85 - "NotificationExceptionHandler.java"
Cohesion: 0.29
Nodes (6): InvalidNotificationRequestException, ExceptionHandler, Logger, ResponseEntity, RestControllerAdvice, NotificationExceptionHandler

### Community 86 - "IndicatorController"
Cohesion: 0.39
Nodes (4): IndicatorController, GetMapping, RequestMapping, RestController

### Community 87 - "TradingModeExceptionHandler.java"
Cohesion: 0.29
Nodes (6): RiskConsentNotGivenException, ExceptionHandler, Logger, ResponseEntity, RestControllerAdvice, TradingModeExceptionHandler

### Community 88 - "plugins"
Cohesion: 0.22
Nodes (8): plugins, rules, react/only-export-components, react/rules-of-hooks, $schema, oxc, typescript, warn

### Community 89 - "BrokerAdapter"
Cohesion: 0.47
Nodes (3): BrokerAdapter, BrokerAdapterRouter, Service

### Community 90 - "CredentialEncryptionService"
Cohesion: 0.19
Nodes (6): CredentialEncryptionService, Component, Logger, CredentialEncryptionServiceTest, Test, SecretKeySpec

### Community 91 - "signal-rule-review skill"
Cohesion: 0.43
Nodes (8): signal-rule-review skill, BacktestHarness (walk-forward JUnit validation), HoldTermCalculator (trend strength x volatility band), SignalRuleEngine (Buy/Sell/Hold rule table), F2.3 Buy/Sell/Hold signal & hold-term, E2-F3-S2 Suggested hold-term alongside the call, F2.4 Backtesting, E2-F4-S1 Backtest rule table against historical data

### Community 92 - "dataviz skill"
Cohesion: 0.19
Nodes (13): dataviz skill, Notification system + WatchlistSignalPoller, SignalBadge colorblind-safe teal/orange/slate palette, Watchlist feature (watchlist_entries), E3 — Dashboard (Frontend), F3.1 Ticker lookup & metrics display, E3-F1-S1 Ticker lookup + stat-tile metrics, E3-F1-S2 Buy/Sell/Hold badge color-coded (+5 more)

### Community 93 - "BacktestReport"
Cohesion: 0.21
Nodes (4): BacktestReport, CheckpointStats, DirectionalOutcomeStats, HoldGateStats

### Community 94 - "ClockConfig.java"
Cohesion: 0.60
Nodes (3): ClockConfig, Bean, Configuration

### Community 95 - "TradingModeService"
Cohesion: 0.14
Nodes (9): OrderAuditEntryRepository, Pageable, Query, OrderRepository, RiskConsentEventRepository, TradingModeEventRepository, Service, TradingModeService (+1 more)

### Community 96 - "BackendApplicationTests.java"
Cohesion: 0.60
Nodes (3): BackendApplicationTests, SpringBootTest, Test

### Community 97 - "OrderQueryControllerTest"
Cohesion: 0.15
Nodes (5): AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, OrderQueryControllerTest

### Community 98 - ".calculate"
Cohesion: 0.22
Nodes (5): MathContext, MovingAverageResult, MovingAverageCrossoverCalculator, Test, MovingAverageCrossoverCalculatorTest

### Community 101 - "SchedulingConfig.java"
Cohesion: 0.83
Nodes (3): Configuration, SchedulingConfig, EnableScheduling

### Community 102 - "F5.3 Order status & history"
Cohesion: 0.50
Nodes (4): OrderCsvExporter (RFC 4180 trade-history export), F5.3 Order status & history, E5-F3-S1 Order status/history page, E5-F3-S2 Export trade history to CSV

### Community 103 - "E7 — Observability & Hardening"
Cohesion: 0.50
Nodes (4): E7 — Observability & Hardening, E7-F1 Structured logging, E7-F2 Security review gate, E7-F3 Backup/restore

### Community 106 - "Candle"
Cohesion: 0.19
Nodes (5): Candle, E2ECandleFixtures, Candle, IndicatorTestFixtures, Candle

### Community 117 - "killswitch/api.ts"
Cohesion: 0.32
Nodes (10): clearKillSwitch(), engageKillSwitch(), EngageKillSwitchResponse, fetchKillSwitchState(), KillSwitchCancelSummary, KillSwitchResponse, KillSwitchState, describeError() (+2 more)

### Community 119 - "MarketDataController"
Cohesion: 0.31
Nodes (5): GetMapping, RequestMapping, RestController, MarketDataController, PriceHistoryResponse

### Community 120 - "OrderQueryController"
Cohesion: 0.29
Nodes (6): GetMapping, PostMapping, RequestMapping, ResponseEntity, RestController, OrderQueryController

### Community 122 - ".runAndVerify"
Cohesion: 0.27
Nodes (4): BacktestCandleCsvLoader, Candle, BacktestHarnessTest, Test

### Community 123 - "MarketDataService"
Cohesion: 0.15
Nodes (9): MarketDataClient, Service, MarketDataService, Component, MarketHoursService, BeforeEach, ExtendWith, Test (+1 more)

### Community 125 - "BrokerCredential"
Cohesion: 0.14
Nodes (6): BrokerCredential, Entity, Override, PrePersist, PreUpdate, Table

### Community 129 - "TickerService"
Cohesion: 0.16
Nodes (10): PostMapping, RequestMapping, ResponseEntity, RestController, TickerController, TickerResponse, TickerRepository, Service (+2 more)

### Community 132 - "Backing up and restoring the Oracle instance (E7-F3-S1)"
Cohesion: 0.40
Nodes (4): Backing up and restoring the Oracle instance (E7-F3-S1), Backup procedure, Notes, Restore-test procedure

## Knowledge Gaps
- **232 isolated node(s):** `com.autotrade.dashboard:backend`, `ALPACA`, `BINANCE`, `PAPER`, `LIVE` (+227 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **21 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `TradingMode` connect `TradingMode` to `BrokerOrderRequest`, `BinanceFuturesTradingAdapter`, `AlpacaTradingAdapter`, `Order`, `TradingModeControllerTest`, `BrokerAdapterContractTest`, `BrokerCredentialService`, `AssetType`, `Broker`, `TradingModeServiceTest`, `TickerSignalOrderE2ETest.java`, `TradingModeEvent`, `TradingMode.java`, `FakeBinanceFuturesTradingServer`, `CoreDataModelIntegrationTest.java`, `.submitOrder`, `TradingModeResponse`, `BrokerAdapterConfig.java`, `BinanceFuturesAdapterConfig.java`, `Test`, `TradingModeService`, `OrderQueryControllerTest`, `OrderQueryController`, `BrokerCredential`?**
  _High betweenness centrality (0.098) - this node is a cross-community bridge._
- **Why does `BrokerCredentialService` connect `BrokerCredentialService` to `BrokerOrderRequest`, `BinanceFuturesTradingAdapter`, `AlpacaTradingAdapter`, `CoreDataModelIntegrationTest.java`, `.submitOrder`, `BrokerAdapterConfig.java`, `BrokerAdapterContractTest`, `IndicatorService`, `AssetType`, `OrderService`, `Broker`, `general-purpose agent (implementation)`, `TickerSignalOrderE2ETest.java`, `TradingMode.java`, `CredentialEncryptionService`, `BinanceFuturesAdapterConfig.java`, `FakeBinanceFuturesTradingServer`?**
  _High betweenness centrality (0.098) - this node is a cross-community bridge._
- **Why does `Broker` connect `Broker` to `BrokerOrderRequest`, `BinanceFuturesTradingAdapter`, `AlpacaTradingAdapter`, `Order`, `AlpacaMarketDataClient`, `BrokerCredentialService`, `AssetType`, `IndicatorSnapshot`, `WatchlistSignalPollerTest.java`, `BinanceMarketDataClient`, `TickerSignalOrderE2ETest.java`, `IndicatorControllerTest.java`, `CoreDataModelIntegrationTest.java`, `.submitOrder`, `IndicatorService`, `TickerNotRegisteredException`, `.getPriceHistory`, `TradingMode`, `OrderControllerTest`, `.handleUnavailable`, `.handleRateLimited`, `MarketDataController`, `MarketDataService`, `BrokerCredential`?**
  _High betweenness centrality (0.077) - this node is a cross-community bridge._
- **What connects `com.autotrade.dashboard:backend`, `ALPACA`, `BINANCE` to the rest of the system?**
  _232 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `BrokerOrderRequest` be split into smaller, more focused modules?**
  _Cohesion score 0.05277262420119563 - nodes in this community are weakly interconnected._
- **Should `BinanceFuturesTradingAdapter` be split into smaller, more focused modules?**
  _Cohesion score 0.06204906204906205 - nodes in this community are weakly interconnected._
- **Should `AlpacaTradingAdapter` be split into smaller, more focused modules?**
  _Cohesion score 0.07925407925407925 - nodes in this community are weakly interconnected._