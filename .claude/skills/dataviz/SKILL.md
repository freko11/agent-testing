---
name: dataviz
description: Project-specific application of this repo's dataviz methodology to the auto-trade dashboard's stat tiles, Buy/Sell/Hold badge, and price chart with indicator overlays (E3). Apply the generic dataviz skill's form/color/palette methodology first, then these specifics.
---

# Where this applies

- **E3-F1-S2**: the Buy/Sell/Hold badge is the single most important visual in
  the app — it has to be legible at a glance. Run three visually distinct,
  accessible colors for Buy/Sell/Hold through the generic dataviz skill's
  color formula and validator rather than defaulting to raw red/green/gray,
  and keep the mapping consistent everywhere it appears (badge, history table,
  CSV export references).
- **E3-F1-S1**: metrics render as stat tiles — follow the generic skill's
  stat-tile guidance rather than ad hoc cards.
- **E3-F2-S1**: the price chart needs candles + MA overlay lines + an RSI
  subplot. Treat the overlay lines as a categorical series (consistent color
  per indicator, reused across sessions) and the RSI subplot as its own scaled
  axis, not squeezed into the price axis.

# Project constraint

This is a single-user personal tool, not a multi-tenant product — don't build
a themeable/configurable palette system. Pick one accessible palette (light +
dark) and apply it consistently across the badge, stat tiles, and chart.
