---
name: security-review
description: Project-specific checklist for the auto-trade dashboard's money-handling code, applied on top of the generic security-review pass. Mandatory before F1.3 (secrets) ships and before E6.1's live-mode switch unlocks, per this repo's Definition of Done.
---

# Checklist specific to this project

- **Broker credentials (F1.3-S1)**: keys encrypted at rest (Jasypt or an OS
  keystore, per the plan's assumption); confirm they never appear in
  application logs, stack traces, or error responses returned to the
  frontend.
- **Dashboard auth (F1.3-S2)**: confirm unauthenticated API calls are actually
  rejected server-side, not just hidden by the frontend router.
- **Adapter HTTP calls (E4.2/E4.3)**: check for injection risk in anything
  built into a request from user input (ticker, amount, leverage) before it
  reaches Alpaca/Binance — these are external API calls with real financial
  side effects.
- **Live-mode gate (E6.1)**: verify the paper-trade threshold and the explicit
  consent step (E6-F1-S2/S3) can't be bypassed via a direct API call that
  skips the UI flow — the gate has to hold server-side, not just in the
  switch's UI.
- **Guardrails (E6.2)**: confirm hard caps and the kill switch are enforced in
  the backend regardless of what the frontend sends, per E6-F2-S1's own
  acceptance criteria.
- **Audit log (E6.3)**: confirm the audit table is genuinely append-only — no
  update/delete path reachable from the API.

Run the generic security-review pass first; this checklist exists to make
sure the review doesn't skip the specific spots in this app where a miss
costs real money.
