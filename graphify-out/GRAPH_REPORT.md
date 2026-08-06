# Graph Report - agent testing  (2026-08-06)

## Corpus Check
- 330 files · ~149,278 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 2762 nodes · 7586 edges · 150 communities (126 shown, 24 thin omitted)
- Extraction: 86% EXTRACTED · 14% INFERRED · 0% AMBIGUOUS · INFERRED: 1049 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `774a9834`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- BrokerOrderRequest
- BinanceFuturesTradingAdapter
- AlpacaTradingAdapter
- Order
- TradingModeResponse
- BrokerAdapterContractTest
- AlpacaMarketDataClient
- .calculate
- SignalCallEntry
- LiveSignalDriftServiceTest.java
- IndicatorSnapshot
- devDependencies
- Regime
- BrokerCredential
- .calculate
- TradingModeServiceTest
- MarketDataExceptionHandler.java
- RiskLimitService
- .calculate
- BinanceMarketDataClient
- TradeForm.tsx
- SignalRuleId
- TradingModeEvent
- SecurityConfig.java
- BrokerCredentialService
- .resolveOrRegister
- order/api.ts
- PriceChart.tsx
- FakeBinanceFuturesTradingServer
- DashboardPage.tsx
- compilerOptions
- Notification
- IndicatorService
- .evaluate
- RiskConsentEvent
- .getChartData
- IndicatorController
- .submitOrder
- JpaRepository
- compilerOptions
- OrderServiceTest.java
- .export
- OrderService
- run skill (project override)
- NotificationServiceTest
- security-review skill
- apiFetch
- MarketDataControllerTest
- Ticker
- Checkpoint
- general-purpose agent (implementation)
- CredentialEncryptionService
- Candle
- .findFirstCrossing
- RetryingBrokerAdapter
- adapter-contract-check skill
- WatchlistController
- TradingModeController
- BrokerAdapterConfig.java
- BinanceFuturesAdapterConfig.java
- LiveSignalDriftService.java
- RiskExceptionHandler.java
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
- ApiErrorResponse
- NotificationControllerTest
- SignalDriftControllerIntegrationTest
- TickerMetrics.tsx
- ExitReason
- PriceHistoryResult
- mvnw
- Changelog
- OrderStatus
- AuthController.java
- NotificationExceptionHandler.java
- .getAccountStatus
- TradingModeExceptionHandler.java
- plugins
- .runAndVerify
- MarketDataUnavailableException
- signal-rule-review skill
- dataviz skill
- NotificationRepository
- ClockConfig.java
- TradingModeService
- BackendApplicationTests.java
- OrderQueryControllerTest
- TickerController
- .calculate
- BackendApplication
- SchedulingConfig.java
- F5.3 Order status & history
- E7 — Observability & Hardening
- CheckpointStats
- MarketHoursServiceTest
- LiveDriftBaselineTest.java
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
- .getPriceHistory
- TickerServiceTest
- TradingModeBanner.tsx
- .placeOrder
- TickerService
- AssetType
- Notification system + WatchlistSignalPoller
- SignalService
- PaperTradeThresholdNotMetException
- BacktestConfig
- .calculate
- ThresholdCalibrationTest
- Backing up and restoring the Oracle instance (E7-F3-S1)
- OrderQueryController
- RegimeCalibrationTest
- OutOfSampleValidationTest
- db-backup.sh
- db-restore.sh
- BrokerCredentialServiceFindTest.java
- .classify
- AlpacaTradingAdapterContractTest
- BinanceFuturesTradingAdapterContractTest
- Broker
- RetryingAlpacaTradingAdapterContractTest
- RetryingBinanceFuturesTradingAdapterContractTest
- MockBrokerAdapterContractTest
- RetryingMockBrokerAdapterContractTest
- InvalidPriceHistoryRequestException
- NoPriceDataException
- SignalNotActionableException

## God Nodes (most connected - your core abstractions)
1. `TradingMode` - 124 edges
2. `Broker` - 105 edges
3. `Ticker` - 100 edges
4. `Candle` - 94 edges
5. `Order` - 93 edges
6. `AssetType` - 84 edges
7. `Changelog` - 66 edges
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

## Communities (150 total, 24 thin omitted)

### Community 0 - "BrokerOrderRequest"
Cohesion: 0.15
Nodes (8): AssetBalance, BrokerOrderRequest, Override, MockBrokerAdapter, MockOrderState, PositionState, Test, MockBrokerAdapterTest

### Community 1 - "BinanceFuturesTradingAdapter"
Cohesion: 0.06
Nodes (23): Credentials, BinanceAccountAsset, BinanceAccountResponse, BinanceAlgoOrderResponse, BinanceErrorResponse, BinanceFuturesTradingAdapter, BinanceOrderResponse, BinancePositionResponse (+15 more)

### Community 2 - "AlpacaTradingAdapter"
Cohesion: 0.08
Nodes (23): AlpacaAccountResponse, AlpacaBracketLeg, AlpacaErrorResponse, AlpacaOrderRequestBody, AlpacaOrderResponse, AlpacaPositionResponse, AlpacaStopLeg, AlpacaTradingAdapter (+15 more)

### Community 3 - "Order"
Cohesion: 0.06
Nodes (7): Entity, Override, PrePersist, PreUpdate, Table, Order, Test

### Community 4 - "TradingModeResponse"
Cohesion: 0.26
Nodes (6): TradingModeResponse, AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, TradingModeControllerTest

### Community 6 - "AlpacaMarketDataClient"
Cohesion: 0.09
Nodes (23): AlpacaBar, AlpacaBarsResponse, AlpacaMarketDataClient, Candle, Component, JsonIgnoreProperties, Override, RestClient (+15 more)

### Community 7 - ".calculate"
Cohesion: 0.09
Nodes (17): HoldTermRule, MODERATE_HIGH, MODERATE_LOW, MODERATE_MEDIUM, STRONG_HIGH, STRONG_LOW, STRONG_MEDIUM, match() (+9 more)

### Community 8 - "SignalCallEntry"
Cohesion: 0.09
Nodes (13): Logger, Service, NotificationService, Entity, Override, PrePersist, Table, SignalCallEntry (+5 more)

### Community 9 - "LiveSignalDriftServiceTest.java"
Cohesion: 0.23
Nodes (7): EntryOrderType, LIMIT, MARKET, OrderSide, BUY, SELL, HttpMethod

### Community 10 - "IndicatorSnapshot"
Cohesion: 0.08
Nodes (5): IndicatorSnapshot, Entity, Override, PrePersist, Table

### Community 11 - "devDependencies"
Cohesion: 0.05
Nodes (36): dependencies, lightweight-charts, react, react-dom, react-router-dom, devDependencies, oxlint, @types/node (+28 more)

### Community 12 - "Regime"
Cohesion: 0.30
Nodes (3): RegimeGatedRuleEngine, Test, RegimeGatedRuleEngineTest

### Community 13 - "BrokerCredential"
Cohesion: 0.11
Nodes (7): BrokerCredential, Entity, Override, PrePersist, PreUpdate, Table, Test

### Community 14 - ".calculate"
Cohesion: 0.24
Nodes (5): AdxCalculator, MathContext, AdxCalculatorTest, Candle, Test

### Community 15 - "TradingModeServiceTest"
Cohesion: 0.23
Nodes (5): SpringBootTest, Test, TestPropertySource, Transactional, TradingModeServiceTest

### Community 16 - "MarketDataExceptionHandler.java"
Cohesion: 0.15
Nodes (6): InsufficientPriceHistoryException, InvalidIndicatorRequestException, Logger, RestControllerAdvice, TickerAssetTypeConflictException, MethodArgumentNotValidException

### Community 17 - "RiskLimitService"
Cohesion: 0.19
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
Cohesion: 0.08
Nodes (27): HoldTerm, HoldTermCalculator, IndicatorId, MA_CROSSOVER, MACD, RSI, Regime, RANGING (+19 more)

### Community 22 - "TradingModeEvent"
Cohesion: 0.20
Nodes (5): Entity, Override, PrePersist, Table, TradingModeEvent

### Community 23 - "SecurityConfig.java"
Cohesion: 0.11
Nodes (22): CsrfCookieWriteFilter, Bean, Configuration, Logger, Override, PasswordEncoder, SecurityConfig, SpaCsrfTokenRequestHandler (+14 more)

### Community 24 - "BrokerCredentialService"
Cohesion: 0.10
Nodes (21): ApplicationRunner, AlpacaTradingCredentialBootstrap, ApplicationArguments, Component, Logger, Override, BinanceTradingCredentialBootstrap, ApplicationArguments (+13 more)

### Community 25 - ".resolveOrRegister"
Cohesion: 0.29
Nodes (5): WatchlistEntry, SpringBootTest, Test, Transactional, WatchlistServiceTest

### Community 26 - "order/api.ts"
Cohesion: 0.14
Nodes (18): RFC-6266, MarketDataError, EntryOrderType, exportOrdersCsv(), fetchOrders(), filenameFromContentDisposition(), OrderSummary, refreshOrder() (+10 more)

### Community 27 - "PriceChart.tsx"
Cohesion: 0.18
Nodes (18): Broker, ChartDataResponse, ChartIndicatorPoint, CandlestickPoint, LinePoint, toBusinessDay(), toCandlestickSeries(), toMaLongSeries() (+10 more)

### Community 28 - "FakeBinanceFuturesTradingServer"
Cohesion: 0.09
Nodes (23): DecryptedCredential, Override, BeforeEach, BeforeEach, FakeAlpacaTradingServer, FakeOrder, ClientHttpRequest, ClientHttpResponse (+15 more)

### Community 29 - "DashboardPage.tsx"
Cohesion: 0.12
Nodes (21): App(), AuthContext, AuthContextValue, AuthProvider(), useAuth(), RequireAuth(), TabItem, Tabs() (+13 more)

### Community 30 - "compilerOptions"
Cohesion: 0.08
Nodes (23): compilerOptions, allowArbitraryExtensions, allowImportingTsExtensions, erasableSyntaxOnly, jsx, lib, module, moduleDetection (+15 more)

### Community 31 - "Notification"
Cohesion: 0.15
Nodes (5): Entity, Override, PrePersist, Table, Notification

### Community 32 - "IndicatorService"
Cohesion: 0.16
Nodes (9): BigDecimalIndicators, IndicatorComputation, IndicatorService, Service, IndicatorSnapshotRepository, GetMapping, RequestMapping, RestController (+1 more)

### Community 33 - ".evaluate"
Cohesion: 0.09
Nodes (9): IndicatorVotes, IndicatorWeights, WeightedVoteRuleEngine, MacdResult, Test, SignalRuleEngineTest, MacdResult, Test (+1 more)

### Community 34 - "RiskConsentEvent"
Cohesion: 0.24
Nodes (5): Entity, Override, PrePersist, Table, RiskConsentEvent

### Community 35 - ".getChartData"
Cohesion: 0.10
Nodes (12): ChartDataResponse, ChartIndicatorPoint, ChartDataResponse, IndicatorControllerTest, AutoConfigureMockMvc, MockMvc, Test, WebMvcTest (+4 more)

### Community 36 - "IndicatorController"
Cohesion: 0.43
Nodes (4): IndicatorController, GetMapping, RequestMapping, RestController

### Community 37 - ".submitOrder"
Cohesion: 0.28
Nodes (5): PlaceOrderRequest, SignalComputation, ExtendWith, Test, OrderServiceTest

### Community 38 - "JpaRepository"
Cohesion: 0.05
Nodes (32): EngageKillSwitchResponse, GetMapping, PostMapping, RequestMapping, RestController, KillSwitchController, Entity, Override (+24 more)

### Community 39 - "compilerOptions"
Cohesion: 0.10
Nodes (19): compilerOptions, allowImportingTsExtensions, erasableSyntaxOnly, lib, module, moduleDetection, noEmit, noFallthroughCasesInSwitch (+11 more)

### Community 40 - "OrderServiceTest.java"
Cohesion: 0.12
Nodes (19): BrokerAdapterAmbiguousOrderException, IndicatorResponse, MacdResult, MovingAverageRelation, EQUAL, SHORT_ABOVE_LONG, SHORT_BELOW_LONG, MovingAverageResult (+11 more)

### Community 41 - ".export"
Cohesion: 0.23
Nodes (3): OrderCsvExporter, Test, OrderCsvExporterTest

### Community 42 - "OrderService"
Cohesion: 0.10
Nodes (15): BrokerAdapter, BrokerAdapterRouter, Service, PostMapping, RequestMapping, ResponseEntity, RestController, OrderController (+7 more)

### Community 43 - "run skill (project override)"
Cohesion: 0.14
Nodes (19): run skill (project override), MarketHoursService (hardcoded NYSE/NASDAQ calendar), oracle-xe Docker Compose service, F1.1 Local dev environment, E1-F1-S1 Docker Compose file for Oracle XE, E1-F1-S2 Spring Boot backend skeleton, E1-F1-S3 React app skeleton with routing, E1-F1-S4 CI pipeline builds/tests both apps (+11 more)

### Community 44 - "NotificationServiceTest"
Cohesion: 0.25
Nodes (4): BeforeEach, ExtendWith, Test, NotificationServiceTest

### Community 45 - "security-review skill"
Cohesion: 0.19
Nodes (18): guardrail-check skill, security-review skill, simplify skill, F1.3 Secrets & config management, E1-F3-S1 Broker API keys encrypted at rest, E1-F3-S2 Dashboard requires login, F4.2 Alpaca adapter (stocks), F6.1 Paper/live mode toggle (+10 more)

### Community 46 - "apiFetch"
Cohesion: 0.20
Nodes (18): apiFetch(), readCookie(), fetchChartData(), AssetType, fetchPriceHistory(), MarketDataErrorCode, parseMarketDataError(), PriceHistoryResponse (+10 more)

### Community 47 - "MarketDataControllerTest"
Cohesion: 0.30
Nodes (5): AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, MarketDataControllerTest

### Community 48 - "Ticker"
Cohesion: 0.09
Nodes (10): Override, PrePersist, Ticker, Entity, Override, PrePersist, Table, WatchlistEntry (+2 more)

### Community 49 - "Checkpoint"
Cohesion: 0.11
Nodes (9): Checkpoint, MAX, MID, MIN, DirectionalAccumulator, CheckpointDrift, DirectionalDrift, LiveDriftBaseline (+1 more)

### Community 50 - "general-purpose agent (implementation)"
Cohesion: 0.24
Nodes (16): Explore agent (research), general-purpose agent (implementation), Plan agent (design gate), Mandatory per-story workflow (update CLAUDE.md, commit, push), CLAUDE.md project status & architecture log, E1 — Platform Foundation, F1.2 Core data model, F1.4 Testing strategy (+8 more)

### Community 51 - "CredentialEncryptionService"
Cohesion: 0.18
Nodes (6): CredentialEncryptionService, Component, Logger, CredentialEncryptionServiceTest, Test, SecretKeySpec

### Community 52 - "Candle"
Cohesion: 0.12
Nodes (9): MathContext, VolatilityCalculator, Candle, E2ECandleFixtures, Candle, IndicatorTestFixtures, Candle, Test (+1 more)

### Community 53 - ".findFirstCrossing"
Cohesion: 0.47
Nodes (3): BacktestHarnessTpSlTest, Candle, Test

### Community 54 - "RetryingBrokerAdapter"
Cohesion: 0.20
Nodes (4): BrokerAdapterRetryPolicy, Logger, Override, RetryingBrokerAdapter

### Community 55 - "adapter-contract-check skill"
Cohesion: 0.18
Nodes (15): adapter-contract-check skill, BrokerAdapter interface (E4-F1-S1), OrderService.submitOrder (bracket order construction), RetryingBrokerAdapter decorator (retry/backoff/outage), TradingModeService (paper/live switch, append-only), F4.1 Adapter interface, E4-F1-S1 BrokerAdapter interface + mock + contract suite, E4-F1-S2 Rate-limit/retry/backoff in adapter contract (+7 more)

### Community 56 - "WatchlistController"
Cohesion: 0.25
Nodes (8): GetMapping, PostMapping, RequestMapping, ResponseEntity, RestController, WatchlistController, WatchlistEntryResponse, DeleteMapping

### Community 57 - "TradingModeController"
Cohesion: 0.31
Nodes (5): GetMapping, PostMapping, RequestMapping, RestController, TradingModeController

### Community 58 - "BrokerAdapterConfig.java"
Cohesion: 0.27
Nodes (8): AlpacaTradingProperties, ConfigurationProperties, BrokerAdapterConfig, Bean, Configuration, EnableConfigurationProperties, RestClient, SimpleClientHttpRequestFactory

### Community 59 - "BinanceFuturesAdapterConfig.java"
Cohesion: 0.27
Nodes (8): BinanceFuturesAdapterConfig, Bean, Configuration, EnableConfigurationProperties, RestClient, SimpleClientHttpRequestFactory, BinanceFuturesTradingProperties, ConfigurationProperties

### Community 60 - "LiveSignalDriftService.java"
Cohesion: 0.19
Nodes (9): DirectionalScoreResult, Component, ConditionalOnProperty, Logger, Scheduled, LiveSignalDriftService, Query, OrderAuditEntryRepository (+1 more)

### Community 61 - "RiskExceptionHandler.java"
Cohesion: 0.46
Nodes (5): ExceptionHandler, Logger, ResponseEntity, RestControllerAdvice, RiskExceptionHandler

### Community 62 - "OrderExceptionHandler"
Cohesion: 0.16
Nodes (8): InvalidTradeRequestException, ExceptionHandler, Logger, ResponseEntity, RestControllerAdvice, OrderExceptionHandler, OrderNotFoundException, OrderRefreshUnavailableException

### Community 63 - "NotificationType"
Cohesion: 0.17
Nodes (11): JsonInclude, NotificationResponse, NotificationType, ORDER_CANCELLED, ORDER_FAILED, ORDER_FILLED, ORDER_PARTIALLY_FILLED, ORDER_PARTIALLY_PROTECTED (+3 more)

### Community 64 - "BrokerOrderResult"
Cohesion: 0.23
Nodes (4): BrokerOrderResult, KillSwitchCancelSummary, BrokerAdapterRouterTest, Test

### Community 65 - "WatchlistControllerTest"
Cohesion: 0.25
Nodes (7): AddWatchlistEntryRequest, AutoConfigureMockMvc, MockMvc, ObjectMapper, Test, WebMvcTest, WatchlistControllerTest

### Community 66 - "RiskLimitConfig.java"
Cohesion: 0.83
Nodes (3): Configuration, EnableConfigurationProperties, RiskLimitConfig

### Community 67 - "TradingMode"
Cohesion: 0.21
Nodes (8): BrokerAccountStatus, TradingMode, LIVE, PAPER, BrokerCredentialNotConfiguredException, JsonInclude, OrderResponse, TradingModeChangeRequest

### Community 68 - "CoreDataModelIntegrationTest.java"
Cohesion: 0.15
Nodes (10): Autowired, BrokerCredentialRepository, BrokerCredentialServiceRotationTest, SpringBootTest, Transactional, CoreDataModelIntegrationTest, SpringBootTest, Test (+2 more)

### Community 69 - "OrderAuditEntry"
Cohesion: 0.17
Nodes (5): Entity, Override, PrePersist, Table, OrderAuditEntry

### Community 70 - ".run"
Cohesion: 0.15
Nodes (9): CrossingEvent, WalkForwardScorer, BacktestHarness, HoldGateAccumulator, RuleEvaluator, HoldGateOutcome, LARGE_MOVE, STABLE (+1 more)

### Community 71 - ".calculate"
Cohesion: 0.30
Nodes (3): VolumeTrendCalculator, Test, VolumeTrendCalculatorTest

### Community 72 - "watchlist/api.ts"
Cohesion: 0.46
Nodes (6): fetchWatchlist(), removeFromWatchlist(), WatchlistEntry, describeError(), Watchlist(), WatchlistProps

### Community 73 - "NotificationController"
Cohesion: 0.26
Nodes (6): GetMapping, PostMapping, RequestMapping, ResponseEntity, RestController, NotificationController

### Community 74 - "OrderControllerTest"
Cohesion: 0.15
Nodes (10): JsonInclude, TradeOrderResponse, KillSwitchEngagedException, RiskLimitExceededException, AutoConfigureMockMvc, MockMvc, ObjectMapper, Test (+2 more)

### Community 75 - "ApiErrorResponse"
Cohesion: 0.41
Nodes (5): ApiErrorResponse, JsonInclude, ExceptionHandler, ResponseEntity, MarketDataExceptionHandler

### Community 76 - "NotificationControllerTest"
Cohesion: 0.26
Nodes (5): AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, NotificationControllerTest

### Community 77 - "SignalDriftControllerIntegrationTest"
Cohesion: 0.14
Nodes (14): AutoConfigureMockMvc, Cookie, MockMvc, SpringBootTest, Test, TestPropertySource, SignalDriftControllerIntegrationTest, AutoConfigureMockMvc (+6 more)

### Community 78 - "TickerMetrics.tsx"
Cohesion: 0.18
Nodes (10): AddToWatchlistButton(), ERROR_MESSAGES, formatOrDash(), relationLabel(), SIGNAL_GLYPH, StatTileProps, StatTileTone, TickerMetricsProps (+2 more)

### Community 79 - "ExitReason"
Cohesion: 0.16
Nodes (9): DirectionalOutcome, LOSS, WASH, WIN, ExitReason, HORIZON_EXPIRED, SL_HIT, TP_HIT (+1 more)

### Community 80 - "PriceHistoryResult"
Cohesion: 0.42
Nodes (5): PriceHistoryResult, Candle, ExtendWith, Test, LiveSignalDriftServiceTest

### Community 81 - "mvnw"
Cohesion: 0.33
Nodes (6): mvnw script, clean(), die(), exec_maven(), set_java_home(), verbose()

### Community 82 - "Changelog"
Cohesion: 0.03
Nodes (66): Changelog, Dark-first premium visual pass, E1-F1-S1 — local Oracle XE via Docker Compose, E1-F1-S2 — Spring Boot backend skeleton, E1-F1-S3 — React app skeleton, E1-F1-S4 — CI pipeline, E1-F1-S5 — env/config profiles, E1-F2 — core data model (+58 more)

### Community 83 - "OrderStatus"
Cohesion: 0.17
Nodes (10): OrderStatus, CANCELLED, FAILED, FILLED, PARTIALLY_FILLED, PARTIALLY_PROTECTED, PENDING, REJECTED (+2 more)

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

### Community 89 - ".runAndVerify"
Cohesion: 0.39
Nodes (3): Candle, BacktestHarnessTest, Test

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
Cohesion: 0.27
Nodes (4): RiskConsentEventRepository, TradingModeEventRepository, Service, TradingModeService

### Community 96 - "BackendApplicationTests.java"
Cohesion: 0.60
Nodes (3): BackendApplicationTests, SpringBootTest, Test

### Community 97 - "OrderQueryControllerTest"
Cohesion: 0.16
Nodes (5): AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, OrderQueryControllerTest

### Community 98 - "TickerController"
Cohesion: 0.36
Nodes (6): PostMapping, RequestMapping, ResponseEntity, RestController, TickerController, TickerResponse

### Community 99 - ".calculate"
Cohesion: 0.24
Nodes (5): MacdResult, MathContext, MacdCalculator, Test, MacdCalculatorTest

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
Cohesion: 0.23
Nodes (5): CheckpointStats, CheckpointStatsTest, Test, IndicatorExpectancyCalibrationTest, Test

### Community 106 - "LiveDriftBaselineTest.java"
Cohesion: 0.27
Nodes (3): BacktestCandleCsvLoader, Test, LiveDriftBaselineTest

### Community 107 - "BacktestReport"
Cohesion: 0.12
Nodes (8): DirectionalOutcomeStats, BacktestReport, HoldGateStats, RegimeSplitStats, Test, RsiOversoldRecalibrationTest, Test, WeightedVoteBacktestTest

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
Cohesion: 0.27
Nodes (5): GetMapping, RequestMapping, RestController, MarketDataController, PriceHistoryResponse

### Community 120 - ".getPriceHistory"
Cohesion: 0.18
Nodes (9): MarketDataClient, Service, MarketDataService, Component, MarketHoursService, BeforeEach, ExtendWith, Test (+1 more)

### Community 121 - "TickerServiceTest"
Cohesion: 0.39
Nodes (4): SpringBootTest, Test, Transactional, TickerServiceTest

### Community 122 - "TradingModeBanner.tsx"
Cohesion: 0.42
Nodes (8): fetchTradingMode(), giveRiskConsent(), switchTradingMode(), TradingMode, TradingModeState, describeError(), otherMode(), TradingModeBanner()

### Community 124 - "TickerService"
Cohesion: 0.28
Nodes (4): TickerRepository, Service, Transactional, TickerService

### Community 125 - "AssetType"
Cohesion: 0.10
Nodes (9): BrokerPosition, AssetType, CRYPTO, STOCK, Entity, Table, SpringBootTest, Transactional (+1 more)

### Community 126 - "Notification system + WatchlistSignalPoller"
Cohesion: 0.40
Nodes (5): Notification system + WatchlistSignalPoller, Watchlist feature (watchlist_entries), F3.3 Watchlist (stretch), E3-F3-S1 Watchlist persisted in Oracle DB, F5.4 Notifications

### Community 127 - "SignalService"
Cohesion: 0.13
Nodes (15): Component, ConditionalOnProperty, Logger, Scheduled, WatchlistSignalPoller, SignalCallEntryRepository, Service, SignalService (+7 more)

### Community 130 - ".calculate"
Cohesion: 0.29
Nodes (4): MathContext, RsiCalculator, Test, RsiCalculatorTest

### Community 131 - "ThresholdCalibrationTest"
Cohesion: 0.39
Nodes (3): Test, NamedCandidate, ThresholdCalibrationTest

### Community 132 - "Backing up and restoring the Oracle instance (E7-F3-S1)"
Cohesion: 0.40
Nodes (4): Backing up and restoring the Oracle instance (E7-F3-S1), Backup procedure, Notes, Restore-test procedure

### Community 133 - "OrderQueryController"
Cohesion: 0.29
Nodes (6): GetMapping, PostMapping, RequestMapping, ResponseEntity, RestController, OrderQueryController

### Community 134 - "RegimeCalibrationTest"
Cohesion: 0.31
Nodes (3): RegimeClassifier, Test, RegimeCalibrationTest

### Community 138 - "BrokerCredentialServiceFindTest.java"
Cohesion: 0.39
Nodes (4): BrokerCredentialServiceFindTest, SpringBootTest, Test, Transactional

### Community 140 - "AlpacaTradingAdapterContractTest"
Cohesion: 0.48
Nodes (3): AlpacaTradingAdapterContractTest, ExtendWith, Override

### Community 141 - "BinanceFuturesTradingAdapterContractTest"
Cohesion: 0.48
Nodes (3): BinanceFuturesTradingAdapterContractTest, ExtendWith, Override

### Community 142 - "Broker"
Cohesion: 0.14
Nodes (8): Broker, ALPACA, BINANCE, BrokerAdapterException, BrokerAdapterRateLimitedException, BrokerAdapterTransientException, BrokerAdapterUnavailableException, MarketDataRateLimitedException

### Community 143 - "RetryingAlpacaTradingAdapterContractTest"
Cohesion: 0.48
Nodes (3): ExtendWith, Override, RetryingAlpacaTradingAdapterContractTest

### Community 144 - "RetryingBinanceFuturesTradingAdapterContractTest"
Cohesion: 0.48
Nodes (3): ExtendWith, Override, RetryingBinanceFuturesTradingAdapterContractTest

## Knowledge Gaps
- **262 isolated node(s):** `com.autotrade.dashboard:backend`, `MIN`, `MID`, `MAX`, `WIN` (+257 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **24 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `TradingMode` connect `TradingMode` to `BrokerOrderRequest`, `BinanceFuturesTradingAdapter`, `AlpacaTradingAdapter`, `Order`, `TradingModeResponse`, `OrderQueryController`, `BrokerAdapterContractTest`, `LiveSignalDriftServiceTest.java`, `BrokerCredentialServiceFindTest.java`, `AlpacaTradingAdapterContractTest`, `BrokerCredential`, `BinanceFuturesTradingAdapterContractTest`, `TradingModeServiceTest`, `RetryingAlpacaTradingAdapterContractTest`, `MockBrokerAdapterContractTest`, `RetryingBinanceFuturesTradingAdapterContractTest`, `RetryingMockBrokerAdapterContractTest`, `TradingModeEvent`, `BrokerCredentialService`, `.submitOrder`, `OrderServiceTest.java`, `OrderService`, `RetryingBrokerAdapter`, `BrokerAdapterConfig.java`, `BinanceFuturesAdapterConfig.java`, `BrokerOrderResult`, `CoreDataModelIntegrationTest.java`, `OrderStatus`, `.getAccountStatus`, `TradingModeService`, `OrderQueryControllerTest`, `.placeOrder`, `AssetType`?**
  _High betweenness centrality (0.110) - this node is a cross-community bridge._
- **Why does `BrokerCredentialService` connect `BrokerCredentialService` to `BinanceFuturesTradingAdapter`, `AlpacaTradingAdapter`, `LiveSignalDriftServiceTest.java`, `BrokerCredentialServiceFindTest.java`, `AlpacaTradingAdapterContractTest`, `BrokerCredential`, `BinanceFuturesTradingAdapterContractTest`, `RetryingAlpacaTradingAdapterContractTest`, `RetryingBinanceFuturesTradingAdapterContractTest`, `FakeBinanceFuturesTradingServer`, `.submitOrder`, `OrderServiceTest.java`, `OrderService`, `security-review skill`, `CredentialEncryptionService`, `BrokerAdapterConfig.java`, `BinanceFuturesAdapterConfig.java`, `CoreDataModelIntegrationTest.java`, `AssetType`?**
  _High betweenness centrality (0.080) - this node is a cross-community bridge._
- **Why does `Candle` connect `Candle` to `.calculate`, `ThresholdCalibrationTest`, `AlpacaMarketDataClient`, `OutOfSampleValidationTest`, `RegimeCalibrationTest`, `LiveSignalDriftServiceTest.java`, `.calculate`, `.calculate`, `BinanceMarketDataClient`, `SignalRuleId`, `IndicatorService`, `.getChartData`, `OrderServiceTest.java`, `MarketDataControllerTest`, `Checkpoint`, `.findFirstCrossing`, `LiveSignalDriftService.java`, `.run`, `.calculate`, `PriceHistoryResult`, `.runAndVerify`, `.calculate`, `CheckpointStats`, `LiveDriftBaselineTest.java`, `BacktestReport`, `MarketDataController`, `.getPriceHistory`, `AssetType`?**
  _High betweenness centrality (0.080) - this node is a cross-community bridge._
- **Are the 6 inferred relationships involving `Candle` (e.g. with `.chartData_marketClosedWithCachedFallback_returns200Stale()` and `.chartData_registeredTicker_returns200WithCandlesAndIndicators()`) actually correct?**
  _`Candle` has 6 INFERRED edges - model-reasoned connections that need verification._
- **What connects `com.autotrade.dashboard:backend`, `MIN`, `MID` to the rest of the system?**
  _262 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `BrokerOrderRequest` be split into smaller, more focused modules?**
  _Cohesion score 0.14935988620199148 - nodes in this community are weakly interconnected._
- **Should `BinanceFuturesTradingAdapter` be split into smaller, more focused modules?**
  _Cohesion score 0.0629076372817168 - nodes in this community are weakly interconnected._