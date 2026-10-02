# PigeonPlus 26.1 移植笔记

目标版本：Minecraft `26.1.2` / NeoForge `26.1.2.75` / Java 25
起点版本：Minecraft `1.21.1` / NeoForge `21.1.248` / Java 21
分支：`dev/26.1/1.1`（`dev/1.21.1/1.1` 保留可用状态）

## 已完成的构建配置迁移

| 文件 | 改动 |
|---|---|
| `gradle.properties` | `java_version=25`、`minecraft_version=26.1.2`、`neo_version=26.1.2.75`、`neo_version_range=[26.1,)`；移除 parchment 两项 |
| `gradle/libs.versions.toml` | 见下表版本；新增 ageratum |
| `gradle/scripts/repositories.gradle` | Cjsah 仓库地址由 `/maven/releases` 改为 `/maven/`，新增 `dev.anvilcraft.resource` 分组，改用 `exclusiveContent` |
| `dependencies.gradle` | 新增 `implementation(libs.ageratum)` |
| `gradle/scripts/moddevgradle.gradle` | 移除 `parchment {}`（26.1 已不用）；`data` 由 `data()` 改为 `clientData()`（我们有 `includeClient()` 的 sounds.json 提供器） |
| `gradle/scripts/publishing.gradle` | `project.archivesBaseName` → `project.base.archivesName.get()`（Gradle 9 移除旧属性） |
| `gradle/wrapper/gradle-wrapper.properties` | Gradle `8.14.5` → `9.5.0`（Java 25 需要 Gradle 9.x） |
| `build.gradle` | 工具链 21 → `JavaLanguageVersion.of(java_version)`（即 25） |

### 版本对齐（取自 AnvilCraft `1.6.0+snapshot.2284` 的 POM，非各自最新版）

```
anvilcraft  = 1.6.0+snapshot.2284   (dev.dubhe:anvilcraft-neoforge-26.1.2)
anvillib    = 2.0.0+snapshot.519    (dev.anvilcraft.lib:anvillib-neoforge-26.1)
ageratum    = 0.0.1+build.109       (dev.anvilcraft.resource:ageratum-neoforge-26.1.2)
neoforge    = 26.1.2.75
jei         = 29.6.2.31
modDevGradle= 2.0.141
```

> 关键：AnvilLib/Ageratum 必须与 AnvilCraft 编译时用的版本一致，否则运行时 `NoSuchMethodError`。

### 构建命令（必须指定 JAVA_HOME）

```powershell
$env:JAVA_HOME = "C:\Users\鸽の念\.jdks\jbr-25.0.3"
.\gradlew.bat compileJava --console=plain
```

本机已有 JBR 25（`~/.jdks/jbr-25.0.3`，含 javac）。默认 `java` 仍是 21，必须显式设置 `JAVA_HOME`。

## 编译现状

构建配置已通过（`gradlew help` 成功）。

> **重要：以 `-Xmaxerrs 10000` 为准。**
> javac 默认只报 100 个错误就截断，此前看到的「189 / 200 个错误」全是
> `100 错误 × 2 行` 的假象，会严重低估工作量。已在 `build.gradle` 里设置
> `options.compilerArgs << "-Xmaxerrs" << "10000"`，务必用真实数字判断进度。

**真实规模：1010 个错误 / 76 个文件**（完成第一批机械修复后为 **926**）。

### 真实错误分类（1010 时）

| 数量 | 错误 | 说明 |
|---|---|---|
| 582 | 找不到符号 | 见下方 API 对照 |
| 116 | 方法不会覆盖或实现超类型的方法 | 签名变更，主要是 BER / 渲染体系 |
| 32 | 无法从静态上下文引用非静态 `has(ItemLike)` | `RegistrumRecipeProvider.has()` 由静态变实例 |
| 28 | `random` 在 `Level` 中是 protected | → `level.getRandom()` |
| 24 | `isClientSide` 在 `Level` 中是 private | → `level.isClientSide()` |
| 14 | lambda 参数类型不兼容 | |
| 12 | `ResourceHandler<FluidResource>` 无法转为 `IFluidHandler` | 流体能力 API 变更 |
| 12 | `ModClientFluidTypeExtensionImpl` 构造器不匹配 | 我们自己的类 + NeoForge API |
| 10 | `Optional<Integer>` 无法转为 `int` | |
| 10 | `addParticle` 签名不匹配 | |
| 10 | `registerBlockEntityRenderer` 类型不匹配 | BER 需要 2 个类型参数 |
| 10 | `FluidRenderHelper.renderFluidBox` 不匹配 | |
| 10 | `BlockEntity.loadAdditional` 不匹配 | 签名变更 |

### 已完成的第一批机械修复（1010 → 926）

| 修复 | 处数 |
|---|---|
| `ResourceLocation` → `Identifier` | 78（14 文件）|
| `advancements.critereon` → `advancements.criterion` | 3 文件 |
| `implements SimpleInstance` → `SimpleInstance`（内部类） | 3 文件 |
| `RenderType.X()` → `RenderTypes.X()`（含 `beaconBeam`/`lines`/`cutoutMovingBlock`/`translucentMovingBlock`）| 4 文件 |
| AnvilLib `providers.RegistrumRecipeProvider` → `providers.generators.` | 6 文件 |
| AnvilCraft 类包路径迁移 | 7 文件 |
| `RegistrumRecipeProvider.has()` → `provider.has()` | 16 |
| `level.isClientSide` → `level.isClientSide()` | 12 |
| `level.random` → `level.getRandom()` | 14 |

### 已确认的 API 变更对照

| 1.21.1 | 26.1.2 | 影响 |
|---|---|---|
| `net.minecraft.resources.ResourceLocation` | `net.minecraft.resources.Identifier` | 78 处，**已修** |
| `net.minecraft.advancements.critereon.*` | `net.minecraft.advancements.criterion.*`（拼写修正） | **已修** |
| `net.minecraft.client.renderer.RenderType` | `net.minecraft.client.renderer.rendertype.RenderType` + 工厂方法移到 `RenderTypes` | 注意：`translucent()/cutout()/solid()` 这类**方块**渲染层不再存在于 `RenderTypes`，BER 里改用 `translucentMovingBlock()` / `cutoutMovingBlock()` |
| `ChunkSectionLayer`（新） | 新的方块渲染层枚举：`SOLID` / `CUTOUT` / `TRANSLUCENT`，带 `pipeline()` | 替代旧的方块 RenderType 概念 |
| `Level.isClientSide`（字段） | `isClientSide()`（方法） | **已修** |
| `Level.random`（字段） | `getRandom()` | **已修** |
| `LightTexture` | `net.minecraft.client.renderer.Lightmap` | |
| `GuiGraphics` | **已移除** → `GuiGraphicsExtractor` + `net.minecraft.client.renderer.state.gui.GuiRenderState` | 38 处，HUD 体系整体重构 |
| `BlockEntityRenderer<T>` | `BlockEntityRenderer<T, S extends BlockEntityRenderState>`：新增 `createRenderState()`，`render()` → `submit(S, PoseStack, SubmitNodeCollector, CameraRenderState)` | 5 个 BER 需重写 |
| `BakedModel` | **已移除** → `net.minecraft.client.renderer.block.model.BlockModel` / `BlockStateModel`（`block/dispatch/`）| |
| `ModelResourceLocation` | **已移除** | 20 处；`event.register(...)` 的模型注册方式也变了（`RegisterAdditional` 已无）|
| `BlockRenderDispatcher` | **已移除** → `BlockStateModelDispatcher` / `BlockModelResolver` | |
| `ItemBlockRenderTypes` | **已移除**（NeoForge 侧待查替代）| 6 处，用于设置流体渲染层 |
| `TextureSheetParticle` | 粒子体系重构 | |
| `MobSpawnType` / `InteractionResultHolder` / `DirectionProperty` / `ItemInteractionResult` / `Properties` / `Input` | 均已移位或改名，待逐个查 | |
| `net.neoforged.neoforge.client.model.data` | **包已移除** | 6 处 |
| `net.neoforged.neoforge.client.model.generators` | **包已移除** | 2 处 |
| `ExistingFileHelper` / `DynamicFluidContainerModel` / `RegisterAdditional` | 移位或移除 | |

### AnvilCraft 26.1.2 类迁移（已查证）

```
api.fluidtank.InfinityFluidTank                        -> api.fluid.InfinityFluidTank
block.BurningHeaterBlock                               -> block.workstation.BurningHeaterBlock
block.GiantAnvilBlock                                  -> block.workstation.GiantAnvilBlock
block.HeaterBlock                                      -> block.power.consumer.HeaterBlock
block.item.FlexibleMultiPartBlockItem                  -> item.block.FlexibleMultiPartBlockItem
block.LargeFluidTankBlock                              -> block.container.LargeFluidTankBlock
block.Layered4LevelCauldronBlock                       -> block.cauldron.Layered4LevelCauldronBlock
item.AnvilHammerItem                                   -> item.tool.AnvilHammerItem
client.renderer.blockentity.FishTankBlockEntityRenderer    -> client.renderer.blockentity.FishTankRenderer
client.renderer.blockentity.LargeFluidTankBlockEntityRenderer -> client.renderer.blockentity.LargeFluidTankRenderer
```

**已从 AnvilCraft 26.1.2 完全移除**（需找替代或改实现）：
```
dev.dubhe.anvilcraft.util.FluidStackPredicate            （用于 BlendingCategory）
dev.dubhe.anvilcraft.client.renderer.FluidTankRenderUtil （FluidTankRenderUtilMixin 的目标）
dev.dubhe.anvilcraft.block.entity.fluid.AbstractPipeCheckValveBlockEntity
```
（`CheckValve` 相关仍有 `block/item/CheckValveItem`、`client/renderer/blockentity/PipeCheckValveBERenderer`、
`client/renderer/blockentity/state/PipeCheckValveRenderState`，BlockEntity 的新名字待查。）

### AnvilLib 2.0.0+519 变更

```
dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider
  -> dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumRecipeProvider
```
且 `has(ItemLike)` 由**静态**变为**实例**方法（调用处需 `provider.has(...)`）。

### 错误最多的文件

```
client/renderer/block/*BlockEntityRenderer.java   （5 个 BER，各 5+ 错）
data/recipe/*RecipeLoader.java                     （RegistrumRecipeProvider，共 28 错）
advancement/criterion/*Trigger.java                （critereon→criterion，共 14 错）
client/hud/*.java                                  （GuiGraphics，共 9 错）
client/AnvilCraftPigeonPlusClient.java             （注册与渲染入口）
```

## 建议的推进顺序

1. ~~**机械替换**~~（已完成第一批：`Identifier`/`criterion`/`RenderTypes`/`isClientSide`/`getRandom`/`has`）
2. **继续低风险替换**：`MobSpawnType`、`InteractionResultHolder`、`DirectionProperty`、`ItemInteractionResult`、
   `Properties`、`Input`、`LightTexture`→`Lightmap`、`ExistingFileHelper` 等，逐个从 26.1.2 jar 查新位置
3. **移除/替代 AnvilCraft 已删类**：`FluidStackPredicate`、`FluidTankRenderUtil`、`AbstractPipeCheckValveBlockEntity`
4. **NeoForge API**：`model.data` / `model.generators` 包移除后的替代、流体能力
   （`ResourceHandler<FluidResource>` vs `IFluidHandler`）、`ItemBlockRenderTypes` 替代
5. **渲染体系重构**（工作量最大）
   - 5 个 BER 改为 `BlockEntityRenderer<T, S>`：实现 `createRenderState()`，`render()` → `submit()`
   - 3 个 HUD 由 `GuiGraphics` 改为 `GuiGraphicsExtractor` + `GuiRenderState`
   - 模型体系：`BakedModel` / `ModelResourceLocation` / `BlockRenderDispatcher` / `ModelData`
   - 3 个 mixin（`FishTank` / `LargeFluidTank` / `FluidTankRenderUtil` 渲染器 mixin）需按新渲染体系重写
6. **验证**：`runClient` 启动 + mixin 注入无报错

## 已建立的排查手段

### 1. 从 jar 查类的新位置

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead((Resolve-Path "build/moddev/artifacts/minecraft-patched-26.1.2.75-merged.jar"))
$zip.Entries | Where-Object { $_.FullName -like "*Foo*" -and $_.FullName -notmatch '\$' } | ForEach-Object { $_.FullName }
$zip.Dispose()
```

### 2. 从依赖的 sources.jar 查准确 API（强烈推荐）

AnvilLib 与 AnvilCraft **都发布了 sources jar**，解压后可直接读到源码与包名，比反编译快得多：

```
~/.gradle/caches/modules-2/files-2.1/dev.anvilcraft.lib/*/2.0.0+snapshot.519/*-sources.jar
~/.gradle/caches/modules-2/files-2.1/dev.dubhe/anvilcraft-neoforge-26.1.2/1.6.0+snapshot.2284/*-sources.jar
```

**批量找出失效导入**的做法：解压 sources jar 建立「可用全限定类名」集合，
再用我们源码里的 `^import (dev\.anvilcraft\.lib\..+);` 与之比对，一次列出全部需迁移的类
（本次据此定位 1 个 AnvilLib + 13 个 AnvilCraft 失效导入）。

### 3. 用 javap 查方法签名

```powershell
& "C:\Program Files\Java\jdk-17\bin\javap.exe" -cp $jar net.minecraft.resources.Identifier
```

## 坑与注意

- **javac 默认 100 错误截断**：见上，务必用 `-Xmaxerrs`，否则严重低估工作量。
- **Gradle 输出编码**：`gradlew` 的 stderr 是**混合编码**（文件路径 UTF-8、javac 中文错误文本另有编码），
  `read`/`Get-Content` 可能整体失败。做法：用 `[System.IO.File]::ReadAllText($p, [Text.UTF8Encoding]::new($false,$false))`
  容错解码后再正则提取。用 `Start-Process -RedirectStandardError` 落盘。
- **`gradlew` 退出码**：PowerShell 管道下 stderr 有输出时 `[exit code: 1]` 不代表构建失败，须看 `BUILD SUCCESSFUL`。
- **PowerShell 变量插值**：`"$g:$a"` 会被当成驱动器限定符报错，需写 `${g}:${a}`。
- **`Select-String` 默认不区分大小写**：查 API 名时容易误判，需要时加 `-CaseSensitive`。
- **批量替换源码文件**：用 `[System.IO.File]::ReadAllText` / `WriteAllText` + `UTF8Encoding($false)`（无 BOM），
  不要用 PowerShell 文本管道（会破坏编码）。替换 `ResourceLocation` 时必须用负向后顾 `(?<!Model)` 排除
  `ModelResourceLocation`，否则会被误改成 `ModelIdentifier`。
- 构建配置改动后需 `--no-configuration-cache` 或让 Gradle 自行失效缓存。
