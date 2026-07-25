-- Core data model for the auto-trade signal dashboard (E1-F2-S1).
-- Conventions: NUMBER(19) surrogate PKs backed by explicit sequences
-- (allocationSize=1 on the JPA side matches INCREMENT BY 1 here — no Oracle
-- IDENTITY columns, so the same DDL is portable to H2 for tests). Enum-like
-- columns are VARCHAR2 + CHECK, mapped as @Enumerated(EnumType.STRING) on the
-- Java side. Money/price/quantity columns are NUMBER(20,8). No ON DELETE
-- CASCADE anywhere — nothing here should ever silently orphan or
-- cascade-delete order history.

CREATE SEQUENCE tickers_seq START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE indicator_snapshots_seq START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE broker_credentials_seq START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE orders_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE TABLE tickers (
    id              NUMBER(19)      NOT NULL,
    symbol          VARCHAR2(20)    NOT NULL,
    asset_type      VARCHAR2(10)    NOT NULL,
    exchange        VARCHAR2(20)    NULL,
    created_at      TIMESTAMP(6)    DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_tickers PRIMARY KEY (id),
    CONSTRAINT uq_tickers_symbol UNIQUE (symbol),
    CONSTRAINT ck_tickers_asset_type CHECK (asset_type IN ('STOCK', 'CRYPTO'))
);

CREATE TABLE indicator_snapshots (
    id                      NUMBER(19)      NOT NULL,
    ticker_id               NUMBER(19)      NOT NULL,
    snapshot_at             TIMESTAMP(6)    NOT NULL,
    price                   NUMBER(20,8)    NOT NULL,
    rsi                     NUMBER(9,4)     NULL,
    macd_line               NUMBER(20,8)    NULL,
    macd_signal             NUMBER(20,8)    NULL,
    macd_histogram          NUMBER(20,8)    NULL,
    ma_short                NUMBER(20,8)    NULL,
    ma_long                 NUMBER(20,8)    NULL,
    volatility              NUMBER(9,4)     NULL,
    volume                  NUMBER(20,4)    NULL,
    volume_trend            NUMBER(9,4)     NULL,
    market_data_source      VARCHAR2(20)    NOT NULL,
    created_at              TIMESTAMP(6)    DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_indicator_snapshots PRIMARY KEY (id),
    CONSTRAINT fk_indicator_snapshots_ticker FOREIGN KEY (ticker_id) REFERENCES tickers (id),
    CONSTRAINT ck_indicator_snapshots_source CHECK (market_data_source IN ('ALPACA', 'BINANCE'))
);

-- Indicator inputs only — no Buy/Sell/Hold call or rule-table version column
-- here. That derivation belongs to a future, additive `signal_calls` table
-- in E2-F3; out of scope for this story.

CREATE TABLE broker_credentials (
    id                          NUMBER(19)      NOT NULL,
    broker                      VARCHAR2(20)    NOT NULL,
    environment                 VARCHAR2(10)    NOT NULL,
    api_key_ciphertext          VARCHAR2(4000)  NOT NULL,
    api_secret_ciphertext       VARCHAR2(4000)  NOT NULL,
    encryption_key_version      VARCHAR2(20)    DEFAULT 'v1-basic' NOT NULL,
    is_active                   NUMBER(1)       DEFAULT 1 NOT NULL,
    created_at                  TIMESTAMP(6)    DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at                  TIMESTAMP(6)    DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_broker_credentials PRIMARY KEY (id),
    CONSTRAINT uq_broker_credentials_broker_env UNIQUE (broker, environment),
    CONSTRAINT ck_broker_credentials_broker CHECK (broker IN ('ALPACA', 'BINANCE')),
    CONSTRAINT ck_broker_credentials_env CHECK (environment IN ('PAPER', 'LIVE')),
    CONSTRAINT ck_broker_credentials_active CHECK (is_active IN (0, 1))
);

-- Broker base URLs deliberately do NOT live here — they live in
-- application-{profile}.properties per the E1-F1-S5 config-profile design.

CREATE TABLE orders (
    id                          NUMBER(19)      NOT NULL,
    ticker_id                   NUMBER(19)      NOT NULL,
    indicator_snapshot_id       NUMBER(19)      NULL,
    broker_credential_id        NUMBER(19)      NOT NULL,
    broker                      VARCHAR2(20)    NOT NULL,
    order_mode                  VARCHAR2(10)    DEFAULT 'PAPER' NOT NULL,
    asset_type                  VARCHAR2(10)    NOT NULL,
    side                        VARCHAR2(10)    NOT NULL,
    quantity                    NUMBER(20,8)    NOT NULL,
    requested_amount_usd        NUMBER(20,8)    NULL,
    leverage                    NUMBER(5,2)     DEFAULT 1.00 NOT NULL,
    entry_order_type            VARCHAR2(10)    DEFAULT 'MARKET' NOT NULL,
    entry_price                 NUMBER(20,8)    NULL,
    take_profit_price           NUMBER(20,8)    NOT NULL,
    stop_loss_price             NUMBER(20,8)    NOT NULL,
    client_order_id             VARCHAR2(64)    NOT NULL,
    broker_order_id             VARCHAR2(64)    NULL,
    status                      VARCHAR2(20)    DEFAULT 'PENDING' NOT NULL,
    rejection_reason            VARCHAR2(500)   NULL,
    submitted_at                TIMESTAMP(6)    NULL,
    filled_at                   TIMESTAMP(6)    NULL,
    created_at                  TIMESTAMP(6)    DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at                  TIMESTAMP(6)    DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_orders PRIMARY KEY (id),
    CONSTRAINT uq_orders_client_order_id UNIQUE (client_order_id),
    CONSTRAINT fk_orders_ticker FOREIGN KEY (ticker_id) REFERENCES tickers (id),
    CONSTRAINT fk_orders_indicator_snapshot FOREIGN KEY (indicator_snapshot_id) REFERENCES indicator_snapshots (id),
    CONSTRAINT fk_orders_broker_credential FOREIGN KEY (broker_credential_id) REFERENCES broker_credentials (id),
    CONSTRAINT ck_orders_broker CHECK (broker IN ('ALPACA', 'BINANCE')),
    CONSTRAINT ck_orders_order_mode CHECK (order_mode IN ('PAPER', 'LIVE')),
    CONSTRAINT ck_orders_asset_type CHECK (asset_type IN ('STOCK', 'CRYPTO')),
    CONSTRAINT ck_orders_side CHECK (side IN ('BUY', 'SELL')),
    CONSTRAINT ck_orders_quantity_positive CHECK (quantity > 0),
    CONSTRAINT ck_orders_leverage_min CHECK (leverage >= 1),
    CONSTRAINT ck_orders_entry_order_type CHECK (entry_order_type IN ('MARKET', 'LIMIT')),
    CONSTRAINT ck_orders_status CHECK (status IN ('PENDING', 'SUBMITTED', 'FILLED', 'PARTIALLY_FILLED', 'REJECTED', 'CANCELLED', 'FAILED')),
    -- Defense-in-depth at the DB level: stocks can never carry leverage even
    -- if application code is bypassed.
    CONSTRAINT ck_orders_stock_no_leverage CHECK ((asset_type = 'STOCK' AND leverage = 1) OR asset_type = 'CRYPTO')
);
