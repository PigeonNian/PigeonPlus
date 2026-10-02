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

**度量方法**：日志里错误列表会出现两遍，且同一行可能报多个错误。
唯一可信的指标是「去重后的 `文件:行` 位置数」。用：

```powershell
$enc = [System.Text.UTF8Encoding]::new($false, $false)   # 容错解码，日志是混合编码
$text = [System.IO.File]::ReadAllText($err, $enc)
$m = [regex]::Matches($text, '\\([^\\]+\.java):(\d+): 错误: ')
($m | ForEach-Object { "$($_.Groups[1].Value):$($_.Groups[2].Value)" } | Sort-Object -Unique).Count
```

| 阶段 | 唯一错误位置 |
|---|---|
| 初始（配置迁移完成） | 417 |
| 第一批机械替换后 | 387 |
| 第二批（粒子 + datagen）后 | 336 |
| 第三批（FluidAction 等）后 | 331 |
| 第四批（客户端入口：流体类型/事件/矩阵）后 | 315（69 个文件）|
| 第五批（BER 三段式 + 独立模型注册）后 | 266 |
| 第六批（全部 BER 三段式完成）后 | 241 |
| 第七批（粒子 + NBT + 光照）后 | 205 |
| 第八批（配方体系）后 | **40** |

> **教训：优先修「被大量引用的签名」。** 205 → 40 的骤降来自两处根因修复：
> `HasCauldronSimple` 由 `Fluid` 改为 `Identifier`，以及
> `RecipeSerializer` 由接口变为 record。它们各自级联出几十个错误，
> 逐个改调用点会做几十次无用功。

### 配方体系被整体重写

**`RecipeSerializer` 从接口变成 record：**
```java
// 旧：class Serializer implements RecipeSerializer<T> { codec(); streamCodec(); }
// 新：
public static final RecipeSerializer<T> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);
```
注册处也从 `Serializer::new` 改为 `() -> T.SERIALIZER`。

**`Recipe` 接口新增/移除的方法：**

| 变化 | 说明 |
|---|---|
| 新增 `placementInfo()` | 返回 `PlacementInfo.NOT_PLACEABLE`（不走合成台摆放）|
| 新增 `recipeBookCategory()` | 如 `RecipeBookCategories.CRAFTING_MISC` |
| 新增 `display()` | 有默认实现，可不覆写 |
| 移除 `assemble(input, registries)` | → `assemble(input)` |
| 移除 `canCraftInDimensions` / `getResultItem` | — |

**`HasCauldronSimple` / `HasCauldron` 改用 `Identifier`：**
```java
// 旧
hasCauldron.fluid(BuiltInRegistries.FLUID.get(id));
hasCauldron.transform(BuiltInRegistries.FLUID.get(id), produce);
hasCauldron.hasFluid();  hasCauldron.transforms()
// 新：直接传 Identifier；hasFluid/transforms 移除，改用空值常量判断
hasCauldron.fluid(id);
hasCauldron.transform(id).produce(produce);   // transform 与 produce 拆成两次调用
HasCauldron.isNotEmpty(hasCauldron.fluid())    // EMPTY / NULL 两个哨兵
```
`HasCauldron` 的包是 `dev.dubhe.anvilcraft.recipe.anvil.predicate.block`
（**不是** `recipe.component`，`HasCauldronSimple` 才在 `recipe.component`）。
`WrapUtils.cauldron2Fluid(Block)` 现在返回 `Identifier`。

### 粒子 API

- `addParticle` 新增 `alwaysShow` 参数（取代 `addAlwaysVisibleParticle`）：
  旧 8 参 → 新 9 参 `addParticle(opts, overrideLimiter, alwaysShow, x,y,z,vx,vy,vz)`。
  原第二参 `true` 语义是 `overrideLimiter`，迁移时须补 `alwaysShow=false` **以保持原行为**。
- `ItemParticleOption` 不再接受 `ItemStack`：改为 `Item` 或 `ItemStackTemplate`
  （用 `ItemStackTemplate.fromNonEmptyStack(stack)` 转换）。

### NBT 序列化（ValueInput / ValueOutput）

```java
// 旧                                   // 新
saveAdditional(CompoundTag, Provider)   saveAdditional(ValueOutput)
loadAdditional(CompoundTag, Provider)   loadAdditional(ValueInput)
tag.putInt/getInt                       output.putInt / input.getIntOr(name, default)
tag.contains(x) ? tag.getInt(x) : d     input.getIntOr(name, d)   // 一行完成
tag.put(name, subTag)                   output.child(name) / input.childOrEmpty(name)
tag.getUUID / putUUID                   移除！改用 UUIDUtil.CODEC / uuidToIntArray
```
- `InfinityFluidTank`：`writeToNBT/readFromNBT` → `serialize(ValueOutput)/deserialize(ValueInput)`
- `ItemStackHandler`：`serializeNBT/deserializeNBT` → `serialize(ValueOutput)/deserialize(ValueInput)`
- `ContainerHelper.saveAllItems/loadAllItems` 也已适配新 API。

### 光照 API

- `Level#getMaxBuildHeight` → `LevelHeightAccessor#getMaxY`
- `BlockState#getLightBlock(level,pos)` → `getLightDampening()`
  （与新版原版 `BeaconBlockEntity` 的写法一致，可直接照抄）

### 剩余 40 个错误的性质（下一步）

```
HUD 体系（GuiGraphics -> GuiGraphicsExtractor）  8 处，5 个类
ItemInteractionResult / InteractionResultHolder   6 处
三个渲染 mixin（AnvilCraft 已删或改名目标类）     10 处
ExistingFileHelper / MobSpawnType / InteractionMap /
  EnchantedBookItem / Input / FluidStackPredicate 6 处
LargeCauldronBlockEntityRendererMixin            6 处（方案见上文）
FluidMixingRecipeSerializerMixin                 1 处（Serializer 变 private）
```

### 独立方块模型的新写法（已落地，可复用）

26.1 没有「可直接丢给渲染器的模型对象」了，渲染一个独立模型要走：

```java
// 1) 顶层静态常量（使用点所在类里）
public static final StandaloneModelKey<BlockStateModel> MY_MODEL =
    StandaloneBlockModels.key("block/my_model");

// 2) 在 ModelEvent.RegisterStandalone 里注册
StandaloneBlockModels.register(event, MY_MODEL, id("block/my_model"));

// 3) 在 BER 的 submit() 里绘制
StandaloneBlockModels.submit(poseStack, collector, MY_MODEL, lightCoords, OverlayTexture.NO_OVERLAY);
```

底层做的事（`StandaloneBlockModels` 已封装）：
- 注册：`SimpleUnbakedStandaloneModel.blockStateModel(id)`；
- 取模型：`Minecraft.getInstance().getModelManager().getStandaloneModel(key)`；
- 展开：`BlockStateModel.collectParts(RandomSource, List<BlockStateModelPart>)`；
- 提交：`SubmitNodeCollector#submitBlockModel(poseStack, RenderType, parts, int[] tints, light, overlay, outline)`；
- 渲染层用 `Sheets.cutoutBlockSheet()`（这些附加模型都带镂空）。

**已按新结构重写的 BER**（零错误）：
`AnvilPumpBlockEntityRenderer`、`BlenderBlockEntityRenderer`、`FeedSpreaderBlockEntityRenderer`。

### 颜色注册改为「染色来源」模型

```java
// 方块：注册 BlockTintSource 的**列表**（每个 tintIndex 一个）
event.register(List.<BlockTintSource>of(state -> 0x6E5F2C), block);

// 物品：RegisterColorHandlersEvent.ItemTintSources，注册 ItemTintSource + MapCodec
```
`DynamicFluidContainerModel.Colors` **已删除**；`DynamicFluidContainerModel` 本身改为完整
`ItemModel`（`net.neoforged.neoforge.client.model.item.`），流体桶染色改为数据驱动，
所以物品染色注册整段可以去掉。

### 流体盒渲染的新写法（已查明，待实施）

`FluidRenderHelper#renderFluidBox` 签名大改（旧版收 `FluidStack` + `MultiBufferSource`）：

```java
public void renderFluidBox(
    TextureAtlasSprite sprite,      // 新增：贴图要从 FluidModel 取
    FluidResource fluid,            // 由 FluidStack 改为 FluidResource
    float minX, minY, minZ, maxX, maxY, maxZ,
    int color,                      // 新增：染色值
    VertexConsumer builder,
    PoseStack.Pose pose,            // 由 MultiBufferSource 改为直接给 buffer + pose
    int light,
    boolean renderBottom,
    boolean invertGasses
)
```

调用方要改成「在 `submitCustomGeometry` 的回调里画」：

```java
FluidModel model = FluidRenderHelper.getModel(
    Minecraft.getInstance().getModelManager().getFluidStateModelSet(),
    resource.getFluid());
var tintSource = model.fluidTintSource();
int tintColor = tintSource == null ? -1 : tintSource.colorAsStack(resource.toStack(1));
TextureAtlasSprite sprite = model.stillMaterial().sprite();

submitNodeCollector.submitCustomGeometry(poseStack, FLUID_RENDER_TYPE, (pose, buffer) ->
    FluidRenderHelper.INSTANCE.renderFluidBox(
        sprite, resource, minX, minY, minZ, maxX, maxY, maxZ,
        tintColor, buffer, pose, state.lightCoords, true, false));
```

`FLUID_RENDER_TYPE` 取自 AnvilCraft 的 `BaseFluidHandlerHolderRenderer.FLUID_RENDER_TYPE`。
参考实现：AnvilCraft `LargeCauldronBlockEntityRenderer#submitFluids`。

### 大炼药锅 BER mixin 的重写方向（待实施）

AnvilCraft 的 `LargeCauldronBlockEntityRenderer` 已改为
`BlockEntityRenderer<LargeCauldronBlockEntity, LargeCauldronRenderState>`，
我们 mixin 里的三个注入点需要重新定位：

| 原注入 | 新的做法 |
|---|---|
| `@Inject(method="render", TAIL)` 渲染喷口盖板 | 改注入 `submit` 的 TAIL；坐标从 `state.blockPos` 取，`level` 从 `Minecraft.getInstance().level` 取 |
| `@Inject(method="drawFluids", HEAD)` 画气体层 | `drawFluids` 已改名 `submitFluids(state, poseStack, submitNodeCollector)`；改用 `submitCustomGeometry` |
| `@Redirect(method="render", drawFire)` 隐藏火焰 | 火焰现在是 `state.getFire()`（`BlockModelRenderState`）。更干净的做法：注入 `extractRenderState` 的 TAIL，有推进剂时把 fire 状态清空 |

若要连同喷口盖板一起渲染，`LargeCauldronAttachmentModels.TOP/BOTTOM` 已经定义好。


### 客户端渲染体系的三处结构变更（已查证，第四批）

**1. `BlockEntityRenderer` 改为 RenderState 模式**

```java
// 26.1
public interface BlockEntityRenderer<T extends BlockEntity, S extends BlockEntityRenderState> {
    S createRenderState();
    default void extractRenderState(T, S, float partialTick, Vec3 cameraPos, CrumblingOverlay);
    void submit(S, PoseStack, SubmitNodeCollector, CameraRenderState);   // 取代 render()
}
```
- `render(...)` **不存在了**；数据提取与绘制拆成两个方法。
- `registerBlockEntityRenderer` 需要 `BlockEntityRendererProvider<T, S>`（2 个类型参数）。
- 渲染方块模型改用 `SubmitNodeCollector#submitBlockModel(PoseStack, RenderType, List<BlockStateModelPart>, int[], int, int, int)`，
  不再有 `BlockRenderDispatcher.renderSingleBlock(BakedModel, ...)`。
- 范例（**推荐照抄**）：原版 `net/minecraft/client/renderer/blockentity/EnchantTableRenderer.java`，
  源码可从 `minecraft-patched-26.1.2.75-sources.jar` 提取。

**2. `RenderLevelStageEvent` 由「枚举阶段」改为「事件子类」**

```java
// 旧：监听 RenderLevelStageEvent，再 if (event.getStage() != Stage.AFTER_LEVEL) return;
// 新：直接声明参数类型
private void onRenderLevelStage(RenderLevelStageEvent.AfterLevel event) { ... }
```
且 `getCamera()` 已移除 → 相机位置改取
`event.getLevelRenderState().cameraRenderState.pos`；
`getModelViewMatrix()` 现在返回**不可变** `Matrix4fc`（旧代码若声明 `Matrix4f` 会报「不兼容的类型」）。

**3. 流体的贴图与渲染层改为数据驱动**

- `ItemBlockRenderTypes` 与 `RenderType.translucent()` 均已移除，**AnvilCraft 26.1 里也没有任何 `setRenderLayer` 调用**。
- `IClientFluidTypeExtensions` 只剩 `getRenderOverlayTexture` / `renderOverlay` / `modifyFogColor` / `modifyFogRender`；
  `getStillTexture` / `getFlowingTexture` / `getTintColor` 全部移除。
- `FluidType` 与 `FluidType.Properties` 也**没有**任何贴图字段。
  → 流体外观（贴图、是否半透明）现由**资源文件**决定；染色在渲染时用 `tintSource.colorAsStack(...)` 取。
- `ModClientFluidTypeExtensionImpl` 构造器由 6 参 `(still, flow, fogColor, fogDist, tintColor, flag)`
  简化为 2 参 `(int fogColor, float fogDistance)`。

**4. `ModelEvent.RegisterAdditional` → `ModelEvent.RegisterStandalone`（API 完全不同）**

```java
// 旧
event.register(new ModelResourceLocation(id, "standalone"));
// 新：需要 StandaloneModelKey<T> + UnbakedStandaloneModel<T>
<T> void register(StandaloneModelKey<T>, UnbakedStandaloneModel<T>);
```
`ModelResourceLocation` 与 `BakedModel` 均已移除。**尚未处理**（需与 BER 重写一起做）。

### 剩余错误最集中的文件

```
~30x  client/AnvilCraftPigeonPlusClient.java   （模型注册 + BER 注册）
21x  client/renderer/block/FeedSpreaderBlockEntityRenderer.java
17x  client/renderer/block/BlenderBlockEntityRenderer.java
16x  mixin/client/LargeCauldronBlockEntityRendererMixin.java
15x  block/entity/CompressedAirDrainFluidHandler.java
14x  client/renderer/block/StasisBeaconBlockEntityRenderer.java
```

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

### 在 26.1.2 中**完全不存在**的类（已逐一核实，需结构性改造）

这些不是改名，而是在 jar 里搜不到任何精确匹配，需要找替代概念或改写实现：

```
MobSpawnType                （曾用于实体生成）
InteractionResultHolder     （交互返回值包装）
DirectionProperty           （方块朝向属性）
ItemInteractionResult       （物品交互结果）
TextureSheetParticle        （粒子基类）
ExistingFileHelper          （数据生成辅助）
FluidStackPredicate         （AnvilCraft）
FluidTankRenderUtil         （AnvilCraft）
AbstractPipeCheckValveBlockEntity（AnvilCraft）
```

已确认存在（可直接改导入）：
```
LightTexture   -> net.minecraft.client.renderer.Lightmap
BlockBehaviour.Properties -> net.minecraft.world.level.block.state.BlockBehaviour$Properties
Input          -> net.minecraft.world.entity.player.Input
```

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

### datagen 模型体系被整体替换（重要）

1.21.1 用的 NeoForge `BlockStateProvider` / `ConfiguredModel` / `ModelFile` /
`ExistingFileHelper` / `BlockModelProvider`（`net.neoforged.neoforge.client.model.generators`
顶层类）在 26.1 **全部移除**，改为原版新的 `BlockModelGenerators` 体系。
AnvilLib 的 `RegistrumBlockModelGenerator extends BlockModelGenerators` 直接暴露新 API。

对照：

| 1.21.1 | 26.1 |
|---|---|
| `provider.models().getExistingFile(id)` | `BlockModelGenerators.plainVariant(id)`（不再需要查找校验）|
| `ConfiguredModel.builder().modelFile(v).rotationY(n).build()` | `MultiVariantGenerator.dispatch(block, variant).with(旋转常量)` |
| `provider.simpleBlock(block, modelFile)` | `BlockModelGenerators.createSimpleBlock(block, variant)` |
| `provider.getVariantBuilder(b).forAllStates(...)` | `MultiVariantGenerator.dispatch(b).with(PropertyDispatch.initial(prop).generate(...))` |
| `prov.withExistingParent(name, id)`（物品模型）| `RegistrumItemModelGenerator.createWithExistingModel(item, id)` |
| 手写 `rotationY()` 等角度函数 | 原版常量 `ROTATION_HORIZONTAL_FACING`（东=90°）/ `ROTATION_HORIZONTAL_FACING_ALT`（南=0°）|

**注意 `blockstate` / `model` 回调的签名也变了**：现在接收
`NonNullSupplier<NonNullBiConsumer<...>>`，所以 lambda 必须再包一层供应商：
```java
.blockstate(() -> (ctx, gen) -> gen.blockStateOutput.accept(...))   // 注意 () -> 
.model(() -> (ctx, gen) -> gen.createWithExistingModel(...))
```
（1.21.1 是直接传 BiConsumer，所以旧写法会报「lambda 表达式中的参数类型不兼容」。）

三轴旋转（如喷口）无法用原版常量表达，需自行
`plainVariant(id).with(X_ROT_90).with(Y_ROT_90)`；`VariantMutator` 常量在
`BlockModelGenerators` 上（`NOP` / `X_ROT_90` / `X_ROT_180` / `Y_ROT_90` …）。

**`AddonBlocks` 已按新 API 重写完毕。**

### 原版源码 jar 可用（重要）

`build/moddev/artifacts/minecraft-patched-26.1.2.75-sources.jar` 是**原版源码**，
解压后可直接阅读 `BlockModelGenerators` 等实现，比反编译快得多：

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead((Resolve-Path "build/moddev/artifacts/minecraft-patched-26.1.2.75-sources.jar"))
$e = $zip.Entries | Where-Object { $_.FullName -eq 'net/minecraft/client/data/models/BlockModelGenerators.java' }
[System.IO.Compression.ZipFileExtensions]::ExtractToFile($e, "$env:TEMP\BMG.java", $true)
$zip.Dispose()
```

### 最佳参考：AnvilCraft 26.1 自己的源码

AnvilCraft 用同一套 AnvilLib，其 `dev/dubhe/anvilcraft/util/registrater/DataGenUtil.java`
是 datagen 迁移的现成范例（`onlyState()` / `horizontalFacingBlock()` /
`transparentBlock()` / `slabBlock()` 等），遇到不确定的 API 直接照抄它的写法最稳。

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
- **查看源码内容一律用 `read` 工具，不要用 `Get-Content`**：`Get-Content` 默认按系统 ANSI（本机 GBK）解码，
  UTF-8 的中文注释会显示成乱码（如 `鍙湪鎮┖`），容易误判成文件损坏。文件本身是好的
  （用 `UTF8Encoding($false, $true)` 严格解码可验证）。
- **`Select-String` 默认不区分大小写**：查 API 名时可能误报（例如查 `FluidAction` 会命中 `fluidaction`）。
- 构建配置改动后需 `--no-configuration-cache` 或让 Gradle 自行失效缓存。
