---
title: Gases in Pipes
navigation:
  title: Gases in Pipes
  weight: 30
items:
  - "anvilcraft_pigeon_plus:gaseous_biogas_bucket"
  - "anvilcraft_pigeon_plus:compressed_air_bucket"
---

# Gases in Pipes

<item id="anvilcraft_pigeon_plus:gaseous_biogas_bucket"/>
<item id="anvilcraft_pigeon_plus:compressed_air_bucket"/>

Biogas and compressed air are both **gases**. Unlike liquids, they do not flow along pipes by gravity — instead they diffuse across the pipe network driven by **pressure differences**.

## Pressure Diffusion

All gas-holding containers in the same network equalize automatically based on pressure:

- Pressure is the amount of gas in a container divided by its total capacity.
- High-pressure containers push gas toward low-pressure ones until both sides are equal.
- Diffusion speed is capped, and directional constraints such as control valves and check valves apply to gases as well.
- Liquids rely on height differences, but **gases ignore gravity** — vertical and horizontal pipes behave identically.

## Pumps and Pressure

A pump's headlift participates in gas diffusion as well. The higher the headlift, the easier it is to push gas into tanks; drawing compressed air from a drain especially relies on the pump's thrust.
Like liquids, pumps can be chained in series to increase the thrust applied to gas.

## Extracting Compressed Air

Compressed air can only be drawn from a drain with **at least one face exposed to open air**. Air enters the fluid network through the pump and diffuses onward to tanks and other containers.

## Liquefaction

When a large tank is **already full of gas** and a pump keeps pumping more gas in, the surplus gas converts into liquid at a compression ratio:

- Biogas → liquefied biogas, ratio 512:1.
- Compressed air → liquid oxygen, ratio 415:1.

Liquefaction therefore requires a "full tank + continuous pressure". Both ordinary pumps and piston pumps work.
