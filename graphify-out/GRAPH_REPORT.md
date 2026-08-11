# Graph Report - agent testing  (2026-08-11)

## Corpus Check
- 350 files · ~191,686 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 3009 nodes · 8394 edges · 156 communities (130 shown, 26 thin omitted)
- Extraction: 86% EXTRACTED · 14% INFERRED · 0% AMBIGUOUS · INFERRED: 1147 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `16e2187e`
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
- OrderAuditEntry
- TradingMode
- IndicatorSnapshot
- devDependencies
- Test
- CoreDataModelIntegrationTest.java
- .calculate
- TradingModeServiceTest
- MarketDataExceptionHandler
- RiskLimitService
- OrderQueryControllerTest
- BinanceMarketDataClient
- TradeForm.tsx
- BacktestReport
- TradingModeEvent
- SecurityConfig.java
- .run
- .resolveOrRegister
- order/api.ts
- PriceChart.tsx
- FakeAlpacaTradingServer
- DashboardPage.tsx
- compilerOptions
- Notification
- SignalController.java
- .calculate
- RiskConsentEvent
- IndicatorService
- .calculate
- OrderServiceTest
- KillSwitchService
- compilerOptions
- BacktestHarnessTest
- TickerService
- WatchlistSignalPollerTest
- run skill (project override)
- NotificationServiceTest
- security-review skill
- apiFetch
- FakeBinanceFuturesTradingServer
- WatchlistEntry
- SignalCallEntry
- general-purpose agent (implementation)
- RetryingBrokerAdapter
- Candle
- .evaluate
- LiveSignalDriftServiceTest
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
- Ticker
- CredentialEncryptionServiceTest
- Checkpoint
- MaCrossoverSeparationCalibrationTest
- .getPriceHistory
- NotificationController
- OrderControllerTest
- MacdHistogramMagnitudeCalibrationTest
- NotificationControllerTest
- SignalDriftControllerIntegrationTest
- TickerMetrics.tsx
- .findFirstCrossing
- .getAccountStatus
- mvnw
- Changelog
- WeightedVoteBacktestTest
- AuthController.java
- NotificationExceptionHandler.java
- TradingModeExceptionHandler.java
- plugins
- .placeOrder
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
- CheckpointStats
- MarketHoursServiceTest
- .run
- RsiOversoldRecalibrationTest
- TickerControllerTest
- tsconfig.json
- Bluesky Icon (SVG symbol)
- Discord Icon (SVG symbol)
- Documentation Icon (SVG symbol)
- App Favicon (Purple Lightning-Bolt Glyph)
- com.autotrade.dashboard:backend
- killswitch/api.ts
- StockPerSymbolRsiOverboughtCalibrationTest
- MarketDataController
- MarketDataService
- TickerServiceTest
- TradingModeBanner.tsx
- SecurityConfigTest
- SignalRuleId
- .handleRateLimited
- CredentialEncryptionService
- E8-F2-S3 — funding-rate carry cost in the backtest's transaction-cost model
- SignalDriftController
- BacktestConfig
- DecryptedCredential
- ThresholdCalibrationTest
- Backing up and restoring the Oracle instance (E7-F3-S1)
- BrokerCredentialServiceFindTest.java
- Notification system + WatchlistSignalPoller
- IndicatorId
- db-backup.sh
- db-restore.sh
- AlpacaTradingAdapterContractTest
- BinanceFuturesTradingAdapterContractTest
- RetryingAlpacaTradingAdapterContractTest
- RetryingBinanceFuturesTradingAdapterContractTest
- LiveSignalDriftService.java
- MockBrokerAdapterContractTest
- E8-F1-S7 — evaluate the per-symbol RSI override and SELL-side regime gate against a stock fixture
- Override
- RetryingMockBrokerAdapterContractTest
- .readDecrypted
- OrderServiceTest.java
- InvalidTradeRequestException
- OrderNotFoundException
- OrderRefreshUnavailableException
- SignalNotActionableException
- PerSymbolAdxThresholds
- DirectionalOutcomeStats
- PaperTradeThresholdNotMetException

## God Nodes (most connected - your core abstractions)
1. `TradingMode` - 125 edges
2. `Candle` - 120 edges
3. `Ticker` - 108 edges
4. `Broker` - 107 edges
5. `Order` - 93 edges
6. `AssetType` - 91 edges
7. `Changelog` - 79 edges
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

## Communities (156 total, 26 thin omitted)

### Community 0 - "BrokerOrderRequest"
Cohesion: 0.17
Nodes (7): BrokerOrderRequest, Override, MockBrokerAdapter, MockOrderState, PositionState, Test, MockBrokerAdapterTest

### Community 1 - "BinanceFuturesTradingAdapter"
Cohesion: 0.06
Nodes (23): Credentials, BinanceAccountAsset, BinanceAccountResponse, BinanceAlgoOrderResponse, BinanceErrorResponse, BinanceFuturesTradingAdapter, BinanceOrderResponse, BinancePositionResponse (+15 more)

### Community 2 - "AlpacaTradingAdapter"
Cohesion: 0.08
Nodes (22): AlpacaAccountResponse, AlpacaBracketLeg, AlpacaErrorResponse, AlpacaOrderRequestBody, AlpacaOrderResponse, AlpacaPositionResponse, AlpacaStopLeg, AlpacaTradingAdapter (+14 more)

### Community 3 - "Order"
Cohesion: 0.07
Nodes (6): Entity, Override, PrePersist, PreUpdate, Table, Order

### Community 4 - "TradingModeResponse"
Cohesion: 0.26
Nodes (6): TradingModeResponse, AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, TradingModeControllerTest

### Community 6 - "AlpacaMarketDataClient"
Cohesion: 0.09
Nodes (23): AlpacaBar, AlpacaBarsResponse, AlpacaMarketDataClient, Candle, Component, JsonIgnoreProperties, Override, RestClient (+15 more)

### Community 7 - ".calculate"
Cohesion: 0.09
Nodes (17): HoldTermRule, MODERATE_HIGH, MODERATE_LOW, MODERATE_MEDIUM, STRONG_HIGH, STRONG_LOW, STRONG_MEDIUM, match() (+9 more)

### Community 8 - "OrderAuditEntry"
Cohesion: 0.19
Nodes (5): Entity, Override, PrePersist, Table, OrderAuditEntry

### Community 9 - "TradingMode"
Cohesion: 0.08
Nodes (36): Autowired, BrokerCredential, Entity, PrePersist, PreUpdate, Table, BrokerCredentialService, Logger (+28 more)

### Community 10 - "IndicatorSnapshot"
Cohesion: 0.10
Nodes (5): IndicatorSnapshot, Entity, Override, PrePersist, Table

### Community 11 - "devDependencies"
Cohesion: 0.05
Nodes (36): dependencies, lightweight-charts, react, react-dom, react-router-dom, devDependencies, oxlint, @types/node (+28 more)

### Community 12 - "Test"
Cohesion: 0.11
Nodes (8): Regime, RANGING, TRENDING, RegimeGatedRuleEngine, Test, RegimeClassifierTest, Test, RegimeGatedRuleEngineTest

### Community 13 - "CoreDataModelIntegrationTest.java"
Cohesion: 0.14
Nodes (11): BrokerCredentialRepository, TickerRepository, CoreDataModelIntegrationTest, SpringBootTest, Test, Transactional, SpringBootTest, Test (+3 more)

### Community 14 - ".calculate"
Cohesion: 0.24
Nodes (5): AdxCalculator, MathContext, AdxCalculatorTest, Candle, Test

### Community 15 - "TradingModeServiceTest"
Cohesion: 0.23
Nodes (5): SpringBootTest, Test, TestPropertySource, Transactional, TradingModeServiceTest

### Community 16 - "MarketDataExceptionHandler"
Cohesion: 0.12
Nodes (11): InsufficientPriceHistoryException, InvalidPriceHistoryRequestException, ExceptionHandler, Logger, ResponseEntity, RestControllerAdvice, MarketDataExceptionHandler, MarketDataUnavailableException (+3 more)

### Community 17 - "RiskLimitService"
Cohesion: 0.20
Nodes (7): Logger, Service, RiskLimitService, ConfigurationProperties, RiskLimitsProperties, Test, RiskLimitServiceTest

### Community 18 - "OrderQueryControllerTest"
Cohesion: 0.09
Nodes (13): Page, PagedResponse, GetMapping, PostMapping, RequestMapping, ResponseEntity, RestController, OrderQueryController (+5 more)

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

### Community 25 - ".resolveOrRegister"
Cohesion: 0.22
Nodes (6): Transactional, WatchlistEntry, SpringBootTest, Test, Transactional, WatchlistServiceTest

### Community 26 - "order/api.ts"
Cohesion: 0.11
Nodes (26): RFC-6266, AuditEntry, AuditEntryPage, fetchAuditEntries(), SignalCall, SignalRuleId, AuditTrail(), describeError() (+18 more)

### Community 27 - "PriceChart.tsx"
Cohesion: 0.18
Nodes (18): Broker, ChartDataResponse, ChartIndicatorPoint, CandlestickPoint, LinePoint, toBusinessDay(), toCandlestickSeries(), toMaLongSeries() (+10 more)

### Community 28 - "FakeAlpacaTradingServer"
Cohesion: 0.32
Nodes (7): FakeAlpacaTradingServer, FakeOrder, ClientHttpRequest, ClientHttpResponse, HttpStatus, ObjectMapper, MockClientHttpRequest

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

### Community 33 - ".calculate"
Cohesion: 0.24
Nodes (5): MacdResult, MathContext, MacdCalculator, Test, MacdCalculatorTest

### Community 34 - "RiskConsentEvent"
Cohesion: 0.24
Nodes (5): Entity, Override, PrePersist, Table, RiskConsentEvent

### Community 35 - "IndicatorService"
Cohesion: 0.09
Nodes (17): ChartDataResponse, ChartIndicatorPoint, IndicatorController, GetMapping, RequestMapping, RestController, BigDecimalIndicators, IndicatorService (+9 more)

### Community 36 - ".calculate"
Cohesion: 0.24
Nodes (5): MathContext, MovingAverageResult, MovingAverageCrossoverCalculator, Test, MovingAverageCrossoverCalculatorTest

### Community 37 - "OrderServiceTest"
Cohesion: 0.27
Nodes (5): PlaceOrderRequest, SignalComputation, ExtendWith, Test, OrderServiceTest

### Community 38 - "KillSwitchService"
Cohesion: 0.06
Nodes (32): EngageKillSwitchResponse, KillSwitchCancelSummary, GetMapping, PostMapping, RequestMapping, RestController, KillSwitchController, Entity (+24 more)

### Community 39 - "compilerOptions"
Cohesion: 0.10
Nodes (19): compilerOptions, allowImportingTsExtensions, erasableSyntaxOnly, lib, module, moduleDetection, noEmit, noFallthroughCasesInSwitch (+11 more)

### Community 40 - "BacktestHarnessTest"
Cohesion: 0.31
Nodes (3): Candle, BacktestHarnessTest, Test

### Community 41 - "TickerService"
Cohesion: 0.17
Nodes (7): Service, Transactional, TickerService, Query, WatchlistEntryRepository, Service, WatchlistService

### Community 42 - "WatchlistSignalPollerTest"
Cohesion: 0.22
Nodes (9): Component, ConditionalOnProperty, Logger, Scheduled, WatchlistSignalPoller, BeforeEach, ExtendWith, Test (+1 more)

### Community 43 - "run skill (project override)"
Cohesion: 0.14
Nodes (19): run skill (project override), MarketHoursService (hardcoded NYSE/NASDAQ calendar), oracle-xe Docker Compose service, F1.1 Local dev environment, E1-F1-S1 Docker Compose file for Oracle XE, E1-F1-S2 Spring Boot backend skeleton, E1-F1-S3 React app skeleton with routing, E1-F1-S4 CI pipeline builds/tests both apps (+11 more)

### Community 44 - "NotificationServiceTest"
Cohesion: 0.29
Nodes (3): ExtendWith, Test, NotificationServiceTest

### Community 45 - "security-review skill"
Cohesion: 0.19
Nodes (18): guardrail-check skill, security-review skill, simplify skill, F1.3 Secrets & config management, E1-F3-S1 Broker API keys encrypted at rest, E1-F3-S2 Dashboard requires login, F4.2 Alpaca adapter (stocks), F6.1 Paper/live mode toggle (+10 more)

### Community 46 - "apiFetch"
Cohesion: 0.18
Nodes (20): apiFetch(), readCookie(), AssetType, fetchPriceHistory(), MarketDataErrorCode, parseMarketDataError(), PriceHistoryResponse, fetchNotifications() (+12 more)

### Community 47 - "FakeBinanceFuturesTradingServer"
Cohesion: 0.27
Nodes (8): FakeBinanceFuturesTradingServer, ClientHttpRequest, ClientHttpResponse, HttpStatus, StoredAlgoOrder, StoredOrder, MultiValueMap, URI

### Community 48 - "WatchlistEntry"
Cohesion: 0.17
Nodes (5): Entity, Override, PrePersist, Table, WatchlistEntry

### Community 49 - "SignalCallEntry"
Cohesion: 0.09
Nodes (13): IndicatorComputation, Entity, Override, PrePersist, Table, SignalCallEntry, IndicatorFactory, BeforeEach (+5 more)

### Community 50 - "general-purpose agent (implementation)"
Cohesion: 0.24
Nodes (16): Explore agent (research), general-purpose agent (implementation), Plan agent (design gate), Mandatory per-story workflow (update CLAUDE.md, commit, push), CLAUDE.md project status & architecture log, E1 — Platform Foundation, F1.2 Core data model, F1.4 Testing strategy (+8 more)

### Community 51 - "RetryingBrokerAdapter"
Cohesion: 0.20
Nodes (4): BrokerAdapterRetryPolicy, Logger, Override, RetryingBrokerAdapter

### Community 52 - "Candle"
Cohesion: 0.07
Nodes (17): MathContext, RsiCalculator, MathContext, VolatilityCalculator, VolumeTrendCalculator, Candle, FixtureSplits, E2ECandleFixtures (+9 more)

### Community 53 - ".evaluate"
Cohesion: 0.09
Nodes (9): IndicatorVotes, IndicatorWeights, WeightedVoteRuleEngine, MacdResult, Test, SignalRuleEngineTest, MacdResult, Test (+1 more)

### Community 54 - "LiveSignalDriftServiceTest"
Cohesion: 0.44
Nodes (4): Candle, ExtendWith, Test, LiveSignalDriftServiceTest

### Community 55 - "adapter-contract-check skill"
Cohesion: 0.18
Nodes (15): adapter-contract-check skill, BrokerAdapter interface (E4-F1-S1), OrderService.submitOrder (bracket order construction), RetryingBrokerAdapter decorator (retry/backoff/outage), TradingModeService (paper/live switch, append-only), F4.1 Adapter interface, E4-F1-S1 BrokerAdapter interface + mock + contract suite, E4-F1-S2 Rate-limit/retry/backoff in adapter contract (+7 more)

### Community 56 - "WatchlistController"
Cohesion: 0.25
Nodes (8): GetMapping, PostMapping, RequestMapping, ResponseEntity, RestController, WatchlistController, WatchlistEntryResponse, DeleteMapping

### Community 57 - "TradingModeController"
Cohesion: 0.25
Nodes (6): TradingModeChangeRequest, GetMapping, PostMapping, RequestMapping, RestController, TradingModeController

### Community 58 - "BrokerAdapterConfig.java"
Cohesion: 0.27
Nodes (8): AlpacaTradingProperties, ConfigurationProperties, BrokerAdapterConfig, Bean, Configuration, EnableConfigurationProperties, RestClient, SimpleClientHttpRequestFactory

### Community 59 - "BinanceFuturesAdapterConfig.java"
Cohesion: 0.27
Nodes (8): BinanceFuturesAdapterConfig, Bean, Configuration, EnableConfigurationProperties, RestClient, SimpleClientHttpRequestFactory, BinanceFuturesTradingProperties, ConfigurationProperties

### Community 60 - "LiveDriftBaselineTest.java"
Cohesion: 0.27
Nodes (3): BacktestCandleCsvLoader, Test, LiveDriftBaselineTest

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
Cohesion: 0.12
Nodes (5): BrokerOrderResult, KillSwitchCancelSummary, BrokerAdapterRouterTest, Test, Test

### Community 65 - "WatchlistControllerTest"
Cohesion: 0.26
Nodes (7): AddWatchlistEntryRequest, AutoConfigureMockMvc, MockMvc, ObjectMapper, Test, WebMvcTest, WatchlistControllerTest

### Community 66 - "RiskLimitConfig.java"
Cohesion: 0.83
Nodes (3): Configuration, EnableConfigurationProperties, RiskLimitConfig

### Community 67 - "Broker"
Cohesion: 0.10
Nodes (10): Broker, ALPACA, BINANCE, BrokerAdapterAmbiguousOrderException, BrokerAdapterException, BrokerAdapterRateLimitedException, BrokerAdapterTransientException, BrokerAdapterUnavailableException (+2 more)

### Community 68 - "Ticker"
Cohesion: 0.12
Nodes (6): Entity, Override, PrePersist, Table, Ticker, TickerNotRegisteredException

### Community 70 - "Checkpoint"
Cohesion: 0.09
Nodes (18): Checkpoint, MAX, MID, MIN, DirectionalAccumulator, DirectionalOutcome, LOSS, WASH (+10 more)

### Community 71 - "MaCrossoverSeparationCalibrationTest"
Cohesion: 0.36
Nodes (3): Test, MaCrossoverSeparationCalibrationTest, SymbolFixture

### Community 72 - ".getPriceHistory"
Cohesion: 0.18
Nodes (9): PriceHistoryResult, IndicatorServiceTest, ExtendWith, Test, AutoConfigureMockMvc, MockMvc, Test, WebMvcTest (+1 more)

### Community 73 - "NotificationController"
Cohesion: 0.29
Nodes (6): GetMapping, PostMapping, RequestMapping, ResponseEntity, RestController, NotificationController

### Community 74 - "OrderControllerTest"
Cohesion: 0.15
Nodes (10): JsonInclude, TradeOrderResponse, KillSwitchEngagedException, RiskLimitExceededException, AutoConfigureMockMvc, MockMvc, ObjectMapper, Test (+2 more)

### Community 75 - "MacdHistogramMagnitudeCalibrationTest"
Cohesion: 0.36
Nodes (3): Test, MacdHistogramMagnitudeCalibrationTest, SymbolFixture

### Community 76 - "NotificationControllerTest"
Cohesion: 0.20
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

### Community 80 - ".getAccountStatus"
Cohesion: 0.26
Nodes (4): AssetBalance, BrokerAccountStatus, Test, RetryingBrokerAdapterTest

### Community 81 - "mvnw"
Cohesion: 0.33
Nodes (6): mvnw script, clean(), die(), exec_maven(), set_java_home(), verbose()

### Community 82 - "Changelog"
Cohesion: 0.03
Nodes (77): Changelog, Dark-first premium visual pass, E1-F1-S1 — local Oracle XE via Docker Compose, E1-F1-S2 — Spring Boot backend skeleton, E1-F1-S3 — React app skeleton, E1-F1-S4 — CI pipeline, E1-F1-S5 — env/config profiles, E1-F2 — core data model (+69 more)

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

### Community 90 - "TradingModeService"
Cohesion: 0.18
Nodes (5): RiskConsentEventRepository, TradingModeEventRepository, Service, TradingModeService, JpaRepository

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

### Community 95 - ".export"
Cohesion: 0.23
Nodes (3): OrderCsvExporter, Test, OrderCsvExporterTest

### Community 96 - "BackendApplicationTests.java"
Cohesion: 0.60
Nodes (3): BackendApplicationTests, SpringBootTest, Test

### Community 97 - "OrderService"
Cohesion: 0.09
Nodes (21): BrokerAdapter, BrokerAdapterRouter, Service, PostMapping, RequestMapping, ResponseEntity, RestController, OrderController (+13 more)

### Community 98 - "OrderAuditControllerIntegrationTest"
Cohesion: 0.29
Nodes (7): AutoConfigureMockMvc, Cookie, MockHttpSession, MockMvc, SpringBootTest, Test, OrderAuditControllerIntegrationTest

### Community 99 - "TickerController"
Cohesion: 0.36
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

### Community 104 - "CheckpointStats"
Cohesion: 0.14
Nodes (7): CheckpointStats, CheckpointStatsTest, Test, HorizonCandidate, IndicatorExpectancyAlternateHorizonCalibrationTest, Test, IndicatorExpectancyCalibrationTest

### Community 106 - ".run"
Cohesion: 0.11
Nodes (11): BacktestHarness, HoldGateAccumulator, IndicatorAccumulator, FunctionalInterface, RuleEvaluator, HoldGateOutcome, LARGE_MOVE, STABLE (+3 more)

### Community 108 - "TickerControllerTest"
Cohesion: 0.32
Nodes (7): RegisterTickerRequest, AutoConfigureMockMvc, MockMvc, ObjectMapper, Test, WebMvcTest, TickerControllerTest

### Community 117 - "killswitch/api.ts"
Cohesion: 0.32
Nodes (10): clearKillSwitch(), engageKillSwitch(), EngageKillSwitchResponse, fetchKillSwitchState(), KillSwitchCancelSummary, KillSwitchResponse, KillSwitchState, describeError() (+2 more)

### Community 119 - "MarketDataController"
Cohesion: 0.43
Nodes (4): GetMapping, RequestMapping, RestController, MarketDataController

### Community 120 - "MarketDataService"
Cohesion: 0.17
Nodes (9): MarketDataClient, Service, MarketDataService, Component, MarketHoursService, BeforeEach, ExtendWith, Test (+1 more)

### Community 121 - "TickerServiceTest"
Cohesion: 0.43
Nodes (4): SpringBootTest, Test, Transactional, TickerServiceTest

### Community 122 - "TradingModeBanner.tsx"
Cohesion: 0.42
Nodes (8): fetchTradingMode(), giveRiskConsent(), switchTradingMode(), TradingMode, TradingModeState, describeError(), otherMode(), TradingModeBanner()

### Community 123 - "SecurityConfigTest"
Cohesion: 0.32
Nodes (6): AutoConfigureMockMvc, Cookie, MockMvc, SpringBootTest, Test, SecurityConfigTest

### Community 124 - "SignalRuleId"
Cohesion: 0.09
Nodes (23): AuditEntryResponse, JsonInclude, HoldTerm, HoldTermCalculator, PerSymbolRuleThresholds, SignalCall, BUY, HOLD (+15 more)

### Community 126 - "CredentialEncryptionService"
Cohesion: 0.39
Nodes (4): CredentialEncryptionService, Component, Logger, SecretKeySpec

### Community 127 - "E8-F2-S3 — funding-rate carry cost in the backtest's transaction-cost model"
Cohesion: 0.33
Nodes (6): Design gate, E8-F2-S3 — funding-rate carry cost in the backtest's transaction-cost model, Illustrative figures (not a ship/no-ship story — purely additive), Implementation, Scope / no-op confirmation, Test coverage

### Community 128 - "SignalDriftController"
Cohesion: 0.29
Nodes (6): ConditionalOnProperty, GetMapping, RequestMapping, RestController, SignalDriftController, SignalDriftReport

### Community 130 - "DecryptedCredential"
Cohesion: 0.15
Nodes (8): DecryptedCredential, Override, BeforeEach, BeforeEach, RestClient, RestClient, BeforeEach, BeforeEach

### Community 131 - "ThresholdCalibrationTest"
Cohesion: 0.39
Nodes (3): Test, NamedCandidate, ThresholdCalibrationTest

### Community 132 - "Backing up and restoring the Oracle instance (E7-F3-S1)"
Cohesion: 0.40
Nodes (4): Backing up and restoring the Oracle instance (E7-F3-S1), Backup procedure, Notes, Restore-test procedure

### Community 133 - "BrokerCredentialServiceFindTest.java"
Cohesion: 0.39
Nodes (4): BrokerCredentialServiceFindTest, SpringBootTest, Test, Transactional

### Community 134 - "Notification system + WatchlistSignalPoller"
Cohesion: 0.40
Nodes (5): Notification system + WatchlistSignalPoller, Watchlist feature (watchlist_entries), F3.3 Watchlist (stretch), E3-F3-S1 Watchlist persisted in Oracle DB, F5.4 Notifications

### Community 135 - "IndicatorId"
Cohesion: 0.19
Nodes (7): IndicatorId, MA_CROSSOVER, MACD, RSI, Test, Test, OutOfSampleValidationTest

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

### Community 142 - "LiveSignalDriftService.java"
Cohesion: 0.19
Nodes (10): Component, ConditionalOnProperty, Logger, Scheduled, LiveSignalDriftService, Page, Pageable, Query (+2 more)

### Community 144 - "E8-F1-S7 — evaluate the per-symbol RSI override and SELL-side regime gate against a stock fixture"
Cohesion: 0.40
Nodes (5): Design gate, E8-F1-S7 — evaluate the per-symbol RSI override and SELL-side regime gate against a stock fixture, Findings — no ship on either axis, but not for the same reason as any prior E8-F1 no-ship, Implementation, Scope / no-op confirmation

### Community 147 - ".readDecrypted"
Cohesion: 0.22
Nodes (5): Transactional, BrokerCredentialServiceRotationTest, SpringBootTest, Test, Transactional

### Community 148 - "OrderServiceTest.java"
Cohesion: 0.12
Nodes (17): IndicatorResponse, MacdResult, MovingAverageRelation, EQUAL, SHORT_ABOVE_LONG, SHORT_BELOW_LONG, MovingAverageResult, MarketClosedException (+9 more)

### Community 154 - "PerSymbolAdxThresholds"
Cohesion: 0.31
Nodes (3): PerSymbolAdxThresholds, Test, PerSymbolAdxThresholdsTest

### Community 157 - "DirectionalOutcomeStats"
Cohesion: 0.10
Nodes (11): DirectionalOutcomeStats, RegimeClassifier, Test, PerSymbolAdxTrendingThresholdCalibrationTest, SymbolFixture, Test, RegimeCalibrationTest, Test (+3 more)

## Knowledge Gaps
- **284 isolated node(s):** `com.autotrade.dashboard:backend`, `MIN`, `MID`, `MAX`, `WIN` (+279 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **26 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Candle` connect `Candle` to `ThresholdCalibrationTest`, `AlpacaMarketDataClient`, `IndicatorId`, `TradingMode`, `.calculate`, `LiveSignalDriftService.java`, `BinanceMarketDataClient`, `OrderServiceTest.java`, `BacktestReport`, `DirectionalOutcomeStats`, `.calculate`, `IndicatorService`, `.calculate`, `BacktestHarnessTest`, `SignalCallEntry`, `LiveSignalDriftServiceTest`, `LiveDriftBaselineTest.java`, `Broker`, `Ticker`, `MaCrossoverSeparationCalibrationTest`, `.getPriceHistory`, `MacdHistogramMagnitudeCalibrationTest`, `.findFirstCrossing`, `WeightedVoteBacktestTest`, `CheckpointStats`, `.run`, `RsiOversoldRecalibrationTest`, `StockPerSymbolRsiOverboughtCalibrationTest`, `MarketDataService`, `SignalRuleId`?**
  _High betweenness centrality (0.105) - this node is a cross-community bridge._
- **Why does `TradingMode` connect `TradingMode` to `BrokerOrderRequest`, `BinanceFuturesTradingAdapter`, `AlpacaTradingAdapter`, `Order`, `TradingModeResponse`, `BrokerCredentialServiceFindTest.java`, `BrokerAdapterContractTest`, `AlpacaTradingAdapterContractTest`, `BinanceFuturesTradingAdapterContractTest`, `RetryingAlpacaTradingAdapterContractTest`, `CoreDataModelIntegrationTest.java`, `RetryingBinanceFuturesTradingAdapterContractTest`, `TradingModeServiceTest`, `MockBrokerAdapterContractTest`, `OrderQueryControllerTest`, `.readDecrypted`, `RetryingMockBrokerAdapterContractTest`, `OrderServiceTest.java`, `TradingModeEvent`, `.run`, `OrderServiceTest`, `RetryingBrokerAdapter`, `TradingModeController`, `BrokerAdapterConfig.java`, `BinanceFuturesAdapterConfig.java`, `BrokerOrderResult`, `Broker`, `.getAccountStatus`, `.exportOrdersCsv`, `.placeOrder`, `TradingModeService`, `OrderService`, `SignalRuleId`?**
  _High betweenness centrality (0.083) - this node is a cross-community bridge._
- **Why does `BrokerCredentialService` connect `TradingMode` to `BinanceFuturesTradingAdapter`, `DecryptedCredential`, `AlpacaTradingAdapter`, `BrokerCredentialServiceFindTest.java`, `AlpacaTradingAdapterContractTest`, `BinanceFuturesTradingAdapterContractTest`, `RetryingAlpacaTradingAdapterContractTest`, `CoreDataModelIntegrationTest.java`, `RetryingBinanceFuturesTradingAdapterContractTest`, `.readDecrypted`, `OrderServiceTest.java`, `.run`, `OrderServiceTest`, `security-review skill`, `BrokerAdapterConfig.java`, `BinanceFuturesAdapterConfig.java`, `Broker`, `OrderService`, `CredentialEncryptionService`?**
  _High betweenness centrality (0.067) - this node is a cross-community bridge._
- **Are the 6 inferred relationships involving `Candle` (e.g. with `.chartData_marketClosedWithCachedFallback_returns200Stale()` and `.chartData_registeredTicker_returns200WithCandlesAndIndicators()`) actually correct?**
  _`Candle` has 6 INFERRED edges - model-reasoned connections that need verification._
- **What connects `com.autotrade.dashboard:backend`, `MIN`, `MID` to the rest of the system?**
  _284 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `BinanceFuturesTradingAdapter` be split into smaller, more focused modules?**
  _Cohesion score 0.0629076372817168 - nodes in this community are weakly interconnected._
- **Should `AlpacaTradingAdapter` be split into smaller, more focused modules?**
  _Cohesion score 0.08397337429595494 - nodes in this community are weakly interconnected._