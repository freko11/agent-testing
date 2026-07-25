---
name: guardrail-check
description: Checklist for verifying E6's risk and safety controls actually hold before calling that epic done or unlocking live mode. Use before E6.1's live-mode gate closes and after any change to guardrail logic.
---

# What to verify

- **Per-order caps (E6-F2-S1)**: backend rejects any order beyond configured
  leverage/position-size caps regardless of what the frontend sent — test
  this by calling the API directly with an over-limit payload, not just
  through the form.
- **Portfolio exposure cap (E6-F2-S3)**: rejects a new order that would push
  *aggregate* open exposure over the cap even when the order itself is
  within per-order limits — this needs a test with existing open positions
  already in place, not just a single fresh order against an empty
  portfolio.
- **Kill switch (E6-F2-S2)**: cancels open orders on *both* adapters and
  blocks new submissions until manually cleared — verify it isn't scoped to
  only whichever adapter was most recently used.
- **Paper/live gating (E6-F1-S1/S2/S3)**: live mode stays disabled until the
  paper-trade threshold is met *and* the explicit consent step is recorded
  with a timestamp — verify both conditions are checked, not just one.
- **Audit trail (E6-F3-S1/S2)**: every order and the signal that triggered it
  is logged with the rule-table version, append-only, before the order
  actually reaches the broker adapter — not logged after the fact, where a
  crash between submission and logging would lose the record.

These checks exist because a bug in the guardrails is worse than a bug in the
happy path: per `docs/agile-plan.md`'s own rationale, this is where
`/code-review` (consider `ultra` tier) is explicitly called for beyond the
review above.
