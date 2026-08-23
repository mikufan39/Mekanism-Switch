# Mekanism-Switch 项目速览（AI 对话用）

> 本文档由 AI 于 2026-08-22 基于源码实测生成，供每次 AI 对话开始前快速了解项目。改代码后请同步更新相关小节（至少复核"核心系统"与"代码审查发现"）。

## 1. 一句话

Minecraft 1.21.1 / NeoForge 的 **Mekanism 附属模组**：新增“交换机”（物品↔SV 交换）、“复位机”（概率修耐久）两台机器，外加可手持的“便携式交换机”（免电即时上传/下载）、MekaSuit 附魔、灵魂出窍、苦力怕保护与 MekaSuit 鞘翅飞行控制（集成 Do a Barrel Roll 飞行代码）。SV 值 ≈ ProjectE EMC 思路。

## 2. 技术栈与版本

| 项 | 值 | 说明 |
|---|---|---|
| Minecraft | 1.21.1 | |
| NeoForge | 21.1.248 | `[1.21.1]` / loader `[1,)` |
| Mekanism | 1.21.1-10.7.19.85 | 必需；`compileOnly` + `localRuntime`（modmaven.dev） |
| Parchment | 1.21.1 + 2024.11.17 | 映射 |
| Java | 21（toolchain） | JAVA_HOME 是 25，toolchain 自动选 21 |
| Gradle | 9.2.1（wrapper） | ModDevGradle `2.0.144`；configuration-cache 开启 |
| Mixin | sponge-mixin 0.15.2 + MixinExtras 0.4.1 | 均 compileOnly，运行时 NeoForge 自带 |
| Mod 元数据 | `mod_id=meks`，`com.mikufan.meks`，`mod_version=0.3.0` | 见 §4 git 状态 |
| 许可 | MIT | ⚠️ 但 flight 包移植自 DABR（GPL-3.0），见 §9 |

## 3. 规模与源码结构

- 共 115 个源文件 / 约 14800 行（Java 92 个文件约 6140 行，其余为 JSON/PNG 资源）。
- 包结构（`src/main/java/com/mikufan/meks/`）：

```
meks/                      核心（2235 行）：注册、Config、命令、SV、机器
├── ExchangeSwitchTile      交换机方块实体（含 ChannelJob 状态机，693 行）★核心
├── RestorationSwitchTile   复位机方块实体（概率修复 RepairJob，465 行）★核心
├── ExchangeSwitchContainer / RestorationSwitchContainer   菜单容器
├── PortableExchangeSwitchItem / PortableExchangeSwitchContainer   便携式交换机（物品+菜单）★
├── KnowledgeEntry          知识库滚动条目（方块/便携两处共用）
├── MeksValues              SV 值系统（预设加载 + 配方推导）★核心
├── MeksAttachments / PlayerExchangeData   玩家 SV+知识（Data Attachment）
├── MeksCommands            /mek sv|knowledge 命令
├── MeksPayloads            全部网络包（9 个）★
├── MeksRegistries          注册（方块/TE/容器/物品/创造页）
├── BlockExchangeSwitch / BlockRestorationSwitch / ChannelUpgradeItem / ExchangeOperation
├── MekaSuitEnchantmentHandler / CreeperExplosionHandler / MeksLang
├── client/                  GUI：GuiExchangeSwitch、GuiRestorationSwitch、
│                            GuiPortableExchangeSwitch（便携版）、GuiKnowledgeScroll（搜索+滚动+拖拽）、
│                            能量条/进度条控件
├── soul/                    灵魂出窍：SoulOutController（状态机）、SoulCamera、
│                            SoulMotion、SoulOutKeybinds（F4）
├── flight/                  ★DABR 飞行控制：
│   ├── api/event（RollEvents/RollGroup/RollContext）、api/rotation
│   ├── impl/…、math/（Expression/ExpressionParser 自定义公式引擎）
│   ├── config/MeksFlightConfig（取值门面；配置在 meks-common.toml [flight]）
│   ├── MeksFlightClient（激活门槛）、RotationModifiers、EventCallbacksClient
│   └── net/FlightNetworking
└── mixin/                   27 个 mixin（7 公共 + 20 客户端）
    ├── EnchantmentMenuMixin / ItemSpecialArmorMixin / PlayerMixin（MekaSuit 附魔）
    ├── client/…（灵魂出窍相关 12 个：camera/渲染/输入/碰撞等）
    └── flight/…（roll 实体同步 4 个 + key 绑定 3 个 + client 视角 5 个）
```

资源（`src/main/resources/`）：`assets/meks`（lang zh_cn+en_us 各 48 键、贴图、模型、blockstates）、`data/meks/sv/`（4 个 SV 预设表）、recipe/loot_table、`meks.mixins.json`、logo.png。另有 `assets/mekanism/gui/…` 直接复用 Mekanism 的 GUI 素材。

## 4. 版本与 Git 状态（2026-08-22 实测）

- 当前分支 **`main`**，停于 `8639e24 chore: release 0.3.0`（gradle.properties 版本号 **0.3.0**）。
- working tree：**便携式交换机功能未提交**（新增 6 个 Java/资源文件 + 改 8 个文件），见 `git status`；本文档与 `AI_TOOLS.md`、`design/gen_portable_switch_tex.py` 为未跟踪文件。
- remote：`github.com/mikufan39/Mekanism-Switch`（push/pull 均正常）。
- remote：`github.com/mikufan39/Mekanism-Switch`（push/pull 均正常）。

## 5. 核心系统详解

### 5.1 SV 值系统（MeksValues，细节见 `docs/sv-values.md`）

- **预设表**：`data/meks/sv/` 下 4 个 JSON，共 **2098 条**：`emc_preset.json`（1415 条，完整来自 ProjectE `pregenerated_emc.json`，仅 minecraft:* 与 mekanism:*）、`cataclysm_preset.json`（151）、`twilightforest_preset.json`（151）、`iceandfire_preset.json`（381）。生成器在 `design/`（Python）。
- **加载**：静态块 `loadPreset()`（只按物品 ID，跳过带 `data` 组件条目——SV 不支持 NBT 差异，已知限制）。
- **推导**：`ServerStarted → ensureInitialized`（GUI/工具提示也会补初始化）：对 Shaped/Shapeless 工作台配方，原料全部有值时 `value = Σ原料值 ÷ 输出数`，最多迭代 **64 轮** 收敛。只覆盖工作台配方；熔炉/机器配方产物需手工预设（这正是三个附属预设存在的原因）。`onServerStopping` 清空派生值。
- **取值**：先 BASE 后 DERIVED，`hasValue = value > 0`。`/mek sv value <item>` 可查。
- **客户端显示**：`MekanismSwitchClient.onItemTooltip` 给有值物品加金色 `SV：值`（仅有值的显示）。

### 5.2 玩家数据（PlayerExchangeData + MeksAttachments）

- `AttachmentType<PlayerExchangeData>`：`sv(long)` + `knowledge(Set<ResourceLocation>)`；Codec 存档、`copyOnDeath`、copyHandler 复制。
- 服务端权威，客户端经 `SyncExchangePayload` 只读展示；GUI 打开/命令/开箱时同步。

### 5.3 交换机（ExchangeSwitchTile）★

- **槽位**：process(29,117)、channel(50,117，需通道升级)、forget(8,117)、energy(152,117)。`ChannelInventorySlot` 限制外部/手动输入一次只进 1 件，多余走另一通道。
- **作业模型**：3 个 `ChannelJob`（process/channel/forget），枚举 `NONE/UPLOAD/DOWNLOAD/FORGET`。
  - 上传：槽放物即自动开始（`autoStartUpload`，`suppressAutoStart` 防重复触发），完成后 `sv += value` 且 `learn(item)`，消耗物品 1 个。
  - 下载：GUI 拖拽知识项 → `startExchange`（校验知识、SV、槽空、车主身份）→ 按 `pendingCount` 逐件扣 SV 并产出。
  - 遗忘：`FORGET` 扣 SV、删除知识、物归玩家（进 forget 槽）。
- **能量/耗时**（`recalculateRequirements`）：上传 2 FE/SV、下载 4 FE/SV；tick = `20 + value×0.1/0.2`，夹在 [minTicks, maxTicks]=[20,600]；再经 `MekanismUtils.getTicks/getEnergyPerTick` 套用 SPEED/ENERGY 升级倍率。能量不足整机停机；`process()` 每 tick 先验资后扣。
- **保护**：`onAdded` 强制 `SecurityMode.PRIVATE`（只车主可开）；作业进行中锁槽（非 INTERNAL 自动化进出被拒）；任务中断恢复用 `suppressAutoStart`。
- **取消**：gui 右键物品槽只打断该槽作业——`CancelExchangePayload` 带 slot（0=process、1=channel、2=forget），服务端校验车主后仅 `cancelJob` 对应槽；两通道相互独立，互不打断；forget 槽右键仅在作业进行中拦截（完成后右键仍走原版取物）。
- **存档**：三个 job 的 operation/pending/progress/suppress/target + `channelUpgrade` 标志进出 NBT；减量更新包带 channelUpgrade。⚠️ `loadAdditional` 里 `channelUpgrade` 必须**先于** `super.loadAdditional()` 读取——ITEM 容器附件（`ContainerType.ITEM`）按 `getInventorySlots(null)` 的列表保存/恢复，该列表成员随 `channelUpgrade` 变化；顺序反了读取时列表少一格、后续槽位索引错位、能量槽物品（末位索引）被 `DataHandlerUtils.readContents` 静默丢弃（表现为退出重进后能量立方消失）。
- **通道升级**：`ChannelUpgradeItem` 潜行右键安装一次（`tryInstallChannelUpgrade`），装后双通道并行。

### 5.4 便携式交换机（PortableExchangeSwitchItem + PortableExchangeSwitchContainer）★

- **物品**：手持右键打开 GUI（`SimpleMenuProvider`，无 tile/能量/副作用）；合成：传送核心、钋球（`mekanism:pellet_polonium`）、反物质球（`mekanism:pellet_antimatter`）环绕方块交换机 3x3。外观 = 便携式QIO仪表板 + 屏幕内上下箭头（形状同通道升级绿色箭头，`design/gen_portable_switch_tex.py` 生成 16x16 纹理与 176x240 GUI 背景）。
- **菜单**：`MekanismContainer` 直连（非 tile 容器），无机器槽/能量/侧面配置/升级；`getInventoryYOffset`=136；数据直接从玩家 `PlayerExchangeData` attachment 读取（与方块交换机**共享同一份**知识+SV，服务端权威）。
- **GUI**（`GuiPortableExchangeSwitch`）：8 列 × 5 行知识库（与方块版同列数、多 1 行；格子尺寸与物品栏一致、左对齐 x=8）+ 搜索框 + 悬停名称/SV；库与物品栏之间 16px 间隔、SV 居中显示（不渲染"物品栏"标签，与方块版一致）；无遗忘/物品/能量槽、无进度条。GUI 高 212 = 物品栏槽区 76 + 底部贴合。
- **交互**（3 个新 payload：`PortableUpload`/`PortableDownload`（带 forget 位）/`RequestPortableSync`）：
  - 上传：左键点击背包槽 = 上传 1 件，Shift+左键 = 整组；**即时完成**（无电力、无耗时）：`sv += value×count`、`learn(item)`、扣实物；无 SV 值物品拒绝并 actionbar 提示；禁止上传便携式交换机自身。
  - 下载：左键点击库条目 = 1 件，Shift+左键 = 整组（maxStack 上限），Shift+右键 = 半组（向上取整，**不遗忘**）；校验知识 + SV 足够 + 背包有空间（36 格预算），扣 SV 后 `inventory.add` **生成真实物品**（非幽灵物品）；失败原因 actionbar 提示（SV 不足/背包满）。
  - **遗忘**：右键点击库条目 = 下载 1 件**成功后**再遗忘知识（防误操作：SV 不足/背包满导致下载失败时不会删知识），成功 actionbar 提示"已遗忘 xxx"；无遗忘槽（方块版才有）。
  - 同步：打开 GUI/操作后 `syncPortableToOwner` 复用 `SyncExchangePayload`（handler 同时支持方块与便携两种容器）。

### 5.5 复位机（RestorationSwitchTile）★

- **槽位**：单格 repairSlot（容量 1，只收可损坏物品；自动化只收已损坏的）+ energy。修复中 `canExtract` 仅 INTERNAL/取消/修满。
- **成本**：`svCost = value>0 ? ceil(value/100) : fallbackSvCost(616)`（每点耐久 = 物品 SV 的 1%，未知值物品固定 616/点）；能量 = svCost×2 FE；tick = `20+svCost×0.1` 夹 [20,600]，升级同交换机。
- **扣费**：`tickAttempt` 按比例 `proportional()` 逐 tick 扣 SV/能量（数学上恰好不多扣）；余额不足自动暂停，恢复后续跑。
- **判定**：每轮结束 `chance = ceil(剩余耐久/maxDamage×100) + failBoost`（上限 100）；成功 -1 耐久并清零 failBoost，失败 +1 failBoost（失败不回本）。修满自动停；换物品/耐久变化会 `resetJob`（**已扣的按比例消耗不退**——设计取舍）。
- **取消**：GUI 右键取消 → `repairCancelled`（存档），允许取回物品；换物品后自动复位取消标志。

### 5.6 MekaSuit 附魔（3 个 mixin + 1 handler）

`ItemSpecialArmorMixin.isEnchantable→true` + `EnchantmentMenuMixin.getEnchantmentList`（第三格强制 `Protection V`，其余格子空）+ `EnchantmentLevelSetEvent`（第三格费用 30 级）+ `PlayerMixin.onEnchantmentPerformed` 改为扣 30 级（费用固定 30 级，常量 `MekaSuitEnchantmentHandler.MEKA_SUIT_ENCHANT_COST`，原 `mekaSuitEnchantCost` 配置已删除）。

### 5.7 苦力怕保护

`ExplosionEvent.Detonate`：源实体是苦力怕时 `affectedBlocks.clear()`（实体伤害保留），配置可关。

### 5.8 灵魂出窍（纯客户端，`soul/`）

- F4 开关（`SoulOutKeybinds`）；要求戴 MekaSuit 头盔、配置开启；`SoulCamera` 是本地假实体（`BlockStateBaseMixin` 让其无碰撞），相机穿墙自由飞行。
- 能量：基础 500 J/tick，每 45 秒翻倍（指数增长 `base×2^(秒/45)`），创意免能；受伤或能量耗尽自动回体（`disableOnDamage`）。
- **服务器不可见**（客户端自嗨）；`freezeBody` 冻结本体但部分服务器会拉回。相关客户端 mixin 12 个（camera/渲染/输入/HUD/发包拦截等）。

### 5.9 飞行控制（`flight/`，DABR 集成）

- 集成 **Do a Barrel Roll 3.7.3（GPL-3.0）** 的飞行管线（`design/dabr-integration.md` 有详细说明，原自研三轴飞行已删除）；在 **鞘翅飞行（`isFallFlying`）+ 配置开 + 未潜水** 时激活（`MeksFlightClient.isFallFlying`）。**无能量消耗**；`enabled` 只从配置文件读取（I 键开关已移除）。
- 激活范围：`activationMode` 配置——默认 `ELYTRA_UNIT`（仅 MekaSuit 胸甲+鞘翅单元用户），可选 `GLOBAL`（任意鞘翅飞行玩家，如原版鞘翅）。
- 管线：`RollEvents.EARLY_CAMERA_MODIFIERS`（键盘 1800 优先级 / 鼠标 `configureRotation` 1000）+ `LATE_CAMERA_MODIFIERS`（控制面效能、平滑、banking、自动回正），组 `FALL_FLYING_GROUP`。
- 数学：自研 `ExpressionParser/Expression` 支持 `$roll/$pitch/$velocity_*/$banking_strength/TO_RAD` 等变量的公式字符串，全部来自 `meks-common.toml [flight]`（原独立文件 `meks-flight-client.toml` 已并入——模组双端必装，单一配置文件即可）。
- 同步：`RollSyncPayload`（C2S）+ `RollSyncS2CPayload`（S2C）→ `RollEntity` mixin（ServerEntity/实体/玩家）。**服务端中继受自身 `[flight] enabled` 门控**（服务端关闭则不接收/不转发）；**新追踪者配对时（`ServerEntity.addPairing` TAIL）首帧推送当前 roll**，中途加入的玩家无需等对方 roll 变化即可看到当前横滚。仅客户端安装则退化为本地效果。
- HUD：自定义 HUD（地平线/动量准星）已整体移除，`GuiMixin` 删除；F3 面板仍显示 roll 值。

## 6. 网络（MeksPayloads）

registrar `versioned("2").optional()`（v1→v2：`CancelExchange` 增加 slot 字段，按槽位定向取消），共 **10 个 payload（8 个 C2S + 2 个 S2C）**：
客户端→服务端：`StartExchange`、`RequestExchangeSync`、`CancelExchange`、`CancelRepair`、`PortableUpload`、`PortableDownload`、`RequestPortableSync`、`RollSync`；服务端→客户端：`SyncExchange`、`RollSyncS2C`。所有 handler `enqueueWork`；GUI 操作一律发包，服务端校验归属后执行。`SyncExchange` 的 handler 同时支持方块交换机与便携式交换机两种容器。

## 7. 配置（单文件，`config/Mekanism/`，服务端权威，改后重启生效）

> 配置已统一为一个文件 **`meks-common.toml`**，注册为 `ModConfig.Type.SERVER`（原 `meks-client.toml` 已并入）：**客户端不读本地配置，直接使用服务端同步下来的值**（飞行、灵魂出窍等客户端行为也遵循服务端）。43 个配置项逐一详解见 `docs/config.md`。

| 段 | 关键项 |
|---|---|
| `[server]` | mekaSuitEnchantment=**false**（附魔固定 30 级） / creeperNoBlockDamage=**false** |
| `[machine.exchangeSwitch]` | upload/downloadFePerSv=2, upload/downloadTicksPerSv=0.1, min/maxTicks=20/600 |
| `[machine.restorationSwitch]` | fallbackSvCost=616, energyPerSv=2, ticksPerSv=0.1, min/maxTicks=20/600 |
| `[flight]` | enabled=**false**, activationMode=ELYTRA_UNIT/GLOBAL, 三轴灵敏度/平滑、banking、自动回正、3 组公式字符串（无能量/HUD 项；原 `meks-flight-client.toml` 已并入） |
| `[soulOut]` | enabled, baseCostPerTick=500, costDoublingSeconds=45, 速度/本体/伤害回体选项（原 `meks-client.toml` 并入） |

⚠️ **`run/config/Mekanism/` 目前只有 `startup.toml`**——meks 配置文件要首次 `runClient` 进世界才会生成，别在 run/config 里找不到就以为没注册。

## 8. 命令

`/mek sv get|value|set|add`、`/mek knowledge list|add|remove|clear|unlock-all`（操作他人需权限 2）。`<item>` 用物品 ID（如 `minecraft:diamond`）。

## 9. 代码审查发现

### ✅ 做得好的
- 服务端权威 + 客户端只读展示；所有网络 handler `enqueueWork`；归属/安全校验到位（强制 PRIVATE、UUID 比对）。
- 作业状态、通道升级、取消标志全部进 NBT；`SyncableEnum/SyncableInt/SyncableItemStack` 同步完整。
- 防误操作设计成熟：作业锁槽、自动化单件分流、`suppressAutoStart`、下载时 `checkForInterrupt` 用 `isSameItemSameComponents` 校验。
- 能量按比例分摊、`proportional()` 数学防多扣；`Math.addExact` 防溢出（命令 add 有 overflow 分支）。
- 注释充分（中英），mixin 方法统一 `meks$` 前缀，DABR 集成来源明文记录。

### ⚠️ 建议处理（按优先级）

1. **许可合规（中，发布前必办）**：`flight/` 包是 **GPL-3.0 (DABR)** 代码的移植/集成，但仓库 `LICENSE` 仍是 MIT，包内无 GPL 声明、无 NOTICE。GPL 派生代码与整体打包的许可处理需要明确（至少：flight 包单独标注 GPL-3.0 来源 + 附 DABR 版权声明；发行物许可描述相应调整）。来源已在 `CHANGELOG`/`design/dabr-integration.md` 注明，但正式许可文件欠缺。
2. **文档/版本漂移（低）**：README 安装段写 `meks-0.2.1.jar`，gradle.properties 已是 0.3.0；README 特性列表可补充便携式交换机与 0.3.0 发布说明。
3. **Git 状态（低）**：便携式交换机功能（本次改动）尚未提交，未推送远端；发布前整理提交并打标签。
4. **复位机资源不回退（低，设计取舍）**：中断/换物品/掉线时按比例已扣的 SV 与能量不退；且失败轮次不退钱 → 期望修复成本高于标价的 1% SV/点。当前“实验性”定位可接受，建议日后文档化或给配置项。
5. **SV 推导精度（已知限制）**：`firstKnownValue` 对 tag 原料取“第一个有值条目”（可能低估）；空原料跳过；64 轮上限；仅工作台配方。`docs/sv-values.md` 已声明，别当 bug 修。
6. **单进程限定（低）**：`MeksValues` 静态表 + 首个服务器初始化，客户端与服务端共用；当前架构（单机/专用服）无碍，勿在未加锁下改成热更新。
7. **便携式交换机（新，待实机验证）**：上传/下载即时结算无能量/耗时；GUI 高 280 用自绘背景（176x280 PNG）；禁止上传便携交换机自身；`register(MenuSupplier)` 注册的无 tile 容器未走 Mekanism item 容器管线——行为等价但属自定义路线，进世界冒烟测试（合成、右键、上传/下载、Shift 整组、背包满提示）后再发布。

### 🔸 代码细节备注
- `ExchangeSwitchTile.DOWNLOAD` 满输出分支会退 SV 并保留 job 重试——实际难触发（起始槽必空、`pendingCount≤maxStack`），属防御性代码，逻辑正确。
- `RestorationSwitchTile.tickAttempt` 在 `ticksRequired<=0` 时直接 return 未重置状态（极小概率，可忽略或顺手修）。
- `MeksValues.loadPreset` 缺文件抛 `IllegalStateException`（启动即崩）——有意为之的 fail-fast。
- 无单元测试/GameTest（`runGameTestServer` 已配置但无测试类），全靠手动验证；加回归测试是未来收益点。

## 10. AI 协作速查

| 想做什么 | 改哪里 |
|---|---|
| 调机器数值/行为 | `Config.java`（默认值）+ `ExchangeSwitchTile`/`RestorationSwitchTile` |
| 加/改 SV 物品 | `data/meks/sv/*.json` + `MeksValues`（派生逻辑）+ 生成器在 `design/` |
| 客户端功能 | `client/` 包 + `mixin/client/` + `soul/` 或 `flight/` |
| 新增网络消息 | `MeksPayloads`（定义+codec+handler+发送方）三处联动 |
| 新增文本 | `assets/meks/lang/zh_cn.json` + `en_us.json`（双语 48 键） |
| 构建验证 | `.\gradlew.bat build`；运行 `.\gradlew.bat runClient`（首次进世界生成配置） |
| 查值 | `/mek sv value <item>`；Git 状态看 `git log --oneline` + `git status` |