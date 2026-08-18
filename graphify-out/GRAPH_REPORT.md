# Graph Report - agent testing  (2026-08-18)

## Corpus Check
- 361 files · ~209,909 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 3206 nodes · 9034 edges · 179 communities (148 shown, 31 thin omitted)
- Extraction: 87% EXTRACTED · 13% INFERRED · 0% AMBIGUOUS · INFERRED: 1198 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `ce10ef28`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- BinanceFuturesTradingAdapter
- Changelog
- KillSwitchService
- DecryptedCredential
- AlpacaTradingAdapter
- SignalCallEntry
- CheckpointStats
- SignalRuleId
- AlpacaMarketDataClient
- devDependencies
- Test
- OrderServiceTest.java
- .calculate
- TradingMode
- Order
- LiveDriftBaselineTest
- order/api.ts
- RegimeOutOfSampleValidationTest
- SignalServiceTest
- Candle
- .run
- SecurityConfig.java
- IndicatorSnapshot
- WatchlistSignalPollerTest
- Override
- OrderQueryControllerTest
- OrderAuditEntryRepositoryTest.java
- DashboardPage.tsx
- .run
- .evaluate
- TickerNotRegisteredException
- OutOfSampleValidationTest
- OrderServiceTest
- BinanceMarketDataClient
- MarketDataService
- OrderControllerTest
- BrokerOrderResult
- TickerMetrics.tsx
- MarketDataExceptionHandler
- Ticker
- TradingModeServiceTest
- OrderAuditEntry
- apiFetch
- PriceChart.tsx
- .getPriceHistory
- Notification
- RiskLimitService
- compilerOptions
- OrderExceptionHandler
- MarketHoursServiceTest
- BrokerOrderRequest
- .findFirstCrossing
- NotificationServiceTest
- .readDecrypted
- compilerOptions
- NotificationService
- FakeBinanceFuturesTradingServer
- run skill (project override)
- TradeForm.tsx
- .calculate
- LiveSignalDriftServiceTest
- CredentialEncryptionServiceTest
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
- RetryingBrokerAdapter
- .getChartData
- .calculate
- TradingModeEvent
- WatchlistEntry
- adapter-contract-check skill
- BrokerAdapterConfig.java
- BinanceFuturesAdapterConfig.java
- SignalDriftControllerIntegrationTest
- E8-F1-S7 — evaluate the per-symbol RSI override and SELL-side regime gate against a stock fixture
- .calculate
- .applySellGate
- StockMaCrossoverSeparationCalibrationTest
- NotificationType
- .switchTo
- TradingModeController
- WatchlistController
- DirectionalOutcomeStats
- OrderAuditControllerIntegrationTest
- NotificationController
- OrderStatus
- TickerControllerTest
- RiskConsentEvent
- PerSymbolRsiOverboughtCalibrationTest
- ThresholdCalibrationTest
- FakeAlpacaTradingServer
- NotificationControllerTest
- SecurityConfigTest
- killswitch/api.ts
- SignalController.java
- ApiErrorResponse
- StockPerSymbolMacdHistogramMagnitudeCalibrationTest
- .getAccountStatus
- HoldTermRangeCalibrationTest.java
- .calculate
- TradingModeExceptionHandler.java
- Checkpoint
- MacdHistogramMagnitudeCalibrationTest
- MaCrossoverSeparationCalibrationTest
- PerSymbolMaCrossoverSeparationCalibrationTest
- RsiOverboughtRecalibrationTest
- BacktestReport
- StockPerSymbolRsiOverboughtCalibrationTest
- plugins
- mvnw
- .calculate
- OrderService
- .placeOrder
- NotificationExceptionHandler.java
- E8-F1-S11 — SELL-only MA-crossover separation gate, evaluated and shipped
- SellMacdHistogramMagnitudeCalibrationTest
- SellMaCrossoverSeparationCalibrationTest
- CredentialEncryptionService
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
- PerSymbolAdxTrendingThresholdCalibrationTest
- MarketDataControllerTest.java
- dataviz skill
- TradingModeService
- ClockConfig.java
- E8-F1-S9 — SELL-only MACD histogram-magnitude gate, evaluated and no-shipped
- .switchTo_live_belowThreshold_throwsPaperTradeThresholdNotMetException_noHistoryPersisted
- BackendApplicationTests.java
- E8-F1-S12 — AAPL evaluated against the MACD-magnitude and MA-crossover-separation axes
- E8-F1-S10 — per-symbol MA-crossover separation calibration, evaluated and no-shipped
- CoreDataModelIntegrationTest
- Backing up and restoring the Oracle instance (E7-F3-S1)
- BackendApplication
- .handleUnavailable
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
- .bullishCandles
- E8-F6-S1 — HoldTermRule day-range calibration (no ship, all 6 branches)
- MarketDataController
- SignalDriftController
- RegimeCalibrationTest
- E8-F5-S2 — funding-adjusted live signal-drift monitoring
- OrderRepository
- MockBrokerAdapterContractTest
- RetryingMockBrokerAdapterContractTest
- .findAllByOrderByLoggedAtDesc
- Notification system + WatchlistSignalPoller

## God Nodes (most connected - your core abstractions)
1. `Candle` - 147 edges
2. `TradingMode` - 125 edges
3. `Ticker` - 111 edges
4. `Broker` - 107 edges
5. `AssetType` - 95 edges
6. `Order` - 93 edges
7. `Changelog` - 87 edges
8. `SignalRuleId` - 68 edges
9. `RuleThresholds` - 64 edges
10. `IndicatorSnapshot` - 62 edges

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

## Communities (179 total, 31 thin omitted)

### Community 0 - "BinanceFuturesTradingAdapter"
Cohesion: 0.06
Nodes (23): Credentials, BinanceAccountAsset, BinanceAccountResponse, BinanceAlgoOrderResponse, BinanceErrorResponse, BinanceFuturesTradingAdapter, BinanceOrderResponse, BinancePositionResponse (+15 more)

### Community 1 - "Changelog"
Cohesion: 0.03
Nodes (77): Changelog, Dark-first premium visual pass, E1-F1-S1 — local Oracle XE via Docker Compose, E1-F1-S2 — Spring Boot backend skeleton, E1-F1-S3 — React app skeleton, E1-F1-S4 — CI pipeline, E1-F1-S5 — env/config profiles, E1-F2 — core data model (+69 more)

### Community 2 - "KillSwitchService"
Cohesion: 0.06
Nodes (31): EngageKillSwitchResponse, GetMapping, PostMapping, RequestMapping, RestController, KillSwitchController, Entity, Override (+23 more)

### Community 3 - "DecryptedCredential"
Cohesion: 0.15
Nodes (8): DecryptedCredential, Override, BeforeEach, BeforeEach, RestClient, RestClient, BeforeEach, BeforeEach

### Community 4 - "AlpacaTradingAdapter"
Cohesion: 0.08
Nodes (24): AlpacaAccountResponse, AlpacaBracketLeg, AlpacaErrorResponse, AlpacaOrderRequestBody, AlpacaOrderResponse, AlpacaPositionResponse, AlpacaStopLeg, AlpacaTradingAdapter (+16 more)

### Community 5 - "SignalCallEntry"
Cohesion: 0.14
Nodes (5): Entity, Override, PrePersist, Table, SignalCallEntry

### Community 6 - "CheckpointStats"
Cohesion: 0.13
Nodes (12): CheckpointStats, IndicatorId, MA_CROSSOVER, MACD, RSI, CheckpointStatsTest, Test, HorizonCandidate (+4 more)

### Community 7 - "SignalRuleId"
Cohesion: 0.15
Nodes (17): SignalCall, BUY, HOLD, SELL, RuleThresholds, SignalRuleEngine, call(), SignalRuleId (+9 more)

### Community 8 - "AlpacaMarketDataClient"
Cohesion: 0.08
Nodes (23): AlpacaBar, AlpacaBarsResponse, AlpacaMarketDataClient, Candle, Component, JsonIgnoreProperties, Override, RestClient (+15 more)

### Community 9 - "devDependencies"
Cohesion: 0.05
Nodes (36): dependencies, lightweight-charts, react, react-dom, react-router-dom, devDependencies, oxlint, @types/node (+28 more)

### Community 10 - "Test"
Cohesion: 0.11
Nodes (8): Regime, RANGING, TRENDING, RegimeGatedRuleEngine, Test, RegimeClassifierTest, Test, RegimeGatedRuleEngineTest

### Community 11 - "OrderServiceTest.java"
Cohesion: 0.09
Nodes (25): IndicatorResponse, BigDecimalIndicators, IndicatorService, Service, MacdResult, MovingAverageRelation, EQUAL, SHORT_ABOVE_LONG (+17 more)

### Community 12 - ".calculate"
Cohesion: 0.06
Nodes (24): HoldTermRule, MODERATE_HIGH, MODERATE_LOW, MODERATE_MEDIUM, STRONG_HIGH, STRONG_LOW, STRONG_MEDIUM, match() (+16 more)

### Community 13 - "TradingMode"
Cohesion: 0.11
Nodes (24): Autowired, BrokerCredential, Entity, PrePersist, PreUpdate, Table, BrokerCredentialRepository, BrokerCredentialService (+16 more)

### Community 14 - "Order"
Cohesion: 0.06
Nodes (8): Entity, Override, PrePersist, PreUpdate, Table, Order, Test, BeforeEach

### Community 16 - "order/api.ts"
Cohesion: 0.11
Nodes (26): RFC-6266, AuditEntry, AuditEntryPage, fetchAuditEntries(), SignalCall, SignalRuleId, AuditTrail(), describeError() (+18 more)

### Community 18 - "SignalServiceTest"
Cohesion: 0.22
Nodes (7): IndicatorComputation, IndicatorFactory, ExtendWith, FunctionalInterface, IndicatorResponse, Test, SignalServiceTest

### Community 19 - "Candle"
Cohesion: 0.13
Nodes (8): MathContext, RsiCalculator, Candle, FixtureSplits, IndicatorTestFixtures, Candle, Test, RsiCalculatorTest

### Community 20 - ".run"
Cohesion: 0.13
Nodes (11): BacktestHarness, HoldGateAccumulator, IndicatorAccumulator, FunctionalInterface, RuleEvaluator, HoldGateOutcome, LARGE_MOVE, STABLE (+3 more)

### Community 21 - "SecurityConfig.java"
Cohesion: 0.11
Nodes (22): CsrfCookieWriteFilter, Bean, Configuration, Logger, Override, PasswordEncoder, SecurityConfig, SpaCsrfTokenRequestHandler (+14 more)

### Community 22 - "IndicatorSnapshot"
Cohesion: 0.08
Nodes (5): IndicatorSnapshot, Entity, Override, PrePersist, Table

### Community 23 - "WatchlistSignalPollerTest"
Cohesion: 0.22
Nodes (9): Component, ConditionalOnProperty, Logger, Scheduled, WatchlistSignalPoller, BeforeEach, ExtendWith, Test (+1 more)

### Community 25 - "OrderQueryControllerTest"
Cohesion: 0.09
Nodes (13): Page, PagedResponse, GetMapping, PostMapping, RequestMapping, ResponseEntity, RestController, OrderQueryController (+5 more)

### Community 26 - "OrderAuditEntryRepositoryTest.java"
Cohesion: 0.14
Nodes (12): IndicatorSnapshotRepository, AuditEntryResponse, JsonInclude, HoldTerm, BacktestDecisionPoint, SpringBootTest, Transactional, TickerSignalOrderE2ETest (+4 more)

### Community 27 - "DashboardPage.tsx"
Cohesion: 0.12
Nodes (21): App(), AuthContext, AuthContextValue, AuthProvider(), useAuth(), RequireAuth(), TabItem, Tabs() (+13 more)

### Community 28 - ".run"
Cohesion: 0.11
Nodes (17): ApplicationRunner, AlpacaTradingCredentialBootstrap, ApplicationArguments, Component, Logger, Override, BinanceTradingCredentialBootstrap, ApplicationArguments (+9 more)

### Community 29 - ".evaluate"
Cohesion: 0.09
Nodes (9): IndicatorVotes, IndicatorWeights, WeightedVoteRuleEngine, MacdResult, Test, SignalRuleEngineTest, MacdResult, Test (+1 more)

### Community 30 - "TickerNotRegisteredException"
Cohesion: 0.15
Nodes (7): MarketClosedException, TickerNotRegisteredException, IndicatorControllerTest, AutoConfigureMockMvc, MockMvc, Test, WebMvcTest

### Community 32 - "OrderServiceTest"
Cohesion: 0.31
Nodes (5): PlaceOrderRequest, SignalComputation, ExtendWith, Test, OrderServiceTest

### Community 33 - "BinanceMarketDataClient"
Cohesion: 0.11
Nodes (12): BinanceMarketDataClient, Candle, Component, Override, RestClient, TooManyRequests, Logger, RetryHelper (+4 more)

### Community 34 - "MarketDataService"
Cohesion: 0.15
Nodes (9): MarketDataClient, Service, MarketDataService, Component, MarketHoursService, BeforeEach, ExtendWith, Test (+1 more)

### Community 35 - "OrderControllerTest"
Cohesion: 0.15
Nodes (10): JsonInclude, TradeOrderResponse, KillSwitchEngagedException, RiskLimitExceededException, AutoConfigureMockMvc, MockMvc, ObjectMapper, Test (+2 more)

### Community 36 - "BrokerOrderResult"
Cohesion: 0.24
Nodes (4): BrokerOrderResult, KillSwitchCancelSummary, BrokerAdapterRouterTest, Test

### Community 37 - "TickerMetrics.tsx"
Cohesion: 0.10
Nodes (24): fetchChartData(), registerTicker(), TickerSummary, Broker, fetchSignal(), HoldTerm, IndicatorResponse, MacdResult (+16 more)

### Community 38 - "MarketDataExceptionHandler"
Cohesion: 0.15
Nodes (10): InsufficientPriceHistoryException, InvalidPriceHistoryRequestException, ExceptionHandler, Logger, ResponseEntity, RestControllerAdvice, MarketDataExceptionHandler, NoPriceDataException (+2 more)

### Community 39 - "Ticker"
Cohesion: 0.15
Nodes (5): Entity, Override, PrePersist, Table, Ticker

### Community 40 - "TradingModeServiceTest"
Cohesion: 0.19
Nodes (5): SpringBootTest, Test, TestPropertySource, Transactional, TradingModeServiceTest

### Community 41 - "OrderAuditEntry"
Cohesion: 0.16
Nodes (5): Entity, Override, PrePersist, Table, OrderAuditEntry

### Community 42 - "apiFetch"
Cohesion: 0.18
Nodes (20): apiFetch(), readCookie(), AssetType, fetchPriceHistory(), MarketDataErrorCode, parseMarketDataError(), PriceHistoryResponse, fetchNotifications() (+12 more)

### Community 43 - "PriceChart.tsx"
Cohesion: 0.18
Nodes (18): Broker, ChartDataResponse, ChartIndicatorPoint, CandlestickPoint, LinePoint, toBusinessDay(), toCandlestickSeries(), toMaLongSeries() (+10 more)

### Community 44 - ".getPriceHistory"
Cohesion: 0.29
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

### Community 48 - "OrderExceptionHandler"
Cohesion: 0.13
Nodes (9): InvalidTradeRequestException, ExceptionHandler, Logger, ResponseEntity, RestControllerAdvice, OrderExceptionHandler, OrderNotFoundException, OrderRefreshUnavailableException (+1 more)

### Community 50 - "BrokerOrderRequest"
Cohesion: 0.16
Nodes (7): BrokerOrderRequest, Override, MockBrokerAdapter, MockOrderState, PositionState, Test, MockBrokerAdapterTest

### Community 51 - ".findFirstCrossing"
Cohesion: 0.28
Nodes (5): CrossingEvent, WalkForwardScorer, BacktestHarnessTpSlTest, Candle, Test

### Community 52 - "NotificationServiceTest"
Cohesion: 0.31
Nodes (3): ExtendWith, Test, NotificationServiceTest

### Community 53 - ".readDecrypted"
Cohesion: 0.22
Nodes (5): Transactional, BrokerCredentialServiceRotationTest, SpringBootTest, Test, Transactional

### Community 54 - "compilerOptions"
Cohesion: 0.10
Nodes (19): compilerOptions, allowImportingTsExtensions, erasableSyntaxOnly, lib, module, moduleDetection, noEmit, noFallthroughCasesInSwitch (+11 more)

### Community 55 - "NotificationService"
Cohesion: 0.21
Nodes (6): Pageable, NotificationRepository, Logger, Service, NotificationService, BeforeEach

### Community 56 - "FakeBinanceFuturesTradingServer"
Cohesion: 0.27
Nodes (8): FakeBinanceFuturesTradingServer, ClientHttpRequest, ClientHttpResponse, HttpStatus, StoredAlgoOrder, StoredOrder, MultiValueMap, URI

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
Cohesion: 0.37
Nodes (5): BeforeEach, Candle, ExtendWith, Test, LiveSignalDriftServiceTest

### Community 62 - ".resolveOrRegister"
Cohesion: 0.18
Nodes (7): AddWatchlistEntryRequest, Transactional, WatchlistEntry, SpringBootTest, Test, Transactional, WatchlistServiceTest

### Community 63 - "TickerService"
Cohesion: 0.11
Nodes (9): TickerRepository, Service, Transactional, TickerService, Query, WatchlistEntryRepository, Service, WatchlistService (+1 more)

### Community 64 - "security-review skill"
Cohesion: 0.19
Nodes (18): guardrail-check skill, security-review skill, simplify skill, F1.3 Secrets & config management, E1-F3-S1 Broker API keys encrypted at rest, E1-F3-S2 Dashboard requires login, F4.2 Alpaca adapter (stocks), F6.1 Paper/live mode toggle (+10 more)

### Community 65 - "Broker"
Cohesion: 0.09
Nodes (10): Broker, ALPACA, BINANCE, BrokerAccountStatus, BrokerAdapterAmbiguousOrderException, BrokerAdapterException, BrokerAdapterRateLimitedException, BrokerAdapterTransientException (+2 more)

### Community 66 - "TradingModeControllerTest"
Cohesion: 0.36
Nodes (5): AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, TradingModeControllerTest

### Community 67 - "WatchlistControllerTest"
Cohesion: 0.33
Nodes (6): AutoConfigureMockMvc, MockMvc, ObjectMapper, Test, WebMvcTest, WatchlistControllerTest

### Community 69 - ".export"
Cohesion: 0.23
Nodes (3): OrderCsvExporter, Test, OrderCsvExporterTest

### Community 70 - "general-purpose agent (implementation)"
Cohesion: 0.24
Nodes (16): Explore agent (research), general-purpose agent (implementation), Plan agent (design gate), Mandatory per-story workflow (update CLAUDE.md, commit, push), CLAUDE.md project status & architecture log, E1 — Platform Foundation, F1.2 Core data model, F1.4 Testing strategy (+8 more)

### Community 71 - "BacktestHarnessTest"
Cohesion: 0.32
Nodes (3): Candle, BacktestHarnessTest, Test

### Community 72 - "RetryingBrokerAdapter"
Cohesion: 0.20
Nodes (4): BrokerAdapterRetryPolicy, Logger, Override, RetryingBrokerAdapter

### Community 73 - ".getChartData"
Cohesion: 0.20
Nodes (7): ChartDataResponse, ChartIndicatorPoint, IndicatorController, GetMapping, RequestMapping, RestController, ChartDataResponse

### Community 74 - ".calculate"
Cohesion: 0.23
Nodes (5): MacdResult, MathContext, MacdCalculator, Test, MacdCalculatorTest

### Community 75 - "TradingModeEvent"
Cohesion: 0.20
Nodes (5): Entity, Override, PrePersist, Table, TradingModeEvent

### Community 76 - "WatchlistEntry"
Cohesion: 0.17
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

### Community 82 - ".calculate"
Cohesion: 0.26
Nodes (4): MathContext, VolatilityCalculator, Test, VolatilityCalculatorTest

### Community 83 - ".applySellGate"
Cohesion: 0.29
Nodes (4): MacdResult, MovingAverageResult, Test, MaCrossoverSellGateTest

### Community 85 - "NotificationType"
Cohesion: 0.17
Nodes (11): JsonInclude, NotificationResponse, NotificationType, ORDER_CANCELLED, ORDER_FAILED, ORDER_FILLED, ORDER_PARTIALLY_FILLED, ORDER_PARTIALLY_PROTECTED (+3 more)

### Community 87 - "TradingModeController"
Cohesion: 0.33
Nodes (5): GetMapping, PostMapping, RequestMapping, RestController, TradingModeController

### Community 88 - "WatchlistController"
Cohesion: 0.25
Nodes (8): GetMapping, PostMapping, RequestMapping, ResponseEntity, RestController, WatchlistController, WatchlistEntryResponse, DeleteMapping

### Community 89 - "DirectionalOutcomeStats"
Cohesion: 0.16
Nodes (7): DirectionalOutcomeStats, Test, PerSymbolMacdHistogramMagnitudeCalibrationTest, SymbolFixture, RegimeSplitStats, Test, StockRegimeOutOfSampleValidationTest

### Community 90 - "OrderAuditControllerIntegrationTest"
Cohesion: 0.29
Nodes (7): AutoConfigureMockMvc, Cookie, MockHttpSession, MockMvc, SpringBootTest, Test, OrderAuditControllerIntegrationTest

### Community 91 - "NotificationController"
Cohesion: 0.29
Nodes (6): GetMapping, PostMapping, RequestMapping, ResponseEntity, RestController, NotificationController

### Community 92 - "OrderStatus"
Cohesion: 0.12
Nodes (12): JsonInclude, OrderResponse, OrderStatus, CANCELLED, FAILED, FILLED, PARTIALLY_FILLED, PARTIALLY_PROTECTED (+4 more)

### Community 93 - "TickerControllerTest"
Cohesion: 0.18
Nodes (13): PostMapping, RequestMapping, ResponseEntity, RestController, RegisterTickerRequest, TickerController, TickerResponse, AutoConfigureMockMvc (+5 more)

### Community 94 - "RiskConsentEvent"
Cohesion: 0.24
Nodes (5): Entity, Override, PrePersist, Table, RiskConsentEvent

### Community 95 - "PerSymbolRsiOverboughtCalibrationTest"
Cohesion: 0.32
Nodes (3): Test, PerSymbolRsiOverboughtCalibrationTest, SymbolFixture

### Community 96 - "ThresholdCalibrationTest"
Cohesion: 0.36
Nodes (3): Test, NamedCandidate, ThresholdCalibrationTest

### Community 97 - "FakeAlpacaTradingServer"
Cohesion: 0.32
Nodes (7): FakeAlpacaTradingServer, FakeOrder, ClientHttpRequest, ClientHttpResponse, HttpStatus, ObjectMapper, MockClientHttpRequest

### Community 98 - "NotificationControllerTest"
Cohesion: 0.20
Nodes (5): AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, NotificationControllerTest

### Community 99 - "SecurityConfigTest"
Cohesion: 0.32
Nodes (6): AutoConfigureMockMvc, Cookie, MockMvc, SpringBootTest, Test, SecurityConfigTest

### Community 100 - "killswitch/api.ts"
Cohesion: 0.32
Nodes (10): clearKillSwitch(), engageKillSwitch(), EngageKillSwitchResponse, fetchKillSwitchState(), KillSwitchCancelSummary, KillSwitchResponse, KillSwitchState, describeError() (+2 more)

### Community 101 - "SignalController.java"
Cohesion: 0.25
Nodes (5): InvalidIndicatorRequestException, GetMapping, RequestMapping, RestController, SignalController

### Community 102 - "ApiErrorResponse"
Cohesion: 0.35
Nodes (7): ApiErrorResponse, JsonInclude, ExceptionHandler, Logger, ResponseEntity, RestControllerAdvice, RiskExceptionHandler

### Community 105 - "HoldTermRangeCalibrationTest.java"
Cohesion: 0.18
Nodes (4): HoldTermCalculator, PerSymbolRuleThresholds, RegimeClassifier, BacktestCandleCsvLoader

### Community 106 - ".calculate"
Cohesion: 0.24
Nodes (5): MathContext, MovingAverageResult, MovingAverageCrossoverCalculator, Test, MovingAverageCrossoverCalculatorTest

### Community 107 - "TradingModeExceptionHandler.java"
Cohesion: 0.29
Nodes (6): RiskConsentNotGivenException, ExceptionHandler, Logger, ResponseEntity, RestControllerAdvice, TradingModeExceptionHandler

### Community 108 - "Checkpoint"
Cohesion: 0.08
Nodes (23): Checkpoint, MAX, MID, MIN, DirectionalAccumulator, DirectionalOutcome, LOSS, WASH (+15 more)

### Community 109 - "MacdHistogramMagnitudeCalibrationTest"
Cohesion: 0.33
Nodes (3): Test, MacdHistogramMagnitudeCalibrationTest, SymbolFixture

### Community 110 - "MaCrossoverSeparationCalibrationTest"
Cohesion: 0.33
Nodes (3): Test, MaCrossoverSeparationCalibrationTest, SymbolFixture

### Community 111 - "PerSymbolMaCrossoverSeparationCalibrationTest"
Cohesion: 0.33
Nodes (3): Test, PerSymbolMaCrossoverSeparationCalibrationTest, SymbolFixture

### Community 113 - "BacktestReport"
Cohesion: 0.14
Nodes (6): BacktestReport, HoldGateStats, Test, RsiOversoldRecalibrationTest, Test, WeightedVoteBacktestTest

### Community 115 - "plugins"
Cohesion: 0.22
Nodes (8): plugins, rules, react/only-export-components, react/rules-of-hooks, $schema, oxc, typescript, warn

### Community 116 - "mvnw"
Cohesion: 0.33
Nodes (6): mvnw script, clean(), die(), exec_maven(), set_java_home(), verbose()

### Community 117 - ".calculate"
Cohesion: 0.30
Nodes (3): VolumeTrendCalculator, Test, VolumeTrendCalculatorTest

### Community 118 - "OrderService"
Cohesion: 0.16
Nodes (12): BrokerAdapter, BrokerAdapterRouter, Service, OrderAuditEntryRepository, PostMapping, RequestMapping, ResponseEntity, RestController (+4 more)

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

### Community 124 - "CredentialEncryptionService"
Cohesion: 0.39
Nodes (4): CredentialEncryptionService, Component, Logger, SecretKeySpec

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
Cohesion: 0.43
Nodes (4): SpringBootTest, Test, Transactional, TickerServiceTest

### Community 130 - "AlpacaTradingAdapterContractTest"
Cohesion: 0.48
Nodes (3): AlpacaTradingAdapterContractTest, ExtendWith, Override

### Community 131 - "BinanceFuturesTradingAdapterContractTest"
Cohesion: 0.48
Nodes (3): BinanceFuturesTradingAdapterContractTest, ExtendWith, Override

### Community 132 - "RetryingAlpacaTradingAdapterContractTest"
Cohesion: 0.48
Nodes (3): ExtendWith, Override, RetryingAlpacaTradingAdapterContractTest

### Community 133 - "RetryingBinanceFuturesTradingAdapterContractTest"
Cohesion: 0.48
Nodes (3): ExtendWith, Override, RetryingBinanceFuturesTradingAdapterContractTest

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
Cohesion: 0.39
Nodes (4): BrokerCredentialServiceFindTest, SpringBootTest, Test, Transactional

### Community 139 - "PerSymbolAdxTrendingThresholdCalibrationTest"
Cohesion: 0.39
Nodes (3): Test, PerSymbolAdxTrendingThresholdCalibrationTest, SymbolFixture

### Community 140 - "MarketDataControllerTest.java"
Cohesion: 0.30
Nodes (5): AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, MarketDataControllerTest

### Community 141 - "dataviz skill"
Cohesion: 0.38
Nodes (7): dataviz skill, SignalBadge colorblind-safe teal/orange/slate palette, F3.1 Ticker lookup & metrics display, E3-F1-S1 Ticker lookup + stat-tile metrics, E3-F1-S2 Buy/Sell/Hold badge color-coded, F3.2 Metric visualization, E3-F2-S1 Price chart with MA/RSI overlays

### Community 142 - "TradingModeService"
Cohesion: 0.39
Nodes (4): RiskConsentEventRepository, TradingModeEventRepository, Service, TradingModeService

### Community 143 - "ClockConfig.java"
Cohesion: 0.60
Nodes (3): ClockConfig, Bean, Configuration

### Community 144 - "E8-F1-S9 — SELL-only MACD histogram-magnitude gate, evaluated and no-shipped"
Cohesion: 0.50
Nodes (4): Design, E8-F1-S9 — SELL-only MACD histogram-magnitude gate, evaluated and no-shipped, Implementation, Scope / no-op confirmation

### Community 146 - "BackendApplicationTests.java"
Cohesion: 0.60
Nodes (3): BackendApplicationTests, SpringBootTest, Test

### Community 147 - "E8-F1-S12 — AAPL evaluated against the MACD-magnitude and MA-crossover-separation axes"
Cohesion: 0.29
Nodes (7): Design, Documentation, E8-F1-S12 — AAPL evaluated against the MACD-magnitude and MA-crossover-separation axes, Result 1: `macdMinHistogramMagnitudePct` — no ship, Result 2a: `maMinSeparationPctOfPrice` per-symbol BUY-side sweep — no ship, Result 2b: already-shipped `MaCrossoverSellGate` value vs. AAPL — active contradiction, Scope confirmation

### Community 148 - "E8-F1-S10 — per-symbol MA-crossover separation calibration, evaluated and no-shipped"
Cohesion: 0.50
Nodes (4): Design, E8-F1-S10 — per-symbol MA-crossover separation calibration, evaluated and no-shipped, Implementation, Scope / no-op confirmation

### Community 149 - "CoreDataModelIntegrationTest"
Cohesion: 0.32
Nodes (5): CoreDataModelIntegrationTest, SpringBootTest, Test, Transactional, EntityManager

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

### Community 169 - "E8-F6-S1 — HoldTermRule day-range calibration (no ship, all 6 branches)"
Cohesion: 0.40
Nodes (5): Design gate, E8-F6-S1 — HoldTermRule day-range calibration (no ship, all 6 branches), Findings, Mechanism, Outcome

### Community 172 - "MarketDataController"
Cohesion: 0.27
Nodes (5): GetMapping, RequestMapping, RestController, MarketDataController, PriceHistoryResponse

### Community 174 - "SignalDriftController"
Cohesion: 0.29
Nodes (6): ConditionalOnProperty, GetMapping, RequestMapping, RestController, SignalDriftController, SignalDriftReport

### Community 176 - "E8-F5-S2 — funding-adjusted live signal-drift monitoring"
Cohesion: 0.29
Nodes (7): Design, Design decision: `possibleDecay` stays cost-only, E8-F5-S2 — funding-adjusted live signal-drift monitoring, Findings: funding materially erodes both directions, Mechanism, Scope confirmation, Testing

### Community 177 - "OrderRepository"
Cohesion: 0.17
Nodes (3): Pageable, Query, OrderRepository

### Community 181 - ".findAllByOrderByLoggedAtDesc"
Cohesion: 0.60
Nodes (3): Page, Pageable, Query

### Community 182 - "Notification system + WatchlistSignalPoller"
Cohesion: 0.40
Nodes (5): Notification system + WatchlistSignalPoller, Watchlist feature (watchlist_entries), F3.3 Watchlist (stretch), E3-F3-S1 Watchlist persisted in Oracle DB, F5.4 Notifications

## Knowledge Gaps
- **324 isolated node(s):** `com.autotrade.dashboard:backend`, `MIN`, `MID`, `MAX`, `WIN` (+319 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **31 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Candle` connect `Candle` to `SignalCallEntry`, `CheckpointStats`, `SignalRuleId`, `AlpacaMarketDataClient`, `OrderServiceTest.java`, `.calculate`, `PerSymbolAdxTrendingThresholdCalibrationTest`, `TradingMode`, `MarketDataControllerTest.java`, `LiveDriftBaselineTest`, `RegimeOutOfSampleValidationTest`, `.run`, `TickerNotRegisteredException`, `OutOfSampleValidationTest`, `BinanceMarketDataClient`, `MarketDataService`, `.bullishCandles`, `MarketDataController`, `.getPriceHistory`, `RegimeCalibrationTest`, `.findFirstCrossing`, `.calculate`, `LiveSignalDriftServiceTest`, `Broker`, `BacktestHarnessTest`, `.getChartData`, `.calculate`, `.calculate`, `StockMaCrossoverSeparationCalibrationTest`, `DirectionalOutcomeStats`, `PerSymbolRsiOverboughtCalibrationTest`, `ThresholdCalibrationTest`, `StockPerSymbolMacdHistogramMagnitudeCalibrationTest`, `HoldTermRangeCalibrationTest.java`, `.calculate`, `Checkpoint`, `MacdHistogramMagnitudeCalibrationTest`, `MaCrossoverSeparationCalibrationTest`, `PerSymbolMaCrossoverSeparationCalibrationTest`, `RsiOverboughtRecalibrationTest`, `BacktestReport`, `StockPerSymbolRsiOverboughtCalibrationTest`, `.calculate`, `SellMacdHistogramMagnitudeCalibrationTest`, `SellMaCrossoverSeparationCalibrationTest`?**
  _High betweenness centrality (0.101) - this node is a cross-community bridge._
- **Why does `SignalRuleId` connect `SignalRuleId` to `SignalCallEntry`, `Test`, `OrderServiceTest.java`, `.calculate`, `TradingMode`, `OrderAuditEntryRepositoryTest.java`, `.evaluate`, `OutOfSampleValidationTest`, `.applySellGate`, `StockMaCrossoverSeparationCalibrationTest`, `DirectionalOutcomeStats`, `PerSymbolRsiOverboughtCalibrationTest`, `ThresholdCalibrationTest`, `StockPerSymbolMacdHistogramMagnitudeCalibrationTest`, `HoldTermRangeCalibrationTest.java`, `MacdHistogramMagnitudeCalibrationTest`, `MaCrossoverSeparationCalibrationTest`, `PerSymbolMaCrossoverSeparationCalibrationTest`, `RsiOverboughtRecalibrationTest`, `BacktestReport`, `StockPerSymbolRsiOverboughtCalibrationTest`?**
  _High betweenness centrality (0.071) - this node is a cross-community bridge._
- **Why does `BrokerCredentialService` connect `TradingMode` to `BinanceFuturesTradingAdapter`, `AlpacaTradingAdapterContractTest`, `DecryptedCredential`, `AlpacaTradingAdapter`, `BinanceFuturesTradingAdapterContractTest`, `RetryingAlpacaTradingAdapterContractTest`, `RetryingBinanceFuturesTradingAdapterContractTest`, `BrokerCredentialServiceFindTest.java`, `OrderServiceTest.java`, `CoreDataModelIntegrationTest`, `OrderAuditEntryRepositoryTest.java`, `.run`, `OrderServiceTest`, `.readDecrypted`, `security-review skill`, `Broker`, `BrokerAdapterConfig.java`, `BinanceFuturesAdapterConfig.java`, `OrderService`, `CredentialEncryptionService`?**
  _High betweenness centrality (0.070) - this node is a cross-community bridge._
- **Are the 6 inferred relationships involving `Candle` (e.g. with `.chartData_marketClosedWithCachedFallback_returns200Stale()` and `.chartData_registeredTicker_returns200WithCandlesAndIndicators()`) actually correct?**
  _`Candle` has 6 INFERRED edges - model-reasoned connections that need verification._
- **What connects `com.autotrade.dashboard:backend`, `MIN`, `MID` to the rest of the system?**
  _324 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `BinanceFuturesTradingAdapter` be split into smaller, more focused modules?**
  _Cohesion score 0.0629076372817168 - nodes in this community are weakly interconnected._
- **Should `Changelog` be split into smaller, more focused modules?**
  _Cohesion score 0.02564102564102564 - nodes in this community are weakly interconnected._