# Graph Report - agent testing  (2026-08-11)

## Corpus Check
- 344 files · ~176,718 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 2925 nodes · 8150 edges · 153 communities (127 shown, 26 thin omitted)
- Extraction: 86% EXTRACTED · 14% INFERRED · 0% AMBIGUOUS · INFERRED: 1112 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `0903edfc`
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
- OrderAuditEntry
- TradingMode
- IndicatorSnapshot
- devDependencies
- Test
- BrokerCredential
- .calculate
- TradingModeServiceTest
- ApiErrorResponse
- RiskLimitService
- OrderQueryControllerTest
- BinanceMarketDataClient
- TradeForm.tsx
- PerSymbolRsiOverboughtCalibrationTest
- TradingModeEvent
- SecurityConfig.java
- .run
- .resolveOrRegister
- marketdata/api.ts
- PriceChart.tsx
- .readDecrypted
- DashboardPage.tsx
- compilerOptions
- Notification
- IndicatorService
- .evaluate
- RiskConsentEvent
- MarketClosedException
- IndicatorController
- OrderServiceTest
- KillSwitchService
- compilerOptions
- RegimeCalibrationTest
- .findFirstCrossing
- WatchlistSignalPollerTest
- run skill (project override)
- NotificationServiceTest
- security-review skill
- apiFetch
- RetryingBrokerAdapter
- WatchlistEntry
- MacdResult
- general-purpose agent (implementation)
- LiveSignalDriftService.java
- Candle
- .evaluate
- .getAccountStatus
- adapter-contract-check skill
- WatchlistController
- TradingModeController
- BrokerAdapterConfig.java
- BinanceFuturesAdapterConfig.java
- LiveDriftBaselineTest.java
- RiskExceptionHandler.java
- ApiErrorResponse
- NotificationType
- BrokerOrderResult
- WatchlistControllerTest
- RiskLimitConfig.java
- Broker
- AssetType
- .calculate
- .run
- Ticker
- .getPriceHistory
- NotificationController
- OrderControllerTest
- MaCrossoverSeparationCalibrationTest
- NotificationControllerTest
- SignalDriftControllerIntegrationTest
- TickerMetrics.tsx
- MovingAverageResult
- LiveSignalDriftServiceTest
- mvnw
- Changelog
- TradingModeService
- AuthController.java
- NotificationExceptionHandler.java
- .placeOrder
- TradingModeExceptionHandler.java
- plugins
- .computeSignal
- JpaRepository
- signal-rule-review skill
- dataviz skill
- NotificationService
- ClockConfig.java
- .order
- BackendApplicationTests.java
- OrderRepository
- OrderAuditControllerIntegrationTest
- TickerController
- BackendApplication
- SchedulingConfig.java
- F5.3 Order status & history
- E7 — Observability & Hardening
- DirectionalOutcomeStats
- MarketHoursServiceTest
- OrderRefreshUnavailableException
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
- .findRegistered
- .getId
- TradingModeBanner.tsx
- SecurityConfigTest
- SignalRuleId
- SignalCallEntry
- Notification system + WatchlistSignalPoller
- OrderService
- WatchlistSignalPoller
- BacktestConfig
- WatchlistService
- ThresholdCalibrationTest
- Backing up and restoring the Oracle instance (E7-F3-S1)
- .export
- WeightedVoteBacktestTest
- BacktestReport
- db-backup.sh
- db-restore.sh
- AlpacaTradingAdapterContractTest
- BinanceFuturesTradingAdapterContractTest
- RetryingAlpacaTradingAdapterContractTest
- RetryingBinanceFuturesTradingAdapterContractTest
- .handleRateLimited
- MockBrokerAdapterContractTest
- MacdHistogramMagnitudeCalibrationTest
- BrokerCredentialServiceFindTest.java
- RetryingMockBrokerAdapterContractTest
- InvalidTradeRequestException
- OrderNotFoundException
- SignalNotActionableException
- RsiOversoldRecalibrationTest
- TickerService.java
- MarketDataControllerTest

## God Nodes (most connected - your core abstractions)
1. `TradingMode` - 125 edges
2. `Candle` - 110 edges
3. `Ticker` - 108 edges
4. `Broker` - 107 edges
5. `Order` - 93 edges
6. `AssetType` - 91 edges
7. `Changelog` - 75 edges
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

## Communities (153 total, 26 thin omitted)

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

### Community 4 - "TradingModeControllerTest"
Cohesion: 0.30
Nodes (5): AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, TradingModeControllerTest

### Community 6 - "AlpacaMarketDataClient"
Cohesion: 0.09
Nodes (23): AlpacaBar, AlpacaBarsResponse, AlpacaMarketDataClient, Candle, Component, JsonIgnoreProperties, Override, RestClient (+15 more)

### Community 7 - ".calculate"
Cohesion: 0.09
Nodes (17): HoldTermRule, MODERATE_HIGH, MODERATE_LOW, MODERATE_MEDIUM, STRONG_HIGH, STRONG_LOW, STRONG_MEDIUM, match() (+9 more)

### Community 8 - "OrderAuditEntry"
Cohesion: 0.16
Nodes (5): Entity, Override, PrePersist, Table, OrderAuditEntry

### Community 9 - "TradingMode"
Cohesion: 0.13
Nodes (22): TradingMode, LIVE, PAPER, EntryOrderType, LIMIT, MARKET, JsonInclude, OrderResponse (+14 more)

### Community 10 - "IndicatorSnapshot"
Cohesion: 0.08
Nodes (10): IndicatorSnapshot, Entity, Override, PrePersist, Table, HoldTerm, SpringBootTest, Test (+2 more)

### Community 11 - "devDependencies"
Cohesion: 0.05
Nodes (36): dependencies, lightweight-charts, react, react-dom, react-router-dom, devDependencies, oxlint, @types/node (+28 more)

### Community 12 - "Test"
Cohesion: 0.12
Nodes (8): Regime, RANGING, TRENDING, RegimeGatedRuleEngine, Test, RegimeClassifierTest, Test, RegimeGatedRuleEngineTest

### Community 13 - "BrokerCredential"
Cohesion: 0.08
Nodes (16): BrokerCredential, Entity, Override, PrePersist, PreUpdate, Table, BrokerCredentialRepository, BrokerCredentialService (+8 more)

### Community 14 - ".calculate"
Cohesion: 0.24
Nodes (5): AdxCalculator, MathContext, AdxCalculatorTest, Candle, Test

### Community 15 - "TradingModeServiceTest"
Cohesion: 0.18
Nodes (6): PaperTradeThresholdNotMetException, SpringBootTest, Test, TestPropertySource, Transactional, TradingModeServiceTest

### Community 16 - "ApiErrorResponse"
Cohesion: 0.15
Nodes (10): InsufficientPriceHistoryException, InvalidPriceHistoryRequestException, ExceptionHandler, Logger, ResponseEntity, RestControllerAdvice, MarketDataExceptionHandler, NoPriceDataException (+2 more)

### Community 17 - "RiskLimitService"
Cohesion: 0.20
Nodes (7): Logger, Service, RiskLimitService, ConfigurationProperties, RiskLimitsProperties, Test, RiskLimitServiceTest

### Community 18 - "OrderQueryControllerTest"
Cohesion: 0.09
Nodes (15): Page, PagedResponse, AuditEntryResponse, JsonInclude, GetMapping, PostMapping, RequestMapping, ResponseEntity (+7 more)

### Community 19 - "BinanceMarketDataClient"
Cohesion: 0.11
Nodes (12): BinanceMarketDataClient, Candle, Component, Override, RestClient, TooManyRequests, Logger, RetryHelper (+4 more)

### Community 20 - "TradeForm.tsx"
Cohesion: 0.18
Nodes (14): placeOrder(), PlaceOrderPayload, TradeOrderResponse, DEFAULT_VALUES, describeResult(), ResultTone, SUBMIT_ERROR_MESSAGES, SubmitState (+6 more)

### Community 21 - "PerSymbolRsiOverboughtCalibrationTest"
Cohesion: 0.35
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
Cohesion: 0.26
Nodes (5): WatchlistEntry, SpringBootTest, Test, Transactional, WatchlistServiceTest

### Community 26 - "marketdata/api.ts"
Cohesion: 0.10
Nodes (28): RFC-6266, AuditEntry, AuditEntryPage, fetchAuditEntries(), SignalCall, SignalRuleId, AuditTrail(), describeError() (+20 more)

### Community 27 - "PriceChart.tsx"
Cohesion: 0.18
Nodes (18): Broker, ChartDataResponse, ChartIndicatorPoint, CandlestickPoint, LinePoint, toBusinessDay(), toCandlestickSeries(), toMaLongSeries() (+10 more)

### Community 28 - ".readDecrypted"
Cohesion: 0.05
Nodes (31): DecryptedCredential, Override, CredentialEncryptionService, Component, Logger, CredentialEncryptionServiceTest, Test, BeforeEach (+23 more)

### Community 29 - "DashboardPage.tsx"
Cohesion: 0.12
Nodes (21): App(), AuthContext, AuthContextValue, AuthProvider(), useAuth(), RequireAuth(), TabItem, Tabs() (+13 more)

### Community 30 - "compilerOptions"
Cohesion: 0.08
Nodes (23): compilerOptions, allowArbitraryExtensions, allowImportingTsExtensions, erasableSyntaxOnly, jsx, lib, module, moduleDetection (+15 more)

### Community 31 - "Notification"
Cohesion: 0.14
Nodes (5): Entity, Override, PrePersist, Table, Notification

### Community 32 - "IndicatorService"
Cohesion: 0.25
Nodes (5): InvalidIndicatorRequestException, GetMapping, RequestMapping, RestController, SignalController

### Community 33 - ".evaluate"
Cohesion: 0.18
Nodes (5): IndicatorWeights, WeightedVoteRuleEngine, MacdResult, Test, WeightedVoteRuleEngineTest

### Community 34 - "RiskConsentEvent"
Cohesion: 0.24
Nodes (5): Entity, Override, PrePersist, Table, RiskConsentEvent

### Community 35 - "MarketClosedException"
Cohesion: 0.14
Nodes (8): ChartDataResponse, ChartIndicatorPoint, MarketClosedException, IndicatorControllerTest, AutoConfigureMockMvc, MockMvc, Test, WebMvcTest

### Community 36 - "IndicatorController"
Cohesion: 0.39
Nodes (4): IndicatorController, GetMapping, RequestMapping, RestController

### Community 37 - "OrderServiceTest"
Cohesion: 0.30
Nodes (5): PlaceOrderRequest, SignalComputation, ExtendWith, Test, OrderServiceTest

### Community 38 - "KillSwitchService"
Cohesion: 0.06
Nodes (31): EngageKillSwitchResponse, GetMapping, PostMapping, RequestMapping, RestController, KillSwitchController, Entity, Override (+23 more)

### Community 39 - "compilerOptions"
Cohesion: 0.10
Nodes (19): compilerOptions, allowImportingTsExtensions, erasableSyntaxOnly, lib, module, moduleDetection, noEmit, noFallthroughCasesInSwitch (+11 more)

### Community 40 - "RegimeCalibrationTest"
Cohesion: 0.18
Nodes (5): RegimeClassifier, Test, RegimeCalibrationTest, Test, RegimeOutOfSampleValidationTest

### Community 41 - ".findFirstCrossing"
Cohesion: 0.28
Nodes (5): CrossingEvent, WalkForwardScorer, BacktestHarnessTpSlTest, Candle, Test

### Community 42 - "WatchlistSignalPollerTest"
Cohesion: 0.46
Nodes (3): ExtendWith, Test, WatchlistSignalPollerTest

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

### Community 47 - "RetryingBrokerAdapter"
Cohesion: 0.20
Nodes (4): BrokerAdapterRetryPolicy, Logger, Override, RetryingBrokerAdapter

### Community 48 - "WatchlistEntry"
Cohesion: 0.20
Nodes (5): Entity, Override, PrePersist, Table, WatchlistEntry

### Community 49 - "MacdResult"
Cohesion: 0.19
Nodes (9): IndicatorComputation, MacdResult, IndicatorFactory, BeforeEach, ExtendWith, FunctionalInterface, IndicatorResponse, Test (+1 more)

### Community 50 - "general-purpose agent (implementation)"
Cohesion: 0.24
Nodes (16): Explore agent (research), general-purpose agent (implementation), Plan agent (design gate), Mandatory per-story workflow (update CLAUDE.md, commit, push), CLAUDE.md project status & architecture log, E1 — Platform Foundation, F1.2 Core data model, F1.4 Testing strategy (+8 more)

### Community 51 - "LiveSignalDriftService.java"
Cohesion: 0.19
Nodes (10): Component, ConditionalOnProperty, Logger, Scheduled, LiveSignalDriftService, Page, Pageable, Query (+2 more)

### Community 52 - "Candle"
Cohesion: 0.05
Nodes (23): MacdResult, MathContext, MacdCalculator, MathContext, MovingAverageResult, MovingAverageCrossoverCalculator, MathContext, RsiCalculator (+15 more)

### Community 53 - ".evaluate"
Cohesion: 0.18
Nodes (4): IndicatorVotes, MacdResult, Test, SignalRuleEngineTest

### Community 54 - ".getAccountStatus"
Cohesion: 0.28
Nodes (4): AssetBalance, BrokerAccountStatus, Test, RetryingBrokerAdapterTest

### Community 55 - "adapter-contract-check skill"
Cohesion: 0.18
Nodes (15): adapter-contract-check skill, BrokerAdapter interface (E4-F1-S1), OrderService.submitOrder (bracket order construction), RetryingBrokerAdapter decorator (retry/backoff/outage), TradingModeService (paper/live switch, append-only), F4.1 Adapter interface, E4-F1-S1 BrokerAdapter interface + mock + contract suite, E4-F1-S2 Rate-limit/retry/backoff in adapter contract (+7 more)

### Community 56 - "WatchlistController"
Cohesion: 0.28
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

### Community 60 - "LiveDriftBaselineTest.java"
Cohesion: 0.20
Nodes (4): LiveDriftBaseline, BacktestCandleCsvLoader, Test, LiveDriftBaselineTest

### Community 61 - "RiskExceptionHandler.java"
Cohesion: 0.46
Nodes (5): ExceptionHandler, Logger, ResponseEntity, RestControllerAdvice, RiskExceptionHandler

### Community 62 - "ApiErrorResponse"
Cohesion: 0.34
Nodes (7): ApiErrorResponse, JsonInclude, ExceptionHandler, Logger, ResponseEntity, RestControllerAdvice, OrderExceptionHandler

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

### Community 67 - "Broker"
Cohesion: 0.09
Nodes (11): Broker, ALPACA, BINANCE, BrokerAdapterAmbiguousOrderException, BrokerAdapterException, BrokerAdapterRateLimitedException, BrokerAdapterTransientException, BrokerAdapterUnavailableException (+3 more)

### Community 68 - "AssetType"
Cohesion: 0.08
Nodes (17): BrokerPosition, IndicatorService, Service, IndicatorSnapshotRepository, MarketDataClient, Service, MarketDataService, AssetType (+9 more)

### Community 69 - ".calculate"
Cohesion: 0.26
Nodes (4): MathContext, VolatilityCalculator, Test, VolatilityCalculatorTest

### Community 70 - ".run"
Cohesion: 0.07
Nodes (21): DirectionalOutcome, LOSS, WASH, WIN, DirectionalScoreResult, ExitReason, HORIZON_EXPIRED, SL_HIT (+13 more)

### Community 71 - "Ticker"
Cohesion: 0.13
Nodes (11): Autowired, IndicatorResponse, TickerSummary, KillSwitchCancelSummary, SignalResponse, Entity, Override, PrePersist (+3 more)

### Community 72 - ".getPriceHistory"
Cohesion: 0.19
Nodes (5): ChartDataResponse, PriceHistoryResult, IndicatorServiceTest, ExtendWith, Test

### Community 73 - "NotificationController"
Cohesion: 0.29
Nodes (6): GetMapping, PostMapping, RequestMapping, ResponseEntity, RestController, NotificationController

### Community 74 - "OrderControllerTest"
Cohesion: 0.15
Nodes (10): JsonInclude, TradeOrderResponse, KillSwitchEngagedException, RiskLimitExceededException, AutoConfigureMockMvc, MockMvc, ObjectMapper, Test (+2 more)

### Community 75 - "MaCrossoverSeparationCalibrationTest"
Cohesion: 0.36
Nodes (3): Test, MaCrossoverSeparationCalibrationTest, SymbolFixture

### Community 76 - "NotificationControllerTest"
Cohesion: 0.20
Nodes (5): AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, NotificationControllerTest

### Community 77 - "SignalDriftControllerIntegrationTest"
Cohesion: 0.27
Nodes (8): AutoConfigureMockMvc, Cookie, MockHttpSession, MockMvc, SpringBootTest, Test, TestPropertySource, SignalDriftControllerIntegrationTest

### Community 78 - "TickerMetrics.tsx"
Cohesion: 0.10
Nodes (23): fetchChartData(), AssetType, registerTicker(), TickerSummary, Broker, fetchSignal(), HoldTerm, IndicatorResponse (+15 more)

### Community 79 - "MovingAverageResult"
Cohesion: 0.20
Nodes (8): BigDecimalIndicators, MovingAverageRelation, EQUAL, SHORT_ABOVE_LONG, SHORT_BELOW_LONG, MovingAverageResult, MovingAverageResult, MovingAverageResult

### Community 80 - "LiveSignalDriftServiceTest"
Cohesion: 0.44
Nodes (4): Candle, ExtendWith, Test, LiveSignalDriftServiceTest

### Community 81 - "mvnw"
Cohesion: 0.33
Nodes (6): mvnw script, clean(), die(), exec_maven(), set_java_home(), verbose()

### Community 82 - "Changelog"
Cohesion: 0.03
Nodes (75): Changelog, Dark-first premium visual pass, E1-F1-S1 — local Oracle XE via Docker Compose, E1-F1-S2 — Spring Boot backend skeleton, E1-F1-S3 — React app skeleton, E1-F1-S4 — CI pipeline, E1-F1-S5 — env/config profiles, E1-F2 — core data model (+67 more)

### Community 83 - "TradingModeService"
Cohesion: 0.25
Nodes (3): TradingModeResponse, Service, TradingModeService

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

### Community 89 - ".computeSignal"
Cohesion: 0.29
Nodes (5): AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, SignalControllerTest

### Community 90 - "JpaRepository"
Cohesion: 0.24
Nodes (3): RiskConsentEventRepository, TradingModeEventRepository, JpaRepository

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

### Community 96 - "BackendApplicationTests.java"
Cohesion: 0.60
Nodes (3): BackendApplicationTests, SpringBootTest, Test

### Community 97 - "OrderRepository"
Cohesion: 0.17
Nodes (3): Pageable, Query, OrderRepository

### Community 98 - "OrderAuditControllerIntegrationTest"
Cohesion: 0.29
Nodes (7): AutoConfigureMockMvc, Cookie, MockHttpSession, MockMvc, SpringBootTest, Test, OrderAuditControllerIntegrationTest

### Community 99 - "TickerController"
Cohesion: 0.29
Nodes (6): PostMapping, RequestMapping, ResponseEntity, RestController, TickerController, TickerResponse

### Community 101 - "SchedulingConfig.java"
Cohesion: 0.83
Nodes (3): Configuration, SchedulingConfig, EnableScheduling

### Community 102 - "F5.3 Order status & history"
Cohesion: 0.50
Nodes (4): OrderCsvExporter (RFC 4180 trade-history export), F5.3 Order status & history, E5-F3-S1 Order status/history page, E5-F3-S2 Export trade history to CSV

### Community 103 - "E7 — Observability & Hardening"
Cohesion: 0.50
Nodes (4): E7 — Observability & Hardening, E7-F1 Structured logging, E7-F2 Security review gate, E7-F3 Backup/restore

### Community 104 - "DirectionalOutcomeStats"
Cohesion: 0.11
Nodes (13): Checkpoint, MAX, MID, MIN, CheckpointStats, DirectionalAccumulator, DirectionalOutcomeStats, CheckpointDrift (+5 more)

### Community 108 - "TickerControllerTest"
Cohesion: 0.32
Nodes (7): RegisterTickerRequest, AutoConfigureMockMvc, MockMvc, ObjectMapper, Test, WebMvcTest, TickerControllerTest

### Community 117 - "killswitch/api.ts"
Cohesion: 0.32
Nodes (10): clearKillSwitch(), engageKillSwitch(), EngageKillSwitchResponse, fetchKillSwitchState(), KillSwitchCancelSummary, KillSwitchResponse, KillSwitchState, describeError() (+2 more)

### Community 118 - "SignalDriftController"
Cohesion: 0.21
Nodes (7): RuleTableVersionDrift, ConditionalOnProperty, GetMapping, RequestMapping, RestController, SignalDriftController, SignalDriftReport

### Community 119 - "MarketDataController"
Cohesion: 0.43
Nodes (4): GetMapping, RequestMapping, RestController, MarketDataController

### Community 120 - ".findRegistered"
Cohesion: 0.23
Nodes (5): Component, MarketHoursService, ExtendWith, Test, MarketDataServiceTest

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
Cohesion: 0.11
Nodes (20): HoldTermCalculator, PerSymbolRuleThresholds, SignalCall, BUY, HOLD, SELL, RuleThresholds, SignalRuleEngine (+12 more)

### Community 125 - "SignalCallEntry"
Cohesion: 0.11
Nodes (5): Entity, Override, PrePersist, Table, SignalCallEntry

### Community 126 - "Notification system + WatchlistSignalPoller"
Cohesion: 0.40
Nodes (5): Notification system + WatchlistSignalPoller, Watchlist feature (watchlist_entries), F3.3 Watchlist (stretch), E3-F3-S1 Watchlist persisted in Oracle DB, F5.4 Notifications

### Community 127 - "OrderService"
Cohesion: 0.14
Nodes (14): BrokerAdapter, BrokerAdapterRouter, Service, PostMapping, RequestMapping, ResponseEntity, RestController, OrderController (+6 more)

### Community 128 - "WatchlistSignalPoller"
Cohesion: 0.31
Nodes (6): Component, ConditionalOnProperty, Logger, Scheduled, WatchlistSignalPoller, BeforeEach

### Community 130 - "WatchlistService"
Cohesion: 0.19
Nodes (5): Query, WatchlistEntryRepository, Service, Transactional, WatchlistService

### Community 131 - "ThresholdCalibrationTest"
Cohesion: 0.39
Nodes (3): Test, NamedCandidate, ThresholdCalibrationTest

### Community 132 - "Backing up and restoring the Oracle instance (E7-F3-S1)"
Cohesion: 0.40
Nodes (4): Backing up and restoring the Oracle instance (E7-F3-S1), Backup procedure, Notes, Restore-test procedure

### Community 135 - "BacktestReport"
Cohesion: 0.16
Nodes (8): IndicatorId, MA_CROSSOVER, MACD, RSI, BacktestReport, HoldGateStats, Test, OutOfSampleValidationTest

### Community 138 - "AlpacaTradingAdapterContractTest"
Cohesion: 0.48
Nodes (3): AlpacaTradingAdapterContractTest, ExtendWith, Override

### Community 139 - "BinanceFuturesTradingAdapterContractTest"
Cohesion: 0.48
Nodes (3): BinanceFuturesTradingAdapterContractTest, ExtendWith, Override

### Community 140 - "RetryingAlpacaTradingAdapterContractTest"
Cohesion: 0.48
Nodes (3): ExtendWith, Override, RetryingAlpacaTradingAdapterContractTest

### Community 141 - "RetryingBinanceFuturesTradingAdapterContractTest"
Cohesion: 0.48
Nodes (3): ExtendWith, Override, RetryingBinanceFuturesTradingAdapterContractTest

### Community 144 - "MacdHistogramMagnitudeCalibrationTest"
Cohesion: 0.36
Nodes (3): Test, MacdHistogramMagnitudeCalibrationTest, SymbolFixture

### Community 145 - "BrokerCredentialServiceFindTest.java"
Cohesion: 0.20
Nodes (8): BrokerCredentialServiceFindTest, SpringBootTest, Test, Transactional, BrokerCredentialServiceRotationTest, SpringBootTest, Test, Transactional

### Community 153 - "MarketDataControllerTest"
Cohesion: 0.29
Nodes (5): AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, MarketDataControllerTest

## Knowledge Gaps
- **272 isolated node(s):** `com.autotrade.dashboard:backend`, `MIN`, `MID`, `MAX`, `WIN` (+267 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **26 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Candle` connect `Candle` to `ThresholdCalibrationTest`, `AlpacaMarketDataClient`, `BacktestReport`, `WeightedVoteBacktestTest`, `IndicatorSnapshot`, `.calculate`, `MacdHistogramMagnitudeCalibrationTest`, `BinanceMarketDataClient`, `PerSymbolRsiOverboughtCalibrationTest`, `RsiOversoldRecalibrationTest`, `MarketClosedException`, `RegimeCalibrationTest`, `.findFirstCrossing`, `LiveSignalDriftService.java`, `LiveDriftBaselineTest.java`, `Broker`, `AssetType`, `.calculate`, `.run`, `Ticker`, `.getPriceHistory`, `MaCrossoverSeparationCalibrationTest`, `LiveSignalDriftServiceTest`, `DirectionalOutcomeStats`, `RsiOverboughtRecalibrationTest`, `.findRegistered`, `SignalRuleId`, `SignalCallEntry`?**
  _High betweenness centrality (0.114) - this node is a cross-community bridge._
- **Why does `TradingMode` connect `TradingMode` to `BrokerOrderRequest`, `BinanceFuturesTradingAdapter`, `AlpacaTradingAdapter`, `Order`, `TradingModeControllerTest`, `BrokerAdapterContractTest`, `AlpacaTradingAdapterContractTest`, `BinanceFuturesTradingAdapterContractTest`, `RetryingAlpacaTradingAdapterContractTest`, `BrokerCredential`, `RetryingBinanceFuturesTradingAdapterContractTest`, `TradingModeServiceTest`, `MockBrokerAdapterContractTest`, `BrokerCredentialServiceFindTest.java`, `OrderQueryControllerTest`, `RetryingMockBrokerAdapterContractTest`, `TradingModeEvent`, `.run`, `OrderServiceTest`, `RetryingBrokerAdapter`, `IndicatorSnapshot`, `.getAccountStatus`, `TradingModeController`, `BrokerAdapterConfig.java`, `BinanceFuturesAdapterConfig.java`, `BrokerOrderResult`, `Broker`, `AssetType`, `Ticker`, `TradingModeService`, `.placeOrder`, `OrderRepository`?**
  _High betweenness centrality (0.082) - this node is a cross-community bridge._
- **Why does `BrokerCredentialService` connect `BrokerCredential` to `BinanceFuturesTradingAdapter`, `AlpacaTradingAdapter`, `Broker`, `AssetType`, `OrderServiceTest`, `Ticker`, `TradingMode`, `AlpacaTradingAdapterContractTest`, `BinanceFuturesTradingAdapterContractTest`, `RetryingAlpacaTradingAdapterContractTest`, `RetryingBinanceFuturesTradingAdapterContractTest`, `security-review skill`, `BrokerCredentialServiceFindTest.java`, `.run`, `BrokerAdapterConfig.java`, `BinanceFuturesAdapterConfig.java`, `.readDecrypted`, `OrderService`?**
  _High betweenness centrality (0.075) - this node is a cross-community bridge._
- **Are the 6 inferred relationships involving `Candle` (e.g. with `.chartData_marketClosedWithCachedFallback_returns200Stale()` and `.chartData_registeredTicker_returns200WithCandlesAndIndicators()`) actually correct?**
  _`Candle` has 6 INFERRED edges - model-reasoned connections that need verification._
- **What connects `com.autotrade.dashboard:backend`, `MIN`, `MID` to the rest of the system?**
  _272 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `BinanceFuturesTradingAdapter` be split into smaller, more focused modules?**
  _Cohesion score 0.06491228070175438 - nodes in this community are weakly interconnected._
- **Should `AlpacaTradingAdapter` be split into smaller, more focused modules?**
  _Cohesion score 0.08283730158730158 - nodes in this community are weakly interconnected._