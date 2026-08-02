---
title: Stasis Beacon
navigation:
  title: Stasis Beacon
  weight: 10
items:
  - "anvilcraft_pigeon_plus:stasis_beacon"
---

# Stasis Beacon

<block id="anvilcraft_pigeon_plus:stasis_beacon"/>

The stasis beacon is a corrupted beacon variant. It has no UI. When active, it creates a blue beam and applies time freeze to entities inside the beam range.

## Base

The stasis beacon only accepts frost metal blocks as base blocks. Its beam narrows when obstructed, just like the corrupted beacon.

## Time Freeze

- Only one non-player entity can be frozen at a time.
- Living entities and falling blocks can be frozen.
- The maximum freeze duration is 30 seconds.
- The freeze ends early after 5 hearts of stored damage.

While frozen, damage and momentum are recorded. When the beam disappears, the limit is reached, or forced release conditions are met, the recorded effects are resolved all at once.

## Nozzle Interaction

After a falling anvil is locked by the stasis beacon, momentum provided by the nozzle center jet continues to accumulate. When stasis is released, the anvil flies out with the accumulated speed.

## Recipe

<recipe id="anvilcraft_pigeon_plus:stasis_beacon"/>
