# Graph Report - agent testing  (2026-08-18)

## Corpus Check
- 363 files · ~215,904 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 3256 nodes · 9211 edges · 172 communities (142 shown, 30 thin omitted)
- Extraction: 87% EXTRACTED · 13% INFERRED · 0% AMBIGUOUS · INFERRED: 1205 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `a418da19`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- BinanceFuturesTradingAdapterTest
- Changelog
- KillSwitchService
- .readDecrypted
- .placeOrder
- BinanceFuturesTradingAdapter
- DirectionalOutcomeStats
- Candle
- AlpacaMarketDataClient
- devDependencies
- Test
- OrderServiceTest.java
- .calculate
- AssetType
- Order
- .evaluate
- order/api.ts
- RegimeOutOfSampleValidationTest
- MacdResult
- IndicatorTestFixtures
- AlpacaTradingAdapter
- SecurityConfig.java
- IndicatorSnapshot
- SignalService
- BrokerCredential
- OrderQueryControllerTest
- CoreDataModelIntegrationTest.java
- DashboardPage.tsx
- BrokerCredentialService
- .evaluate
- IndicatorService
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
- OrderAuditEntry
- apiFetch
- PriceChart.tsx
- .getPriceHistory
- Notification
- RiskLimitService
- compilerOptions
- ApiErrorResponse
- MarketHoursServiceTest
- MockBrokerAdapter
- VolatilityBandCutoffCalibrationTest
- NotificationService
- BrokerCredentialServiceRotationTest.java
- compilerOptions
- UnanimityGapAnalysisTest
- FakeBinanceFuturesTradingServer
- run skill (project override)
- TradeForm.tsx
- .calculate
- LiveSignalDriftServiceTest
- CredentialEncryptionServiceTest
- .resolveOrRegister
- TickerService
- security-review skill
- TradingMode
- TradingModeResponse
- WatchlistControllerTest
- BrokerAdapterContractTest
- .export
- general-purpose agent (implementation)
- .run
- .fetchRecentCandles
- TickerController
- .calculate
- TradingModeEvent
- WatchlistEntry
- adapter-contract-check skill
- BrokerAdapterConfig.java
- BinanceFuturesAdapterConfig.java
- SignalDriftControllerIntegrationTest
- E8-F1-S7 — evaluate the per-symbol RSI override and SELL-side regime gate against a stock fixture
- .fetchRecentCandles
- .applySellGate
- StockMaCrossoverSeparationCalibrationTest
- MarketDataClientConfig.java
- WeightedVoteBacktestTest
- TradingModeController
- WatchlistController
- PerSymbolMacdHistogramMagnitudeCalibrationTest
- OrderAuditControllerIntegrationTest
- RetryHelper
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
- RiskExceptionHandler.java
- StockPerSymbolMacdHistogramMagnitudeCalibrationTest
- AlpacaMarketDataProperties
- StockRegimeOutOfSampleValidationTest
- .calculate
- TradingModeExceptionHandler.java
- LiveSignalDriftService.java
- MacdHistogramMagnitudeCalibrationTest
- MaCrossoverSeparationCalibrationTest
- E8-F6-S2 — `VOLATILITY_LOW_MAX`/`VOLATILITY_MEDIUM_MAX` cutoff calibration (no ship)
- RsiOverboughtRecalibrationTest
- BacktestReport
- StockPerSymbolRsiOverboughtCalibrationTest
- plugins
- mvnw
- E8-F6-S3 — is `TrendStrength.STRONG` real-but-rare or structurally unreachable? (leave as-is)
- OrderService
- BinanceMarketDataProperties
- NotificationExceptionHandler.java
- E8-F1-S11 — SELL-only MA-crossover separation gate, evaluated and shipped
- InvalidTradeRequestException
- SellMaCrossoverSeparationCalibrationTest
- CredentialEncryptionService
- TradingModeBanner.tsx
- AuthController.java
- E8-F1-S8 — per-symbol `macdMinHistogramMagnitudePct` calibration on the BUY side
- PerSymbolAdxThresholds
- .getId
- OrderNotFoundException
- OrderRefreshUnavailableException
- RetryingAlpacaTradingAdapterContractTest
- SignalNotActionableException
- signal-rule-review skill
- E8-F3-S6 — `WEIGHTED_MAJORITY_FRACTION` calibration
- .handleRateLimited
- E8-F2-S3 — funding-rate carry cost in the backtest's transaction-cost model
- BrokerCredentialServiceFindTest.java
- PerSymbolAdxTrendingThresholdCalibrationTest
- dataviz skill
- TradingModeService
- ClockConfig.java
- E8-F1-S9 — SELL-only MACD histogram-magnitude gate, evaluated and no-shipped
- PaperTradeThresholdNotMetException
- BackendApplicationTests.java
- E8-F1-S12 — AAPL evaluated against the MACD-magnitude and MA-crossover-separation axes
- E8-F1-S10 — per-symbol MA-crossover separation calibration, evaluated and no-shipped
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
- SignalDriftController
- RegimeCalibrationTest
- E8-F5-S2 — funding-adjusted live signal-drift monitoring
- Notification system + WatchlistSignalPoller

## God Nodes (most connected - your core abstractions)
1. `Candle` - 156 edges
2. `TradingMode` - 125 edges
3. `Ticker` - 111 edges
4. `Broker` - 107 edges
5. `AssetType` - 95 edges
6. `Order` - 93 edges
7. `Changelog` - 89 edges
8. `SignalRuleId` - 70 edges
9. `RuleThresholds` - 66 edges
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

## Communities (172 total, 30 thin omitted)

### Community 0 - "BinanceFuturesTradingAdapterTest"
Cohesion: 0.12
Nodes (7): Credentials, Override, BrokerOrderRequest, BinanceFuturesTradingAdapterTest, ExtendWith, MockRestServiceServer, Test

### Community 1 - "Changelog"
Cohesion: 0.03
Nodes (77): Changelog, Dark-first premium visual pass, E1-F1-S1 — local Oracle XE via Docker Compose, E1-F1-S2 — Spring Boot backend skeleton, E1-F1-S3 — React app skeleton, E1-F1-S4 — CI pipeline, E1-F1-S5 — env/config profiles, E1-F2 — core data model (+69 more)

### Community 2 - "KillSwitchService"
Cohesion: 0.06
Nodes (31): EngageKillSwitchResponse, GetMapping, PostMapping, RequestMapping, RestController, KillSwitchController, Entity, Override (+23 more)

### Community 3 - ".readDecrypted"
Cohesion: 0.12
Nodes (9): DecryptedCredential, Override, Transactional, BeforeEach, BeforeEach, RestClient, RestClient, BeforeEach (+1 more)

### Community 4 - ".placeOrder"
Cohesion: 0.16
Nodes (7): Override, RestClientException, TooManyRequests, AlpacaTradingAdapterTest, ExtendWith, MockRestServiceServer, Test

### Community 5 - "BinanceFuturesTradingAdapter"
Cohesion: 0.10
Nodes (17): Credentials, BinanceAccountAsset, BinanceAccountResponse, BinanceAlgoOrderResponse, BinanceErrorResponse, BinanceFuturesTradingAdapter, BinanceOrderResponse, BinancePositionResponse (+9 more)

### Community 6 - "DirectionalOutcomeStats"
Cohesion: 0.09
Nodes (13): Checkpoint, MAX, MID, MIN, CheckpointStats, DirectionalOutcomeStats, CheckpointDrift, CheckpointStatsTest (+5 more)

### Community 7 - "Candle"
Cohesion: 0.07
Nodes (32): MathContext, RsiCalculator, MathContext, VolatilityCalculator, VolumeTrendCalculator, Candle, HoldTermCalculator, MaCrossoverSellGate (+24 more)

### Community 8 - "AlpacaMarketDataClient"
Cohesion: 0.22
Nodes (8): AlpacaBar, AlpacaBarsResponse, AlpacaMarketDataClient, Component, JsonIgnoreProperties, Override, RestClient, TooManyRequests

### Community 9 - "devDependencies"
Cohesion: 0.05
Nodes (36): dependencies, lightweight-charts, react, react-dom, react-router-dom, devDependencies, oxlint, @types/node (+28 more)

### Community 10 - "Test"
Cohesion: 0.11
Nodes (8): Regime, RANGING, TRENDING, RegimeGatedRuleEngine, Test, RegimeClassifierTest, Test, RegimeGatedRuleEngineTest

### Community 11 - "OrderServiceTest.java"
Cohesion: 0.12
Nodes (19): IndicatorResponse, MovingAverageRelation, EQUAL, SHORT_ABOVE_LONG, SHORT_BELOW_LONG, MovingAverageResult, MarketClosedException, TickerSummary (+11 more)

### Community 12 - ".calculate"
Cohesion: 0.08
Nodes (19): HoldTermRule, MODERATE_HIGH, MODERATE_LOW, MODERATE_MEDIUM, STRONG_HIGH, STRONG_LOW, STRONG_MEDIUM, match() (+11 more)

### Community 13 - "AssetType"
Cohesion: 0.21
Nodes (11): BrokerPosition, EntryOrderType, LIMIT, MARKET, OrderSide, BUY, SELL, AssetType (+3 more)

### Community 14 - "Order"
Cohesion: 0.06
Nodes (6): Entity, Override, PrePersist, PreUpdate, Table, Order

### Community 15 - ".evaluate"
Cohesion: 0.17
Nodes (4): IndicatorVotes, MacdResult, Test, SignalRuleEngineTest

### Community 16 - "order/api.ts"
Cohesion: 0.11
Nodes (26): RFC-6266, AuditEntry, AuditEntryPage, fetchAuditEntries(), SignalCall, SignalRuleId, AuditTrail(), describeError() (+18 more)

### Community 18 - "MacdResult"
Cohesion: 0.20
Nodes (8): IndicatorComputation, MacdResult, IndicatorFactory, ExtendWith, FunctionalInterface, IndicatorResponse, Test, SignalServiceTest

### Community 19 - "IndicatorTestFixtures"
Cohesion: 0.13
Nodes (6): IndicatorTestFixtures, Candle, Test, VolatilityCalculatorTest, Test, VolumeTrendCalculatorTest

### Community 20 - "AlpacaTradingAdapter"
Cohesion: 0.11
Nodes (16): AlpacaAccountResponse, AlpacaBracketLeg, AlpacaErrorResponse, AlpacaOrderRequestBody, AlpacaOrderResponse, AlpacaPositionResponse, AlpacaStopLeg, AlpacaTradingAdapter (+8 more)

### Community 21 - "SecurityConfig.java"
Cohesion: 0.11
Nodes (22): CsrfCookieWriteFilter, Bean, Configuration, Logger, Override, PasswordEncoder, SecurityConfig, SpaCsrfTokenRequestHandler (+14 more)

### Community 22 - "IndicatorSnapshot"
Cohesion: 0.06
Nodes (11): IndicatorSnapshot, Entity, Override, PrePersist, Table, HoldTerm, Entity, Override (+3 more)

### Community 23 - "SignalService"
Cohesion: 0.16
Nodes (13): Component, ConditionalOnProperty, Logger, Scheduled, WatchlistSignalPoller, SignalCallEntryRepository, Service, SignalService (+5 more)

### Community 24 - "BrokerCredential"
Cohesion: 0.12
Nodes (6): BrokerCredential, Entity, Override, PrePersist, PreUpdate, Table

### Community 25 - "OrderQueryControllerTest"
Cohesion: 0.09
Nodes (15): Page, PagedResponse, AuditEntryResponse, JsonInclude, GetMapping, PostMapping, RequestMapping, ResponseEntity (+7 more)

### Community 26 - "CoreDataModelIntegrationTest.java"
Cohesion: 0.09
Nodes (15): BrokerCredentialRepository, IndicatorSnapshotRepository, TickerRepository, CoreDataModelIntegrationTest, SpringBootTest, Test, Transactional, SpringBootTest (+7 more)

### Community 27 - "DashboardPage.tsx"
Cohesion: 0.12
Nodes (21): App(), AuthContext, AuthContextValue, AuthProvider(), useAuth(), RequireAuth(), TabItem, Tabs() (+13 more)

### Community 28 - "BrokerCredentialService"
Cohesion: 0.10
Nodes (20): ApplicationRunner, AlpacaTradingCredentialBootstrap, ApplicationArguments, Component, Logger, Override, BinanceTradingCredentialBootstrap, ApplicationArguments (+12 more)

### Community 29 - ".evaluate"
Cohesion: 0.15
Nodes (5): IndicatorWeights, WeightedVoteRuleEngine, MacdResult, Test, WeightedVoteRuleEngineTest

### Community 30 - "IndicatorService"
Cohesion: 0.10
Nodes (16): ChartDataResponse, ChartIndicatorPoint, IndicatorController, GetMapping, RequestMapping, RestController, BigDecimalIndicators, IndicatorService (+8 more)

### Community 31 - "IndicatorId"
Cohesion: 0.19
Nodes (7): IndicatorId, MA_CROSSOVER, MACD, RSI, Test, Test, OutOfSampleValidationTest

### Community 32 - "OrderServiceTest"
Cohesion: 0.26
Nodes (4): PlaceOrderRequest, SignalComputation, Test, OrderServiceTest

### Community 33 - "BinanceMarketDataClient"
Cohesion: 0.23
Nodes (6): BinanceMarketDataClient, Candle, Component, Override, RestClient, TooManyRequests

### Community 34 - "MarketDataService"
Cohesion: 0.10
Nodes (14): MarketDataClient, GetMapping, RequestMapping, RestController, MarketDataController, Service, MarketDataService, Component (+6 more)

### Community 35 - "OrderControllerTest"
Cohesion: 0.15
Nodes (10): JsonInclude, TradeOrderResponse, KillSwitchEngagedException, RiskLimitExceededException, AutoConfigureMockMvc, MockMvc, ObjectMapper, Test (+2 more)

### Community 36 - "BrokerOrderResult"
Cohesion: 0.16
Nodes (3): BrokerOrderResult, KillSwitchCancelSummary, Test

### Community 37 - "TickerMetrics.tsx"
Cohesion: 0.10
Nodes (24): fetchChartData(), registerTicker(), TickerSummary, Broker, fetchSignal(), HoldTerm, IndicatorResponse, MacdResult (+16 more)

### Community 38 - "MarketDataExceptionHandler"
Cohesion: 0.13
Nodes (11): InsufficientPriceHistoryException, InvalidIndicatorRequestException, InvalidPriceHistoryRequestException, ExceptionHandler, Logger, ResponseEntity, RestControllerAdvice, MarketDataExceptionHandler (+3 more)

### Community 39 - "Ticker"
Cohesion: 0.12
Nodes (7): Autowired, Entity, Override, PrePersist, Table, Ticker, TickerNotRegisteredException

### Community 40 - "TradingModeServiceTest"
Cohesion: 0.23
Nodes (5): SpringBootTest, Test, TestPropertySource, Transactional, TradingModeServiceTest

### Community 41 - "OrderAuditEntry"
Cohesion: 0.10
Nodes (9): Entity, Override, PrePersist, Table, OrderAuditEntry, Page, Pageable, Query (+1 more)

### Community 42 - "apiFetch"
Cohesion: 0.18
Nodes (20): apiFetch(), readCookie(), AssetType, fetchPriceHistory(), MarketDataErrorCode, parseMarketDataError(), PriceHistoryResponse, fetchNotifications() (+12 more)

### Community 43 - "PriceChart.tsx"
Cohesion: 0.18
Nodes (18): Broker, ChartDataResponse, ChartIndicatorPoint, CandlestickPoint, LinePoint, toBusinessDay(), toCandlestickSeries(), toMaLongSeries() (+10 more)

### Community 44 - ".getPriceHistory"
Cohesion: 0.13
Nodes (9): PriceHistoryResult, IndicatorServiceTest, ExtendWith, Test, AutoConfigureMockMvc, MockMvc, Test, WebMvcTest (+1 more)

### Community 45 - "Notification"
Cohesion: 0.09
Nodes (16): Entity, Override, PrePersist, Table, Notification, JsonInclude, NotificationResponse, NotificationType (+8 more)

### Community 46 - "RiskLimitService"
Cohesion: 0.20
Nodes (7): Logger, Service, RiskLimitService, ConfigurationProperties, RiskLimitsProperties, Test, RiskLimitServiceTest

### Community 47 - "compilerOptions"
Cohesion: 0.08
Nodes (23): compilerOptions, allowArbitraryExtensions, allowImportingTsExtensions, erasableSyntaxOnly, jsx, lib, module, moduleDetection (+15 more)

### Community 48 - "ApiErrorResponse"
Cohesion: 0.34
Nodes (7): ApiErrorResponse, JsonInclude, ExceptionHandler, Logger, ResponseEntity, RestControllerAdvice, OrderExceptionHandler

### Community 50 - "MockBrokerAdapter"
Cohesion: 0.07
Nodes (17): AssetBalance, BrokerAdapterRetryPolicy, Logger, Override, RetryingBrokerAdapter, BrokerAdapterRouterTest, Test, Override (+9 more)

### Community 51 - "VolatilityBandCutoffCalibrationTest"
Cohesion: 0.10
Nodes (12): CrossingEvent, WalkForwardScorer, BacktestHarnessTpSlTest, Candle, Test, DayAccumulator, Test, SymbolFixture (+4 more)

### Community 52 - "NotificationService"
Cohesion: 0.10
Nodes (15): GetMapping, PostMapping, RequestMapping, ResponseEntity, RestController, NotificationController, Pageable, NotificationRepository (+7 more)

### Community 53 - "BrokerCredentialServiceRotationTest.java"
Cohesion: 0.60
Nodes (4): BrokerCredentialServiceRotationTest, SpringBootTest, Test, Transactional

### Community 54 - "compilerOptions"
Cohesion: 0.10
Nodes (19): compilerOptions, allowImportingTsExtensions, erasableSyntaxOnly, lib, module, moduleDetection, noEmit, noFallthroughCasesInSwitch (+11 more)

### Community 55 - "UnanimityGapAnalysisTest"
Cohesion: 0.35
Nodes (5): DirectionalTally, DissentTally, Test, SymbolFixture, UnanimityGapAnalysisTest

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
Cohesion: 0.25
Nodes (6): Transactional, WatchlistEntry, SpringBootTest, Test, Transactional, WatchlistServiceTest

### Community 63 - "TickerService"
Cohesion: 0.16
Nodes (7): Service, Transactional, TickerService, Query, WatchlistEntryRepository, Service, WatchlistService

### Community 64 - "security-review skill"
Cohesion: 0.21
Nodes (17): guardrail-check skill, security-review skill, simplify skill, F1.3 Secrets & config management, E1-F3-S1 Broker API keys encrypted at rest, E1-F3-S2 Dashboard requires login, F4.2 Alpaca adapter (stocks), F6.1 Paper/live mode toggle (+9 more)

### Community 65 - "TradingMode"
Cohesion: 0.09
Nodes (15): Broker, ALPACA, BINANCE, BrokerAccountStatus, BrokerAdapterAmbiguousOrderException, BrokerAdapterException, BrokerAdapterRateLimitedException, BrokerAdapterTransientException (+7 more)

### Community 66 - "TradingModeResponse"
Cohesion: 0.26
Nodes (6): TradingModeResponse, AutoConfigureMockMvc, MockMvc, Test, WebMvcTest, TradingModeControllerTest

### Community 67 - "WatchlistControllerTest"
Cohesion: 0.25
Nodes (7): AddWatchlistEntryRequest, AutoConfigureMockMvc, MockMvc, ObjectMapper, Test, WebMvcTest, WatchlistControllerTest

### Community 68 - "BrokerAdapterContractTest"
Cohesion: 0.09
Nodes (15): AlpacaTradingAdapterContractTest, ExtendWith, Override, BinanceFuturesTradingAdapterContractTest, ExtendWith, Override, BrokerAdapterContractTest, Test (+7 more)

### Community 69 - ".export"
Cohesion: 0.23
Nodes (3): OrderCsvExporter, Test, OrderCsvExporterTest

### Community 70 - "general-purpose agent (implementation)"
Cohesion: 0.24
Nodes (16): Explore agent (research), general-purpose agent (implementation), Plan agent (design gate), Mandatory per-story workflow (update CLAUDE.md, commit, push), CLAUDE.md project status & architecture log, E1 — Platform Foundation, F1.2 Core data model, F1.4 Testing strategy (+8 more)

### Community 71 - ".run"
Cohesion: 0.11
Nodes (9): LiveDriftBaseline, Candle, BacktestHarnessTest, Test, Test, SellMacdHistogramMagnitudeCalibrationTest, SymbolFixture, Test (+1 more)

### Community 72 - ".fetchRecentCandles"
Cohesion: 0.33
Nodes (4): Candle, AlpacaMarketDataClientTest, MockRestServiceServer, Test

### Community 73 - "TickerController"
Cohesion: 0.26
Nodes (6): PostMapping, RequestMapping, ResponseEntity, RestController, TickerController, TickerResponse

### Community 74 - ".calculate"
Cohesion: 0.16
Nodes (7): MacdResult, MathContext, MacdCalculator, Test, MacdCalculatorTest, Test, RsiCalculatorTest

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

### Community 82 - ".fetchRecentCandles"
Cohesion: 0.33
Nodes (4): BinanceMarketDataClientTest, BeforeEach, MockRestServiceServer, Test

### Community 83 - ".applySellGate"
Cohesion: 0.27
Nodes (4): MacdResult, MovingAverageResult, Test, MaCrossoverSellGateTest

### Community 85 - "MarketDataClientConfig.java"
Cohesion: 0.40
Nodes (6): Bean, Configuration, EnableConfigurationProperties, RestClient, SimpleClientHttpRequestFactory, MarketDataClientConfig

### Community 87 - "TradingModeController"
Cohesion: 0.25
Nodes (6): TradingModeChangeRequest, GetMapping, PostMapping, RequestMapping, RestController, TradingModeController

### Community 88 - "WatchlistController"
Cohesion: 0.28
Nodes (8): GetMapping, PostMapping, RequestMapping, ResponseEntity, RestController, WatchlistController, WatchlistEntryResponse, DeleteMapping

### Community 89 - "PerSymbolMacdHistogramMagnitudeCalibrationTest"
Cohesion: 0.32
Nodes (3): Test, PerSymbolMacdHistogramMagnitudeCalibrationTest, SymbolFixture

### Community 90 - "OrderAuditControllerIntegrationTest"
Cohesion: 0.29
Nodes (7): AutoConfigureMockMvc, Cookie, MockHttpSession, MockMvc, SpringBootTest, Test, OrderAuditControllerIntegrationTest

### Community 92 - "OrderStatus"
Cohesion: 0.17
Nodes (10): OrderStatus, CANCELLED, FAILED, FILLED, PARTIALLY_FILLED, PARTIALLY_PROTECTED, PENDING, REJECTED (+2 more)

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

### Community 97 - "FakeAlpacaTradingServer"
Cohesion: 0.32
Nodes (7): FakeAlpacaTradingServer, FakeOrder, ClientHttpRequest, ClientHttpResponse, HttpStatus, ObjectMapper, MockClientHttpRequest

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
Cohesion: 0.43
Nodes (4): GetMapping, RequestMapping, RestController, SignalController

### Community 102 - "RiskExceptionHandler.java"
Cohesion: 0.46
Nodes (5): ExceptionHandler, Logger, ResponseEntity, RestControllerAdvice, RiskExceptionHandler

### Community 104 - "AlpacaMarketDataProperties"
Cohesion: 0.50
Nodes (3): AlpacaMarketDataProperties, ConfigurationProperties, BeforeEach

### Community 106 - ".calculate"
Cohesion: 0.24
Nodes (5): MathContext, MovingAverageResult, MovingAverageCrossoverCalculator, Test, MovingAverageCrossoverCalculatorTest

### Community 107 - "TradingModeExceptionHandler.java"
Cohesion: 0.29
Nodes (6): RiskConsentNotGivenException, ExceptionHandler, Logger, ResponseEntity, RestControllerAdvice, TradingModeExceptionHandler

### Community 108 - "LiveSignalDriftService.java"
Cohesion: 0.06
Nodes (29): DirectionalAccumulator, DirectionalOutcome, LOSS, WASH, WIN, DirectionalScoreResult, ExitReason, HORIZON_EXPIRED (+21 more)

### Community 109 - "MacdHistogramMagnitudeCalibrationTest"
Cohesion: 0.36
Nodes (3): Test, MacdHistogramMagnitudeCalibrationTest, SymbolFixture

### Community 110 - "MaCrossoverSeparationCalibrationTest"
Cohesion: 0.36
Nodes (3): Test, MaCrossoverSeparationCalibrationTest, SymbolFixture

### Community 111 - "E8-F6-S2 — `VOLATILITY_LOW_MAX`/`VOLATILITY_MEDIUM_MAX` cutoff calibration (no ship)"
Cohesion: 0.50
Nodes (4): E8-F6-S2 — `VOLATILITY_LOW_MAX`/`VOLATILITY_MEDIUM_MAX` cutoff calibration (no ship), Findings, Mechanism, Outcome

### Community 113 - "BacktestReport"
Cohesion: 0.12
Nodes (8): BacktestReport, HoldGateStats, Test, PerSymbolMaCrossoverSeparationCalibrationTest, SymbolFixture, RegimeSplitStats, Test, RsiOversoldRecalibrationTest

### Community 115 - "plugins"
Cohesion: 0.22
Nodes (8): plugins, rules, react/only-export-components, react/rules-of-hooks, $schema, oxc, typescript, warn

### Community 116 - "mvnw"
Cohesion: 0.33
Nodes (6): mvnw script, clean(), die(), exec_maven(), set_java_home(), verbose()

### Community 117 - "E8-F6-S3 — is `TrendStrength.STRONG` real-but-rare or structurally unreachable? (leave as-is)"
Cohesion: 0.50
Nodes (4): E8-F6-S3 — is `TrendStrength.STRONG` real-but-rare or structurally unreachable? (leave as-is), Findings, Mechanism, Outcome

### Community 118 - "OrderService"
Cohesion: 0.10
Nodes (14): BrokerAdapter, BrokerAdapterRouter, Service, PostMapping, RequestMapping, ResponseEntity, RestController, OrderController (+6 more)

### Community 120 - "NotificationExceptionHandler.java"
Cohesion: 0.29
Nodes (6): InvalidNotificationRequestException, ExceptionHandler, Logger, ResponseEntity, RestControllerAdvice, NotificationExceptionHandler

### Community 121 - "E8-F1-S11 — SELL-only MA-crossover separation gate, evaluated and shipped"
Cohesion: 0.29
Nodes (7): Design, E8-F1-S11 — SELL-only MA-crossover separation gate, evaluated and shipped, Fixture fallout, Implementation, New test coverage, Production wiring, Scope confirmation

### Community 123 - "SellMaCrossoverSeparationCalibrationTest"
Cohesion: 0.40
Nodes (3): Test, SellMaCrossoverSeparationCalibrationTest, SymbolFixture

### Community 124 - "CredentialEncryptionService"
Cohesion: 0.33
Nodes (5): CredentialEncryptionService, Component, Logger, Broker-credential encryption key rotation procedure, SecretKeySpec

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

### Community 129 - ".getId"
Cohesion: 0.33
Nodes (4): SpringBootTest, Test, Transactional, TickerServiceTest

### Community 132 - "RetryingAlpacaTradingAdapterContractTest"
Cohesion: 0.48
Nodes (3): ExtendWith, Override, RetryingAlpacaTradingAdapterContractTest

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

### Community 139 - "PerSymbolAdxTrendingThresholdCalibrationTest"
Cohesion: 0.36
Nodes (3): Test, PerSymbolAdxTrendingThresholdCalibrationTest, SymbolFixture

### Community 141 - "dataviz skill"
Cohesion: 0.38
Nodes (7): dataviz skill, SignalBadge colorblind-safe teal/orange/slate palette, F3.1 Ticker lookup & metrics display, E3-F1-S1 Ticker lookup + stat-tile metrics, E3-F1-S2 Buy/Sell/Hold badge color-coded, F3.2 Metric visualization, E3-F2-S1 Price chart with MA/RSI overlays

### Community 142 - "TradingModeService"
Cohesion: 0.18
Nodes (5): RiskConsentEventRepository, TradingModeEventRepository, Service, TradingModeService, JpaRepository

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

### Community 169 - "E8-F6-S1 — HoldTermRule day-range calibration (no ship, all 6 branches)"
Cohesion: 0.40
Nodes (5): Design gate, E8-F6-S1 — HoldTermRule day-range calibration (no ship, all 6 branches), Findings, Mechanism, Outcome

### Community 174 - "SignalDriftController"
Cohesion: 0.29
Nodes (6): ConditionalOnProperty, GetMapping, RequestMapping, RestController, SignalDriftController, SignalDriftReport

### Community 176 - "E8-F5-S2 — funding-adjusted live signal-drift monitoring"
Cohesion: 0.29
Nodes (7): Design, Design decision: `possibleDecay` stays cost-only, E8-F5-S2 — funding-adjusted live signal-drift monitoring, Findings: funding materially erodes both directions, Mechanism, Scope confirmation, Testing

### Community 182 - "Notification system + WatchlistSignalPoller"
Cohesion: 0.40
Nodes (5): Notification system + WatchlistSignalPoller, Watchlist feature (watchlist_entries), F3.3 Watchlist (stretch), E3-F3-S1 Watchlist persisted in Oracle DB, F5.4 Notifications

## Knowledge Gaps
- **330 isolated node(s):** `com.autotrade.dashboard:backend`, `MIN`, `MID`, `MAX`, `WIN` (+325 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **30 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Candle` connect `Candle` to `DirectionalOutcomeStats`, `OrderServiceTest.java`, `.calculate`, `PerSymbolAdxTrendingThresholdCalibrationTest`, `RegimeOutOfSampleValidationTest`, `IndicatorTestFixtures`, `IndicatorSnapshot`, `IndicatorService`, `IndicatorId`, `BinanceMarketDataClient`, `MarketDataService`, `Ticker`, `.bullishCandles`, `OrderAuditEntry`, `.getPriceHistory`, `RegimeCalibrationTest`, `VolatilityBandCutoffCalibrationTest`, `UnanimityGapAnalysisTest`, `.calculate`, `LiveSignalDriftServiceTest`, `.run`, `.fetchRecentCandles`, `.calculate`, `.fetchRecentCandles`, `StockMaCrossoverSeparationCalibrationTest`, `WeightedVoteBacktestTest`, `PerSymbolMacdHistogramMagnitudeCalibrationTest`, `PerSymbolRsiOverboughtCalibrationTest`, `ThresholdCalibrationTest`, `StockPerSymbolMacdHistogramMagnitudeCalibrationTest`, `StockRegimeOutOfSampleValidationTest`, `.calculate`, `LiveSignalDriftService.java`, `MacdHistogramMagnitudeCalibrationTest`, `MaCrossoverSeparationCalibrationTest`, `RsiOverboughtRecalibrationTest`, `BacktestReport`, `StockPerSymbolRsiOverboughtCalibrationTest`, `SellMaCrossoverSeparationCalibrationTest`?**
  _High betweenness centrality (0.120) - this node is a cross-community bridge._
- **Why does `SignalRuleId` connect `Candle` to `Test`, `OrderServiceTest.java`, `.calculate`, `AssetType`, `.evaluate`, `IndicatorSnapshot`, `OrderQueryControllerTest`, `CoreDataModelIntegrationTest.java`, `.evaluate`, `IndicatorId`, `OrderAuditEntry`, `UnanimityGapAnalysisTest`, `.applySellGate`, `StockMaCrossoverSeparationCalibrationTest`, `PerSymbolMacdHistogramMagnitudeCalibrationTest`, `PerSymbolRsiOverboughtCalibrationTest`, `ThresholdCalibrationTest`, `StockPerSymbolMacdHistogramMagnitudeCalibrationTest`, `LiveSignalDriftService.java`, `MacdHistogramMagnitudeCalibrationTest`, `MaCrossoverSeparationCalibrationTest`, `RsiOverboughtRecalibrationTest`, `BacktestReport`, `StockPerSymbolRsiOverboughtCalibrationTest`?**
  _High betweenness centrality (0.080) - this node is a cross-community bridge._
- **Why does `TradingMode` connect `TradingMode` to `BinanceFuturesTradingAdapterTest`, `.readDecrypted`, `.placeOrder`, `BinanceFuturesTradingAdapter`, `RetryingAlpacaTradingAdapterContractTest`, `BrokerCredentialServiceFindTest.java`, `OrderServiceTest.java`, `AssetType`, `Order`, `TradingModeService`, `AlpacaTradingAdapter`, `IndicatorSnapshot`, `BrokerCredential`, `OrderQueryControllerTest`, `CoreDataModelIntegrationTest.java`, `BrokerCredentialService`, `OrderServiceTest`, `BrokerOrderResult`, `TradingModeServiceTest`, `MockBrokerAdapter`, `BrokerCredentialServiceRotationTest.java`, `TradingModeResponse`, `BrokerAdapterContractTest`, `TradingModeEvent`, `BrokerAdapterConfig.java`, `BinanceFuturesAdapterConfig.java`, `TradingModeController`, `OrderStatus`, `OrderService`?**
  _High betweenness centrality (0.064) - this node is a cross-community bridge._
- **Are the 6 inferred relationships involving `Candle` (e.g. with `.chartData_marketClosedWithCachedFallback_returns200Stale()` and `.chartData_registeredTicker_returns200WithCandlesAndIndicators()`) actually correct?**
  _`Candle` has 6 INFERRED edges - model-reasoned connections that need verification._
- **What connects `com.autotrade.dashboard:backend`, `MIN`, `MID` to the rest of the system?**
  _330 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `BinanceFuturesTradingAdapterTest` be split into smaller, more focused modules?**
  _Cohesion score 0.11582491582491583 - nodes in this community are weakly interconnected._
- **Should `Changelog` be split into smaller, more focused modules?**
  _Cohesion score 0.02564102564102564 - nodes in this community are weakly interconnected._