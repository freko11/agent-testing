# Graph Report - agent testing  (2026-08-17)

## Corpus Check
- 358 files · ~214,151 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 3137 nodes · 8798 edges · 171 communities (141 shown, 30 thin omitted)
- Extraction: 87% EXTRACTED · 13% INFERRED · 0% AMBIGUOUS · INFERRED: 1183 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `bd92c3c8`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- BinanceFuturesTradingAdapter
- Changelog
- KillSwitchService
- .evaluate
- AlpacaTradingAdapter
- SignalCallEntry
- DirectionalOutcomeStats
- Candle
- AlpacaMarketDataClient
- devDependencies
- Test
- OrderServiceTest.java
- .calculate
- AssetType
- Order
- LiveDriftBaselineTest.java
- order/api.ts
- RegimeClassifier
- MacdResult
- BacktestHarness.java
- IndicatorTestFixtures
- SecurityConfig.java
- IndicatorSnapshot
- SignalService
- BrokerCredential
- OrderQueryControllerTest
- CoreDataModelIntegrationTest.java
- DashboardPage.tsx
- BrokerCredentialService
- LiveSignalDriftService.java
- .getChartData
- IndicatorId
- OrderServiceTest
- BinanceMarketDataClient
- MarketDataService
- OrderControllerTest
- BrokerOrderResult
- TickerMetrics.tsx
- MarketDataExceptionHandler
- Ticker
- TradingModeServiceTest
- RetryingBrokerAdapter
- apiFetch
- PriceChart.tsx
- .getPriceHistory
- Notification
- RiskLimitService
- compilerOptions
- ApiErrorResponse
- MarketHoursServiceTest
- BrokerOrderRequest
- .findFirstCrossing
- NotificationServiceTest
- TradingMode
- compilerOptions
- NotificationService
- FakeBinanceFuturesTradingServer
- run skill (project override)
- TradeForm.tsx
- .calculate
- LiveSignalDriftServiceTest
- OrderService
- .resolveOrRegister
- TickerService
- security-review skill
- Broker
- TradingModeControllerTest
- WatchlistControllerTest
- BrokerAdapterContractTest
- .export
- general-purpose agent (implementation)
- BacktestHarnessTest
- CredentialEncryptionService
- IndicatorController
- TradingModeEvent
- WatchlistEntry
- adapter-contract-check skill
- BrokerAdapterConfig.java
- BinanceFuturesAdapterConfig.java
- SignalDriftControllerIntegrationTest
- E8-F1-S7 — evaluate the per-symbol RSI override and SELL-side regime gate against a stock fixture
- .run
- .calculate
- .getAccountStatus
- NotificationType
- OrderRepository
- TradingModeResponse
- WatchlistController
- BacktestReport
- OrderAuditControllerIntegrationTest
- NotificationController
- OrderStatus
- TickerControllerTest
- RiskConsentEvent
- PerSymbolRsiOverboughtCalibrationTest
- ThresholdCalibrationTest
- TickerNotRegisteredException
- NotificationControllerTest
- SecurityConfigTest
- killswitch/api.ts
- IndicatorService
- RiskExceptionHandler.java
- MarketDataController
- .calculate
- .forSymbol
- TickerController
- TradingModeExceptionHandler.java
- .run
- MacdHistogramMagnitudeCalibrationTest
- MaCrossoverSeparationCalibrationTest
- PerSymbolMaCrossoverSeparationCalibrationTest
- RsiOverboughtRecalibrationTest
- RsiOversoldRecalibrationTest
- StockPerSymbolRsiOverboughtCalibrationTest
- plugins
- mvnw
- WeightedMajorityFractionCalibrationTest
- BrokerAdapter
- SignalDriftController
- NotificationExceptionHandler.java
- E8-F1-S11 — SELL-only MA-crossover separation gate, evaluated and shipped
- SellMacdHistogramMagnitudeCalibrationTest
- SellMaCrossoverSeparationCalibrationTest
- .placeOrder
- TradingModeBanner.tsx
- AuthController.java
- E8-F1-S8 — per-symbol `macdMinHistogramMagnitudePct` calibration on the BUY side
- PerSymbolAdxThresholds
- TickerServiceTest
- AlpacaTradingAdapterContractTest
- BinanceFuturesTradingAdapterContractTest
- RetryingAlpacaTradingAdapterContractTest
- RetryingBinanceFuturesTradingAdapterContractTest
- signal-rule-review skill
- E8-F3-S6 — `WEIGHTED_MAJORITY_FRACTION` calibration
- .handleRateLimited
- E8-F2-S3 — funding-rate carry cost in the backtest's transaction-cost model
- BrokerCredentialServiceFindTest.java
- MockBrokerAdapterContractTest
- RetryingMockBrokerAdapterContractTest
- dataviz skill
- .handleUnavailable
- ClockConfig.java
- E8-F1-S9 — SELL-only MACD histogram-magnitude gate, evaluated and no-shipped
- PaperTradeThresholdNotMetException
- BackendApplicationTests.java
- BrokerAdapterRouterTest
- E8-F1-S10 — per-symbol MA-crossover separation calibration, evaluated and no-shipped
- Notification system + WatchlistSignalPoller
- Backing up and restoring the Oracle instance (E7-F3-S1)
- BackendApplication
- InvalidTradeRequestException
- SchedulingConfig.java
- RiskLimitConfig.java
- F5.3 Order status & history
- E7 — Observability & Hardening
- BacktestConfig
- tsconfig.json
- Bluesky Icon (SVG symbol)
- Discord Icon (SVG symbol)
- Documentation Icon (SVG symbol)
- db-backup.sh
- db-restore.sh
- App Favicon (Purple Lightning-Bolt Glyph)
- com.autotrade.dashboard:backend
- OrderNotFoundException
- OrderRefreshUnavailableException
- SignalNotActionableException

## God Nodes (most connected - your core abstractions)
1. `Candle` - 138 edges
2. `TradingMode` - 125 edges
3. `Ticker` - 111 edges
4. `Broker` - 107 edges
5. `AssetType` - 94 edges
6. `Order` - 93 edges
7. `Changelog` - 84 edges
8. `SignalRuleId` - 63 edges
9. `IndicatorSnapshot` - 62 edges
10. `BrokerCredential` - 60 edges

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

## Communities (171 total, 30 thin omitted)

### Community 0 - "BinanceFuturesTradingAdapter"
Cohesion: 0.06
Nodes (22): Credentials, BinanceAccountAsset, BinanceAccountResponse, BinanceAlgoOrderResponse, BinanceErrorResponse, BinanceFuturesTradingAdapter, BinanceOrderResponse, BinancePositionResponse (+14 more)

### Community 1 - "Changelog"
Cohesion: 0.03
Nodes (77): Changelog, Dark-first premium visual pass, E1-F1-S1 — local Oracle XE via Docker Compose, E1-F1-S2 — Spring Boot backend skeleton, E1-F1-S3 — React app skeleton, E1-F1-S4 — CI pipeline, E1-F1-S5 — env/config profiles, E1-F2 — core data model (+69 more)

### Community 2 - "KillSwitchService"
Cohesion: 0.06
Nodes (31): EngageKillSwitchResponse, GetMapping, PostMapping, RequestMapping, RestController, KillSwitchController, Entity, Override (+23 more)

### Community 3 - ".evaluate"
Cohesion: 0.07
Nodes (13): IndicatorVotes, IndicatorWeights, WeightedVoteRuleEngine, MacdResult, MovingAverageResult, Test, MaCrossoverSellGateTest, MacdResult (+5 more)

### Community 4 - "AlpacaTradingAdapter"
Cohesion: 0.08
Nodes (23): AlpacaAccountResponse, AlpacaBracketLeg, AlpacaErrorResponse, AlpacaOrderRequestBody, AlpacaOrderResponse, AlpacaPositionResponse, AlpacaStopLeg, AlpacaTradingAdapter (+15 more)

### Community 5 - "SignalCallEntry"
Cohesion: 0.09
Nodes (10): Entity, Override, PrePersist, Table, OrderAuditEntry, Entity, Override, PrePersist (+2 more)

### Community 6 - "DirectionalOutcomeStats"
Cohesion: 0.11
Nodes (11): Checkpoint, MAX, MID, MIN, CheckpointStats, DirectionalOutcomeStats, CheckpointDrift, DirectionalDrift (+3 more)

### Community 7 - "Candle"
Cohesion: 0.12
Nodes (21): Candle, SignalCall, BUY, HOLD, SELL, RuleThresholds, SignalRuleEngine, call() (+13 more)

### Community 8 - "AlpacaMarketDataClient"
Cohesion: 0.09
Nodes (23): AlpacaBar, AlpacaBarsResponse, AlpacaMarketDataClient, Candle, Component, JsonIgnoreProperties, Override, RestClient (+15 more)

### Community 9 - "devDependencies"
Cohesion: 0.05
Nodes (36): dependencies, lightweight-charts, react, react-dom, react-router-dom, devDependencies, oxlint, @types/node (+28 more)

### Community 10 - "Test"
Cohesion: 0.10
Nodes (8): Regime, RANGING, TRENDING, RegimeGatedRuleEngine, Test, RegimeClassifierTest, Test, RegimeGatedRuleEngineTest

### Community 11 - "OrderServiceTest.java"
Cohesion: 0.10
Nodes (21): IndicatorResponse, MovingAverageRelation, EQUAL, SHORT_ABOVE_LONG, SHORT_BELOW_LONG, MarketClosedException, TickerSummary, KillSwitchCancelSummary (+13 more)

### Community 12 - ".calculate"
Cohesion: 0.09
Nodes (17): HoldTermRule, MODERATE_HIGH, MODERATE_LOW, MODERATE_MEDIUM, STRONG_HIGH, STRONG_LOW, STRONG_MEDIUM, match() (+9 more)

### Community 13 - "AssetType"
Cohesion: 0.20
Nodes (10): EntryOrderType, LIMIT, MARKET, OrderSide, BUY, SELL, AssetType, CRYPTO (+2 more)

### Community 14 - "Order"
Cohesion: 0.05
Nodes (7): Entity, Override, PrePersist, PreUpdate, Table, Order, Test

### Community 15 - "LiveDriftBaselineTest.java"
Cohesion: 0.20
Nodes (4): LiveDriftBaseline, BacktestCandleCsvLoader, Test, LiveDriftBaselineTest

### Community 16 - "order/api.ts"
Cohesion: 0.11
Nodes (26): RFC-6266, AuditEntry, AuditEntryPage, fetchAuditEntries(), SignalCall, SignalRuleId, AuditTrail(), describeError() (+18 more)

### Community 17 - "RegimeClassifier"
Cohesion: 0.10
Nodes (10): RegimeClassifier, Test, PerSymbolAdxTrendingThresholdCalibrationTest, SymbolFixture, Test, RegimeCalibrationTest, Test, RegimeOutOfSampleValidationTest (+2 more)

### Community 18 - "MacdResult"
Cohesion: 0.20
Nodes (8): IndicatorComputation, MacdResult, MovingAverageResult, IndicatorFactory, FunctionalInterface, IndicatorResponse, Test, SignalServiceTest

### Community 19 - "BacktestHarness.java"
Cohesion: 0.08
Nodes (14): MathContext, MacdCalculator, MathContext, MovingAverageResult, MovingAverageCrossoverCalculator, MathContext, RsiCalculator, MathContext (+6 more)

### Community 20 - "IndicatorTestFixtures"
Cohesion: 0.16
Nodes (4): IndicatorTestFixtures, Candle, Test, RsiCalculatorTest

### Community 21 - "SecurityConfig.java"
Cohesion: 0.11
Nodes (22): CsrfCookieWriteFilter, Bean, Configuration, Logger, Override, PasswordEncoder, SecurityConfig, SpaCsrfTokenRequestHandler (+14 more)

### Community 22 - "IndicatorSnapshot"
Cohesion: 0.09
Nodes (5): IndicatorSnapshot, Entity, Override, PrePersist, Table

### Community 23 - "SignalService"
Cohesion: 0.17
Nodes (12): Component, ConditionalOnProperty, Logger, Scheduled, WatchlistSignalPoller, SignalCallEntryRepository, Service, SignalService (+4 more)

### Community 24 - "BrokerCredential"
Cohesion: 0.08
Nodes (11): BrokerCredential, Entity, Override, PrePersist, PreUpdate, Table, Transactional, BrokerCredentialServiceRotationTest (+3 more)

### Community 25 - "OrderQueryControllerTest"
Cohesion: 0.08
Nodes (18): Page, PagedResponse, AuditEntryResponse, JsonInclude, Page, Pageable, Query, GetMapping (+10 more)

### Community 26 - "CoreDataModelIntegrationTest.java"
Cohesion: 0.14
Nodes (13): Autowired, BrokerCredentialRepository, IndicatorSnapshotRepository, TickerRepository, CoreDataModelIntegrationTest, SpringBootTest, Test, Transactional (+5 more)

### Community 27 - "DashboardPage.tsx"
Cohesion: 0.12
Nodes (21): App(), AuthContext, AuthContextValue, AuthProvider(), useAuth(), RequireAuth(), TabItem, Tabs() (+13 more)

### Community 28 - "BrokerCredentialService"
Cohesion: 0.14
Nodes (15): ApplicationRunner, AlpacaTradingCredentialBootstrap, Component, Logger, BinanceTradingCredentialBootstrap, ApplicationArguments, Component, Logger (+7 more)

### Community 29 - "LiveSignalDriftService.java"
Cohesion: 0.09
Nodes (19): DirectionalAccumulator, DirectionalOutcome, LOSS, WASH, WIN, DirectionalScoreResult, ExitReason, HORIZON_EXPIRED (+11 more)

### Community 30 - ".getChartData"
Cohesion: 0.16
Nodes (8): ChartDataResponse, ChartIndicatorPoint, ChartDataResponse, IndicatorControllerTest, AutoConfigureMockMvc, MockMvc, Test, WebMvcTest

### Community 31 - "IndicatorId"
Cohesion: 0.14
Nodes (10): IndicatorId, MA_CROSSOVER, MACD, RSI, HorizonCandidate, IndicatorExpectancyAlternateHorizonCalibrationTest, Test, Test (+2 more)

### Community 32 - "OrderServiceTest"
Cohesion: 0.35
Nodes (3): PlaceOrderRequest, Test, OrderServiceTest

### Community 33 - "BinanceMarketDataClient"
Cohesion: 0.11
Nodes (12): BinanceMarketDataClient, Candle, Component, Override, RestClient, TooManyRequests, Logger, RetryHelper (+4 more)

### Community 34 - "MarketDataService"
Cohesion: 0.15
Nodes (9): MarketDataClient, Service, MarketDataService, Component, MarketHoursService, BeforeEach, ExtendWith, Test (+1 more)

### Community 35 - "OrderControllerTest"
Cohesion: 0.15
Nodes (10): JsonInclude, TradeOrderResponse, KillSwitchEngagedException, RiskLimitExceededException, AutoConfigureMockMvc, MockMvc, ObjectMapper, Test (+2 more)

### Community 37 - "TickerMetrics.tsx"
Cohesion: 0.10
Nodes (24): fetchChartData(), registerTicker(), TickerSummary, Broker, fetchSignal(), HoldTerm, IndicatorResponse, MacdResult (+16 more)

### Community 38 - "MarketDataExceptionHandler"
Cohesion: 0.13
Nodes (11): InsufficientPriceHistoryException, InvalidIndicatorRequestException, InvalidPriceHistoryRequestException, ExceptionHandler, Logger, ResponseEntity, RestControllerAdvice, MarketDataExceptionHandler (+3 more)

### Community 39 - "Ticker"
Cohesion: 0.12
Nodes (6): Entity, Override, PrePersist, Table, Ticker, Query

### Community 40 - "TradingModeServiceTest"
Cohesion: 0.23
Nodes (5): SpringBootTest, Test, TestPropertySource, Transactional, TradingModeServiceTest

### Community 41 - "RetryingBrokerAdapter"
Cohesion: 0.20
Nodes (4): BrokerAdapterRetryPolicy, Logger, Override, RetryingBrokerAdapter

### Community 42 - "apiFetch"
Cohesion: 0.18
Nodes (20): apiFetch(), readCookie(), AssetType, fetchPriceHistory(), MarketDataErrorCode, parseMarketDataError(), PriceHistoryResponse, fetchNotifications() (+12 more)

### Community 43 - "PriceChart.tsx"
Cohesion: 0.18
Nodes (18): Broker, ChartDataResponse, ChartIndicatorPoint, CandlestickPoint, LinePoint, toBusinessDay(), toCandlestickSeries(), toMaLongSeries() (+10 more)

### Community 44 - ".getPriceHistory"
Cohesion: 0.18
Nodes (5): PriceHistoryResult, IndicatorServiceTest, BeforeEach, ExtendWith, Test

### Community 45 - "Notification"
Cohesion: 0.14
Nodes (5): Entity, Override, PrePersist, Table, Notification

### Community 46 - "RiskLimitService"
Cohesion: 0.20
Nodes (7): Logger, Service, RiskLimitService, ConfigurationProperties, RiskLimitsProperties, Test, RiskLimitServiceTest

### Community 47 - "compilerOptions"
Cohesion: 0.08
Nodes (23): compilerOptions, allowArbitraryExtensions, allowImportingTsExtensions, erasableSyntaxOnly, jsx, lib, module, moduleDetection (+15 more)

### Community 48 - "ApiErrorResponse"
Cohesion: 0.34
Nodes (7): ApiErrorResponse, JsonInclude, ExceptionHandler, Logger, ResponseEntity, RestControllerAdvice, OrderExceptionHandler

### Community 50 - "BrokerOrderRequest"
Cohesion: 0.16
Nodes (8): AssetBalance, BrokerOrderRequest, Override, MockBrokerAdapter, MockOrderState, PositionState, Test, MockBrokerAdapterTest

### Community 51 - ".findFirstCrossing"
Cohesion: 0.28
Nodes (5): CrossingEvent, WalkForwardScorer, BacktestHarnessTpSlTest, Candle, Test

### Community 52 - "NotificationServiceTest"
Cohesion: 0.29
Nodes (3): ExtendWith, Test, NotificationServiceTest

### Community 53 - "TradingMode"
Cohesion: 0.15
Nodes (8): BrokerAccountStatus, TradingMode, LIVE, PAPER, BrokerCredentialNotConfiguredException, JsonInclude, OrderResponse, TradingModeChangeRequest

### Community 54 - "compilerOptions"
Cohesion: 0.10
Nodes (19): compilerOptions, allowImportingTsExtensions, erasableSyntaxOnly, lib, module, moduleDetection, noEmit, noFallthroughCasesInSwitch (+11 more)

### Community 55 - "NotificationService"
Cohesion: 0.18
Nodes (6): Pageable, NotificationRepository, Logger, Service, NotificationService, BeforeEach

### Community 56 - "FakeBinanceFuturesTradingServer"
Cohesion: 0.11
Nodes (19): BeforeEach, FakeAlpacaTradingServer, FakeOrder, ClientHttpRequest, ClientHttpResponse, HttpStatus, ObjectMapper, RestClient (+11 more)

### Community 57 - "run skill (project override)"
Cohesion: 0.14
Nodes (19): run skill (project override), MarketHoursService (hardcoded NYSE/NASDAQ calendar), oracle-xe Docker Compose service, F1.1 Local dev environment, E1-F1-S1 Docker Compose file for Oracle XE, E1-F1-S2 Spring Boot backend skeleton, E1-F1-S3 React app skeleton with routing, E1-F1-S4 CI pipeline builds/tests both apps (+11 more)

### Community 58 - "TradeForm.tsx"
Cohesion: 0.18
Nodes (14): placeOrder(), PlaceOrderPayload, TradeOrderResponse, DEFAULT_VALUES, describeResult(), ResultTone, SUBMIT_ERROR_MESSAGES, SubmitState (+6 more)

### Community 59 - ".calculate"
Cohesion: 0.24
Nodes (5): AdxCalculator, MathContext, AdxCalculatorTest, Candle, Test

### Community 60 - "LiveSignalDriftServiceTest"
Cohesion: 0.35
Nodes (5): BeforeEach, Candle, ExtendWith, Test, LiveSignalDriftServiceTest

### Community 61 - "OrderService"
Cohesion: 0.22
Nodes (8): PostMapping, RequestMapping, ResponseEntity, RestController, OrderController, Logger, Service, OrderService

### Community 62 - ".resolveOrRegister"
Cohesion: 0.26
Nodes (5): WatchlistEntry, SpringBootTest, Test, Transactional, WatchlistServiceTest

### Community 63 - "TickerService"
Cohesion: 0.19
Nodes (7): Service, Transactional, TickerService, WatchlistEntryRepository, Service, Transactional, WatchlistService

### Community 64 - "security-review skill"
Cohesion: 0.19
Nodes (18): guardrail-check skill, security-review skill, simplify skill, F1.3 Secrets & config management, E1-F3-S1 Broker API keys encrypted at rest, E1-F3-S2 Dashboard requires login, F4.2 Alpaca adapter (stocks), F6.1 Paper/live mode toggle (+10 more)

### Community 65 - "Broker"
Cohesion: 0.14
Nodes (8): Broker, ALPACA, BINANCE, BrokerAdapterAmbiguousOrderException, BrokerAdapterException, BrokerAdapterRateLimitedException, BrokerAdapterTransientException, BrokerAdapterUnavailableException

### Community 66 - "TradingModeControllerTest"
Cohesion: 0.30
Nodes (5): AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, TradingModeControllerTest

### Community 67 - "WatchlistControllerTest"
Cohesion: 0.25
Nodes (7): AddWatchlistEntryRequest, AutoConfigureMockMvc, MockMvc, ObjectMapper, Test, WebMvcTest, WatchlistControllerTest

### Community 69 - ".export"
Cohesion: 0.23
Nodes (3): OrderCsvExporter, Test, OrderCsvExporterTest

### Community 70 - "general-purpose agent (implementation)"
Cohesion: 0.24
Nodes (16): Explore agent (research), general-purpose agent (implementation), Plan agent (design gate), Mandatory per-story workflow (update CLAUDE.md, commit, push), CLAUDE.md project status & architecture log, E1 — Platform Foundation, F1.2 Core data model, F1.4 Testing strategy (+8 more)

### Community 71 - "BacktestHarnessTest"
Cohesion: 0.35
Nodes (3): Candle, BacktestHarnessTest, Test

### Community 72 - "CredentialEncryptionService"
Cohesion: 0.19
Nodes (6): CredentialEncryptionService, Component, Logger, CredentialEncryptionServiceTest, Test, SecretKeySpec

### Community 73 - "IndicatorController"
Cohesion: 0.43
Nodes (4): IndicatorController, GetMapping, RequestMapping, RestController

### Community 75 - "TradingModeEvent"
Cohesion: 0.20
Nodes (5): Entity, Override, PrePersist, Table, TradingModeEvent

### Community 76 - "WatchlistEntry"
Cohesion: 0.22
Nodes (5): Entity, Override, PrePersist, Table, WatchlistEntry

### Community 77 - "adapter-contract-check skill"
Cohesion: 0.18
Nodes (15): adapter-contract-check skill, BrokerAdapter interface (E4-F1-S1), OrderService.submitOrder (bracket order construction), RetryingBrokerAdapter decorator (retry/backoff/outage), TradingModeService (paper/live switch, append-only), F4.1 Adapter interface, E4-F1-S1 BrokerAdapter interface + mock + contract suite, E4-F1-S2 Rate-limit/retry/backoff in adapter contract (+7 more)

### Community 78 - "BrokerAdapterConfig.java"
Cohesion: 0.27
Nodes (8): AlpacaTradingProperties, ConfigurationProperties, BrokerAdapterConfig, Bean, Configuration, EnableConfigurationProperties, RestClient, SimpleClientHttpRequestFactory

### Community 79 - "BinanceFuturesAdapterConfig.java"
Cohesion: 0.27
Nodes (8): BinanceFuturesAdapterConfig, Bean, Configuration, EnableConfigurationProperties, RestClient, SimpleClientHttpRequestFactory, BinanceFuturesTradingProperties, ConfigurationProperties

### Community 80 - "SignalDriftControllerIntegrationTest"
Cohesion: 0.27
Nodes (8): AutoConfigureMockMvc, Cookie, MockHttpSession, MockMvc, SpringBootTest, Test, TestPropertySource, SignalDriftControllerIntegrationTest

### Community 81 - "E8-F1-S7 — evaluate the per-symbol RSI override and SELL-side regime gate against a stock fixture"
Cohesion: 0.40
Nodes (5): Design gate, E8-F1-S7 — evaluate the per-symbol RSI override and SELL-side regime gate against a stock fixture, Findings — no ship on either axis, but not for the same reason as any prior E8-F1 no-ship, Implementation, Scope / no-op confirmation

### Community 82 - ".run"
Cohesion: 0.33
Nodes (5): ApplicationArguments, Override, AlpacaTradingCredentialBootstrapTest, ExtendWith, Test

### Community 85 - "NotificationType"
Cohesion: 0.17
Nodes (11): JsonInclude, NotificationResponse, NotificationType, ORDER_CANCELLED, ORDER_FAILED, ORDER_FILLED, ORDER_PARTIALLY_FILLED, ORDER_PARTIALLY_PROTECTED (+3 more)

### Community 86 - "OrderRepository"
Cohesion: 0.14
Nodes (8): Pageable, Query, OrderRepository, RiskConsentEventRepository, TradingModeEventRepository, Service, TradingModeService, JpaRepository

### Community 87 - "TradingModeResponse"
Cohesion: 0.24
Nodes (6): GetMapping, PostMapping, RequestMapping, RestController, TradingModeController, TradingModeResponse

### Community 88 - "WatchlistController"
Cohesion: 0.25
Nodes (8): GetMapping, PostMapping, RequestMapping, ResponseEntity, RestController, WatchlistController, WatchlistEntryResponse, DeleteMapping

### Community 89 - "BacktestReport"
Cohesion: 0.11
Nodes (8): BacktestReport, HoldGateStats, Test, PerSymbolMacdHistogramMagnitudeCalibrationTest, SymbolFixture, RegimeSplitStats, Test, WeightedVoteBacktestTest

### Community 90 - "OrderAuditControllerIntegrationTest"
Cohesion: 0.29
Nodes (7): AutoConfigureMockMvc, Cookie, MockHttpSession, MockMvc, SpringBootTest, Test, OrderAuditControllerIntegrationTest

### Community 91 - "NotificationController"
Cohesion: 0.29
Nodes (6): GetMapping, PostMapping, RequestMapping, ResponseEntity, RestController, NotificationController

### Community 92 - "OrderStatus"
Cohesion: 0.13
Nodes (14): BrokerPosition, OrderStatus, CANCELLED, FAILED, FILLED, PARTIALLY_FILLED, PARTIALLY_PROTECTED, PENDING (+6 more)

### Community 93 - "TickerControllerTest"
Cohesion: 0.32
Nodes (7): RegisterTickerRequest, AutoConfigureMockMvc, MockMvc, ObjectMapper, Test, WebMvcTest, TickerControllerTest

### Community 94 - "RiskConsentEvent"
Cohesion: 0.24
Nodes (5): Entity, Override, PrePersist, Table, RiskConsentEvent

### Community 95 - "PerSymbolRsiOverboughtCalibrationTest"
Cohesion: 0.35
Nodes (3): Test, PerSymbolRsiOverboughtCalibrationTest, SymbolFixture

### Community 96 - "ThresholdCalibrationTest"
Cohesion: 0.39
Nodes (3): Test, NamedCandidate, ThresholdCalibrationTest

### Community 97 - "TickerNotRegisteredException"
Cohesion: 0.23
Nodes (6): TickerNotRegisteredException, AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, MarketDataControllerTest

### Community 98 - "NotificationControllerTest"
Cohesion: 0.26
Nodes (5): AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, NotificationControllerTest

### Community 99 - "SecurityConfigTest"
Cohesion: 0.32
Nodes (6): AutoConfigureMockMvc, Cookie, MockMvc, SpringBootTest, Test, SecurityConfigTest

### Community 100 - "killswitch/api.ts"
Cohesion: 0.32
Nodes (10): clearKillSwitch(), engageKillSwitch(), EngageKillSwitchResponse, fetchKillSwitchState(), KillSwitchCancelSummary, KillSwitchResponse, KillSwitchState, describeError() (+2 more)

### Community 101 - "IndicatorService"
Cohesion: 0.21
Nodes (7): BigDecimalIndicators, IndicatorService, Service, GetMapping, RequestMapping, RestController, SignalController

### Community 102 - "RiskExceptionHandler.java"
Cohesion: 0.46
Nodes (5): ExceptionHandler, Logger, ResponseEntity, RestControllerAdvice, RiskExceptionHandler

### Community 103 - "MarketDataController"
Cohesion: 0.27
Nodes (5): GetMapping, RequestMapping, RestController, MarketDataController, PriceHistoryResponse

### Community 105 - ".forSymbol"
Cohesion: 0.31
Nodes (3): PerSymbolRuleThresholds, Test, PerSymbolRuleThresholdsTest

### Community 106 - "TickerController"
Cohesion: 0.36
Nodes (6): PostMapping, RequestMapping, ResponseEntity, RestController, TickerController, TickerResponse

### Community 107 - "TradingModeExceptionHandler.java"
Cohesion: 0.29
Nodes (6): RiskConsentNotGivenException, ExceptionHandler, Logger, ResponseEntity, RestControllerAdvice, TradingModeExceptionHandler

### Community 108 - ".run"
Cohesion: 0.13
Nodes (11): MacdResult, BacktestHarness, HoldGateAccumulator, IndicatorAccumulator, FunctionalInterface, RuleEvaluator, HoldGateOutcome, LARGE_MOVE (+3 more)

### Community 109 - "MacdHistogramMagnitudeCalibrationTest"
Cohesion: 0.36
Nodes (3): Test, MacdHistogramMagnitudeCalibrationTest, SymbolFixture

### Community 110 - "MaCrossoverSeparationCalibrationTest"
Cohesion: 0.36
Nodes (3): Test, MaCrossoverSeparationCalibrationTest, SymbolFixture

### Community 111 - "PerSymbolMaCrossoverSeparationCalibrationTest"
Cohesion: 0.36
Nodes (3): Test, PerSymbolMaCrossoverSeparationCalibrationTest, SymbolFixture

### Community 115 - "plugins"
Cohesion: 0.22
Nodes (8): plugins, rules, react/only-export-components, react/rules-of-hooks, $schema, oxc, typescript, warn

### Community 116 - "mvnw"
Cohesion: 0.33
Nodes (6): mvnw script, clean(), die(), exec_maven(), set_java_home(), verbose()

### Community 117 - "WeightedMajorityFractionCalibrationTest"
Cohesion: 0.48
Nodes (3): FractionCandidate, Test, WeightedMajorityFractionCalibrationTest

### Community 118 - "BrokerAdapter"
Cohesion: 0.60
Nodes (3): BrokerAdapter, BrokerAdapterRouter, Service

### Community 119 - "SignalDriftController"
Cohesion: 0.29
Nodes (6): ConditionalOnProperty, GetMapping, RequestMapping, RestController, SignalDriftController, SignalDriftReport

### Community 120 - "NotificationExceptionHandler.java"
Cohesion: 0.29
Nodes (6): InvalidNotificationRequestException, ExceptionHandler, Logger, ResponseEntity, RestControllerAdvice, NotificationExceptionHandler

### Community 121 - "E8-F1-S11 — SELL-only MA-crossover separation gate, evaluated and shipped"
Cohesion: 0.29
Nodes (7): Design, E8-F1-S11 — SELL-only MA-crossover separation gate, evaluated and shipped, Fixture fallout, Implementation, New test coverage, Production wiring, Scope confirmation

### Community 122 - "SellMacdHistogramMagnitudeCalibrationTest"
Cohesion: 0.40
Nodes (3): Test, SellMacdHistogramMagnitudeCalibrationTest, SymbolFixture

### Community 123 - "SellMaCrossoverSeparationCalibrationTest"
Cohesion: 0.40
Nodes (3): Test, SellMaCrossoverSeparationCalibrationTest, SymbolFixture

### Community 125 - "TradingModeBanner.tsx"
Cohesion: 0.42
Nodes (8): fetchTradingMode(), giveRiskConsent(), switchTradingMode(), TradingMode, TradingModeState, describeError(), otherMode(), TradingModeBanner()

### Community 126 - "AuthController.java"
Cohesion: 0.39
Nodes (6): Authentication, AuthController, GetMapping, RequestMapping, ResponseEntity, RestController

### Community 127 - "E8-F1-S8 — per-symbol `macdMinHistogramMagnitudePct` calibration on the BUY side"
Cohesion: 0.29
Nodes (7): Design gate, E8-F1-S8 — per-symbol `macdMinHistogramMagnitudePct` calibration on the BUY side, Findings, Fixture fallout, Implementation, Scope / no-op confirmation, Test coverage

### Community 128 - "PerSymbolAdxThresholds"
Cohesion: 0.31
Nodes (3): PerSymbolAdxThresholds, Test, PerSymbolAdxThresholdsTest

### Community 129 - "TickerServiceTest"
Cohesion: 0.39
Nodes (4): SpringBootTest, Test, Transactional, TickerServiceTest

### Community 130 - "AlpacaTradingAdapterContractTest"
Cohesion: 0.48
Nodes (3): AlpacaTradingAdapterContractTest, ExtendWith, Override

### Community 131 - "BinanceFuturesTradingAdapterContractTest"
Cohesion: 0.33
Nodes (4): BinanceFuturesTradingAdapterContractTest, BeforeEach, ExtendWith, Override

### Community 132 - "RetryingAlpacaTradingAdapterContractTest"
Cohesion: 0.48
Nodes (3): ExtendWith, Override, RetryingAlpacaTradingAdapterContractTest

### Community 133 - "RetryingBinanceFuturesTradingAdapterContractTest"
Cohesion: 0.19
Nodes (7): DecryptedCredential, Override, BeforeEach, BeforeEach, ExtendWith, Override, RetryingBinanceFuturesTradingAdapterContractTest

### Community 134 - "signal-rule-review skill"
Cohesion: 0.43
Nodes (8): signal-rule-review skill, BacktestHarness (walk-forward JUnit validation), HoldTermCalculator (trend strength x volatility band), SignalRuleEngine (Buy/Sell/Hold rule table), F2.3 Buy/Sell/Hold signal & hold-term, E2-F3-S2 Suggested hold-term alongside the call, F2.4 Backtesting, E2-F4-S1 Backtest rule table against historical data

### Community 135 - "E8-F3-S6 — `WEIGHTED_MAJORITY_FRACTION` calibration"
Cohesion: 0.33
Nodes (6): Design, E8-F3-S6 — `WEIGHTED_MAJORITY_FRACTION` calibration, Mechanism: a calibration seam, not a wired change, Scope confirmation, Sweep and result: no ship, kept at 0.5, Test coverage

### Community 137 - "E8-F2-S3 — funding-rate carry cost in the backtest's transaction-cost model"
Cohesion: 0.33
Nodes (6): Design gate, E8-F2-S3 — funding-rate carry cost in the backtest's transaction-cost model, Illustrative figures (not a ship/no-ship story — purely additive), Implementation, Scope / no-op confirmation, Test coverage

### Community 138 - "BrokerCredentialServiceFindTest.java"
Cohesion: 0.53
Nodes (4): BrokerCredentialServiceFindTest, SpringBootTest, Test, Transactional

### Community 141 - "dataviz skill"
Cohesion: 0.38
Nodes (7): dataviz skill, SignalBadge colorblind-safe teal/orange/slate palette, F3.1 Ticker lookup & metrics display, E3-F1-S1 Ticker lookup + stat-tile metrics, E3-F1-S2 Buy/Sell/Hold badge color-coded, F3.2 Metric visualization, E3-F2-S1 Price chart with MA/RSI overlays

### Community 143 - "ClockConfig.java"
Cohesion: 0.60
Nodes (3): ClockConfig, Bean, Configuration

### Community 144 - "E8-F1-S9 — SELL-only MACD histogram-magnitude gate, evaluated and no-shipped"
Cohesion: 0.50
Nodes (4): Design, E8-F1-S9 — SELL-only MACD histogram-magnitude gate, evaluated and no-shipped, Implementation, Scope / no-op confirmation

### Community 146 - "BackendApplicationTests.java"
Cohesion: 0.60
Nodes (3): BackendApplicationTests, SpringBootTest, Test

### Community 148 - "E8-F1-S10 — per-symbol MA-crossover separation calibration, evaluated and no-shipped"
Cohesion: 0.50
Nodes (4): Design, E8-F1-S10 — per-symbol MA-crossover separation calibration, evaluated and no-shipped, Implementation, Scope / no-op confirmation

### Community 149 - "Notification system + WatchlistSignalPoller"
Cohesion: 0.40
Nodes (5): Notification system + WatchlistSignalPoller, Watchlist feature (watchlist_entries), F3.3 Watchlist (stretch), E3-F3-S1 Watchlist persisted in Oracle DB, F5.4 Notifications

### Community 150 - "Backing up and restoring the Oracle instance (E7-F3-S1)"
Cohesion: 0.40
Nodes (4): Backing up and restoring the Oracle instance (E7-F3-S1), Backup procedure, Notes, Restore-test procedure

### Community 153 - "SchedulingConfig.java"
Cohesion: 0.83
Nodes (3): Configuration, SchedulingConfig, EnableScheduling

### Community 154 - "RiskLimitConfig.java"
Cohesion: 0.83
Nodes (3): Configuration, EnableConfigurationProperties, RiskLimitConfig

### Community 155 - "F5.3 Order status & history"
Cohesion: 0.50
Nodes (4): OrderCsvExporter (RFC 4180 trade-history export), F5.3 Order status & history, E5-F3-S1 Order status/history page, E5-F3-S2 Export trade history to CSV

### Community 156 - "E7 — Observability & Hardening"
Cohesion: 0.50
Nodes (4): E7 — Observability & Hardening, E7-F1 Structured logging, E7-F2 Security review gate, E7-F3 Backup/restore

## Knowledge Gaps
- **308 isolated node(s):** `com.autotrade.dashboard:backend`, `MIN`, `MID`, `MAX`, `WIN` (+303 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **30 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Candle` connect `Candle` to `SignalCallEntry`, `DirectionalOutcomeStats`, `AlpacaMarketDataClient`, `OrderServiceTest.java`, `AssetType`, `LiveDriftBaselineTest.java`, `RegimeClassifier`, `BacktestHarness.java`, `IndicatorTestFixtures`, `LiveSignalDriftService.java`, `.getChartData`, `IndicatorId`, `BinanceMarketDataClient`, `MarketDataService`, `.getPriceHistory`, `.findFirstCrossing`, `.calculate`, `LiveSignalDriftServiceTest`, `Broker`, `BacktestHarnessTest`, `.calculate`, `BacktestReport`, `PerSymbolRsiOverboughtCalibrationTest`, `ThresholdCalibrationTest`, `TickerNotRegisteredException`, `IndicatorService`, `MarketDataController`, `.calculate`, `.run`, `MacdHistogramMagnitudeCalibrationTest`, `MaCrossoverSeparationCalibrationTest`, `PerSymbolMaCrossoverSeparationCalibrationTest`, `RsiOverboughtRecalibrationTest`, `RsiOversoldRecalibrationTest`, `StockPerSymbolRsiOverboughtCalibrationTest`, `WeightedMajorityFractionCalibrationTest`, `SellMacdHistogramMagnitudeCalibrationTest`, `SellMaCrossoverSeparationCalibrationTest`?**
  _High betweenness centrality (0.085) - this node is a cross-community bridge._
- **Why does `TradingMode` connect `TradingMode` to `BinanceFuturesTradingAdapter`, `AlpacaTradingAdapterContractTest`, `BinanceFuturesTradingAdapterContractTest`, `AlpacaTradingAdapter`, `RetryingAlpacaTradingAdapterContractTest`, `RetryingBinanceFuturesTradingAdapterContractTest`, `BrokerCredentialServiceFindTest.java`, `MockBrokerAdapterContractTest`, `RetryingMockBrokerAdapterContractTest`, `AssetType`, `Order`, `OrderServiceTest.java`, `BrokerCredential`, `OrderQueryControllerTest`, `CoreDataModelIntegrationTest.java`, `BrokerCredentialService`, `OrderServiceTest`, `BrokerOrderResult`, `TradingModeServiceTest`, `RetryingBrokerAdapter`, `BrokerOrderRequest`, `Broker`, `TradingModeControllerTest`, `BrokerAdapterContractTest`, `.exportOrdersCsv`, `TradingModeEvent`, `BrokerAdapterConfig.java`, `BinanceFuturesAdapterConfig.java`, `.run`, `.getAccountStatus`, `OrderRepository`, `TradingModeResponse`, `OrderStatus`, `.placeOrder`?**
  _High betweenness centrality (0.070) - this node is a cross-community bridge._
- **Why does `BrokerCredentialService` connect `BrokerCredentialService` to `BinanceFuturesTradingAdapter`, `AlpacaTradingAdapterContractTest`, `BinanceFuturesTradingAdapterContractTest`, `AlpacaTradingAdapter`, `RetryingBinanceFuturesTradingAdapterContractTest`, `RetryingAlpacaTradingAdapterContractTest`, `BrokerCredentialServiceFindTest.java`, `OrderServiceTest.java`, `AssetType`, `BrokerCredential`, `CoreDataModelIntegrationTest.java`, `OrderServiceTest`, `OrderService`, `security-review skill`, `Broker`, `CredentialEncryptionService`, `BrokerAdapterConfig.java`, `BinanceFuturesAdapterConfig.java`, `.run`, `OrderRepository`, `OrderStatus`?**
  _High betweenness centrality (0.068) - this node is a cross-community bridge._
- **Are the 6 inferred relationships involving `Candle` (e.g. with `.chartData_marketClosedWithCachedFallback_returns200Stale()` and `.chartData_registeredTicker_returns200WithCandlesAndIndicators()`) actually correct?**
  _`Candle` has 6 INFERRED edges - model-reasoned connections that need verification._
- **What connects `com.autotrade.dashboard:backend`, `MIN`, `MID` to the rest of the system?**
  _308 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `BinanceFuturesTradingAdapter` be split into smaller, more focused modules?**
  _Cohesion score 0.06491228070175438 - nodes in this community are weakly interconnected._
- **Should `Changelog` be split into smaller, more focused modules?**
  _Cohesion score 0.02564102564102564 - nodes in this community are weakly interconnected._