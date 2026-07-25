---
name: simplify
description: Project-specific simplify watch-outs for the auto-trade dashboard — where premature abstraction is a real risk in this codebase (adapters, rule engine) versus where a check that looks redundant is actually a deliberate safety net. Apply on top of the generic simplify pass, right before each commit.
---

# Watch-outs specific to this project

- **BrokerAdapter (E4.1)**: it's tempting to design for "any future broker" —
  don't. The interface only needs to satisfy Alpaca and Binance today; a third
  adapter isn't scoped. If a method or config only exists to anticipate a
  hypothetical broker, cut it.
- **Signal rule engine (E2.3)**: keep the Buy/Sell/Hold rule table a plain,
  readable mapping (documented thresholds → call). Don't turn it into a
  generic rule-engine framework or plugin system — there's one rule table,
  not many.
- **Indicator math (E2.2)**: prefer whichever of hand-rolled or `ta4j` the
  `Explore` agent's comparison favors — don't hand-roll a second
  implementation "to be safe" once one is chosen and tested against fixtures.
- **Risk guardrails (E6.2)** — the opposite case: don't simplify away an
  explicit check because it looks redundant with another one. Per-order caps,
  the portfolio exposure cap, and the kill switch are deliberately separate
  safety nets (per `docs/agile-plan.md`'s own rationale); collapsing them into
  one "big validation function" would remove that redundancy on purpose-built
  safety code.

Apply the generic simplify pass (reuse, efficiency, altitude cleanup) as
usual — these are just the project-specific judgment calls layered on top.
