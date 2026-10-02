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

构建配置已通过（`gradlew help` 成功）。`compileJava` 约 **189 个错误**，根因集中在少数几类。

### 已确认的 API 变更对照

| 1.21.1 | 26.1.2 | 影响 |
|---|---|---|
| `net.minecraft.resources.ResourceLocation` | `net.minecraft.resources.Identifier` | **20 处** |
| `net.minecraft.advancements.critereon.*` | `net.minecraft.advancements.criterion.*`（拼写修正） | 14 处 |
| `net.minecraft.client.renderer.RenderType` | `net.minecraft.client.renderer.rendertype.RenderType` | 16 处 |
| `GuiGraphics` | **已移除** → `GuiGraphicsExtractor`（HUD 体系重构） | 38 处 |
| `BlockEntityRenderer<T>` | 需要 **2 个类型参数**（RenderState 重构） | 5 个类 |
| `BakedModel` / `ModelResourceLocation` / `BlockRenderDispatcher` | 已移除/改名（模型体系重构） | 28 处 |
| `LightTexture` / `ItemBlockRenderTypes` / `TextureSheetParticle` | 已移除/改名 | 12 处 |
| `SimpleInstance`（criterion 下） | 已移除/改名 | 4 处 |
| `RegistrumRecipeProvider` | AnvilLib 2.0.0+519 中已改名/移位 | 28 处 |
| `Layered4LevelCauldronBlock` | AnvilCraft 26.1.2 中已改名/移位 | 4 处 |
| `DirectionProperty` / `ItemInteractionResult` | 已移除/改名 | 21 处 |

### 错误最多的文件

```
client/renderer/block/*BlockEntityRenderer.java   （5 个 BER，各 5+ 错）
data/recipe/*RecipeLoader.java                     （RegistrumRecipeProvider，共 28 错）
advancement/criterion/*Trigger.java                （critereon→criterion，共 14 错）
client/hud/*.java                                  （GuiGraphics，共 9 错）
client/AnvilCraftPigeonPlusClient.java             （注册与渲染入口）
```

## 建议的推进顺序

1. **机械替换**（低风险、消除大部分错误）
   - `ResourceLocation` → `Identifier`（含 import 与静态引用）
   - `advancements.critereon` → `advancements.criterion`
   - `RenderType` 包路径
2. **查询 AnvilLib 2.0.0+519 的实际 API**（`RegistrumRecipeProvider` 等）
   - 从 `~/.gradle/caches/modules-2/files-2.1/dev.anvilcraft.lib/` 解 jar 核对
3. **查询 AnvilCraft 26.1.2 的实际 API**（`Layered4LevelCauldronBlock` 等）
4. **渲染体系重构**（工作量最大）
   - `BlockEntityRenderer` 的 RenderState 改造（5 个 BER）
   - HUD 由 `GuiGraphics` 改为 `GuiGraphicsExtractor`
   - 模型体系（`BakedModel` / `ModelResourceLocation` / `ModelData`）
5. **验证**：`runClient` 启动 + mixin 注入无报错

## 参考：如何从 jar 里核对 API

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead((Resolve-Path "build/moddev/artifacts/minecraft-patched-26.1.2.75-merged.jar"))
$zip.Entries | Where-Object { $_.FullName -like "*Foo*" } | ForEach-Object { $_.FullName }
$zip.Dispose()
```

依赖 jar 缓存位置：
- `~/.gradle/caches/modules-2/files-2.1/dev.dubhe/anvilcraft-neoforge-26.1.2/`
- `~/.gradle/caches/modules-2/files-2.1/dev.anvilcraft.lib/`
- `~/.gradle/caches/modules-2/files-2.1/dev.anvilcraft.resource/ageratum-neoforge-26.1.2/`

## 坑与注意

- **Gradle 输出编码**：`gradlew` 的 stderr 是 UTF-8，用 PowerShell 的 `Get-Content`（默认 ANSI）读会乱码；用 `Start-Process -RedirectStandardError` 落盘后，以 UTF-8 读取（本会话中 `read` / `grep` 工具可直接正确读取）。
- **`gradlew` 退出码**：PowerShell 管道下 stderr 有输出时 `[exit code: 1]` 不代表构建失败，须看 `BUILD SUCCESSFUL` 文本。
- **PowerShell 变量插值**：`"$g:$a"` 会被当成驱动器限定符报错，需写 `${g}:${a}`。
- 构建配置改动后需 `--no-configuration-cache` 或让 Gradle 自行失效缓存。
