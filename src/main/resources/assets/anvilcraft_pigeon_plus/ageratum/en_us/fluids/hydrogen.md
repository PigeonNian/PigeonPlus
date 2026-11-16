---
title: Hydrogen and Liquid Hydrogen
navigation:
  title: Hydrogen and Liquid Hydrogen
  weight: 15
items:
  - "anvilcraft:hydrogen_bucket"
  - "anvilcraft_pigeon_plus:liquid_hydrogen_bucket"
---

# Hydrogen and Liquid Hydrogen

<item id="anvilcraft:hydrogen_bucket"/>
<item id="anvilcraft_pigeon_plus:liquid_hydrogen_bucket"/>

Liquid hydrogen is the fuel for hydrogen-oxygen nozzle exhaust. It is not separated out of compressed air: biogas is first reformed into hydrogen gas, which is then liquefied.

## Steam Reforming

Put **1 bucket of liquefied biogas + 2 buckets of water** into a large cauldron, and make sure the 3x3 area below it has all of the following:

- a **working blender**: an air blender or an electric blender both work;
- a **heat source**: an electric heater, a burning heater, or a heating electric blender;
- a **block of magnet**, which acts as the catalyst bed and is not consumed.

Then drop a **giant anvil** onto the cauldron. The methane and water reform at the stoichiometric ratio and produce **2048 mB of hydrogen**:

> CH₄ + 2 H₂O → CO₂↑ + 4 H₂

Carbon dioxide has no fluid form here, so it is vented as exhaust and the recipe only consumes methane and water.

## Liquefaction

Hydrogen is a gas: it diffuses through the pipe network by pressure, so pump it into a large tank first. Once the tank is full and pressure keeps building, hydrogen liquefies at **400:1**:

- 400 mB of hydrogen → 1 mB of liquid hydrogen.

In total, **1 bucket of liquefied biogas + 2 buckets of water** yields about **5 buckets of liquid hydrogen** (2048 ÷ 400).

## Usage

Liquid hydrogen is only used by the nozzle: together with liquid oxygen it forms hydrogen-oxygen exhaust, which accelerates up to about 160 b/t, the lowest of the three. It does, however, burn only 0.5 buckets of liquid oxygen per bucket of fuel, making it the most oxidizer-efficient choice for long runs. See [Exhaust and Fuel](propellants).
