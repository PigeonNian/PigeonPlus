---
title: Air Blender
navigation:
  title: Air Blender
  weight: 40
items:
  - "anvilcraft_pigeon_plus:blender"
  - "anvilcraft_pigeon_plus:mixed_biomass_bucket"
  - "anvilcraft_pigeon_plus:compressed_air_bucket"
---

# Air Blender

<block id="anvilcraft_pigeon_plus:blender"/>

The air blender is used to process biomass-related fluids and serves as the starting point of the biogas production chain.

## Mixed Biomass

Crops and other materials can be converted into mixed biomass through blending. Mixed biomass can then participate in reactions inside a large cauldron to produce biogas; this requires a running blender and a heat source within the 3x3 area below the cauldron, see [Biogas](../fluids/biogas).

<item id="anvilcraft_pigeon_plus:mixed_biomass_bucket"/>

## Compressed Air

The air blender only runs while it is being fed **compressed air** from its input side, and it consumes **20 mB of compressed air per tick** while blending. It stores up to 1000 mB of compressed air internally, so pump compressed air into the input face to keep the machine working.

<item id="anvilcraft_pigeon_plus:compressed_air_bucket"/>

## Blender Heads

The blender only runs with a head installed; without one it will not turn even when fed compressed air.

| Head | How to install | What it does |
|---|---|---|
| Iron head | Right-click with an **iron shovel** | Only makes the machine run, no heat; enough for recipes that do not need heat, such as blending crops into mixed biomass |
| Blaze head | Right-click with a **blaze rod** | Heats the block above while running; recipes that need heat (liquid chocolate, brewing, and so on) require it |

- Remove: **sneak and right-click with an empty hand**; the head goes back to your inventory, or drops at your feet if the inventory is full.
- Swap: remove the old head first, then install the new one; the machine switches over on its own.

An electric blender heats itself while powered, so it can run heat recipes even with an iron head; see [Electric Blender](electric_blender).

## Recipe

<recipe id="anvilcraft_pigeon_plus:blender"/>
