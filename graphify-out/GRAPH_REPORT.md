# Graph Report - agent testing  (2026-08-17)

## Corpus Check
- 360 files · ~221,653 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 3183 nodes · 8946 edges · 184 communities (150 shown, 34 thin omitted)
- Extraction: 87% EXTRACTED · 13% INFERRED · 0% AMBIGUOUS · INFERRED: 1194 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `57ab9268`
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
- SignalRuleId
- AlpacaMarketDataClient
- devDependencies
- Test
- OrderServiceTest.java
- .calculate
- AssetType.java
- Order
- .run
- order/api.ts
- RegimeOutOfSampleValidationTest
- SignalServiceTest
- Candle
- .runCombinedCallExpectancy
- SecurityConfig.java
- IndicatorSnapshot
- WatchlistSignalPollerTest
- BrokerCredential
- OrderQueryControllerTest
- CoreDataModelIntegrationTest.java
- DashboardPage.tsx
- BrokerCredentialService
- .evaluate
- .getChartData
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
- ApiErrorResponse
- MarketHoursServiceTest
- BrokerOrderRequest
- .findFirstCrossing
- NotificationServiceTest
- TradingMode
- compilerOptions
- .findAllByOrderByCreatedAtDesc
- .readDecrypted
- run skill (project override)
- TradeForm.tsx
- .calculate
- LiveSignalDriftServiceTest
- OrderController
- .resolveOrRegister
- TickerService
- security-review skill
- BrokerAdapterException
- .switchTo
- WatchlistControllerTest
- BrokerAdapterContractTest
- .export
- general-purpose agent (implementation)
- BacktestHarnessTest
- RetryingBrokerAdapter
- IndicatorController
- .calculate
- TradingModeEvent
- WatchlistEntry
- adapter-contract-check skill
- BrokerAdapterConfig.java
- BinanceFuturesAdapterConfig.java
- SignalDriftControllerIntegrationTest
- E8-F1-S7 — evaluate the per-symbol RSI override and SELL-side regime gate against a stock fixture
- IndicatorId
- .applySellGate
- StockMaCrossoverSeparationCalibrationTest
- NotificationType
- TradingModeController
- WatchlistController
- PerSymbolMacdHistogramMagnitudeCalibrationTest
- OrderAuditControllerIntegrationTest
- NotificationController
- OrderStatus
- TickerControllerTest
- RiskConsentEvent
- PerSymbolRsiOverboughtCalibrationTest
- ThresholdCalibrationTest
- .fetchRecentCandles
- NotificationControllerTest
- SecurityConfigTest
- killswitch/api.ts
- SignalController.java
- RiskExceptionHandler.java
- StockPerSymbolMacdHistogramMagnitudeCalibrationTest
- .getAccountStatus
- .forSymbol
- TickerController
- TradingModeExceptionHandler.java
- LiveSignalDriftService.java
- MacdHistogramMagnitudeCalibrationTest
- MaCrossoverSeparationCalibrationTest
- PerSymbolMaCrossoverSeparationCalibrationTest
- BacktestReport
- RsiOversoldRecalibrationTest
- StockPerSymbolRsiOverboughtCalibrationTest
- plugins
- mvnw
- .fetchRecentCandles
- OrderService
- .placeOrder
- NotificationExceptionHandler.java
- E8-F1-S11 — SELL-only MA-crossover separation gate, evaluated and shipped
- SellMacdHistogramMagnitudeCalibrationTest
- SellMaCrossoverSeparationCalibrationTest
- MarketDataClientConfig.java
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
- MarketDataControllerTest
- dataviz skill
- WeightedVoteBacktestTest
- ClockConfig.java
- E8-F1-S9 — SELL-only MACD histogram-magnitude gate, evaluated and no-shipped
- .switchTo_live_belowThreshold_throwsPaperTradeThresholdNotMetException_noHistoryPersisted
- BackendApplicationTests.java
- E8-F1-S12 — AAPL evaluated against the MACD-magnitude and MA-crossover-separation axes
- E8-F1-S10 — per-symbol MA-crossover separation calibration, evaluated and no-shipped
- RetryHelper
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
- AlpacaMarketDataProperties
- MarketDataController
- BinanceMarketDataProperties
- SignalDriftController
- RegimeCalibrationTest
- E8-F5-S2 — funding-adjusted live signal-drift monitoring
- StockRegimeOutOfSampleValidationTest
- MockBrokerAdapterContractTest
- RetryingMockBrokerAdapterContractTest
- .findAllByOrderByLoggedAtDesc
- Notification system + WatchlistSignalPoller

## God Nodes (most connected - your core abstractions)
1. `Candle` - 142 edges
2. `TradingMode` - 125 edges
3. `Ticker` - 111 edges
4. `Broker` - 107 edges
5. `AssetType` - 95 edges
6. `Order` - 93 edges
7. `Changelog` - 86 edges
8. `SignalRuleId` - 67 edges
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

## Communities (184 total, 34 thin omitted)

### Community 0 - "BinanceFuturesTradingAdapter"
Cohesion: 0.06
Nodes (23): Credentials, BinanceAccountAsset, BinanceAccountResponse, BinanceAlgoOrderResponse, BinanceErrorResponse, BinanceFuturesTradingAdapter, BinanceOrderResponse, BinancePositionResponse (+15 more)

### Community 1 - "Changelog"
Cohesion: 0.03
Nodes (77): Changelog, Dark-first premium visual pass, E1-F1-S1 — local Oracle XE via Docker Compose, E1-F1-S2 — Spring Boot backend skeleton, E1-F1-S3 — React app skeleton, E1-F1-S4 — CI pipeline, E1-F1-S5 — env/config profiles, E1-F2 — core data model (+69 more)

### Community 2 - "KillSwitchService"
Cohesion: 0.06
Nodes (31): EngageKillSwitchResponse, GetMapping, PostMapping, RequestMapping, RestController, KillSwitchController, Entity, Override (+23 more)

### Community 3 - ".evaluate"
Cohesion: 0.18
Nodes (4): IndicatorVotes, MacdResult, Test, SignalRuleEngineTest

### Community 4 - "AlpacaTradingAdapter"
Cohesion: 0.08
Nodes (24): AlpacaAccountResponse, AlpacaBracketLeg, AlpacaErrorResponse, AlpacaOrderRequestBody, AlpacaOrderResponse, AlpacaPositionResponse, AlpacaStopLeg, AlpacaTradingAdapter (+16 more)

### Community 5 - "SignalCallEntry"
Cohesion: 0.14
Nodes (5): Entity, Override, PrePersist, Table, SignalCallEntry

### Community 6 - "DirectionalOutcomeStats"
Cohesion: 0.14
Nodes (9): Checkpoint, MAX, MID, MIN, CheckpointStats, DirectionalOutcomeStats, CheckpointStatsTest, Test (+1 more)

### Community 7 - "SignalRuleId"
Cohesion: 0.14
Nodes (18): MaCrossoverSellGate, SignalCall, BUY, HOLD, SELL, RuleThresholds, SignalRuleEngine, call() (+10 more)

### Community 8 - "AlpacaMarketDataClient"
Cohesion: 0.22
Nodes (8): AlpacaBar, AlpacaBarsResponse, AlpacaMarketDataClient, Component, JsonIgnoreProperties, Override, RestClient, TooManyRequests

### Community 9 - "devDependencies"
Cohesion: 0.05
Nodes (36): dependencies, lightweight-charts, react, react-dom, react-router-dom, devDependencies, oxlint, @types/node (+28 more)

### Community 10 - "Test"
Cohesion: 0.10
Nodes (8): Regime, RANGING, TRENDING, RegimeGatedRuleEngine, Test, RegimeClassifierTest, Test, RegimeGatedRuleEngineTest

### Community 11 - "OrderServiceTest.java"
Cohesion: 0.10
Nodes (23): IndicatorResponse, BigDecimalIndicators, IndicatorService, Service, MacdResult, MovingAverageRelation, EQUAL, SHORT_ABOVE_LONG (+15 more)

### Community 12 - ".calculate"
Cohesion: 0.09
Nodes (17): HoldTermRule, MODERATE_HIGH, MODERATE_LOW, MODERATE_MEDIUM, STRONG_HIGH, STRONG_LOW, STRONG_MEDIUM, match() (+9 more)

### Community 13 - "AssetType.java"
Cohesion: 0.24
Nodes (7): EntryOrderType, LIMIT, MARKET, OrderSide, BUY, SELL, HttpMethod

### Community 14 - "Order"
Cohesion: 0.06
Nodes (7): Entity, Override, PrePersist, PreUpdate, Table, Order, Test

### Community 15 - ".run"
Cohesion: 0.18
Nodes (5): LiveDriftBaseline, BacktestDecisionPoint, RegimeSplitStats, Test, LiveDriftBaselineTest

### Community 16 - "order/api.ts"
Cohesion: 0.11
Nodes (26): RFC-6266, AuditEntry, AuditEntryPage, fetchAuditEntries(), SignalCall, SignalRuleId, AuditTrail(), describeError() (+18 more)

### Community 18 - "SignalServiceTest"
Cohesion: 0.20
Nodes (8): IndicatorComputation, IndicatorFactory, BeforeEach, ExtendWith, FunctionalInterface, IndicatorResponse, Test, SignalServiceTest

### Community 19 - "Candle"
Cohesion: 0.06
Nodes (20): MathContext, MovingAverageCrossoverCalculator, MathContext, RsiCalculator, MathContext, VolatilityCalculator, VolumeTrendCalculator, Candle (+12 more)

### Community 20 - ".runCombinedCallExpectancy"
Cohesion: 0.17
Nodes (7): BacktestHarness, IndicatorAccumulator, FunctionalInterface, RuleEvaluator, FractionCandidate, Test, WeightedMajorityFractionCalibrationTest

### Community 21 - "SecurityConfig.java"
Cohesion: 0.11
Nodes (22): CsrfCookieWriteFilter, Bean, Configuration, Logger, Override, PasswordEncoder, SecurityConfig, SpaCsrfTokenRequestHandler (+14 more)

### Community 22 - "IndicatorSnapshot"
Cohesion: 0.08
Nodes (5): IndicatorSnapshot, Entity, Override, PrePersist, Table

### Community 23 - "WatchlistSignalPollerTest"
Cohesion: 0.22
Nodes (9): Component, ConditionalOnProperty, Logger, Scheduled, WatchlistSignalPoller, BeforeEach, ExtendWith, Test (+1 more)

### Community 24 - "BrokerCredential"
Cohesion: 0.12
Nodes (6): BrokerCredential, Entity, Override, PrePersist, PreUpdate, Table

### Community 25 - "OrderQueryControllerTest"
Cohesion: 0.09
Nodes (15): Page, PagedResponse, AuditEntryResponse, JsonInclude, GetMapping, PostMapping, RequestMapping, ResponseEntity (+7 more)

### Community 26 - "CoreDataModelIntegrationTest.java"
Cohesion: 0.13
Nodes (11): IndicatorSnapshotRepository, TickerRepository, CoreDataModelIntegrationTest, SpringBootTest, Test, Transactional, SpringBootTest, Test (+3 more)

### Community 27 - "DashboardPage.tsx"
Cohesion: 0.12
Nodes (21): App(), AuthContext, AuthContextValue, AuthProvider(), useAuth(), RequireAuth(), TabItem, Tabs() (+13 more)

### Community 28 - "BrokerCredentialService"
Cohesion: 0.10
Nodes (20): ApplicationRunner, AlpacaTradingCredentialBootstrap, ApplicationArguments, Component, Logger, Override, BinanceTradingCredentialBootstrap, ApplicationArguments (+12 more)

### Community 29 - ".evaluate"
Cohesion: 0.16
Nodes (5): IndicatorWeights, WeightedVoteRuleEngine, MacdResult, Test, WeightedVoteRuleEngineTest

### Community 30 - ".getChartData"
Cohesion: 0.14
Nodes (9): ChartDataResponse, ChartIndicatorPoint, ChartDataResponse, MarketClosedException, IndicatorControllerTest, AutoConfigureMockMvc, MockMvc, Test (+1 more)

### Community 32 - "OrderServiceTest"
Cohesion: 0.30
Nodes (4): PlaceOrderRequest, SignalComputation, Test, OrderServiceTest

### Community 33 - "BinanceMarketDataClient"
Cohesion: 0.23
Nodes (6): BinanceMarketDataClient, Candle, Component, Override, RestClient, TooManyRequests

### Community 34 - "MarketDataService"
Cohesion: 0.16
Nodes (9): MarketDataClient, Service, MarketDataService, Component, MarketHoursService, BeforeEach, ExtendWith, Test (+1 more)

### Community 35 - "OrderControllerTest"
Cohesion: 0.15
Nodes (10): JsonInclude, TradeOrderResponse, KillSwitchEngagedException, RiskLimitExceededException, AutoConfigureMockMvc, MockMvc, ObjectMapper, Test (+2 more)

### Community 36 - "BrokerOrderResult"
Cohesion: 0.21
Nodes (4): BrokerOrderResult, KillSwitchCancelSummary, BrokerAdapterRouterTest, Test

### Community 37 - "TickerMetrics.tsx"
Cohesion: 0.10
Nodes (24): fetchChartData(), registerTicker(), TickerSummary, Broker, fetchSignal(), HoldTerm, IndicatorResponse, MacdResult (+16 more)

### Community 38 - "MarketDataExceptionHandler"
Cohesion: 0.12
Nodes (11): InsufficientPriceHistoryException, InvalidPriceHistoryRequestException, ExceptionHandler, Logger, ResponseEntity, RestControllerAdvice, MarketDataExceptionHandler, MarketDataUnavailableException (+3 more)

### Community 39 - "Ticker"
Cohesion: 0.10
Nodes (15): Autowired, Broker, ALPACA, BINANCE, BrokerAdapterTransientException, HoldTerm, AssetType, CRYPTO (+7 more)

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
Cohesion: 0.27
Nodes (5): PriceHistoryResult, IndicatorServiceTest, BeforeEach, ExtendWith, Test

### Community 45 - "Notification"
Cohesion: 0.14
Nodes (5): Entity, Override, PrePersist, Table, Notification

### Community 46 - "RiskLimitService"
Cohesion: 0.19
Nodes (7): Logger, Service, RiskLimitService, ConfigurationProperties, RiskLimitsProperties, Test, RiskLimitServiceTest

### Community 47 - "compilerOptions"
Cohesion: 0.08
Nodes (23): compilerOptions, allowArbitraryExtensions, allowImportingTsExtensions, erasableSyntaxOnly, jsx, lib, module, moduleDetection (+15 more)

### Community 48 - "ApiErrorResponse"
Cohesion: 0.34
Nodes (7): ApiErrorResponse, JsonInclude, ExceptionHandler, Logger, ResponseEntity, RestControllerAdvice, OrderExceptionHandler

### Community 50 - "BrokerOrderRequest"
Cohesion: 0.16
Nodes (7): BrokerOrderRequest, Override, MockBrokerAdapter, MockOrderState, PositionState, Test, MockBrokerAdapterTest

### Community 51 - ".findFirstCrossing"
Cohesion: 0.47
Nodes (3): BacktestHarnessTpSlTest, Candle, Test

### Community 52 - "NotificationServiceTest"
Cohesion: 0.22
Nodes (4): BeforeEach, ExtendWith, Test, NotificationServiceTest

### Community 53 - "TradingMode"
Cohesion: 0.13
Nodes (12): BrokerCredentialRepository, BrokerAccountStatus, TradingMode, LIVE, PAPER, BrokerCredentialNotConfiguredException, JsonInclude, OrderResponse (+4 more)

### Community 54 - "compilerOptions"
Cohesion: 0.10
Nodes (19): compilerOptions, allowImportingTsExtensions, erasableSyntaxOnly, lib, module, moduleDetection, noEmit, noFallthroughCasesInSwitch (+11 more)

### Community 56 - ".readDecrypted"
Cohesion: 0.06
Nodes (30): DecryptedCredential, Override, Transactional, CredentialEncryptionService, Component, Logger, CredentialEncryptionServiceTest, Test (+22 more)

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

### Community 61 - "OrderController"
Cohesion: 0.39
Nodes (5): PostMapping, RequestMapping, ResponseEntity, RestController, OrderController

### Community 62 - ".resolveOrRegister"
Cohesion: 0.22
Nodes (6): Transactional, WatchlistEntry, SpringBootTest, Test, Transactional, WatchlistServiceTest

### Community 63 - "TickerService"
Cohesion: 0.19
Nodes (7): Service, Transactional, TickerService, Query, WatchlistEntryRepository, Service, WatchlistService

### Community 64 - "security-review skill"
Cohesion: 0.19
Nodes (18): guardrail-check skill, security-review skill, simplify skill, F1.3 Secrets & config management, E1-F3-S1 Broker API keys encrypted at rest, E1-F3-S2 Dashboard requires login, F4.2 Alpaca adapter (stocks), F6.1 Paper/live mode toggle (+10 more)

### Community 65 - "BrokerAdapterException"
Cohesion: 0.18
Nodes (3): BrokerAdapterException, BrokerAdapterRateLimitedException, BrokerAdapterUnavailableException

### Community 66 - ".switchTo"
Cohesion: 0.28
Nodes (6): TradingModeResponse, AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, TradingModeControllerTest

### Community 67 - "WatchlistControllerTest"
Cohesion: 0.26
Nodes (7): AddWatchlistEntryRequest, AutoConfigureMockMvc, MockMvc, ObjectMapper, Test, WebMvcTest, WatchlistControllerTest

### Community 69 - ".export"
Cohesion: 0.23
Nodes (3): OrderCsvExporter, Test, OrderCsvExporterTest

### Community 70 - "general-purpose agent (implementation)"
Cohesion: 0.24
Nodes (16): Explore agent (research), general-purpose agent (implementation), Plan agent (design gate), Mandatory per-story workflow (update CLAUDE.md, commit, push), CLAUDE.md project status & architecture log, E1 — Platform Foundation, F1.2 Core data model, F1.4 Testing strategy (+8 more)

### Community 71 - "BacktestHarnessTest"
Cohesion: 0.31
Nodes (3): Candle, BacktestHarnessTest, Test

### Community 72 - "RetryingBrokerAdapter"
Cohesion: 0.21
Nodes (4): BrokerAdapterRetryPolicy, Logger, Override, RetryingBrokerAdapter

### Community 73 - "IndicatorController"
Cohesion: 0.43
Nodes (4): IndicatorController, GetMapping, RequestMapping, RestController

### Community 74 - ".calculate"
Cohesion: 0.11
Nodes (10): MacdResult, MathContext, MacdCalculator, MovingAverageResult, Test, MacdCalculatorTest, Test, MovingAverageCrossoverCalculatorTest (+2 more)

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

### Community 82 - "IndicatorId"
Cohesion: 0.21
Nodes (8): IndicatorId, MA_CROSSOVER, MACD, RSI, HorizonCandidate, IndicatorExpectancyAlternateHorizonCalibrationTest, Test, Test

### Community 83 - ".applySellGate"
Cohesion: 0.29
Nodes (4): MacdResult, MovingAverageResult, Test, MaCrossoverSellGateTest

### Community 85 - "NotificationType"
Cohesion: 0.17
Nodes (11): JsonInclude, NotificationResponse, NotificationType, ORDER_CANCELLED, ORDER_FAILED, ORDER_FILLED, ORDER_PARTIALLY_FILLED, ORDER_PARTIALLY_PROTECTED (+3 more)

### Community 87 - "TradingModeController"
Cohesion: 0.24
Nodes (6): TradingModeChangeRequest, GetMapping, PostMapping, RequestMapping, RestController, TradingModeController

### Community 88 - "WatchlistController"
Cohesion: 0.25
Nodes (8): GetMapping, PostMapping, RequestMapping, ResponseEntity, RestController, WatchlistController, WatchlistEntryResponse, DeleteMapping

### Community 89 - "PerSymbolMacdHistogramMagnitudeCalibrationTest"
Cohesion: 0.32
Nodes (3): Test, PerSymbolMacdHistogramMagnitudeCalibrationTest, SymbolFixture

### Community 90 - "OrderAuditControllerIntegrationTest"
Cohesion: 0.29
Nodes (7): AutoConfigureMockMvc, Cookie, MockHttpSession, MockMvc, SpringBootTest, Test, OrderAuditControllerIntegrationTest

### Community 91 - "NotificationController"
Cohesion: 0.29
Nodes (6): GetMapping, PostMapping, RequestMapping, ResponseEntity, RestController, NotificationController

### Community 92 - "OrderStatus"
Cohesion: 0.11
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

### Community 97 - ".fetchRecentCandles"
Cohesion: 0.33
Nodes (4): Candle, AlpacaMarketDataClientTest, MockRestServiceServer, Test

### Community 98 - "NotificationControllerTest"
Cohesion: 0.26
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

### Community 102 - "RiskExceptionHandler.java"
Cohesion: 0.46
Nodes (5): ExceptionHandler, Logger, ResponseEntity, RestControllerAdvice, RiskExceptionHandler

### Community 105 - ".forSymbol"
Cohesion: 0.31
Nodes (3): PerSymbolRuleThresholds, Test, PerSymbolRuleThresholdsTest

### Community 106 - "TickerController"
Cohesion: 0.26
Nodes (6): PostMapping, RequestMapping, ResponseEntity, RestController, TickerController, TickerResponse

### Community 107 - "TradingModeExceptionHandler.java"
Cohesion: 0.29
Nodes (6): RiskConsentNotGivenException, ExceptionHandler, Logger, ResponseEntity, RestControllerAdvice, TradingModeExceptionHandler

### Community 108 - "LiveSignalDriftService.java"
Cohesion: 0.06
Nodes (24): DirectionalAccumulator, DirectionalOutcome, LOSS, WASH, WIN, DirectionalScoreResult, ExitReason, HORIZON_EXPIRED (+16 more)

### Community 109 - "MacdHistogramMagnitudeCalibrationTest"
Cohesion: 0.36
Nodes (3): Test, MacdHistogramMagnitudeCalibrationTest, SymbolFixture

### Community 110 - "MaCrossoverSeparationCalibrationTest"
Cohesion: 0.36
Nodes (3): Test, MaCrossoverSeparationCalibrationTest, SymbolFixture

### Community 111 - "PerSymbolMaCrossoverSeparationCalibrationTest"
Cohesion: 0.36
Nodes (3): Test, PerSymbolMaCrossoverSeparationCalibrationTest, SymbolFixture

### Community 112 - "BacktestReport"
Cohesion: 0.20
Nodes (4): BacktestReport, HoldGateStats, Test, RsiOverboughtRecalibrationTest

### Community 115 - "plugins"
Cohesion: 0.22
Nodes (8): plugins, rules, react/only-export-components, react/rules-of-hooks, $schema, oxc, typescript, warn

### Community 116 - "mvnw"
Cohesion: 0.33
Nodes (6): mvnw script, clean(), die(), exec_maven(), set_java_home(), verbose()

### Community 117 - ".fetchRecentCandles"
Cohesion: 0.33
Nodes (4): BinanceMarketDataClientTest, BeforeEach, MockRestServiceServer, Test

### Community 118 - "OrderService"
Cohesion: 0.09
Nodes (22): BrokerAdapter, BrokerAdapterAmbiguousOrderException, BrokerAdapterRouter, Service, NotificationRepository, Logger, Service, NotificationService (+14 more)

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

### Community 124 - "MarketDataClientConfig.java"
Cohesion: 0.40
Nodes (6): Bean, Configuration, EnableConfigurationProperties, RestClient, SimpleClientHttpRequestFactory, MarketDataClientConfig

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

### Community 140 - "MarketDataControllerTest"
Cohesion: 0.27
Nodes (5): AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, MarketDataControllerTest

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

### Community 147 - "E8-F1-S12 — AAPL evaluated against the MACD-magnitude and MA-crossover-separation axes"
Cohesion: 0.29
Nodes (7): Design, Documentation, E8-F1-S12 — AAPL evaluated against the MACD-magnitude and MA-crossover-separation axes, Result 1: `macdMinHistogramMagnitudePct` — no ship, Result 2a: `maMinSeparationPctOfPrice` per-symbol BUY-side sweep — no ship, Result 2b: already-shipped `MaCrossoverSellGate` value vs. AAPL — active contradiction, Scope confirmation

### Community 148 - "E8-F1-S10 — per-symbol MA-crossover separation calibration, evaluated and no-shipped"
Cohesion: 0.50
Nodes (4): Design, E8-F1-S10 — per-symbol MA-crossover separation calibration, evaluated and no-shipped, Implementation, Scope / no-op confirmation

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

### Community 171 - "AlpacaMarketDataProperties"
Cohesion: 0.50
Nodes (3): AlpacaMarketDataProperties, ConfigurationProperties, BeforeEach

### Community 172 - "MarketDataController"
Cohesion: 0.27
Nodes (5): GetMapping, RequestMapping, RestController, MarketDataController, PriceHistoryResponse

### Community 174 - "SignalDriftController"
Cohesion: 0.29
Nodes (6): ConditionalOnProperty, GetMapping, RequestMapping, RestController, SignalDriftController, SignalDriftReport

### Community 176 - "E8-F5-S2 — funding-adjusted live signal-drift monitoring"
Cohesion: 0.29
Nodes (7): Design, Design decision: `possibleDecay` stays cost-only, E8-F5-S2 — funding-adjusted live signal-drift monitoring, Findings: funding materially erodes both directions, Mechanism, Scope confirmation, Testing

### Community 181 - ".findAllByOrderByLoggedAtDesc"
Cohesion: 0.60
Nodes (3): Page, Pageable, Query

### Community 182 - "Notification system + WatchlistSignalPoller"
Cohesion: 0.40
Nodes (5): Notification system + WatchlistSignalPoller, Watchlist feature (watchlist_entries), F3.3 Watchlist (stretch), E3-F3-S1 Watchlist persisted in Oracle DB, F5.4 Notifications

## Knowledge Gaps
- **320 isolated node(s):** `com.autotrade.dashboard:backend`, `MIN`, `MID`, `MAX`, `WIN` (+315 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **34 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Candle` connect `Candle` to `SignalCallEntry`, `DirectionalOutcomeStats`, `SignalRuleId`, `OrderServiceTest.java`, `PerSymbolAdxTrendingThresholdCalibrationTest`, `MarketDataControllerTest`, `WeightedVoteBacktestTest`, `.run`, `RegimeOutOfSampleValidationTest`, `.runCombinedCallExpectancy`, `.getChartData`, `OutOfSampleValidationTest`, `BinanceMarketDataClient`, `MarketDataService`, `Ticker`, `MarketDataController`, `.getPriceHistory`, `RegimeCalibrationTest`, `StockRegimeOutOfSampleValidationTest`, `.findFirstCrossing`, `.calculate`, `LiveSignalDriftServiceTest`, `BacktestHarnessTest`, `.calculate`, `IndicatorId`, `StockMaCrossoverSeparationCalibrationTest`, `PerSymbolMacdHistogramMagnitudeCalibrationTest`, `PerSymbolRsiOverboughtCalibrationTest`, `ThresholdCalibrationTest`, `.fetchRecentCandles`, `StockPerSymbolMacdHistogramMagnitudeCalibrationTest`, `LiveSignalDriftService.java`, `MacdHistogramMagnitudeCalibrationTest`, `MaCrossoverSeparationCalibrationTest`, `PerSymbolMaCrossoverSeparationCalibrationTest`, `BacktestReport`, `RsiOversoldRecalibrationTest`, `StockPerSymbolRsiOverboughtCalibrationTest`, `.fetchRecentCandles`, `SellMacdHistogramMagnitudeCalibrationTest`, `SellMaCrossoverSeparationCalibrationTest`?**
  _High betweenness centrality (0.087) - this node is a cross-community bridge._
- **Why does `SignalRuleId` connect `SignalRuleId` to `.evaluate`, `SignalCallEntry`, `Test`, `OrderServiceTest.java`, `.calculate`, `.run`, `Candle`, `.runCombinedCallExpectancy`, `OrderQueryControllerTest`, `.evaluate`, `OutOfSampleValidationTest`, `Ticker`, `.applySellGate`, `StockMaCrossoverSeparationCalibrationTest`, `PerSymbolMacdHistogramMagnitudeCalibrationTest`, `PerSymbolRsiOverboughtCalibrationTest`, `ThresholdCalibrationTest`, `StockPerSymbolMacdHistogramMagnitudeCalibrationTest`, `MacdHistogramMagnitudeCalibrationTest`, `MaCrossoverSeparationCalibrationTest`, `PerSymbolMaCrossoverSeparationCalibrationTest`, `BacktestReport`, `RsiOversoldRecalibrationTest`, `StockPerSymbolRsiOverboughtCalibrationTest`?**
  _High betweenness centrality (0.082) - this node is a cross-community bridge._
- **Why does `TradingMode` connect `TradingMode` to `BinanceFuturesTradingAdapter`, `AlpacaTradingAdapterContractTest`, `BinanceFuturesTradingAdapterContractTest`, `AlpacaTradingAdapter`, `RetryingAlpacaTradingAdapterContractTest`, `RetryingBinanceFuturesTradingAdapterContractTest`, `BrokerCredentialServiceFindTest.java`, `OrderServiceTest.java`, `AssetType.java`, `Order`, `BrokerCredential`, `OrderQueryControllerTest`, `CoreDataModelIntegrationTest.java`, `BrokerCredentialService`, `OrderServiceTest`, `BrokerOrderResult`, `Ticker`, `TradingModeServiceTest`, `.exportOrdersCsv`, `BrokerOrderRequest`, `MockBrokerAdapterContractTest`, `RetryingMockBrokerAdapterContractTest`, `.readDecrypted`, `BrokerAdapterException`, `.switchTo`, `BrokerAdapterContractTest`, `RetryingBrokerAdapter`, `TradingModeEvent`, `BrokerAdapterConfig.java`, `BinanceFuturesAdapterConfig.java`, `.currentState`, `TradingModeController`, `OrderStatus`, `.getAccountStatus`, `OrderService`, `.placeOrder`?**
  _High betweenness centrality (0.070) - this node is a cross-community bridge._
- **Are the 6 inferred relationships involving `Candle` (e.g. with `.chartData_marketClosedWithCachedFallback_returns200Stale()` and `.chartData_registeredTicker_returns200WithCandlesAndIndicators()`) actually correct?**
  _`Candle` has 6 INFERRED edges - model-reasoned connections that need verification._
- **What connects `com.autotrade.dashboard:backend`, `MIN`, `MID` to the rest of the system?**
  _320 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `BinanceFuturesTradingAdapter` be split into smaller, more focused modules?**
  _Cohesion score 0.0629076372817168 - nodes in this community are weakly interconnected._
- **Should `Changelog` be split into smaller, more focused modules?**
  _Cohesion score 0.02564102564102564 - nodes in this community are weakly interconnected._