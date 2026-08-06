---
title: Gas Escape
navigation:
  title: Gas Escape
  weight: 40
items:
  - "anvilcraft_pigeon_plus:gaseous_biogas_bucket"
  - "anvilcraft_pigeon_plus:compressed_air_bucket"
---

# Gas Escape

<item id="anvilcraft_pigeon_plus:gaseous_biogas_bucket"/>

Gases are **unique**: unlike liquids, they do not stay stable inside a container — they keep **escaping** into the air through open container openings.

## Escape Rules

- **Large cauldron**: when the contents contain gas, it escapes once every 20 ticks, 100 mB each time.
- **Fish tank**: when the space above is not fully sealed by full blocks, gas escapes 100 mB every 20 ticks.
- **Drain**: as long as the drain remains in its **air-extracting state** (no face fully sealed), stored gas escapes 250 mB every 5 ticks, releasing color-matched particles that drift upward.

## Sealing Stops Escape

- Covering the large cauldron with a giant anvil or full blocks prevents gas from escaping.
- Blocking the space above a fish tank with full blocks likewise stops the escape.

## Special Effect of Biogas Escape

When a drain's contents are biogas, monster spawning is suppressed within a 15×15×15 area around the drain, and a range outline similar to an induction light is displayed. This effect is independent of the escape itself: even if you don't intend to store biogas, filling a drain with it works as a temporary "mob repelling zone".

## Practical Tips

- For long-term storage, seal the container — otherwise gas will gradually leak away.
- Gas diffuses through the pipe network by pressure, while escape happens at the **storage container** end. Together, gas in open containers diffuses outward while leaking away.
