---
title: 气动搅拌机
navigation:
  title: 气动搅拌机
  weight: 40
items:
  - "anvilcraft_pigeon_plus:blender"
  - "anvilcraft_pigeon_plus:mixed_biomass_bucket"
  - "anvilcraft_pigeon_plus:compressed_air_bucket"
---

# 气动搅拌机

<block id="anvilcraft_pigeon_plus:blender"/>

气动搅拌机用于处理生物质相关流体，是沼气生产链的起点。

## 混合生物质

作物等材料可以通过搅拌转化为混合生物质。混合生物质随后可以在大型炼药锅中参与反应，产生沼气——这需要锅底下方 3x3 范围内有搅拌机运转并提供热源，详见[沼气](../fluids/biogas)。

<item id="anvilcraft_pigeon_plus:mixed_biomass_bucket"/>

## 压缩空气

气动搅拌机只有在**输入端**持续通入**压缩空气**时才会运转，运转期间每 tick 消耗 **20mB 压缩空气**。内部最多可储存 1000mB 压缩空气，请向输入端泵入压缩空气以保持运转。

<item id="anvilcraft_pigeon_plus:compressed_air_bucket"/>

## 搅拌头

搅拌机必须装上搅拌头才会运转，没装头的机器即使通入压缩空气也不会转。

| 搅拌头 | 安装方式 | 作用 |
|---|---|---|
| 铁搅拌头 | 手持**铁铲**右键 | 只让机器转起来，不供热；不需要加热的配方（如把作物搅成混合生物质）用它就够 |
| 烈焰搅拌头 | 手持**烈焰棒**右键 | 运转时向上方供热；需要加热的配方（熔融巧克力、酿造等）必须用它 |

- 取下：**空手潜行右键**，搅拌头会回到背包，背包满了就掉在脚下。
- 更换：先取下，再装新的，机器会自动换成对应的搅拌头。

电搅拌机通电时会自行加热，因此它装铁搅拌头也能做需要加热的配方，详见[电搅拌机](electric_blender)。

## 配方

<recipe id="anvilcraft_pigeon_plus:blender"/>

