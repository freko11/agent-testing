# Graph Report - agent testing  (2026-08-09)

## Corpus Check
- 343 files · ~166,081 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 2877 nodes · 7980 edges · 162 communities (133 shown, 29 thin omitted)
- Extraction: 86% EXTRACTED · 14% INFERRED · 0% AMBIGUOUS · INFERRED: 1102 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `00aa2885`
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
- TradingMode
- IndicatorSnapshot
- devDependencies
- Regime
- .readDecrypted
- .calculate
- TradingModeServiceTest
- MarketDataExceptionHandler
- RiskLimitService
- OrderQueryControllerTest
- BinanceMarketDataClient
- TradeForm.tsx
- PerSymbolRsiOverboughtCalibrationTest
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
- .evaluate
- RiskConsentEvent
- IndicatorControllerTest
- IndicatorService
- OrderServiceTest
- KillSwitchService
- compilerOptions
- OrderServiceTest.java
- .export
- OrderService
- run skill (project override)
- NotificationServiceTest
- security-review skill
- apiFetch
- .getPriceHistory
- WatchlistEntry
- .evaluate
- general-purpose agent (implementation)
- CredentialEncryptionServiceTest
- Candle
- .findFirstCrossing
- BrokerCredentialService
- adapter-contract-check skill
- WatchlistController
- TradingModeResponse
- BrokerAdapterConfig.java
- BinanceFuturesAdapterConfig.java
- Checkpoint
- RiskExceptionHandler.java
- ApiErrorResponse
- NotificationType
- BrokerOrderResult
- WatchlistControllerTest
- RiskLimitConfig.java
- Broker
- LiveSignalDriftService.java
- OrderAuditEntry
- .run
- MovingAverageResult
- FakeAlpacaTradingServer
- NotificationController
- OrderControllerTest
- RetryingBrokerAdapter
- NotificationControllerTest
- SignalDriftControllerIntegrationTest
- TickerMetrics.tsx
- TickerController
- LiveSignalDriftServiceTest
- mvnw
- Changelog
- .calculate
- AuthController.java
- NotificationExceptionHandler.java
- .calculate
- TradingModeExceptionHandler.java
- plugins
- .getAccountStatus
- TickerService
- signal-rule-review skill
- dataviz skill
- NotificationRepository
- ClockConfig.java
- TradingModeService
- BackendApplicationTests.java
- OrderStatus
- OrderAuditControllerIntegrationTest
- .calculate
- BackendApplication
- SchedulingConfig.java
- F5.3 Order status & history
- E7 — Observability & Hardening
- CheckpointStats
- MarketHoursServiceTest
- RetryingBrokerAdapterOutageTest
- RsiOverboughtRecalibrationTest
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
- MarketDataService
- .getId
- TradingModeBanner.tsx
- SecurityConfigTest
- SignalRuleId
- Ticker
- Notification system + WatchlistSignalPoller
- TradeOrderResponse
- PaperTradeThresholdNotMetException
- BacktestConfig
- CredentialEncryptionService
- ThresholdCalibrationTest
- Backing up and restoring the Oracle instance (E7-F3-S1)
- AlpacaTradingAdapterContractTest
- .calculate
- IndicatorId
- db-backup.sh
- db-restore.sh
- BacktestReport
- WeightedVoteBacktestTest.java
- BinanceFuturesTradingAdapterContractTest
- RetryingAlpacaTradingAdapterContractTest
- .handleRateLimited
- RetryingBinanceFuturesTradingAdapterContractTest
- RuleThresholds
- BrokerCredentialServiceFindTest.java
- MockBrokerAdapterContractTest
- RetryingMockBrokerAdapterContractTest
- .calculate
- DirectionalOutcomeStats
- RsiOversoldRecalibrationTest
- BacktestHarnessTest.java
- BacktestHarness.java
- MarketDataControllerTest.java
- SignalControllerTest
- BrokerCredentialServiceRotationTest.java
- .bullishCandles
- Override
- InvalidTradeRequestException
- OrderNotFoundException
- OrderRefreshUnavailableException
- SignalNotActionableException

## God Nodes (most connected - your core abstractions)
1. `TradingMode` - 125 edges
2. `Candle` - 107 edges
3. `Broker` - 106 edges
4. `Ticker` - 101 edges
5. `Order` - 93 edges
6. `AssetType` - 87 edges
7. `Changelog` - 71 edges
8. `IndicatorSnapshot` - 62 edges
9. `BrokerCredential` - 60 edges
10. `BinanceFuturesTradingAdapter` - 58 edges

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

## Communities (162 total, 29 thin omitted)

### Community 0 - "BrokerOrderRequest"
Cohesion: 0.16
Nodes (7): BrokerOrderRequest, Override, MockBrokerAdapter, MockOrderState, PositionState, Test, MockBrokerAdapterTest

### Community 1 - "BinanceFuturesTradingAdapter"
Cohesion: 0.06
Nodes (22): Credentials, BinanceAccountAsset, BinanceAccountResponse, BinanceAlgoOrderResponse, BinanceErrorResponse, BinanceFuturesTradingAdapter, BinanceOrderResponse, BinancePositionResponse (+14 more)

### Community 2 - "AlpacaTradingAdapter"
Cohesion: 0.08
Nodes (22): AlpacaAccountResponse, AlpacaBracketLeg, AlpacaErrorResponse, AlpacaOrderRequestBody, AlpacaOrderResponse, AlpacaPositionResponse, AlpacaStopLeg, AlpacaTradingAdapter (+14 more)

### Community 3 - "Order"
Cohesion: 0.06
Nodes (8): Entity, Override, PrePersist, PreUpdate, Table, Order, Test, BeforeEach

### Community 4 - ".switchTo"
Cohesion: 0.31
Nodes (5): AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, TradingModeControllerTest

### Community 6 - "AlpacaMarketDataClient"
Cohesion: 0.09
Nodes (23): AlpacaBar, AlpacaBarsResponse, AlpacaMarketDataClient, Candle, Component, JsonIgnoreProperties, Override, RestClient (+15 more)

### Community 7 - ".calculate"
Cohesion: 0.09
Nodes (17): HoldTermRule, MODERATE_HIGH, MODERATE_LOW, MODERATE_MEDIUM, STRONG_HIGH, STRONG_LOW, STRONG_MEDIUM, match() (+9 more)

### Community 8 - "SignalCallEntry"
Cohesion: 0.09
Nodes (17): AuditEntryResponse, JsonInclude, HoldTerm, SignalCall, BUY, HOLD, SELL, Entity (+9 more)

### Community 9 - "TradingMode"
Cohesion: 0.11
Nodes (19): BrokerCredential, Entity, PrePersist, PreUpdate, Table, TradingMode, LIVE, PAPER (+11 more)

### Community 10 - "IndicatorSnapshot"
Cohesion: 0.07
Nodes (5): IndicatorSnapshot, Entity, Override, PrePersist, Table

### Community 11 - "devDependencies"
Cohesion: 0.05
Nodes (36): dependencies, lightweight-charts, react, react-dom, react-router-dom, devDependencies, oxlint, @types/node (+28 more)

### Community 12 - "Regime"
Cohesion: 0.14
Nodes (8): Regime, RANGING, TRENDING, RegimeGatedRuleEngine, Test, RegimeClassifierTest, Test, RegimeGatedRuleEngineTest

### Community 13 - ".readDecrypted"
Cohesion: 0.11
Nodes (10): DecryptedCredential, Override, Transactional, BeforeEach, BeforeEach, BeforeEach, BeforeEach, RestClient (+2 more)

### Community 14 - ".calculate"
Cohesion: 0.24
Nodes (5): AdxCalculator, MathContext, AdxCalculatorTest, Candle, Test

### Community 15 - "TradingModeServiceTest"
Cohesion: 0.22
Nodes (5): SpringBootTest, Test, TestPropertySource, Transactional, TradingModeServiceTest

### Community 16 - "MarketDataExceptionHandler"
Cohesion: 0.12
Nodes (11): InsufficientPriceHistoryException, InvalidPriceHistoryRequestException, ExceptionHandler, Logger, ResponseEntity, RestControllerAdvice, MarketDataExceptionHandler, MarketDataUnavailableException (+3 more)

### Community 17 - "RiskLimitService"
Cohesion: 0.20
Nodes (7): Logger, Service, RiskLimitService, ConfigurationProperties, RiskLimitsProperties, Test, RiskLimitServiceTest

### Community 18 - "OrderQueryControllerTest"
Cohesion: 0.10
Nodes (13): Page, PagedResponse, GetMapping, PostMapping, RequestMapping, ResponseEntity, RestController, OrderQueryController (+5 more)

### Community 19 - "BinanceMarketDataClient"
Cohesion: 0.11
Nodes (12): BinanceMarketDataClient, Candle, Component, Override, RestClient, TooManyRequests, Logger, RetryHelper (+4 more)

### Community 20 - "TradeForm.tsx"
Cohesion: 0.18
Nodes (14): placeOrder(), PlaceOrderPayload, TradeOrderResponse, DEFAULT_VALUES, describeResult(), ResultTone, SUBMIT_ERROR_MESSAGES, SubmitState (+6 more)

### Community 21 - "PerSymbolRsiOverboughtCalibrationTest"
Cohesion: 0.32
Nodes (3): Test, PerSymbolRsiOverboughtCalibrationTest, SymbolFixture

### Community 22 - "TradingModeEvent"
Cohesion: 0.20
Nodes (5): Entity, Override, PrePersist, Table, TradingModeEvent

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
Cohesion: 0.10
Nodes (28): RFC-6266, AuditEntry, AuditEntryPage, fetchAuditEntries(), SignalCall, SignalRuleId, AuditTrail(), describeError() (+20 more)

### Community 27 - "PriceChart.tsx"
Cohesion: 0.18
Nodes (18): Broker, ChartDataResponse, ChartIndicatorPoint, CandlestickPoint, LinePoint, toBusinessDay(), toCandlestickSeries(), toMaLongSeries() (+10 more)

### Community 28 - "FakeBinanceFuturesTradingServer"
Cohesion: 0.27
Nodes (8): FakeBinanceFuturesTradingServer, ClientHttpRequest, ClientHttpResponse, HttpStatus, StoredAlgoOrder, StoredOrder, MultiValueMap, URI

### Community 29 - "DashboardPage.tsx"
Cohesion: 0.12
Nodes (21): App(), AuthContext, AuthContextValue, AuthProvider(), useAuth(), RequireAuth(), TabItem, Tabs() (+13 more)

### Community 30 - "compilerOptions"
Cohesion: 0.08
Nodes (23): compilerOptions, allowArbitraryExtensions, allowImportingTsExtensions, erasableSyntaxOnly, jsx, lib, module, moduleDetection (+15 more)

### Community 31 - "Notification"
Cohesion: 0.16
Nodes (5): Entity, Override, PrePersist, Table, Notification

### Community 32 - "SignalController.java"
Cohesion: 0.25
Nodes (5): InvalidIndicatorRequestException, GetMapping, RequestMapping, RestController, SignalController

### Community 33 - ".evaluate"
Cohesion: 0.16
Nodes (5): IndicatorVotes, SignalRuleEngine, MacdResult, Test, SignalRuleEngineTest

### Community 34 - "RiskConsentEvent"
Cohesion: 0.22
Nodes (5): Entity, Override, PrePersist, Table, RiskConsentEvent

### Community 35 - "IndicatorControllerTest"
Cohesion: 0.23
Nodes (5): IndicatorControllerTest, AutoConfigureMockMvc, MockMvc, Test, WebMvcTest

### Community 36 - "IndicatorService"
Cohesion: 0.17
Nodes (9): ChartDataResponse, ChartIndicatorPoint, IndicatorController, GetMapping, RequestMapping, RestController, IndicatorService, ChartDataResponse (+1 more)

### Community 37 - "OrderServiceTest"
Cohesion: 0.25
Nodes (6): Query, PlaceOrderRequest, SignalComputation, ExtendWith, Test, OrderServiceTest

### Community 38 - "KillSwitchService"
Cohesion: 0.06
Nodes (31): EngageKillSwitchResponse, GetMapping, PostMapping, RequestMapping, RestController, KillSwitchController, Entity, Override (+23 more)

### Community 39 - "compilerOptions"
Cohesion: 0.10
Nodes (19): compilerOptions, allowImportingTsExtensions, erasableSyntaxOnly, lib, module, moduleDetection, noEmit, noFallthroughCasesInSwitch (+11 more)

### Community 40 - "OrderServiceTest.java"
Cohesion: 0.24
Nodes (8): Autowired, MovingAverageRelation, EQUAL, SHORT_ABOVE_LONG, SHORT_BELOW_LONG, TickerSummary, KillSwitchCancelSummary, SignalResponse

### Community 41 - ".export"
Cohesion: 0.15
Nodes (3): OrderCsvExporter, Test, OrderCsvExporterTest

### Community 42 - "OrderService"
Cohesion: 0.11
Nodes (19): BrokerAdapterRouter, Service, Logger, Service, NotificationService, Component, ConditionalOnProperty, Logger (+11 more)

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
Cohesion: 0.19
Nodes (19): apiFetch(), readCookie(), fetchPriceHistory(), parseMarketDataError(), fetchNotifications(), fetchUnreadCount(), markAllNotificationsRead(), markNotificationRead() (+11 more)

### Community 47 - ".getPriceHistory"
Cohesion: 0.18
Nodes (7): MarketClosedException, PriceHistoryResult, TickerNotRegisteredException, IndicatorServiceTest, BeforeEach, ExtendWith, Test

### Community 48 - "WatchlistEntry"
Cohesion: 0.20
Nodes (5): Entity, Override, PrePersist, Table, WatchlistEntry

### Community 49 - ".evaluate"
Cohesion: 0.18
Nodes (5): IndicatorWeights, MacdResult, MovingAverageResult, Test, WeightedVoteRuleEngineTest

### Community 50 - "general-purpose agent (implementation)"
Cohesion: 0.24
Nodes (16): Explore agent (research), general-purpose agent (implementation), Plan agent (design gate), Mandatory per-story workflow (update CLAUDE.md, commit, push), CLAUDE.md project status & architecture log, E1 — Platform Foundation, F1.2 Core data model, F1.4 Testing strategy (+8 more)

### Community 52 - "Candle"
Cohesion: 0.22
Nodes (4): Candle, FixtureSplits, IndicatorTestFixtures, Candle

### Community 53 - ".findFirstCrossing"
Cohesion: 0.28
Nodes (5): CrossingEvent, WalkForwardScorer, BacktestHarnessTpSlTest, Candle, Test

### Community 54 - "BrokerCredentialService"
Cohesion: 0.10
Nodes (17): BrokerCredentialRepository, BrokerCredentialService, Logger, Service, IndicatorSnapshotRepository, Pageable, OrderRepository, TickerRepository (+9 more)

### Community 55 - "adapter-contract-check skill"
Cohesion: 0.18
Nodes (15): adapter-contract-check skill, BrokerAdapter interface (E4-F1-S1), OrderService.submitOrder (bracket order construction), RetryingBrokerAdapter decorator (retry/backoff/outage), TradingModeService (paper/live switch, append-only), F4.1 Adapter interface, E4-F1-S1 BrokerAdapter interface + mock + contract suite, E4-F1-S2 Rate-limit/retry/backoff in adapter contract (+7 more)

### Community 56 - "WatchlistController"
Cohesion: 0.25
Nodes (8): GetMapping, PostMapping, RequestMapping, ResponseEntity, RestController, WatchlistController, WatchlistEntryResponse, DeleteMapping

### Community 57 - "TradingModeResponse"
Cohesion: 0.24
Nodes (6): GetMapping, PostMapping, RequestMapping, RestController, TradingModeController, TradingModeResponse

### Community 58 - "BrokerAdapterConfig.java"
Cohesion: 0.27
Nodes (8): AlpacaTradingProperties, ConfigurationProperties, BrokerAdapterConfig, Bean, Configuration, EnableConfigurationProperties, RestClient, SimpleClientHttpRequestFactory

### Community 59 - "BinanceFuturesAdapterConfig.java"
Cohesion: 0.27
Nodes (8): BinanceFuturesAdapterConfig, Bean, Configuration, EnableConfigurationProperties, RestClient, SimpleClientHttpRequestFactory, BinanceFuturesTradingProperties, ConfigurationProperties

### Community 60 - "Checkpoint"
Cohesion: 0.10
Nodes (12): Checkpoint, MAX, MID, MIN, DirectionalAccumulator, CheckpointDrift, DirectionalDrift, LiveDriftBaseline (+4 more)

### Community 61 - "RiskExceptionHandler.java"
Cohesion: 0.46
Nodes (5): ExceptionHandler, Logger, ResponseEntity, RestControllerAdvice, RiskExceptionHandler

### Community 62 - "ApiErrorResponse"
Cohesion: 0.34
Nodes (7): ApiErrorResponse, JsonInclude, ExceptionHandler, Logger, ResponseEntity, RestControllerAdvice, OrderExceptionHandler

### Community 63 - "NotificationType"
Cohesion: 0.14
Nodes (11): JsonInclude, NotificationResponse, NotificationType, ORDER_CANCELLED, ORDER_FAILED, ORDER_FILLED, ORDER_PARTIALLY_FILLED, ORDER_PARTIALLY_PROTECTED (+3 more)

### Community 64 - "BrokerOrderResult"
Cohesion: 0.20
Nodes (5): BrokerAdapter, BrokerOrderResult, KillSwitchCancelSummary, BrokerAdapterRouterTest, Test

### Community 65 - "WatchlistControllerTest"
Cohesion: 0.25
Nodes (7): AddWatchlistEntryRequest, AutoConfigureMockMvc, MockMvc, ObjectMapper, Test, WebMvcTest, WatchlistControllerTest

### Community 66 - "RiskLimitConfig.java"
Cohesion: 0.83
Nodes (3): Configuration, EnableConfigurationProperties, RiskLimitConfig

### Community 67 - "Broker"
Cohesion: 0.11
Nodes (10): Broker, ALPACA, BINANCE, BrokerAdapterAmbiguousOrderException, BrokerAdapterException, BrokerAdapterRateLimitedException, BrokerAdapterTransientException, BrokerAdapterUnavailableException (+2 more)

### Community 68 - "LiveSignalDriftService.java"
Cohesion: 0.19
Nodes (10): Component, ConditionalOnProperty, Logger, Scheduled, LiveSignalDriftService, Page, Pageable, Query (+2 more)

### Community 69 - "OrderAuditEntry"
Cohesion: 0.18
Nodes (5): Entity, Override, PrePersist, Table, OrderAuditEntry

### Community 70 - ".run"
Cohesion: 0.09
Nodes (17): DirectionalOutcome, LOSS, WASH, WIN, DirectionalScoreResult, ExitReason, HORIZON_EXPIRED, SL_HIT (+9 more)

### Community 71 - "MovingAverageResult"
Cohesion: 0.21
Nodes (9): IndicatorResponse, BigDecimalIndicators, IndicatorComputation, MacdResult, MovingAverageResult, MovingAverageResult, ExtendWith, Test (+1 more)

### Community 72 - "FakeAlpacaTradingServer"
Cohesion: 0.27
Nodes (8): FakeAlpacaTradingServer, FakeOrder, ClientHttpRequest, ClientHttpResponse, HttpStatus, ObjectMapper, RestClient, MockClientHttpRequest

### Community 73 - "NotificationController"
Cohesion: 0.29
Nodes (6): GetMapping, PostMapping, RequestMapping, ResponseEntity, RestController, NotificationController

### Community 74 - "OrderControllerTest"
Cohesion: 0.18
Nodes (8): KillSwitchEngagedException, RiskLimitExceededException, AutoConfigureMockMvc, MockMvc, ObjectMapper, Test, WebMvcTest, OrderControllerTest

### Community 75 - "RetryingBrokerAdapter"
Cohesion: 0.18
Nodes (5): BrokerAdapterRetryPolicy, BrokerPosition, Logger, Override, RetryingBrokerAdapter

### Community 76 - "NotificationControllerTest"
Cohesion: 0.26
Nodes (5): AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, NotificationControllerTest

### Community 77 - "SignalDriftControllerIntegrationTest"
Cohesion: 0.27
Nodes (8): AutoConfigureMockMvc, Cookie, MockHttpSession, MockMvc, SpringBootTest, Test, TestPropertySource, SignalDriftControllerIntegrationTest

### Community 78 - "TickerMetrics.tsx"
Cohesion: 0.10
Nodes (23): fetchChartData(), AssetType, registerTicker(), TickerSummary, Broker, fetchSignal(), HoldTerm, IndicatorResponse (+15 more)

### Community 79 - "TickerController"
Cohesion: 0.29
Nodes (6): PostMapping, RequestMapping, ResponseEntity, RestController, TickerController, TickerResponse

### Community 80 - "LiveSignalDriftServiceTest"
Cohesion: 0.44
Nodes (4): Candle, ExtendWith, Test, LiveSignalDriftServiceTest

### Community 81 - "mvnw"
Cohesion: 0.33
Nodes (6): mvnw script, clean(), die(), exec_maven(), set_java_home(), verbose()

### Community 82 - "Changelog"
Cohesion: 0.03
Nodes (71): Changelog, Dark-first premium visual pass, E1-F1-S1 — local Oracle XE via Docker Compose, E1-F1-S2 — Spring Boot backend skeleton, E1-F1-S3 — React app skeleton, E1-F1-S4 — CI pipeline, E1-F1-S5 — env/config profiles, E1-F2 — core data model (+63 more)

### Community 83 - ".calculate"
Cohesion: 0.20
Nodes (5): MacdResult, MathContext, MacdCalculator, Test, MacdCalculatorTest

### Community 84 - "AuthController.java"
Cohesion: 0.39
Nodes (6): Authentication, AuthController, GetMapping, RequestMapping, ResponseEntity, RestController

### Community 85 - "NotificationExceptionHandler.java"
Cohesion: 0.29
Nodes (6): InvalidNotificationRequestException, ExceptionHandler, Logger, ResponseEntity, RestControllerAdvice, NotificationExceptionHandler

### Community 86 - ".calculate"
Cohesion: 0.26
Nodes (4): MathContext, VolatilityCalculator, Test, VolatilityCalculatorTest

### Community 87 - "TradingModeExceptionHandler.java"
Cohesion: 0.29
Nodes (6): RiskConsentNotGivenException, ExceptionHandler, Logger, ResponseEntity, RestControllerAdvice, TradingModeExceptionHandler

### Community 88 - "plugins"
Cohesion: 0.22
Nodes (8): plugins, rules, react/only-export-components, react/rules-of-hooks, $schema, oxc, typescript, warn

### Community 89 - ".getAccountStatus"
Cohesion: 0.28
Nodes (4): AssetBalance, BrokerAccountStatus, Test, RetryingBrokerAdapterTest

### Community 90 - "TickerService"
Cohesion: 0.18
Nodes (6): Service, Transactional, TickerService, WatchlistEntryRepository, Service, WatchlistService

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
Cohesion: 0.24
Nodes (4): RiskConsentEventRepository, TradingModeEventRepository, Service, TradingModeService

### Community 96 - "BackendApplicationTests.java"
Cohesion: 0.60
Nodes (3): BackendApplicationTests, SpringBootTest, Test

### Community 97 - "OrderStatus"
Cohesion: 0.14
Nodes (12): JsonInclude, OrderResponse, OrderStatus, CANCELLED, FAILED, FILLED, PARTIALLY_FILLED, PARTIALLY_PROTECTED (+4 more)

### Community 98 - "OrderAuditControllerIntegrationTest"
Cohesion: 0.29
Nodes (7): AutoConfigureMockMvc, Cookie, MockHttpSession, MockMvc, SpringBootTest, Test, OrderAuditControllerIntegrationTest

### Community 99 - ".calculate"
Cohesion: 0.24
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

### Community 104 - "CheckpointStats"
Cohesion: 0.23
Nodes (4): CheckpointStats, CheckpointStatsTest, Test, IndicatorExpectancyCalibrationTest

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

### Community 120 - "MarketDataService"
Cohesion: 0.17
Nodes (9): MarketDataClient, Service, MarketDataService, Component, MarketHoursService, BeforeEach, ExtendWith, Test (+1 more)

### Community 121 - ".getId"
Cohesion: 0.33
Nodes (4): SpringBootTest, Test, Transactional, TickerServiceTest

### Community 122 - "TradingModeBanner.tsx"
Cohesion: 0.42
Nodes (8): fetchTradingMode(), giveRiskConsent(), switchTradingMode(), TradingMode, TradingModeState, describeError(), otherMode(), TradingModeBanner()

### Community 123 - "SecurityConfigTest"
Cohesion: 0.32
Nodes (6): AutoConfigureMockMvc, Cookie, MockMvc, SpringBootTest, Test, SecurityConfigTest

### Community 124 - "SignalRuleId"
Cohesion: 0.18
Nodes (11): SignalRuleId, BEARISH_MAJORITY, BEARISH_UNANIMOUS, BULLISH_MAJORITY, BULLISH_UNANIMOUS, CONFLICTING_SIGNALS, NO_STRONG_SIGNAL, NO_VOLUME_DATA (+3 more)

### Community 125 - "Ticker"
Cohesion: 0.13
Nodes (9): Entity, Override, PrePersist, Table, Ticker, Query, ExtendWith, Test (+1 more)

### Community 126 - "Notification system + WatchlistSignalPoller"
Cohesion: 0.40
Nodes (5): Notification system + WatchlistSignalPoller, Watchlist feature (watchlist_entries), F3.3 Watchlist (stretch), E3-F3-S1 Watchlist persisted in Oracle DB, F5.4 Notifications

### Community 127 - "TradeOrderResponse"
Cohesion: 0.27
Nodes (7): PostMapping, RequestMapping, ResponseEntity, RestController, OrderController, JsonInclude, TradeOrderResponse

### Community 130 - "CredentialEncryptionService"
Cohesion: 0.39
Nodes (4): CredentialEncryptionService, Component, Logger, SecretKeySpec

### Community 131 - "ThresholdCalibrationTest"
Cohesion: 0.36
Nodes (3): Test, NamedCandidate, ThresholdCalibrationTest

### Community 132 - "Backing up and restoring the Oracle instance (E7-F3-S1)"
Cohesion: 0.40
Nodes (4): Backing up and restoring the Oracle instance (E7-F3-S1), Backup procedure, Notes, Restore-test procedure

### Community 133 - "AlpacaTradingAdapterContractTest"
Cohesion: 0.48
Nodes (3): AlpacaTradingAdapterContractTest, ExtendWith, Override

### Community 135 - "IndicatorId"
Cohesion: 0.19
Nodes (7): IndicatorId, MA_CROSSOVER, MACD, RSI, Test, Test, OutOfSampleValidationTest

### Community 138 - "BacktestReport"
Cohesion: 0.27
Nodes (3): BacktestReport, HoldGateStats, RegimeSplitStats

### Community 139 - "WeightedVoteBacktestTest.java"
Cohesion: 0.26
Nodes (3): WeightedVoteRuleEngine, Test, WeightedVoteBacktestTest

### Community 140 - "BinanceFuturesTradingAdapterContractTest"
Cohesion: 0.48
Nodes (3): BinanceFuturesTradingAdapterContractTest, ExtendWith, Override

### Community 141 - "RetryingAlpacaTradingAdapterContractTest"
Cohesion: 0.48
Nodes (3): ExtendWith, Override, RetryingAlpacaTradingAdapterContractTest

### Community 143 - "RetryingBinanceFuturesTradingAdapterContractTest"
Cohesion: 0.48
Nodes (3): ExtendWith, Override, RetryingBinanceFuturesTradingAdapterContractTest

### Community 144 - "RuleThresholds"
Cohesion: 0.23
Nodes (5): PerSymbolRuleThresholds, RuleThresholds, Test, MacdHistogramMagnitudeCalibrationTest, SymbolFixture

### Community 145 - "BrokerCredentialServiceFindTest.java"
Cohesion: 0.39
Nodes (4): BrokerCredentialServiceFindTest, SpringBootTest, Test, Transactional

### Community 149 - "DirectionalOutcomeStats"
Cohesion: 0.22
Nodes (5): DirectionalOutcomeStats, Test, RegimeCalibrationTest, Test, RegimeOutOfSampleValidationTest

### Community 151 - "BacktestHarnessTest.java"
Cohesion: 0.36
Nodes (3): Candle, BacktestHarnessTest, Test

### Community 152 - "BacktestHarness.java"
Cohesion: 0.18
Nodes (5): MathContext, RsiCalculator, VolumeTrendCalculator, HoldTermCalculator, RegimeClassifier

### Community 153 - "MarketDataControllerTest.java"
Cohesion: 0.36
Nodes (5): AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, MarketDataControllerTest

### Community 154 - "SignalControllerTest"
Cohesion: 0.31
Nodes (5): AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, SignalControllerTest

### Community 155 - "BrokerCredentialServiceRotationTest.java"
Cohesion: 0.60
Nodes (4): BrokerCredentialServiceRotationTest, SpringBootTest, Test, Transactional

## Knowledge Gaps
- **268 isolated node(s):** `com.autotrade.dashboard:backend`, `MIN`, `MID`, `MAX`, `WIN` (+263 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **29 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Candle` connect `Candle` to `ThresholdCalibrationTest`, `.calculate`, `AlpacaMarketDataClient`, `IndicatorId`, `TradingMode`, `IndicatorSnapshot`, `WeightedVoteBacktestTest.java`, `.calculate`, `RuleThresholds`, `BinanceMarketDataClient`, `.calculate`, `PerSymbolRsiOverboughtCalibrationTest`, `DirectionalOutcomeStats`, `BacktestHarnessTest.java`, `BacktestHarness.java`, `RsiOversoldRecalibrationTest`, `.bullishCandles`, `IndicatorControllerTest`, `IndicatorService`, `OrderServiceTest.java`, `.getPriceHistory`, `.findFirstCrossing`, `Checkpoint`, `Broker`, `LiveSignalDriftService.java`, `.run`, `MovingAverageResult`, `LiveSignalDriftServiceTest`, `.calculate`, `.calculate`, `.calculate`, `CheckpointStats`, `RsiOverboughtRecalibrationTest`, `MarketDataService`?**
  _High betweenness centrality (0.097) - this node is a cross-community bridge._
- **Why does `TradingMode` connect `TradingMode` to `BrokerOrderRequest`, `BinanceFuturesTradingAdapter`, `AlpacaTradingAdapter`, `Order`, `.switchTo`, `AlpacaTradingAdapterContractTest`, `BrokerAdapterContractTest`, `SignalCallEntry`, `BinanceFuturesTradingAdapterContractTest`, `.readDecrypted`, `RetryingAlpacaTradingAdapterContractTest`, `TradingModeServiceTest`, `RetryingBinanceFuturesTradingAdapterContractTest`, `BrokerCredentialServiceFindTest.java`, `OrderQueryControllerTest`, `MockBrokerAdapterContractTest`, `RetryingMockBrokerAdapterContractTest`, `TradingModeEvent`, `.run`, `BrokerCredentialServiceRotationTest.java`, `OrderServiceTest`, `OrderServiceTest.java`, `.export`, `OrderService`, `BrokerCredentialService`, `TradingModeResponse`, `BrokerAdapterConfig.java`, `BinanceFuturesAdapterConfig.java`, `BrokerOrderResult`, `Broker`, `RetryingBrokerAdapter`, `.getAccountStatus`, `TradingModeService`, `OrderStatus`?**
  _High betweenness centrality (0.079) - this node is a cross-community bridge._
- **Why does `BrokerCredentialService` connect `BrokerCredentialService` to `BinanceFuturesTradingAdapter`, `CredentialEncryptionService`, `AlpacaTradingAdapter`, `OrderServiceTest`, `AlpacaTradingAdapterContractTest`, `OrderServiceTest.java`, `TradingMode`, `OrderService`, `BinanceFuturesTradingAdapterContractTest`, `.readDecrypted`, `RetryingAlpacaTradingAdapterContractTest`, `RetryingBinanceFuturesTradingAdapterContractTest`, `security-review skill`, `BrokerCredentialServiceFindTest.java`, `.run`, `BrokerAdapterConfig.java`, `BinanceFuturesAdapterConfig.java`?**
  _High betweenness centrality (0.077) - this node is a cross-community bridge._
- **Are the 6 inferred relationships involving `Candle` (e.g. with `.chartData_marketClosedWithCachedFallback_returns200Stale()` and `.chartData_registeredTicker_returns200WithCandlesAndIndicators()`) actually correct?**
  _`Candle` has 6 INFERRED edges - model-reasoned connections that need verification._
- **What connects `com.autotrade.dashboard:backend`, `MIN`, `MID` to the rest of the system?**
  _268 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `BinanceFuturesTradingAdapter` be split into smaller, more focused modules?**
  _Cohesion score 0.06400343642611683 - nodes in this community are weakly interconnected._
- **Should `AlpacaTradingAdapter` be split into smaller, more focused modules?**
  _Cohesion score 0.08283730158730158 - nodes in this community are weakly interconnected._