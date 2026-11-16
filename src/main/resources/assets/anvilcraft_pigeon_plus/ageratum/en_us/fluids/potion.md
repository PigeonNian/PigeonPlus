---
title: Potions and Brewing
navigation:
  title: Potions and Brewing
  weight: 25
items:
  - "anvilcraft_pigeon_plus:splash_bottle"
  - "anvilcraft_pigeon_plus:lingering_bottle"
---

# Potions and Brewing

<item id="anvilcraft_pigeon_plus:splash_bottle"/>
<item id="anvilcraft_pigeon_plus:lingering_bottle"/>

In this mod, potions are a fluid: they can be poured into tanks, moved through pipes, and brewed a whole cauldron at a time.

## Handling Potions

One bottle of potion is **250 mB**.

- **Pour in**: right-click a fluid tank, fish tank or large cauldron while holding a potion. The potion goes in and you are left with the matching empty bottle.
- **Fill up**: right-click a container that holds potion while holding an empty bottle (glass bottle, splash bottle or lingering bottle) to get the matching type of potion.

Like water, potions can travel through pipes and be stored in tanks.

## Brewing

Both scales work the same way: put the reagent into the container, then strike it with an anvil.

### Fish Tank: One Tank at a Time

- A running blender with a blaze head must sit **directly below** the fish tank; an air blender or an electric blender both work.
- Throw the reagent into the fish tank and strike it once with an anvil.
- The **entire tank** of potion is converted at once, consuming only 1 reagent.

### Large Cauldron: Bulk Brewing

- The **3x3 area directly below** the cauldron needs both of the following at the same time:
  - a running blender with a blaze head, air or electric;
  - a heat source: an electric heater, a lit burning heater, or a heating electric blender.
- When heat comes only from a burning heater, each successful brew consumes one charge of its burn time.
- Throw the reagent into the cauldron and strike it with a **giant anvil**.
- Up to **8 buckets** are converted at once, again consuming only 1 reagent.

### Brewing Rules

- Which reagents work and what they produce is exactly the same as the vanilla brewing stand.
- When several reagents are inside the container, **nether wart** is used first (turning water into an awkward potion), then effect ingredients, and only then strengthening ingredients like redstone or glowstone.
- Mundane and Thick potions are never produced; once a potion can no longer be strengthened or transformed, further reagents are not consumed.

## Soaking

A fish tank or large cauldron holding potion can also run soaking recipes.

### Soaking Mobs

- Let a mob soak inside a fish tank or large cauldron filled with potion.
- Every 20 ticks it consumes 250 mB and gains that potion's effects, with the same duration and effects as drinking a bottle.
- If it already has the same or a stronger effect, nothing is consumed; instant potions such as healing or harming trigger once every 20 ticks instead.

### Soaking Items

Put the material into the container and strike it; each time consumes 250 mB of potion:

| Put in | Get |
|---|---|
| 2 Pills | 2 pills carrying that effect; eating one grants the effect |
| 8 Arrows | 8 tipped arrows |
| 1 Block of Resin + any Weakness potion | 1 Weak Resin Block (requires AnvilCraft: Pig+) |

Fish tanks and cauldrons holding potion emit particles in the matching colour, so you can tell at a glance what is inside.

## Splash Bottles and Lingering Bottles

- **Splash Bottle**:

<recipe id="anvilcraft_pigeon_plus:splash_bottle"/>

- **Lingering Bottle**:

<recipe id="anvilcraft_pigeon_plus:lingering_bottle"/>

- Use them to fill potion from a container and get splash potions or lingering potions; pour the potion back to get the matching empty bottle.
