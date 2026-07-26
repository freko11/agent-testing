# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Status

E1-F1-S1 (local Oracle XE via Docker Compose) is done. `docker-compose.yml` runs
`gvenzl/oracle-xe:21-slim`, with credentials and host port sourced from `.env` (see
`.env.example` — never commit `.env`). Local data persists to `oracle-data/`
(gitignored). Connect from SQL Developer / `sqlplus` with:
- Host: `localhost`, Port: `1522` (not the default 1521 — this dev machine already
  runs a native Oracle listener on 1521, so the container's host port defaults to
  1522 via `ORACLE_HOST_PORT` in `.env`; override if your machine doesn't conflict)
- Service name: `XEPDB1`
- App user/password: `ORACLE_APP_USER` / `ORACLE_APP_USER_PASSWORD` from `.env`
- SYS password: `ORACLE_PASSWORD` from `.env` (SYSDBA role required for SYS)

Bring the DB up with `docker compose up -d`; first boot takes ~60-90s before the
healthcheck reports healthy (`docker compose ps`).

E1-F1-S2 (Spring Boot backend skeleton) is done. `backend/` is a Maven project
(Java 21, Spring Boot 4.1, package `com.autotrade.dashboard`) with `web`, `actuator`,
`data-jpa`, `validation` starters plus the `ojdbc11` Oracle driver. Run it with
`./mvnw spring-boot:run` from `backend/` (requires `ORACLE_APP_USER_PASSWORD` in the
environment, matching `.env`'s app-user password — Spring Boot reads env vars via
relaxed binding, no extra wiring needed). `/health` (actuator remapped to web
base-path `/`) returns 200 once the Oracle XE container from S1 is up, since the
datasource is already pointed at it (`jdbc:oracle:thin:@//localhost:${ORACLE_HOST_PORT:1522}/XEPDB1`)
— no JPA entities/repositories yet, that's F1.2. Build/test: `./mvnw verify`.

E1-F1-S3 (React app skeleton) is done. `frontend/` is a Vite + React 19 + TypeScript
app with `react-router-dom`, one placeholder route (`/` → `DashboardPage`). Run with
`npm install && npm run dev` from `frontend/`; build with `npm run build`.

E1-F1-S4 (CI pipeline) is done. `.github/workflows/ci.yml` runs on every push and PR:
a `backend` job (`./mvnw -B verify`, Temurin 21) and a `frontend` job (`npm ci && npm run build`,
Node 22). Backend tests run against an in-memory H2 datasource
(`backend/src/test/resources/application.properties` overrides the main Oracle
datasource for the test classpath only) so CI and local `mvn verify` don't need Docker
or `.env` — Oracle is still what the running app uses in dev/paper/prod.
GitHub branch protection on `master` ("failure blocks merge") is now enabled via
`gh api .../branches/master/protection`, requiring the `backend` and `frontend` CI
jobs to pass (strict mode — branch must be up to date) before a PR can merge. This
required making `github.com/freko11/agent-testing` **public** first — branch
protection on private repos needs a paid GitHub plan (Pro/Team), which this account
doesn't have; the user explicitly chose public visibility over upgrading. This
completes E1-F1-S4's acceptance criteria in full.

E1-F1-S5 (env/config profiles) is done. Three Spring profiles in `backend/src/main/resources/`:
- `local` (default — activates automatically if `SPRING_PROFILES_ACTIVE` is unset):
  Docker Compose Oracle XE via `ORACLE_HOST_PORT`/`ORACLE_APP_USER`/`ORACLE_APP_USER_PASSWORD`.
- `paper`: reads `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` (defaults fall back to the same
  local XE instance until a real paper-environment DB exists); this is also where E4's
  Alpaca-paper/Binance-testnet base URLs will be added.
- `prod`: same `DB_*` vars, no defaults — every value must come from the environment,
  so it can't silently start on placeholder config. Not usable until E6's live-mode
  gate exists.

Switch profiles via `SPRING_PROFILES_ACTIVE=paper` (env var) or
`-Dspring-boot.run.profiles=paper` (mvnw) — no code changes needed; verified both
`local` (default) and `paper` bring the app up against the same Docker XE instance
using their respective env-var names.

This completes E1-F1 (local dev environment) — DB, backend, frontend, CI, and profiles
all in place.

E1-F2 (core data model) is done — all three stories:
- **E1-F2-S1**: Oracle tables for `tickers`, `indicator_snapshots`, `broker_credentials`,
  and `orders` (`backend/src/main/resources/db/migration/V1__init_core_schema.sql`),
  with PK/FK/CHECK/UNIQUE constraints — including a DB-level defense-in-depth check
  that stock orders can never carry leverage, ahead of E6's guardrails.
- **E1-F2-S2**: A JPA entity + Spring Data repository per table (`broker`, `indicator`,
  `order`, `ticker` packages under `com.autotrade.dashboard`), with a `CredentialCipherConverter`
  (AES/GCM, key from `CREDENTIAL_ENC_KEY` env var — see `.env.example`) encrypting
  `broker_credentials`' key/secret columns at rest. This is F1.2's minimal safeguard
  only; full key rotation/keystore-backed encryption is separate F1.3 work.
  `CoreDataModelIntegrationTest` proves CRUD round-trips through all four repositories
  plus unique/FK/check-constraint violations.
- **E1-F2-S3**: Flyway (`V1__init_core_schema.sql` + `V2__add_supporting_indexes.sql`)
  is the single source of schema truth; `spring.jpa.hibernate.ddl-auto=validate` on
  every profile (local/paper/prod/test) so Hibernate only checks entity mappings
  against what Flyway already created, never generates DDL itself.

Two real bugs were found and fixed while bringing this into `master` (this code was
originally produced by a background agent run that never committed and skipped the
`Plan`-agent design gate the story called for; it was reviewed, debugged, and verified
against a live Oracle XE container — not just the H2 test profile — before merging):
`BrokerCredential.isActive` needed `@Convert(NumericBooleanConverter.class)` +
`@JdbcTypeCode(SqlTypes.NUMERIC)` to bind a Java `boolean` against an Oracle
`NUMBER(1)` column (plain `boolean` defaults to native `BOOLEAN`, which Oracle XE
21c doesn't have); and the migration's `NOT NULL DEFAULT x` column clauses had to be
reordered to `DEFAULT x NOT NULL` (Oracle requires `DEFAULT` before inline
constraints — H2's Oracle-compatibility mode silently accepted the wrong order, real
Oracle rejected it with `ORA-00907`). Backend tests (`./mvnw verify`, H2 in Oracle
mode) and a live run against the Docker Oracle XE container (`./mvnw spring-boot:run`,
`local` profile — Flyway migrations applied, `/health` returned `UP`) both pass.

A third bug surfaced in CI itself, on the push that landed E1-F2: `backend/mvnw` had
been committed with mode `100644` instead of `100755`, so the `backend` CI job failed
with exit code 126 ("permission denied") the first time GitHub Actions actually
executed it — this is a known Windows/git gotcha, since Windows has no executable
bit, so `mvnw` silently loses it unless explicitly `chmod +x`'d and staged before
commit. Fixed via `git update-index --chmod=+x backend/mvnw`. If a future wrapper
script (`gradlew`, a shell script, etc.) is added from a Windows checkout, check its
mode with `git ls-files -s <path>` before committing — it should read `100755`, not
`100644`.

Next up per the plan's build sequence: E1-F3 (secrets & config management — encrypted
credential storage already has a first pass via F1.2's `CredentialCipherConverter`,
but F1.3-S1's full key-rotation requirement and F1.3-S2's dashboard login are still
open) or E2 (Signal Engine), per the build sequence in `docs/agile-plan.md`.

Beyond that, no other source code yet. An agile delivery plan for the project has been drafted at
`docs/agile-plan.md` — an auto-trade signal dashboard (React frontend, Java/Spring
Boot backend, Oracle Database via local Oracle XE, broker adapters starting with
Alpaca for stocks and Binance for crypto). It covers epics/features/user stories
(INVEST format) and the recommended subagent/skill usage for solo-driven
implementation. The plan was expanded with stories closing gaps found in a review:
CI pipeline, DB migrations, app auth, a testing strategy, market-hours handling,
backtesting, adapter rate-limit/retry/outage handling, trade export, notifications,
a live-mode consent step, a portfolio-level exposure cap, rule-versioned audit
entries, and DB backup/restore. A consistency pass then fixed a stale story count,
extended the Plan-agent/`code-review` gates to cover E6's guardrail logic (not just
E4/E5), and flagged that E5's notification story softly depends on the stretch
watchlist feature. There are no build, lint, or test commands yet since no code has
been written.

Project-specific subagent definitions now live in `.claude/agents/` — `Plan.md`,
`Explore.md`, and `general-purpose.md` — customizing the three subagent roles the
agile plan calls for (design gate, research, background implementation) with this
project's domain specifics (Spring Boot/JPA/Oracle conventions, Alpaca/Binance order
semantics, the E2 rule-engine and E6 guardrail requirements). These override the
generic built-in agents of the same name for work done in this repo.

Project-specific skill definitions now live in `.claude/skills/`. Four amend
generic skills with this project's specifics — `run.md` (this stack's launch
sequence: Oracle XE via Docker Compose, Spring Boot backend, React frontend),
`dataviz.md` (Buy/Sell/Hold badge palette, stat tiles, candlestick+indicator
chart for E3), `simplify.md` (where premature abstraction is a real risk here —
adapters, rule engine — versus where a check that looks redundant is a
deliberate E6 safety net), and `security-review.md` (a checklist covering
broker-credential handling, the live-mode gate, and guardrail enforcement).
Three are new, non-generic project skills with no generic equivalent —
`signal-rule-review.md` (E2.3/E2.4 rule-table + backtest checklist),
`adapter-contract-check.md` (E4.1 BrokerAdapter conformance, retry/backoff,
idempotency), and `guardrail-check.md` (E6 risk-control verification before
the live-mode gate closes).

A repo-readiness audit (full directory inventory, cross-checked against
`docs/agile-plan.md`) confirmed the "no source code yet" status above is
accurate and found no gaps owned by an existing story except a few config-
hygiene items, now closed: a `.gitignore` was added (covering Java/Maven/
Gradle, Node, `.env*`, IDE files, and local Oracle data volumes, since E1's
skeleton stories are about to generate exactly what it excludes); the
backend-framework assumption in `docs/agile-plan.md` was confirmed as
**Spring Boot** (moved out of "assumptions" into a new "Confirmed decisions"
section, no objection raised); and F1.3-S1's acceptance criteria now
require a checked-in `.env.example` documenting every required config key
(broker keys, Oracle connection string) with no real values. The indicator-
library choice (`ta4j` vs. hand-rolled) and the ticker asset-type detection
rule remain intentionally open in `docs/agile-plan.md` — both are deferred
by design to when E2-F2 is actually picked up, not pre-build blockers. The
`fewer-permission-prompts` skill is worth running once E1 scaffolding
exists (not before — there's nothing to allowlist yet).

When code is added to this repository, update this file with:
- Build, lint, and test commands (including how to run a single test)
- High-level architecture and project structure

## Mandatory workflow

Every change to the codebase, no matter how small, must:
1. Update this CLAUDE.md file to reflect the change (new commands, architecture shifts, updated status, etc.).
2. Be committed to git immediately after — do not batch multiple unrelated changes into one commit, and do not leave changes uncommitted.
3. Use a meaningful commit message that describes what actually changed and why — no generic messages like "update", "fix", or "changes".

This applies to every edit session: if files change, CLAUDE.md changes and a git commit follows in the same turn.
