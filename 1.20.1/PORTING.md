# 1.20.1 Forge 移植进度

主模组 1.21.1 NeoForge（`F:\Work\Frilien's Magic\src\`）→ 1.20.1 Forge（`F:\Work\Frilien's Magic\1.20.1\`）。

**两套代码各自维护**，都按各自版本的惯用 API 写，不互相迁就。

## 环境

| 项 | 值 |
|---|---|
| Minecraft | 1.20.1 |
| Forge | 47.4.27 |
| Java | 17 |
| Gradle | 8.8（ForgeGradle 6.x） |
| 映射 | official |
| 包名 | `yifei.frliliens.magic`（与 1.21 版相同） |
| 模组版本 | 1.0.3 |

构建：`cd 1.20.1 && gradlew.bat build`
产物：`build/libs/friliensmagic-1.0.3.jar`

网络说明：`maven.minecraftforge.net` 可达；`maven.neoforged.net` 与 `neoforged.net` 超时（Forge 用不到）。

## 已完成

| 模块 | 文件 | 说明 |
|---|---|---|
| 骨架 | 主类、`gradle.properties`、`build.gradle`、`mods.toml`、`pack.mcmeta` | pack_format 15 |
| 配置 | `config/Config.java` | 81 项，`ForgeConfigSpec` |
| 数据 | `attachment/` 5 个 record | 不可变 record + 手写 NBT |
| 能力 | `capability/` 6 个 | Capability 体系（替代附件），7 种数据 |
| 网络 | `network/` 4 个 | SimpleChannel + 同步包 + 2 个双向包 |
| 法术 | `spell/` 20 个 | 框架 + 15 个法术 |
| 物品 | `item/` 6 个 | 法杖 / 17 卷轴 / 5 药水 / 刨冰 / 刷怪蛋 |
| 伤害 | `damage/ModDamageTypes` | 纯原版 API，直接复用 |
| 效果 | `effect/` 2 个 | 魔力隐匿 |
| 护盾 | `spell/defense/` 2 个 | 伤害拦截 + 弹射物偏转 |
| 客户端 | `client/` 9 个 | 魔力条 HUD、护盾渲染、法术轮盘、按键、编辑界面 |
| 实体 | `entity/` 2 个 | 委托村民 |
| 委托 | `quest/` 14 个 | 委托池、数据包加载、命令、通报 |
| 战利品 | `loot/` 2 个 | 维度注入修正器 |
| 提示 | `world/` 3 个 | 欢迎语（标题）+ 社群信息（聊天栏） |
| 事件 | `handler/` 7 个 | 魔力回复、飞行、隐匿、委托进度与村民 |
| 资源 | `data/` 7 个 | 伤害类型、战利品、配方、标签 |

共 79 文件 7921 行（1.21 版 77 文件 6583 行）。

**构建**：`cd 1.20.1 && gradlew.bat build`
产物：`build/libs/friliensmagic-1.0.3.jar`（5913 KB）

**资源校验**：`python tools/audit_1201.py`（检查物品/模型/贴图/名称/配方与战利品字段名）

## 移植已完成

文件层面 1.21 的 77 个 Java 文件全部有对应实现
（`FriliensMagicClient` 与 `ModAttachments` 是被替代而非缺失：
前者改为 `client/FriliensMagicClient`，后者由 `capability/` 体系取代）。

数据包 JSON 已按 1.20.1 的字段名修正：

| 文件 | 1.21 | 1.20.1 |
|---|---|---|
| `recipe/*.json` 的 result | `"id"` | `"item"`（Forge 的 `CraftingHelper` 读这个字段） |
| `loot_modifiers/*.json` 的 items | `"count"` | `"Count"`（DFU 的 `ItemStack.CODEC` 字段名，大写） |

模型文件直接从 1.21 复制（**不要**用 id 推断贴图名——`scroll_clean` 用的是
`scroll_deepblue`、`scroll_float` 用的是 `scroll_sage`、`shaved_ice` 在 `item/food/` 下）。

## 待进游戏验证

以下项目**未在游戏内测试**，是首次运行的关注点：

1. 配方能否正常合成（字段名已按源码核对，但未经实战）
2. 战利品修正器是否正确注入卷轴
3. 护盾着色器 `assets/friliensmagic/shaders/core/shield.*` 能否编译
   （GLSL 是原版风格，应该兼容；`shield.json` 的字段格式未逐项核对）
4. 委托村民的发光与委托交互
5. 魔力条 HUD 的位置与缩放

## 已知可优化项（非缺陷）

- `textures/gui/icon_mana_conceal_transparent.png` 与
  `textures/mob_effect/mana_conceal.png` 内容完全相同且各 2.1 MB
  （1.21 项目就是这样），可考虑去重以减小 jar 体积
- 2 张 GUI 立绘（`frieren_1.png` 109 KB、`fern.png` 87 KB）可考虑压缩

## 关键 API 差异（踩过的坑）

### 1. 附件 → Capability（最大工程量）

1.20.1 **没有** `AttachmentType`。7 项玩家数据全部改为 Capability：

```java
// 1.21
player.getData(ModAttachments.MANA)
player.setData(ModAttachments.MANA, v)
player.syncData(ModAttachments.MANA)     // 自动同步

// 1.20.1
PlayerDataHelper.mana(player)
PlayerDataHelper.setMana(player, v)
PlayerDataHelper.syncMana(player)        // 必须显式发包
```

`Capability<T>` 的 `T` 是**接口类型**，不是数据类型。所以 `PlayerDataCapability`
有两个泛型参数：`<数据类, 接口类>`。写成 `Capability<ManaData>` 编译不过。

### 2. 配置取值

| 1.21 NeoForge | 1.20.1 Forge |
|---|---|
| `ModConfigSpec` | `ForgeConfigSpec` |
| `.getAsInt()` / `.getAsDouble()` / `.getAsBoolean()` | `.get()`（只有这一个） |

`ConfigValue<T> implements Supplier<T>`，所以 `get()` 返回**装箱**类型。
`(int) x.get()` 合法，但 `(float) x.get()` **不合法**——需要 `x.get().floatValue()`。

### 3. 事件签名

| 1.21 | 1.20.1 |
|---|---|
| `PlayerTickEvent.Post` | `TickEvent.PlayerTickEvent` + `event.phase == END` |
| `ServerTickEvent.Post` | `TickEvent.ServerTickEvent` |
| `LivingIncomingDamageEvent` | `LivingHurtEvent` |
| `EntityTickEvent.Pre`（弹射物） | `ProjectileImpactEvent`（更好：命中瞬间判定） |
| `ProjectileImpactEvent.setCanceled(true)` | `setImpactResult(ImpactResult.STOP_AT_CURRENT_NO_DAMAGE)` |

### 4. 其它

- `DeferredHolder` → `RegistryObject`
- `DeferredItem` / `DeferredRegister.Items` → `DeferredRegister<Item>` + `RegistryObject`
- `DeferredSpawnEggItem` → `ForgeSpawnEggItem`
- `RegistryObject` **不实现** `Holder`，传给 `MobEffectInstance` 要 `.get()`
- `SoundEvents.RESPAWN_ANCHOR_DEPLETE` 是 `Holder.Reference<SoundEvent>`，要 `.value()`
- `Item.getUseDuration(ItemStack, LivingEntity)` → `getUseDuration(ItemStack)`
- `Item.appendHoverText(..., TooltipContext, ...)` → `appendHoverText(..., Level, ...)`
- `FoodProperties.Builder.saturationModifier` → `saturationMod`
- `IConfigScreenFactory` / `ConfigurationScreen` 在 1.20.1 不存在，配置界面要自己写

### 5. 客户端与渲染（差异最集中的一块）

**`@Mod` 注解没有 `dist` 参数**（已用字节码核对 `javafmllanguage-1.20.1-47.4.27.jar`
里的 `Mod.class`，只有 `value` 与 `bus`）。所以客户端入口不能写成
`@Mod(value = ..., dist = Dist.CLIENT)`，改为普通类，由主类用
`DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientEntry.register(bus))` 调用。

**GUI 覆盖层**：

| 1.21 | 1.20.1 |
|---|---|
| `RegisterGuiLayersEvent.registerAboveAll(ResourceLocation, GuiLayer)` | `RegisterGuiOverlaysEvent.registerAboveAll(String, IGuiOverlay)` |
| `GuiLayer.render(GuiGraphics, DeltaTracker)` | `IGuiOverlay.render(ForgeGui, GuiGraphics, float, int, int)` |

注意 id 是 **String** 不是 `ResourceLocation`，且方法参数多两个，
所以方法引用对不上，要写成显式 lambda。

**BufferBuilder / VertexConsumer**：

| 1.21 | 1.20.1 |
|---|---|
| `tesselator.begin(Mode, Format)` | `tesselator.getBuilder()` 然后 `buffer.begin(Mode, Format)` |
| `buffer.buildOrThrow()` | `buffer.end()` |
| `.addVertex(Matrix4f, x, y, z)` | `.vertex(Matrix4f, x, y, z)` |
| `.setColor(float r, g, b, a)` | `.color(int r, g, b, a)`（**整数**，浮点要 ×255） |
| `.setNormal(x, y, z)` | `.normal(Matrix3f, x, y, z)`（要法线矩阵） |
| 每个顶点不用显式收尾 | 需要 `.endVertex()` |

**Screen**：

| 1.21 | 1.20.1 |
|---|---|
| `renderBackground(GuiGraphics, int, int, float)` | `renderBackground(GuiGraphics)` |
| `mouseScrolled(double, double, double, double)` | `mouseScrolled(double, double, double)`（无横向） |

**其它**：

- 客户端 tick：`ClientTickEvent.Post` → `TickEvent.ClientTickEvent` + 判 `phase == END`
  （不判会一条 tick 调用两次，按键响应翻倍）
- `TickEvent.PlayerTickEvent` / `ServerTickEvent` 的实体字段是
  `public final Player player`，**没有** `getEntity()`（非 tick 事件才有）
- `RenderLevelStageEvent.getPartialTick()` 在 1.20.1 直接返回 `float`，
  没有 1.21 的 `DeltaTracker.getGameTimeDeltaPartialTick(boolean)`
- `DeltaTracker` 类在 1.20.1 不存在

### 6. DataFixerUpper 版本差异（隐蔽但影响大）

1.20.1 用 **DFU 6.0.8**，1.21 用 10.x。以下 API 在 6.0.8 里**不存在**：

| 1.21 (DFU 10.x) | 1.20.1 (DFU 6.0.8) |
|---|---|
| `Codec.validate(fn)` / `MapCodec.validate(fn)` | 无 → 校验挪到解析后手工做 |
| `DataResult.Error`（带 `message()`） | 无 → `error()` 返回 `Optional<PartialResult<T>>`，用 `.map(PartialResult::message)` |

踩坑记录：查 API 时先看了 `datafixerupper-10.0.21.jar`，结论是「有 validate」，
但 1.20.1 实际用的是 `6.0.8`。**核对版本再用**。

### 7. SavedData 的签名

| 1.21 | 1.20.1 |
|---|---|
| `save(CompoundTag, HolderLookup.Provider)` | `save(CompoundTag)` |
| `load(CompoundTag, HolderLookup.Provider)` | `load(CompoundTag)` |
| `computeIfAbsent(SavedData.Factory<T>, String)` | `computeIfAbsent(Function<CompoundTag,T>, Supplier<T>, String)` |

`SavedData.Factory` 在 1.20.1 **不存在**。

### 8. 战利品修正器

`IGlobalLootModifier#codec()` 在 1.21 返回 `MapCodec`，1.20.1 返回 `Codec`，
所以用 `RecordCodecBuilder.create(...)`（两参数 `apply`）而不是 `mapCodec(...)`。
注册表键从 `NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS`
换成 `ForgeRegistries.GLOBAL_LOOT_MODIFIER_SERIALIZERS`。
另外 `ItemStack.OPTIONAL_CODEC` 在 1.20.1 不存在，用 `ItemStack.CODEC`。

### 9. capability 必须显式同步（最容易漏，且症状很迷惑）

1.21 的附件带 `sync()`，改完自动推送客户端。1.20.1 的 capability
**完全没有这个机制**，所以每一处写操作后面都必须跟一个同步调用：

```java
PlayerDataHelper.setQuest(player, quest.accept());
PlayerDataHelper.syncQuest(player);          // 漏了这行，客户端永远看旧值
```

**漏掉的症状很迷惑**——不是报错，而是「功能像没反应」：

| 漏了 | 症状 |
|---|---|
| `syncQuest` | 委托系统看起来完全没用（接了委托客户端不知道） |
| `syncSelectedSpell` | 法术轮盘显示的选中法术永远不变 |
| `syncMana` | 魔力条不动 |

用 `python tools/audit_sync_1201.py` 可以扫出所有「set 了但没 sync」的位置。

另外还需要**登录时全量推一次**（`PlayerDataSyncHandler`）——
否则玩家刚进游戏时客户端没有任何数据，魔力条要等到第一次自然回复
（最多 5 秒）才有数值。

### 10. 资源编码（中文乱码的根源）

**两条独立的坑，都会让中文变成乱码：**

**① `gradle.properties` 里的中文**

Java 规范规定 `.properties` 按 **ISO-8859-1** 读取，`-Dfile.encoding` 也改不了。
gradle.properties 是 UTF-8，中文会被逐字节当成 Latin-1，再以 UTF-8 写出，
结果每个非 ASCII 字符都有字节被替换成 `?`（`《` 的 `E3 80 8A` 变成 `E3 3F 8B`）。

**修法：中文不要放在 gradle.properties 里**，直接写进 `mods.toml`
（TOML 由 Forge 以 UTF-8 读取，安全）。

**② Gradle 处理资源时的默认编码**

如果 JVM 默认编码是 GBK（中文 Windows），ProcessResources 也可能读坏 UTF-8 源文件。

**修法**：`org.gradle.jvmargs=-Xmx3G -Dfile.encoding=UTF-8`，
而且**改动后必须停掉 Gradle 守护进程**（`gradlew --stop`），
否则旧守护进程带着旧编码继续跑，看起来像没生效。

校验：`python tools/verify_mods_toml.py` —— 会对产物做严格 UTF-8 解码并逐字符比对。

### 11. 物品模型里的 NeoForge 专属字段

1.21 的 Blockbench 会导出这两个键：

```json
"loader": "neoforge:item_layers",
"neoforge_data": { "layers": { "0": { "block_light": 15, "sky_light": 15 } } }
```

**Forge 1.20.1 不认识它们**——`loader` 会去找不存在的模型加载器，
模型加载失败，物品显示成紫黑格。直接删掉即可（1.20.1 没有按 layer 指定光照的机制）。
顺带把 `builtin/generated` 换成现代的 `minecraft:item/generated`。

用 `python tools/fix_models_1201.py` 处理。

### 12. 创造模式物品栏

1.21 的 `withTabsBefore` 接受其它分类的翻译键，1.20.1 接受
`ResourceKey<CreativeModeTab>`：

```java
// 1.20.1
.withTabsBefore(CreativeModeTabs.COMBAT)
```

### 13. 字符串索引表要把「所有」数据表都收进去

移植时把 1.21 的附件改成 capability，需要一张「标识字符串 → 能力」的查找表
（因为同步包只能传字符串）。这里踩过一个**编译期无法发现**的坑：

```java
// 错误：只遍历了玩家表
private static Map<String, Kind<?>> buildIndex() {
    for (Kind<?> kind : KINDS) { ... }        // VILLAGER_KINDS 被漏掉了
}
```

后果：`byId("quest_villager")` 返回 null → `setQuestVillager` 直接失败 →
**委托村民永远标不上，右键没有任何反馈，而日志里只有一行 `write=false`**。

1.21 版不会遇到这个问题，因为那边用的是类型化常量 `ModAttachments.QUEST_VILLAGER`，
根本不需要查表——**是移植引入的风险，不是原版缺陷**。

现在加了两道保险：
- `buildIndex()` 遍历 `KINDS` 与 `VILLAGER_KINDS`
- `PlayerDataKinds.verifyIndex()` 在 `commonSetup` 里自检，漏项直接抛异常
  （宁可启动就报错，也不要在游戏里表现为「功能没反应」）

### 14. 粒子在 1.20.1 有 32 格距离剔除（关键差异）

ServerLevel#sendParticles 只把粒子发给**距离玩家 32 格以内**的位置，
超出部分静默丢弃（overrideLimiter 为 false 时）：

`java
if (blockpos.closerToCenterThan(new Vec3(x, y, z), p_8638_ ? 512.0D : 32.0D)) { ... }
`

Zoltraak 的光柱铺满整个射程，学会「长距离魔法」后射程可达 200 格以上，
于是 **85% 的粒子到不了客户端**（实测日志：ttempted=17291 accepted=1979）。

**修法：不用 sendParticles，改为直接构造粒子包并设 overrideLimiter = true。**

这个做法来自本仓库另一个模组 F:\Work\Twelve Talismans 的
SmartParticleDispatcher#dispatch——它在同样的整合包里粒子显示正常：

`java
ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket(
        options, true, x, y, z, xDist, yDist, zDist, speed, count);
for (ServerPlayer sp : level.players()) {
    if (sp.level() == level) {
        sp.connection.send(packet);
    }
}
`

本模组封装成 ModNetwork.sendParticlePacket(...)。

**同时按距离降密度**（同样参照那个模组的 LOD 思路）：
主分支的射程通常只有 30 格（240 步 ≈ 2400 次发包）可以承受，
但 200 格以上按 8 步/格 会变成 1.7 万次发包，单次施放就能拖垮帧率。

| 距离 | 密度 |
|---|---|
| 0–10 格 | 全密度 |
| 10–30 格 | 半密度 |
| 30 格以上 | 1/3 密度 |

**粒子参数与主分支逐项一致**（已用脚本比对）：
BEAM_RADIUS=0.6、STEPS_PER_BLOCK=8.0，
核心 (0.95,0.97,1.0)/2.0、柱壁 (0.5,0.7,1.0)/1.2、外晕 (0.3,0.5,1.0)/0.7。

### 15. BufferBuilder 必须 begin/end 严格配对（否则崩在别的模组里）

（这一节来自一次走弯路的尝试：曾把光柱改成客户端几何渲染，
后来按需求还原成主分支的粒子效果，该尝试的代码已删除。
但这里记录的坑对以后写渲染代码仍然有效。）

**踩过的坑**：写成「先 egin()，再判断有没有东西可画，没东西就跳过 end()」：

`java
buffer.begin(Mode.QUADS, POSITION_COLOR);
int drawn = 0;
for (...) { drawn++; }
if (drawn > 0) { tesselator.end(); }   // 错！drawn == 0 时缓冲永远停在 building
`

后果：Tesselator.getInstance() 的 BufferBuilder 是**全局共享**的，
一旦卡在 building 状态，之后**任何**模组的 egin() 都会抛
IllegalStateException: Already building!。实测崩在完全无关的地方：

`
Caused by: java.lang.IllegalStateException: Already building!
  at BufferBuilder.begin(BufferBuilder.java:100)
  at xaero.lib.client.graphics.util.ImmediateRenderUtil.coloredRectangle(...)
`

**两条修法**：

1. **把判断移到 egin() 之前**——先筛出要画的内容，确认非空再开始写缓冲
2. **必要时用私有 Tesselator 实例**：Tesselator 有公开构造函数，
   自建实例后缓冲完全私有，不影响别的模组也不被影响

**为什么值得警惕**：本整合包里 BufferBuilder 被 **10 个 mixin** 修改
（ImmediatelyFast、Embeddium、Oculus、ModernFix 各若干），
共享缓冲的中间状态极难排查，崩点会跑到毫不相干的模组里去。

ShieldRenderer 没这个问题——它 egin() 后无条件 end()。
