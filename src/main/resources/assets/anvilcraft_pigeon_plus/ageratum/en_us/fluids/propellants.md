---
title: Exhaust and Fuel
navigation:
  title: Exhaust and Fuel
  weight: 20
items:
  - "anvilcraft_pigeon_plus:nozzle"
  - "anvilcraft_pigeon_plus:liquid_oxygen_bucket"
  - "anvilcraft_pigeon_plus:liquid_hydrogen_bucket"
  - "anvilcraft_pigeon_plus:liquefied_biogas_bucket"
---

# Exhaust and Fuel

<item id="anvilcraft_pigeon_plus:nozzle"/>
<item id="anvilcraft_pigeon_plus:liquid_oxygen_bucket"/>
<item id="anvilcraft_pigeon_plus:liquid_hydrogen_bucket"/>
<item id="anvilcraft_pigeon_plus:liquefied_biogas_bucket"/>

## Oxidizer: Liquid Oxygen

Liquid oxygen is the oxidizer shared by all three exhaust types. It is not a fuel itself.

- **Compressed air**: pressurize it into a large tank to liquefy it (see [Gases in Pipes](gas_diffusion))
- Every **415 mB** of compressed air yields **1 mB** of liquid oxygen.
- **Oxygen**: liquefy AnvilCraft's oxygen directly. Every **100 mB** yields **1 mB** of liquid oxygen.

Liquid oxygen sits at about 90 K. Poured into the world, it evaporates within 1-4 seconds, so move it with tanks, pipes or buckets instead of leaving it on the ground.

## Fuel: Liquid Hydrogen

Liquid hydrogen is the fuel used by hydrogen-oxygen exhaust.

At about 20 K it is colder than liquid oxygen and evaporates faster, so it cannot be stored in the open either.

## The Three Exhaust Types

| Exhaust | Fuel | Oxidizer | Oxygen per bucket of fuel | Acceleration in 30 s | Flame |
| --- | --- | --- | --- | --- | --- |
| Kerosene exhaust | Crude oil (`#c:oil`) | Liquid oxygen | 2 buckets | about 320 b/t | Orange-yellow fading to bright white |
| Methane exhaust | Liquefied biogas | Liquid oxygen | 1.5 buckets | about 192 b/t | Pale blue outer flame |
| Hydrogen-oxygen exhaust | Liquid hydrogen | Liquid oxygen | 0.5 buckets | about 160 b/t | Nearly colorless warm white |

- **Acceleration limit** is what a single nozzle accumulates over 30 seconds while a stasis beacon locks a falling anvil.
- When several fuels are present at once they are used in the order **methane → hydrogen → kerosene**.

### Kerosene Exhaust

Triggered when crude oil and liquid oxygen are both present and ignited. Highest acceleration limit and a bright white-orange flame; pick it when thrust matters most.

### Methane Exhaust

Triggered when liquefied biogas and liquid oxygen are both present and ignited. Middle acceleration limit and a sky-blue outer flame; a natural fit for a base that already runs a biogas line.

### Hydrogen-Oxygen Exhaust

Triggered when liquid hydrogen and liquid oxygen are both present and ignited. Lowest acceleration limit, but the flame is almost colorless and the cleanest looking.

## Ignition

1. Put the fuel and liquid oxygen into the large cauldron.
2. Drop in a fire starter to ignite it: a torch, soul torch, campfire, soul campfire or blaze powder is consumed; a red-hot tungsten or netherite block is not, so it can stay in the cauldron to relight it repeatedly.
3. Install the [nozzle](../devices/nozzle) on the side of the cauldron; its facing sets the exhaust direction.
4. A redstone signal on the nozzle shuts the exhaust off immediately.

## Consumption

- While running, the nozzle drains **1 bucket (1000 mB)** of fuel from the cauldron every **second**.
- The matching liquid oxygen is drained in the same step: 2 buckets per bucket of kerosene, 1.5 for methane and 0.5 for hydrogen. A partial fuel draw only takes the proportional amount.
