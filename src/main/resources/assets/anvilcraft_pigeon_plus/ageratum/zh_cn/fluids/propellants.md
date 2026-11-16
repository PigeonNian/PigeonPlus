---
title: 燃气与燃料
navigation:
  title: 燃气与燃料
  weight: 20
items:
  - "anvilcraft_pigeon_plus:nozzle"
  - "anvilcraft_pigeon_plus:liquid_oxygen_bucket"
  - "anvilcraft_pigeon_plus:liquid_hydrogen_bucket"
  - "anvilcraft_pigeon_plus:liquefied_biogas_bucket"
---

# 燃气与燃料

<item id="anvilcraft_pigeon_plus:nozzle"/>
<item id="anvilcraft_pigeon_plus:liquid_oxygen_bucket"/>
<item id="anvilcraft_pigeon_plus:liquid_hydrogen_bucket"/>
<item id="anvilcraft_pigeon_plus:liquefied_biogas_bucket"/>

## 氧化剂：液氧

液氧是三种燃气共用的氧化剂，本身不是燃料。

- **压缩空气**：在大型储罐中加压液化（见[气体与管道](gas_diffusion)）
- 每 **415 mB** 压缩空气产出 **1 mB** 液氧。
- **氧气**：直接液化 AnvilCraft 的氧气，每 **100 mB** 产出 **1 mB** 液氧。

液氧约 90 K，倒进世界会在 1～4 秒内蒸发，所以要用储罐、管道或桶转运，不能像水一样留在原地。

## 燃料：液氢

液氢是氢氧燃气专用的燃料，由沼气经蒸汽重整制取（见[氢气与液氢](hydrogen)）。

它比液氧更冷（约 20 K）、更易蒸发，同样不能露天存放。

## 三种燃气

| 燃气 | 燃料 | 氧化剂 | 每桶燃料耗氧 | 30 秒累计加速 | 火焰 |
| --- | --- | --- | --- | --- | --- |
| 煤油燃气 | 原油（`#c:oil`） | 液氧 | 2 桶 | 约 320 b/t | 橙黄转亮白 |
| 甲烷燃气 | 液化沼气 | 液氧 | 1.5 桶 | 约 192 b/t | 带淡蓝色外焰 |
| 氢氧燃气 | 液氢 | 液氧 | 0.5 桶 | 约 160 b/t | 近乎无色的暖白 |

- **加速上限**指单个喷管持续喷射 30 秒、配合静滞信标锁定铁砧时能累计到的速度。
- 三种燃料同时存在时，按 **甲烷 → 氢氧 → 煤油** 的顺序取用。

### 煤油燃气

原油与液氧共同存在并被点燃时触发。加速上限最高，火焰偏明亮的白橙色，是推力优先时的选择。

### 甲烷燃气

液化沼气与液氧共同存在并被点燃时触发。加速上限居中，火焰带天蓝色外焰，适合已经有沼气产线的基地。

### 氢氧燃气

液氢与液氧共同存在并被点燃时触发。加速上限最低，但火焰近乎无色，视觉上最干净。

## 点火

1. 往大型炼药锅里放入燃料与液氧；
2. 再丢入引火物点燃：火把、灵魂火把、营火、灵魂营火或烈焰粉会被消耗；烧红的钨块 / 下界合金块不会消耗，可以长期留在锅里反复点火；
3. 在锅侧装好[喷管](../devices/nozzle)，喷管朝向决定喷射方向；
4. 喷管收到红石信号时，燃气立即关闭。

## 消耗

- 喷管运转期间每 **1 秒**从锅里抽走 **1 桶（1000 mB）** 燃料。
- 同一时刻按最佳混合比抽走液氧：煤油 2 桶、甲烷 1.5 桶、氢氧 0.5 桶。燃料不足一整桶时按同比例少扣，不会多抽氧。
