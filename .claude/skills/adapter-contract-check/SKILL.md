---
name: adapter-contract-check
description: Checklist for verifying a BrokerAdapter implementation (Alpaca, Binance, or any future adapter) satisfies the shared contract from E4.1 before it's wired into trade execution. Use after implementing or changing an adapter.
---

# What to verify

- **Interface conformance**: placeOrder, getPosition, cancelOrder,
  getAccountStatus all implemented and pass the shared adapter contract test
  suite (E4-F1-S1) — not just a subset that happens to work for the happy
  path.
- **Retry/backoff (E4-F1-S2)**: transient failures retry with backoff; a hard
  rate-limit error surfaces as a distinct, user-visible state rather than a
  generic failure or a silent retry loop.
- **Outage handling (E4-F1-S3)**: when the broker is unreachable, the failure
  is visible in the UI, and — this is the one that actually costs money if
  wrong — a retry after a timeout can never result in the same order being
  submitted twice. Check the idempotency mechanism (client order ID,
  idempotency key, whatever the broker's API supports) explicitly; don't
  assume "we retry carefully" is enough without a concrete dedup mechanism.
- **Leverage/order bounds**: bounds enforced by the adapter match the
  specific broker's actual limits (Alpaca stock orders default leverage 1x
  per E5-F1-S2; Binance leverage bounded per E4-F3-S2) — not a single shared
  constant that's wrong for one of the two.
- **Paper/testnet vs. live keys**: confirm the adapter reads its credentials
  and base URL from the paper/live mode switch (E6-F1-S1), so flipping the
  switch can't accidentally leave one adapter pointed at live while another
  is still on paper.
