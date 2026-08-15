# Graph Report - agent testing  (2026-08-15)

## Corpus Check
- 354 files · ~204,366 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 3073 nodes · 8583 edges · 161 communities (139 shown, 22 thin omitted)
- Extraction: 86% EXTRACTED · 14% INFERRED · 0% AMBIGUOUS · INFERRED: 1166 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `c5bdb8e9`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- MockBrokerAdapter
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
- OrderAuditEntryRepositoryTest.java
- .evaluate
- TradingModeServiceTest
- MarketDataExceptionHandler
- RiskLimitService
- .listAuditEntries_mapsRepositoryPageToPagedResponseOfAuditEntryResponse
- BinanceMarketDataClient
- TradeForm.tsx
- BacktestReport
- TradingModeEvent
- SecurityConfig.java
- .run
- .add
- order/api.ts
- PriceChart.tsx
- FakeAlpacaTradingServer
- DashboardPage.tsx
- compilerOptions
- Notification
- SignalController.java
- MovingAverageResult
- RiskConsentEvent
- .getChartData
- .calculate
- OrderServiceTest
- KillSwitchService
- compilerOptions
- .calculate
- WatchlistService
- IndicatorService
- run skill (project override)
- NotificationServiceTest
- security-review skill
- apiFetch
- FakeBinanceFuturesTradingServer
- WatchlistEntry
- SignalCallEntry
- general-purpose agent (implementation)
- TradeOrderResponse
- Candle
- PerSymbolMaCrossoverSeparationCalibrationTest
- PriceHistoryResult
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
- PerSymbolAdxTrendingThresholdCalibrationTest
- OrderServiceTest.java
- CredentialEncryptionServiceTest
- LiveSignalDriftService.java
- PerSymbolMacdHistogramMagnitudeCalibrationTest
- Ticker
- NotificationController
- OrderControllerTest
- MacdHistogramMagnitudeCalibrationTest
- NotificationControllerTest
- SignalDriftControllerIntegrationTest
- TickerMetrics.tsx
- .findFirstCrossing
- RegimeOutOfSampleValidationTest
- mvnw
- Changelog
- PerSymbolRsiOverboughtCalibrationTest
- AuthController.java
- NotificationExceptionHandler.java
- IndicatorId
- TradingModeExceptionHandler.java
- plugins
- WeightedVoteBacktestTest
- TradingModeService
- signal-rule-review skill
- dataviz skill
- NotificationService
- ClockConfig.java
- .export
- BackendApplicationTests.java
- OrderService
- OrderAuditControllerIntegrationTest
- TickerController
- BackendApplication
- SchedulingConfig.java
- F5.3 Order status & history
- E7 — Observability & Hardening
- DirectionalOutcomeStats
- MarketHoursServiceTest
- .run
- .forSymbol
- TickerControllerTest
- tsconfig.json
- Bluesky Icon (SVG symbol)
- Discord Icon (SVG symbol)
- Documentation Icon (SVG symbol)
- App Favicon (Purple Lightning-Bolt Glyph)
- com.autotrade.dashboard:backend
- killswitch/api.ts
- .calculate
- MarketDataController
- .getPriceHistory
- TickerService
- TradingModeBanner.tsx
- SecurityConfigTest
- SignalRuleId
- .handleRateLimited
- CredentialEncryptionService
- E8-F2-S3 — funding-rate carry cost in the backtest's transaction-cost model
- SignalDriftController
- BacktestConfig
- .readDecrypted
- RuleThresholds
- Backing up and restoring the Oracle instance (E7-F3-S1)
- BrokerCredentialServiceFindTest.java
- .calculate
- BacktestHarnessTest
- db-backup.sh
- db-restore.sh
- Notification system + WatchlistSignalPoller
- E8-F1-S10 — per-symbol MA-crossover separation calibration, evaluated and no-shipped
- MaCrossoverSeparationCalibrationTest
- E8-F1-S7 — evaluate the per-symbol RSI override and SELL-side regime gate against a stock fixture
- OrderStatus
- IndicatorController
- RsiOversoldRecalibrationTest
- E8-F1-S8 — per-symbol `macdMinHistogramMagnitudePct` calibration on the BUY side
- OrderQueryControllerTest
- PerSymbolAdxThresholds
- StockPerSymbolRsiOverboughtCalibrationTest
- RegimeClassifier
- .switchTo_live_belowThreshold_throwsPaperTradeThresholdNotMetException_noHistoryPersisted
- MarketDataControllerTest
- SellMacdHistogramMagnitudeCalibrationTest
- DecryptedCredential
- .calculate
- .calculate
- E8-F1-S9 — SELL-only MACD histogram-magnitude gate, evaluated and no-shipped
- InvalidTradeRequestException
- OrderNotFoundException
- OrderRefreshUnavailableException
- SignalNotActionableException

## God Nodes (most connected - your core abstractions)
1. `Candle` - 130 edges
2. `TradingMode` - 125 edges
3. `Ticker` - 110 edges
4. `Broker` - 107 edges
5. `Order` - 93 edges
6. `AssetType` - 91 edges
7. `Changelog` - 82 edges
8. `IndicatorSnapshot` - 62 edges
9. `SignalRuleId` - 61 edges
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

## Communities (161 total, 22 thin omitted)

### Community 0 - "MockBrokerAdapter"
Cohesion: 0.07
Nodes (16): AssetBalance, BrokerAccountStatus, BrokerAdapterRetryPolicy, Logger, Override, RetryingBrokerAdapter, Override, MockBrokerAdapter (+8 more)

### Community 1 - "BinanceFuturesTradingAdapter"
Cohesion: 0.06
Nodes (27): Credentials, BinanceAccountAsset, BinanceAccountResponse, BinanceAlgoOrderResponse, BinanceErrorResponse, BinanceFuturesTradingAdapter, BinanceOrderResponse, BinancePositionResponse (+19 more)

### Community 2 - "AlpacaTradingAdapter"
Cohesion: 0.08
Nodes (23): AlpacaAccountResponse, AlpacaBracketLeg, AlpacaErrorResponse, AlpacaOrderRequestBody, AlpacaOrderResponse, AlpacaPositionResponse, AlpacaStopLeg, AlpacaTradingAdapter (+15 more)

### Community 3 - "Order"
Cohesion: 0.06
Nodes (7): Entity, Override, PrePersist, PreUpdate, Table, Order, Test

### Community 4 - "TradingModeControllerTest"
Cohesion: 0.30
Nodes (5): AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, TradingModeControllerTest

### Community 5 - "BrokerAdapterContractTest"
Cohesion: 0.09
Nodes (15): AlpacaTradingAdapterContractTest, ExtendWith, Override, BrokerAdapterContractTest, Test, Override, MockBrokerAdapterContractTest, ExtendWith (+7 more)

### Community 6 - "AlpacaMarketDataClient"
Cohesion: 0.09
Nodes (23): AlpacaBar, AlpacaBarsResponse, AlpacaMarketDataClient, Candle, Component, JsonIgnoreProperties, Override, RestClient (+15 more)

### Community 7 - ".calculate"
Cohesion: 0.08
Nodes (18): HoldTermCalculator, HoldTermRule, MODERATE_HIGH, MODERATE_LOW, MODERATE_MEDIUM, STRONG_HIGH, STRONG_LOW, STRONG_MEDIUM (+10 more)

### Community 8 - "OrderAuditEntry"
Cohesion: 0.15
Nodes (5): Entity, Override, PrePersist, Table, OrderAuditEntry

### Community 9 - "TradingMode"
Cohesion: 0.09
Nodes (31): Broker, ALPACA, BINANCE, BrokerCredential, Entity, Override, PrePersist, PreUpdate (+23 more)

### Community 10 - "IndicatorSnapshot"
Cohesion: 0.09
Nodes (5): IndicatorSnapshot, Entity, Override, PrePersist, Table

### Community 11 - "devDependencies"
Cohesion: 0.05
Nodes (36): dependencies, lightweight-charts, react, react-dom, react-router-dom, devDependencies, oxlint, @types/node (+28 more)

### Community 12 - "Test"
Cohesion: 0.11
Nodes (8): Regime, RANGING, TRENDING, RegimeGatedRuleEngine, Test, RegimeClassifierTest, Test, RegimeGatedRuleEngineTest

### Community 13 - "OrderAuditEntryRepositoryTest.java"
Cohesion: 0.10
Nodes (14): IndicatorSnapshotRepository, TickerRepository, CoreDataModelIntegrationTest, SpringBootTest, Test, Transactional, SpringBootTest, Transactional (+6 more)

### Community 14 - ".evaluate"
Cohesion: 0.09
Nodes (9): IndicatorVotes, IndicatorWeights, WeightedVoteRuleEngine, MacdResult, Test, SignalRuleEngineTest, MacdResult, Test (+1 more)

### Community 15 - "TradingModeServiceTest"
Cohesion: 0.23
Nodes (5): SpringBootTest, Test, TestPropertySource, Transactional, TradingModeServiceTest

### Community 16 - "MarketDataExceptionHandler"
Cohesion: 0.12
Nodes (11): InsufficientPriceHistoryException, InvalidPriceHistoryRequestException, ExceptionHandler, Logger, ResponseEntity, RestControllerAdvice, MarketDataExceptionHandler, MarketDataUnavailableException (+3 more)

### Community 17 - "RiskLimitService"
Cohesion: 0.20
Nodes (7): Logger, Service, RiskLimitService, ConfigurationProperties, RiskLimitsProperties, Test, RiskLimitServiceTest

### Community 18 - ".listAuditEntries_mapsRepositoryPageToPagedResponseOfAuditEntryResponse"
Cohesion: 0.16
Nodes (8): Page, PagedResponse, GetMapping, PostMapping, RequestMapping, ResponseEntity, RestController, OrderQueryController

### Community 19 - "BinanceMarketDataClient"
Cohesion: 0.11
Nodes (12): BinanceMarketDataClient, Candle, Component, Override, RestClient, TooManyRequests, Logger, RetryHelper (+4 more)

### Community 20 - "TradeForm.tsx"
Cohesion: 0.18
Nodes (14): placeOrder(), PlaceOrderPayload, TradeOrderResponse, DEFAULT_VALUES, describeResult(), ResultTone, SUBMIT_ERROR_MESSAGES, SubmitState (+6 more)

### Community 21 - "BacktestReport"
Cohesion: 0.17
Nodes (5): BacktestReport, HoldGateStats, RegimeSplitStats, Test, RsiOverboughtRecalibrationTest

### Community 22 - "TradingModeEvent"
Cohesion: 0.20
Nodes (5): Entity, Override, PrePersist, Table, TradingModeEvent

### Community 23 - "SecurityConfig.java"
Cohesion: 0.11
Nodes (22): CsrfCookieWriteFilter, Bean, Configuration, Logger, Override, PasswordEncoder, SecurityConfig, SpaCsrfTokenRequestHandler (+14 more)

### Community 24 - ".run"
Cohesion: 0.11
Nodes (17): ApplicationRunner, AlpacaTradingCredentialBootstrap, ApplicationArguments, Component, Logger, Override, BinanceTradingCredentialBootstrap, ApplicationArguments (+9 more)

### Community 25 - ".add"
Cohesion: 0.27
Nodes (5): WatchlistEntry, SpringBootTest, Test, Transactional, WatchlistServiceTest

### Community 26 - "order/api.ts"
Cohesion: 0.11
Nodes (26): RFC-6266, AuditEntry, AuditEntryPage, fetchAuditEntries(), SignalCall, SignalRuleId, AuditTrail(), describeError() (+18 more)

### Community 27 - "PriceChart.tsx"
Cohesion: 0.18
Nodes (18): Broker, ChartDataResponse, ChartIndicatorPoint, CandlestickPoint, LinePoint, toBusinessDay(), toCandlestickSeries(), toMaLongSeries() (+10 more)

### Community 28 - "FakeAlpacaTradingServer"
Cohesion: 0.19
Nodes (11): BeforeEach, FakeAlpacaTradingServer, FakeOrder, ClientHttpRequest, ClientHttpResponse, HttpStatus, ObjectMapper, RestClient (+3 more)

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
Cohesion: 0.13
Nodes (14): BigDecimalIndicators, IndicatorComputation, MacdResult, MovingAverageResult, AutoConfigureMockMvc, MockMvc, Test, WebMvcTest (+6 more)

### Community 34 - "RiskConsentEvent"
Cohesion: 0.24
Nodes (5): Entity, Override, PrePersist, Table, RiskConsentEvent

### Community 35 - ".getChartData"
Cohesion: 0.09
Nodes (11): ChartIndicatorPoint, ChartDataResponse, MarketClosedException, IndicatorControllerTest, AutoConfigureMockMvc, MockMvc, Test, WebMvcTest (+3 more)

### Community 36 - ".calculate"
Cohesion: 0.24
Nodes (5): MathContext, MovingAverageResult, MovingAverageCrossoverCalculator, Test, MovingAverageCrossoverCalculatorTest

### Community 37 - "OrderServiceTest"
Cohesion: 0.31
Nodes (5): PlaceOrderRequest, SignalComputation, ExtendWith, Test, OrderServiceTest

### Community 38 - "KillSwitchService"
Cohesion: 0.06
Nodes (31): EngageKillSwitchResponse, GetMapping, PostMapping, RequestMapping, RestController, KillSwitchController, Entity, Override (+23 more)

### Community 39 - "compilerOptions"
Cohesion: 0.10
Nodes (19): compilerOptions, allowImportingTsExtensions, erasableSyntaxOnly, lib, module, moduleDetection, noEmit, noFallthroughCasesInSwitch (+11 more)

### Community 40 - ".calculate"
Cohesion: 0.24
Nodes (5): AdxCalculator, MathContext, AdxCalculatorTest, Candle, Test

### Community 41 - "WatchlistService"
Cohesion: 0.19
Nodes (5): Query, WatchlistEntryRepository, Service, Transactional, WatchlistService

### Community 42 - "IndicatorService"
Cohesion: 0.13
Nodes (15): IndicatorService, Service, Component, ConditionalOnProperty, Logger, Scheduled, WatchlistSignalPoller, SignalCallEntryRepository (+7 more)

### Community 43 - "run skill (project override)"
Cohesion: 0.14
Nodes (19): run skill (project override), MarketHoursService (hardcoded NYSE/NASDAQ calendar), oracle-xe Docker Compose service, F1.1 Local dev environment, E1-F1-S1 Docker Compose file for Oracle XE, E1-F1-S2 Spring Boot backend skeleton, E1-F1-S3 React app skeleton with routing, E1-F1-S4 CI pipeline builds/tests both apps (+11 more)

### Community 44 - "NotificationServiceTest"
Cohesion: 0.27
Nodes (3): ExtendWith, Test, NotificationServiceTest

### Community 45 - "security-review skill"
Cohesion: 0.21
Nodes (17): guardrail-check skill, security-review skill, simplify skill, F1.3 Secrets & config management, E1-F3-S1 Broker API keys encrypted at rest, E1-F3-S2 Dashboard requires login, F4.2 Alpaca adapter (stocks), F6.1 Paper/live mode toggle (+9 more)

### Community 46 - "apiFetch"
Cohesion: 0.18
Nodes (20): apiFetch(), readCookie(), AssetType, fetchPriceHistory(), MarketDataErrorCode, parseMarketDataError(), PriceHistoryResponse, fetchNotifications() (+12 more)

### Community 47 - "FakeBinanceFuturesTradingServer"
Cohesion: 0.29
Nodes (7): FakeBinanceFuturesTradingServer, ClientHttpRequest, ClientHttpResponse, HttpStatus, StoredAlgoOrder, StoredOrder, MultiValueMap

### Community 48 - "WatchlistEntry"
Cohesion: 0.20
Nodes (5): Entity, Override, PrePersist, Table, WatchlistEntry

### Community 49 - "SignalCallEntry"
Cohesion: 0.14
Nodes (5): Entity, Override, PrePersist, Table, SignalCallEntry

### Community 50 - "general-purpose agent (implementation)"
Cohesion: 0.24
Nodes (16): Explore agent (research), general-purpose agent (implementation), Plan agent (design gate), Mandatory per-story workflow (update CLAUDE.md, commit, push), CLAUDE.md project status & architecture log, E1 — Platform Foundation, F1.2 Core data model, F1.4 Testing strategy (+8 more)

### Community 51 - "TradeOrderResponse"
Cohesion: 0.27
Nodes (7): PostMapping, RequestMapping, ResponseEntity, RestController, OrderController, JsonInclude, TradeOrderResponse

### Community 52 - "Candle"
Cohesion: 0.16
Nodes (6): Candle, FixtureSplits, E2ECandleFixtures, Candle, IndicatorTestFixtures, Candle

### Community 53 - "PerSymbolMaCrossoverSeparationCalibrationTest"
Cohesion: 0.36
Nodes (3): Test, PerSymbolMaCrossoverSeparationCalibrationTest, SymbolFixture

### Community 54 - "PriceHistoryResult"
Cohesion: 0.42
Nodes (5): PriceHistoryResult, Candle, ExtendWith, Test, LiveSignalDriftServiceTest

### Community 55 - "adapter-contract-check skill"
Cohesion: 0.18
Nodes (15): adapter-contract-check skill, BrokerAdapter interface (E4-F1-S1), OrderService.submitOrder (bracket order construction), RetryingBrokerAdapter decorator (retry/backoff/outage), TradingModeService (paper/live switch, append-only), F4.1 Adapter interface, E4-F1-S1 BrokerAdapter interface + mock + contract suite, E4-F1-S2 Rate-limit/retry/backoff in adapter contract (+7 more)

### Community 56 - "WatchlistController"
Cohesion: 0.25
Nodes (8): GetMapping, PostMapping, RequestMapping, ResponseEntity, RestController, WatchlistController, WatchlistEntryResponse, DeleteMapping

### Community 57 - "TradingModeResponse"
Cohesion: 0.22
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
Cohesion: 0.17
Nodes (11): JsonInclude, NotificationResponse, NotificationType, ORDER_CANCELLED, ORDER_FAILED, ORDER_FILLED, ORDER_PARTIALLY_FILLED, ORDER_PARTIALLY_PROTECTED (+3 more)

### Community 64 - "BrokerOrderResult"
Cohesion: 0.24
Nodes (4): BrokerOrderResult, KillSwitchCancelSummary, BrokerAdapterRouterTest, Test

### Community 65 - "WatchlistControllerTest"
Cohesion: 0.25
Nodes (7): AddWatchlistEntryRequest, AutoConfigureMockMvc, MockMvc, ObjectMapper, Test, WebMvcTest, WatchlistControllerTest

### Community 66 - "RiskLimitConfig.java"
Cohesion: 0.83
Nodes (3): Configuration, EnableConfigurationProperties, RiskLimitConfig

### Community 67 - "PerSymbolAdxTrendingThresholdCalibrationTest"
Cohesion: 0.39
Nodes (3): Test, PerSymbolAdxTrendingThresholdCalibrationTest, SymbolFixture

### Community 68 - "OrderServiceTest.java"
Cohesion: 0.09
Nodes (17): BrokerAdapterAmbiguousOrderException, BrokerAdapterException, BrokerAdapterRateLimitedException, BrokerAdapterTransientException, BrokerAdapterUnavailableException, ChartDataResponse, IndicatorResponse, MovingAverageRelation (+9 more)

### Community 70 - "LiveSignalDriftService.java"
Cohesion: 0.19
Nodes (10): Component, ConditionalOnProperty, Logger, Scheduled, LiveSignalDriftService, Page, Pageable, Query (+2 more)

### Community 71 - "PerSymbolMacdHistogramMagnitudeCalibrationTest"
Cohesion: 0.32
Nodes (3): Test, PerSymbolMacdHistogramMagnitudeCalibrationTest, SymbolFixture

### Community 72 - "Ticker"
Cohesion: 0.10
Nodes (9): Autowired, Entity, Override, PrePersist, Table, Ticker, TickerResponse, TickerNotRegisteredException (+1 more)

### Community 73 - "NotificationController"
Cohesion: 0.29
Nodes (6): GetMapping, PostMapping, RequestMapping, ResponseEntity, RestController, NotificationController

### Community 74 - "OrderControllerTest"
Cohesion: 0.18
Nodes (8): KillSwitchEngagedException, RiskLimitExceededException, AutoConfigureMockMvc, MockMvc, ObjectMapper, Test, WebMvcTest, OrderControllerTest

### Community 75 - "MacdHistogramMagnitudeCalibrationTest"
Cohesion: 0.36
Nodes (3): Test, MacdHistogramMagnitudeCalibrationTest, SymbolFixture

### Community 76 - "NotificationControllerTest"
Cohesion: 0.26
Nodes (5): AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, NotificationControllerTest

### Community 77 - "SignalDriftControllerIntegrationTest"
Cohesion: 0.27
Nodes (8): AutoConfigureMockMvc, Cookie, MockHttpSession, MockMvc, SpringBootTest, Test, TestPropertySource, SignalDriftControllerIntegrationTest

### Community 78 - "TickerMetrics.tsx"
Cohesion: 0.10
Nodes (24): fetchChartData(), registerTicker(), TickerSummary, Broker, fetchSignal(), HoldTerm, IndicatorResponse, MacdResult (+16 more)

### Community 79 - ".findFirstCrossing"
Cohesion: 0.28
Nodes (5): CrossingEvent, WalkForwardScorer, BacktestHarnessTpSlTest, Candle, Test

### Community 81 - "mvnw"
Cohesion: 0.33
Nodes (6): mvnw script, clean(), die(), exec_maven(), set_java_home(), verbose()

### Community 82 - "Changelog"
Cohesion: 0.03
Nodes (77): Changelog, Dark-first premium visual pass, E1-F1-S1 — local Oracle XE via Docker Compose, E1-F1-S2 — Spring Boot backend skeleton, E1-F1-S3 — React app skeleton, E1-F1-S4 — CI pipeline, E1-F1-S5 — env/config profiles, E1-F2 — core data model (+69 more)

### Community 83 - "PerSymbolRsiOverboughtCalibrationTest"
Cohesion: 0.35
Nodes (3): Test, PerSymbolRsiOverboughtCalibrationTest, SymbolFixture

### Community 84 - "AuthController.java"
Cohesion: 0.39
Nodes (6): Authentication, AuthController, GetMapping, RequestMapping, ResponseEntity, RestController

### Community 85 - "NotificationExceptionHandler.java"
Cohesion: 0.29
Nodes (6): InvalidNotificationRequestException, ExceptionHandler, Logger, ResponseEntity, RestControllerAdvice, NotificationExceptionHandler

### Community 86 - "IndicatorId"
Cohesion: 0.14
Nodes (10): IndicatorId, MA_CROSSOVER, MACD, RSI, HorizonCandidate, IndicatorExpectancyAlternateHorizonCalibrationTest, Test, Test (+2 more)

### Community 87 - "TradingModeExceptionHandler.java"
Cohesion: 0.29
Nodes (6): RiskConsentNotGivenException, ExceptionHandler, Logger, ResponseEntity, RestControllerAdvice, TradingModeExceptionHandler

### Community 88 - "plugins"
Cohesion: 0.22
Nodes (8): plugins, rules, react/only-export-components, react/rules-of-hooks, $schema, oxc, typescript, warn

### Community 90 - "TradingModeService"
Cohesion: 0.23
Nodes (5): RiskConsentEventRepository, TradingModeEventRepository, Service, TradingModeService, JpaRepository

### Community 91 - "signal-rule-review skill"
Cohesion: 0.43
Nodes (8): signal-rule-review skill, BacktestHarness (walk-forward JUnit validation), HoldTermCalculator (trend strength x volatility band), SignalRuleEngine (Buy/Sell/Hold rule table), F2.3 Buy/Sell/Hold signal & hold-term, E2-F3-S2 Suggested hold-term alongside the call, F2.4 Backtesting, E2-F4-S1 Backtest rule table against historical data

### Community 92 - "dataviz skill"
Cohesion: 0.38
Nodes (7): dataviz skill, SignalBadge colorblind-safe teal/orange/slate palette, F3.1 Ticker lookup & metrics display, E3-F1-S1 Ticker lookup + stat-tile metrics, E3-F1-S2 Buy/Sell/Hold badge color-coded, F3.2 Metric visualization, E3-F2-S1 Price chart with MA/RSI overlays

### Community 93 - "NotificationService"
Cohesion: 0.18
Nodes (6): Pageable, NotificationRepository, Logger, Service, NotificationService, BeforeEach

### Community 94 - "ClockConfig.java"
Cohesion: 0.60
Nodes (3): ClockConfig, Bean, Configuration

### Community 95 - ".export"
Cohesion: 0.23
Nodes (3): OrderCsvExporter, Test, OrderCsvExporterTest

### Community 96 - "BackendApplicationTests.java"
Cohesion: 0.60
Nodes (3): BackendApplicationTests, SpringBootTest, Test

### Community 97 - "OrderService"
Cohesion: 0.14
Nodes (10): BrokerAdapter, BrokerAdapterRouter, Service, Pageable, Query, OrderRepository, Logger, Service (+2 more)

### Community 98 - "OrderAuditControllerIntegrationTest"
Cohesion: 0.29
Nodes (7): AutoConfigureMockMvc, Cookie, MockHttpSession, MockMvc, SpringBootTest, Test, OrderAuditControllerIntegrationTest

### Community 99 - "TickerController"
Cohesion: 0.39
Nodes (5): PostMapping, RequestMapping, ResponseEntity, RestController, TickerController

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
Cohesion: 0.16
Nodes (5): CheckpointStats, DirectionalOutcomeStats, CheckpointStatsTest, Test, IndicatorExpectancyCalibrationTest

### Community 106 - ".run"
Cohesion: 0.10
Nodes (17): DirectionalOutcome, LOSS, WASH, WIN, DirectionalScoreResult, ExitReason, HORIZON_EXPIRED, SL_HIT (+9 more)

### Community 107 - ".forSymbol"
Cohesion: 0.31
Nodes (3): PerSymbolRuleThresholds, Test, PerSymbolRuleThresholdsTest

### Community 108 - "TickerControllerTest"
Cohesion: 0.32
Nodes (7): RegisterTickerRequest, AutoConfigureMockMvc, MockMvc, ObjectMapper, Test, WebMvcTest, TickerControllerTest

### Community 117 - "killswitch/api.ts"
Cohesion: 0.32
Nodes (10): clearKillSwitch(), engageKillSwitch(), EngageKillSwitchResponse, fetchKillSwitchState(), KillSwitchCancelSummary, KillSwitchResponse, KillSwitchState, describeError() (+2 more)

### Community 118 - ".calculate"
Cohesion: 0.26
Nodes (4): MathContext, VolatilityCalculator, Test, VolatilityCalculatorTest

### Community 119 - "MarketDataController"
Cohesion: 0.27
Nodes (5): GetMapping, RequestMapping, RestController, MarketDataController, PriceHistoryResponse

### Community 120 - ".getPriceHistory"
Cohesion: 0.19
Nodes (9): MarketDataClient, Service, MarketDataService, Component, MarketHoursService, BeforeEach, ExtendWith, Test (+1 more)

### Community 121 - "TickerService"
Cohesion: 0.23
Nodes (7): Service, Transactional, TickerService, SpringBootTest, Test, Transactional, TickerServiceTest

### Community 122 - "TradingModeBanner.tsx"
Cohesion: 0.42
Nodes (8): fetchTradingMode(), giveRiskConsent(), switchTradingMode(), TradingMode, TradingModeState, describeError(), otherMode(), TradingModeBanner()

### Community 123 - "SecurityConfigTest"
Cohesion: 0.32
Nodes (6): AutoConfigureMockMvc, Cookie, MockMvc, SpringBootTest, Test, SecurityConfigTest

### Community 124 - "SignalRuleId"
Cohesion: 0.12
Nodes (20): AuditEntryResponse, JsonInclude, HoldTerm, SignalCall, BUY, HOLD, SELL, SignalRuleEngine (+12 more)

### Community 126 - "CredentialEncryptionService"
Cohesion: 0.33
Nodes (5): CredentialEncryptionService, Component, Logger, Broker-credential encryption key rotation procedure, SecretKeySpec

### Community 127 - "E8-F2-S3 — funding-rate carry cost in the backtest's transaction-cost model"
Cohesion: 0.33
Nodes (6): Design gate, E8-F2-S3 — funding-rate carry cost in the backtest's transaction-cost model, Illustrative figures (not a ship/no-ship story — purely additive), Implementation, Scope / no-op confirmation, Test coverage

### Community 128 - "SignalDriftController"
Cohesion: 0.29
Nodes (6): ConditionalOnProperty, GetMapping, RequestMapping, RestController, SignalDriftController, SignalDriftReport

### Community 130 - ".readDecrypted"
Cohesion: 0.24
Nodes (5): Transactional, BrokerCredentialServiceRotationTest, SpringBootTest, Test, Transactional

### Community 131 - "RuleThresholds"
Cohesion: 0.35
Nodes (4): RuleThresholds, Test, NamedCandidate, ThresholdCalibrationTest

### Community 132 - "Backing up and restoring the Oracle instance (E7-F3-S1)"
Cohesion: 0.40
Nodes (4): Backing up and restoring the Oracle instance (E7-F3-S1), Backup procedure, Notes, Restore-test procedure

### Community 133 - "BrokerCredentialServiceFindTest.java"
Cohesion: 0.39
Nodes (4): BrokerCredentialServiceFindTest, SpringBootTest, Test, Transactional

### Community 134 - ".calculate"
Cohesion: 0.21
Nodes (5): MacdResult, MathContext, MacdCalculator, Test, MacdCalculatorTest

### Community 135 - "BacktestHarnessTest"
Cohesion: 0.31
Nodes (3): Candle, BacktestHarnessTest, Test

### Community 138 - "Notification system + WatchlistSignalPoller"
Cohesion: 0.40
Nodes (5): Notification system + WatchlistSignalPoller, Watchlist feature (watchlist_entries), F3.3 Watchlist (stretch), E3-F3-S1 Watchlist persisted in Oracle DB, F5.4 Notifications

### Community 139 - "E8-F1-S10 — per-symbol MA-crossover separation calibration, evaluated and no-shipped"
Cohesion: 0.50
Nodes (4): Design, E8-F1-S10 — per-symbol MA-crossover separation calibration, evaluated and no-shipped, Implementation, Scope / no-op confirmation

### Community 142 - "MaCrossoverSeparationCalibrationTest"
Cohesion: 0.36
Nodes (3): Test, MaCrossoverSeparationCalibrationTest, SymbolFixture

### Community 144 - "E8-F1-S7 — evaluate the per-symbol RSI override and SELL-side regime gate against a stock fixture"
Cohesion: 0.40
Nodes (5): Design gate, E8-F1-S7 — evaluate the per-symbol RSI override and SELL-side regime gate against a stock fixture, Findings — no ship on either axis, but not for the same reason as any prior E8-F1 no-ship, Implementation, Scope / no-op confirmation

### Community 145 - "OrderStatus"
Cohesion: 0.17
Nodes (10): OrderStatus, CANCELLED, FAILED, FILLED, PARTIALLY_FILLED, PARTIALLY_PROTECTED, PENDING, REJECTED (+2 more)

### Community 149 - "IndicatorController"
Cohesion: 0.39
Nodes (4): IndicatorController, GetMapping, RequestMapping, RestController

### Community 151 - "E8-F1-S8 — per-symbol `macdMinHistogramMagnitudePct` calibration on the BUY side"
Cohesion: 0.29
Nodes (7): Design gate, E8-F1-S8 — per-symbol `macdMinHistogramMagnitudePct` calibration on the BUY side, Findings, Fixture fallout, Implementation, Scope / no-op confirmation, Test coverage

### Community 153 - "OrderQueryControllerTest"
Cohesion: 0.14
Nodes (5): AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, OrderQueryControllerTest

### Community 154 - "PerSymbolAdxThresholds"
Cohesion: 0.31
Nodes (3): PerSymbolAdxThresholds, Test, PerSymbolAdxThresholdsTest

### Community 157 - "RegimeClassifier"
Cohesion: 0.17
Nodes (5): RegimeClassifier, Test, RegimeCalibrationTest, Test, StockRegimeOutOfSampleValidationTest

### Community 160 - "MarketDataControllerTest"
Cohesion: 0.27
Nodes (5): AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, MarketDataControllerTest

### Community 162 - "SellMacdHistogramMagnitudeCalibrationTest"
Cohesion: 0.40
Nodes (3): Test, SellMacdHistogramMagnitudeCalibrationTest, SymbolFixture

### Community 163 - "DecryptedCredential"
Cohesion: 0.25
Nodes (5): DecryptedCredential, Override, BeforeEach, RestClient, BeforeEach

### Community 165 - ".calculate"
Cohesion: 0.30
Nodes (3): VolumeTrendCalculator, Test, VolumeTrendCalculatorTest

### Community 166 - ".calculate"
Cohesion: 0.29
Nodes (4): MathContext, RsiCalculator, Test, RsiCalculatorTest

### Community 169 - "E8-F1-S9 — SELL-only MACD histogram-magnitude gate, evaluated and no-shipped"
Cohesion: 0.50
Nodes (4): Design, E8-F1-S9 — SELL-only MACD histogram-magnitude gate, evaluated and no-shipped, Implementation, Scope / no-op confirmation

## Knowledge Gaps
- **296 isolated node(s):** `com.autotrade.dashboard:backend`, `MIN`, `MID`, `MAX`, `WIN` (+291 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **22 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Candle` connect `Candle` to `RuleThresholds`, `.calculate`, `AlpacaMarketDataClient`, `OrderAuditEntry`, `BacktestHarnessTest`, `TradingMode`, `MaCrossoverSeparationCalibrationTest`, `BinanceMarketDataClient`, `BacktestReport`, `RsiOversoldRecalibrationTest`, `StockPerSymbolRsiOverboughtCalibrationTest`, `RegimeClassifier`, `MarketDataControllerTest`, `SellMacdHistogramMagnitudeCalibrationTest`, `.getChartData`, `.calculate`, `.calculate`, `.calculate`, `.calculate`, `IndicatorService`, `PerSymbolMaCrossoverSeparationCalibrationTest`, `PriceHistoryResult`, `Checkpoint`, `PerSymbolAdxTrendingThresholdCalibrationTest`, `OrderServiceTest.java`, `LiveSignalDriftService.java`, `PerSymbolMacdHistogramMagnitudeCalibrationTest`, `Ticker`, `MacdHistogramMagnitudeCalibrationTest`, `.findFirstCrossing`, `RegimeOutOfSampleValidationTest`, `PerSymbolRsiOverboughtCalibrationTest`, `IndicatorId`, `WeightedVoteBacktestTest`, `DirectionalOutcomeStats`, `.run`, `.calculate`, `MarketDataController`, `.getPriceHistory`, `SignalRuleId`?**
  _High betweenness centrality (0.092) - this node is a cross-community bridge._
- **Why does `BrokerCredentialService` connect `TradingMode` to `BinanceFuturesTradingAdapter`, `.readDecrypted`, `DecryptedCredential`, `AlpacaTradingAdapter`, `OrderServiceTest`, `OrderServiceTest.java`, `OrderService`, `BrokerCredentialServiceFindTest.java`, `BrokerAdapterContractTest`, `OrderAuditEntryRepositoryTest.java`, `.run`, `BrokerAdapterConfig.java`, `BinanceFuturesAdapterConfig.java`, `CredentialEncryptionService`?**
  _High betweenness centrality (0.076) - this node is a cross-community bridge._
- **Why does `TradingMode` connect `TradingMode` to `MockBrokerAdapter`, `BinanceFuturesTradingAdapter`, `.readDecrypted`, `AlpacaTradingAdapter`, `Order`, `BrokerCredentialServiceFindTest.java`, `BrokerAdapterContractTest`, `TradingModeControllerTest`, `OrderAuditEntryRepositoryTest.java`, `TradingModeServiceTest`, `OrderStatus`, `.listAuditEntries_mapsRepositoryPageToPagedResponseOfAuditEntryResponse`, `TradingModeEvent`, `.run`, `OrderQueryControllerTest`, `OrderServiceTest`, `TradingModeResponse`, `BrokerAdapterConfig.java`, `BinanceFuturesAdapterConfig.java`, `BrokerOrderResult`, `OrderServiceTest.java`, `TradingModeService`, `OrderService`, `SignalRuleId`?**
  _High betweenness centrality (0.070) - this node is a cross-community bridge._
- **Are the 6 inferred relationships involving `Candle` (e.g. with `.chartData_marketClosedWithCachedFallback_returns200Stale()` and `.chartData_registeredTicker_returns200WithCandlesAndIndicators()`) actually correct?**
  _`Candle` has 6 INFERRED edges - model-reasoned connections that need verification._
- **What connects `com.autotrade.dashboard:backend`, `MIN`, `MID` to the rest of the system?**
  _296 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `MockBrokerAdapter` be split into smaller, more focused modules?**
  _Cohesion score 0.06728395061728396 - nodes in this community are weakly interconnected._
- **Should `BinanceFuturesTradingAdapter` be split into smaller, more focused modules?**
  _Cohesion score 0.0564274378416505 - nodes in this community are weakly interconnected._