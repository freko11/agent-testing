---
name: run
description: Launch and drive the auto-trade signal dashboard stack (Oracle XE, Spring Boot backend, React frontend) to verify a feature end-to-end. Project-specific override of the generic `run` skill — consulted first per its own fallback rule.
---

# Launching this stack

1. `docker compose up -d` — starts Oracle XE (per E1-F1-S1's compose file once
   it exists). Wait for it to report healthy before starting the backend;
   Oracle XE's first boot can take longer than subsequent ones.
2. `./mvnw spring-boot:run` (or the Gradle equivalent, whichever E1-F1-S2
   settles on) — starts the Spring Boot backend. Confirm `/health` returns 200
   before treating it as ready.
3. `npm run dev` in the frontend directory — starts the React dev server
   (E1-F1-S3).

If any of these commands don't match what actually got built in E1, treat this
file as stale and update it — it documents the plan's intent, not a verified
script, until E1 lands.

## Golden path to click through per story

Enter a ticker → confirm price history loads (E2-F1-S1) → confirm indicators +
Buy/Sell/Hold call render (E2-F2/F3, E3-F1) → confirm the chart with overlays
renders (E3-F2) → (once E4/E5 exist) submit a paper trade and confirm status
updates (E5-F3-S1). Don't stop at "it built" — this skill's whole point is
confirming the feature works in the running app, per this repo's Definition
of Done.

## Also check

- Stock tickers outside market hours show the distinct "market closed" state,
  not stale data (E2-F1-S3).
- An unknown ticker shows a specific error, not a blank screen (E2-F1-S2).
- A broker outage shows a visible "broker unavailable" state once E4 exists
  (E4-F1-S3).
