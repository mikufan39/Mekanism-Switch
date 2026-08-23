# 配置文件总览（Mekanism-Switch）

本文档整理本模组全部配置文件与可配置项。所有配置项基于 `src/main/java/com/mikufan/meks/Config.java`（定义与默认值）及代码实际消费点核实。

## 0. 文件位置与生效规则

| 文件 | 类型 | 段 | 项数 |
|---|---|---|---|
| `config/Mekanism/meks-common.toml` | SERVER（服务端权威，同步到客户端） | `[server]` / `[machine.exchangeSwitch]` / `[machine.restorationSwitch]` / `[flight]` / `[soulOut]` | 2 + 6 + 5 + 23 + 7 = **43** |

- **全部配置都在这一个文件里**（原 `meks-client.toml` 已并入；原 `meks-flight-client.toml` 此前也已并入）。
- **服务端权威**（`ModConfig.Type.SERVER`）：客户端不再读取本地配置文件——玩家连接服务器时，客户端直接使用**服务端**的配置值（飞行、灵魂出窍等客户端行为也遵循服务端设置）。多人服务器上请改**服务端**的 `config/Mekanism/meks-common.toml`；单人游戏即本地存档对应的 config 目录。
- ⚠️ 主菜单行为：未连接服务器时配置尚未加载，此时按默认值处理（如飞行 `enabled=false`、灵魂 `enabled=true` 等）——所以进主菜单时灵魂出窍会表现为默认开启状态，连接/进世界后切换为服务端值；主菜单按 F4 等操作不会崩溃也不会生效。
- 生成时机：配置文件在**首次启动服务端（含进入单人存档世界）**后生成。
- **改配置需重启服务端**（NeoForge 静态读取，无热重载）。
- 默认值若被非法修改，加载时会被钳制到合法范围，不会崩溃。

---

## 1. 全部配置项详解

### 1.1 `[server]` 服务端

#### `mekaSuitEnchantment`（布尔，默认 `false`）
MekaSuit 附魔台功能总开关（**默认关闭**）。开启后 MekaSuit 护甲（`ItemMekaSuitArmor`）**可在附魔台附魔**：
- 附魔台界面第三格强制显示 **Protection V**，其余两格为空；
- 附魔固定收取 **30 级**经验（原 `mekaSuitEnchantCost` 配置项已删除，费用为硬编码常量 `MekaSuitEnchantmentHandler.MEKA_SUIT_ENCHANT_COST`）；
- 普通物品附魔行为不受影响。
关闭后 MekaSuit 元件回到原版不可附魔状态。代码位置：`EnchantmentMenuMixin` / `ItemSpecialArmorMixin` / `PlayerMixin` / `MekaSuitEnchantmentHandler`。

#### `creeperNoBlockDamage`（布尔，默认 `false`）
苦力怕保护（**默认关闭**）。开启后苦力怕爆炸**不破坏方块**（`ExplosionEvent.Detonate` 中 `affectedBlocks.clear()`），但**爆炸对实体（玩家/生物）的伤害仍正常造成**；关闭则保持原版破坏行为。代码位置：`CreeperExplosionHandler`。

### 1.2 `[machine.exchangeSwitch]` 交换机（物品↔SV 交换）

> 计算方式（`ExchangeSwitchTile.recalculateRequirements`）：
> `总耗时(ticks) = clamp(20 + SV值 × ticksPerSv, minTicks, maxTicks)`（先按 INT 取整）
> `总能量(FE) = SV值 × fePerSv`
> 每 tick 能量 = `总能量 ÷ 总耗时`（向下取整、至少 1），再经 `MekanismUtils.getTicks/getEnergyPerTick` 套用 **SPEED（速度）与 ENERGY（能量）升级**的倍率。
> 上传与下载分开计费；遗忘操作（FORGET）无能量/耗时。

#### `uploadFePerSv`（长整数，默认 `2`，范围 0–2^63−1）
**上传**（物品 → SV）每 1 点 SV 消耗的总能量（FE）。默认 2 FE/SV，即价值 1000 SV 的物品上传耗 2000 FE。

#### `downloadFePerSv`（长整数，默认 `2`，范围 0–2^63−1）
**下载**（SV → 物品）每 1 点 SV 消耗的总能量（FE）。默认 2 FE/SV，与上传同价。

#### `uploadTicksPerSv`（浮点数，默认 `0.1`，范围 0–100）
**上传**时每 1 点 SV 增加的耗时（tick）。默认 0.1 → 1000 SV 的物品需 `20 + 100 = 120` tick（6 秒）；单件最低 20 tick。

#### `downloadTicksPerSv`（浮点数，默认 `0.1`，范围 0–100）
**下载**时每 1 点 SV 增加的耗时（tick）。默认 0.1 → 1000 SV 需 `20 + 100 = 120` tick（6 秒），与上传相同。

#### `minTicks`（整数，默认 `20`，范围 1–10000）
单次交换操作的最短耗时（tick）。1 秒 = 20 tick。

#### `maxTicks`（整数，默认 `600`，范围 1–10000）
单次交换操作的最长耗时（tick）。默认 600 = 30 秒——超高价值物品（如 1M SV 物品）会被钳制在 30 秒以内。代码里实际取 `max(minTicks, maxTicks)`，配置成小于 minTicks 会被自动提升。

> 注意：以上耗时/能量均为**基础值**，最终还会受机器内 SPEED / ENERGY 升级影响（Mekanism 惯例：SPEED 减耗时+加能量、ENERGY 减能量+加耗时）。便携式交换机（物品版）**不走本段配置**：上传/下载即时完成、零能量零耗时。

### 1.3 `[machine.restorationSwitch]` 复位机（概率修耐久）

> 计算方式（`RestorationSwitchTile.recalculateRequirements`）：
> `svCost = SV值 > 0 ? ceil(SV值 ÷ 100) : fallbackSvCost`（每修复 1 点耐久）
> `总能量(FE) = svCost × energyPerSv`
> `总耗时(ticks) = clamp(20 + svCost × ticksPerSv, minTicks, maxTicks)`
> 每 tick 按比例扣 SV 与能量（`proportional()`，数学上不多扣）；能量不足自动暂停、恢复后续跑。
> 修复判定：每轮结束时 `成功率% = ceil(剩余耐久 ÷ 最大耐久 × 100) + failBoost`（上限 100）；成功 −1 耐久并清零 failBoost，失败 +1 failBoost（失败**不退**已扣成本，属于设计取舍）。

#### `fallbackSvCost`（长整数，默认 `616`，范围 1–2^63−1）
**没有已知 SV 值**的物品每修复 1 点耐久的 SV 成本。默认 616。有 SV 值的物品用 `ceil(SV÷100)`，通常远低于此值。

#### `energyPerSv`（长整数，默认 `2`，范围 0–2^63−1）
每 1 点 SV 成本对应的总能量消耗（FE）。默认 2 FE/SV → 修 1 点耐久（未知物品）耗 `616 × 2 = 1232` FE。

#### `ticksPerSv`（浮点数，默认 `0.1`，范围 0–100）
每 1 点 SV 成本增加的耗时（tick）。默认 0.1 → 未知物品单点 `20 + floor(616×0.1) = 81` tick。

#### `minTicks`（整数，默认 `20`，范围 1–10000）
单次修复尝试的最短耗时（tick）。

#### `maxTicks`（整数，默认 `600`，范围 1–10000）
单次修复尝试的最长耗时（tick）；小于 minTicks 时自动提升为 minTicks。复位机也吃 SPEED / ENERGY 升级。

### 1.4 `[flight]` 飞行控制（DABR 集成，共 23 项）

> 激活条件（`MeksFlightClient`）：`enabled=true` + 玩家鞘翅飞行（`isFallFlying()`）+ 按 `activationMode` 判定装备 + 未潜水（`disableWhenSubmerged`）。**默认关闭**，**无能量消耗**；**没有游戏内开关按键**，开关只读配置。服务端 `enabled=false` 时服务端不接收/不转发 roll 同步（详见 `docs/flight-controls.md`）。

#### 开关与激活范围

| 键 | 类型/默认 | 范围 | 说明 |
|---|---|---|---|
| `enabled` | 布尔 `false` | — | 飞行控制总开关（**默认关闭**）。也门控服务端 roll 同步中继（`MeksPayloads` / `ServerEntityMixin`）。 |
| `activationMode` | 枚举 `"ELYTRA_UNIT"` | `ELYTRA_UNIT` / `GLOBAL` | `ELYTRA_UNIT`：仅 MekaSuit 胸甲（已安装启用鞘翅单元）的鞘翅飞行玩家；`GLOBAL`：任意鞘翅飞行玩家（含原版鞘翅）。 |

#### 鼠标模式

| 键 | 类型/默认 | 范围 | 说明 |
|---|---|---|---|
| `switchRollAndYaw` | 布尔 `false` | — | 交换横滚与偏航轴。`false` 时鼠标 X → 横滚、A/D → 偏航；`true` 时鼠标 X → 偏航、A/D → 横滚。 |
| `invertPitch` | 布尔 `false` | — | 反转俯仰轴（鼠标 Y）。注意与原版“反转鼠标”设置是**独立**的，两者叠加。 |
| `momentumBasedMouse` | 布尔 `false` | — | 动量式鼠标：移动鼠标给相机施加角动量而非直接控制角度（`MouseMixin`）。 |
| `momentumMouseDeadzone` | 浮点 `0.2` | 0–1 | 动量模式下鼠标的死区（单位：每秒格子数/块），低于该速度的鼠标移动不产生动量。 |

#### 限制条件

| 键 | 类型/默认 | 范围 | 说明 |
|---|---|---|---|
| `disableWhenSubmerged` | 布尔 `true` | — | 玩家浸没在水中时临时禁用飞行控制（横滚自动回正、交还原版操控）。 |

#### 灵敏度（鼠标控制强度）

| 键 | 类型/默认 | 范围 | 说明 |
|---|---|---|---|
| `sensitivityPitch` | 浮点 `1.0` | 0–100 | 俯仰轴灵敏度倍率。 |
| `sensitivityYaw` | 浮点 `0.4` | 0–100 | 偏航轴灵敏度倍率。默认低于其他轴。 |
| `sensitivityRoll` | 浮点 `1.0` | 0–100 | 横滚轴灵敏度倍率。 |

#### 平滑（时间滤波强度，越大越迟钝）

| 键 | 类型/默认 | 范围 | 说明 |
|---|---|---|---|
| `smoothingPitch` | 浮点 `1.0` | 0–100 | 俯仰平滑。设为 0 关闭该轴平滑。 |
| `smoothingYaw` | 浮点 `2.5` | 0–100 | 偏航平滑（默认最强，转向有惯性感）。 |
| `smoothingRoll` | 浮点 `1.0` | 0–100 | 横滚平滑。 |

#### 倾斜补偿（banking）

| 键 | 类型/默认 | 范围 | 说明 |
|---|---|---|---|
| `enableBanking` | 布尔 `true` | — | 转弯时相机随横滚倾斜（banking 修饰器总开关）。 |
| `bankingStrength` | 浮点 `20.0` | 0–100 | 倾斜强度。公式中以 `$banking_strength` 变量注入，默认公式内还有 `×10` 系数，实际位移 ≈ 横滚 × 强度 × 10。 |

#### 控制面效能（模拟真实飞行气动）

| 键 | 类型/默认 | 范围 | 说明 |
|---|---|---|---|
| `simulateControlSurfaceEfficacy` | 布尔 `false` | — | 模拟控制面效能：根据速度矢量与视线方向的夹角缩放各轴输入（默认关闭——沿用 DABR 的关闭默认，避免低速时操控失灵）。 |
| `elevatorEfficacyFormula` | 字符串（公式） | 见 §2 | 升降舵（俯仰）效能公式。 |
| `aileronEfficacyFormula` | 字符串（公式） | 见 §2 | 副翼（横滚）效能公式。 |
| `rudderEfficacyFormula` | 字符串（公式） | 见 §2 | 方向舵（偏航）效能公式。 |

#### 自动回正

| 键 | 类型/默认 | 范围 | 说明 |
|---|---|---|---|
| `automaticRighting` | 布尔 `false` | — | 自动把相机回正到水平飞行（回正算法：小角度时 `rollDelta = -roll³/3 + roll`，大角度不干预）。 |
| `rightingStrength` | 浮点 `50.0` | 0–100 | 回正强度，实际系数为 `10 × rightingStrength`。 |

#### 视角偏移公式（banking X/Y）

| 键 | 类型/默认 | 说明 |
|---|---|---|
| `bankingXFormula` | `"sin($roll * TO_RAD) * cos($pitch * TO_RAD) * 10 * $banking_strength"` | 倾斜时相机在 X 轴的偏移量公式。 |
| `bankingYFormula` | `"(-1 + cos($roll * TO_RAD)) * cos($pitch * TO_RAD) * 10 * $banking_strength"` | 倾斜时相机在 Y 轴的偏移量公式（随横滚抬高/压低视点）。 |

公式每渲染帧求值一次，结果乘以帧间隔作为相机偏移；NaN 结果会被兜底为 0。

### 1.5 `[soulOut]` 灵魂出窍（纯客户端，并入通用配置）

> 功能前提：配置 `enabled=true` + 佩戴 MekaSuit 头盔 + 按 **F4** 开关（`soul/SoulOutKeybinds`）。服务端不可见，纯客户端自嗨；`MekaSuit` 头盔能量耗尽或受伤（`disableOnDamage`）自动回体。**虽然行为在客户端，但本段配置同样遵循服务端值**（原 `meks-client.toml` 已并入本文件）。

| 键 | 类型/默认 | 范围 | 说明 |
|---|---|---|---|
| `enabled` | 布尔 `true` | — | 灵魂出窍功能总开关（仍可在游戏内按 F4 切换状态，但功能被禁用时无反应）。 |
| `baseCostPerTick` | 长整数 `500` | 1–2^63−1 | 出窍后头盔**每 tick 基础能量消耗**（J/tick，500 J = 每秒 1 万 J）。**创造模式不扣能量**。 |
| `costDoublingSeconds` | 整数 `45` | 0–3600 | 单 tick 消耗翻倍周期（秒）：`消耗 = base × 2^(出窍秒数/45)`，指数增长。设为 `0` 则**关闭增长**，恒定基础消耗。 |
| `horizontalSpeed` | 浮点 `1.0` | 0.01–10 | 灵魂相机水平移动速度倍率。 |
| `verticalSpeed` | 浮点 `1.0` | 0.01–10 | 灵魂相机垂直移动速度倍率。 |
| `freezeBody` | 布尔 `true` | — | 出窍时冻结本体（不可移动）。纯客户端表现，部分服务器可能把本体拉回原位（已知限制）。 |
| `showBody` | 布尔 `true` | — | 出窍时在**原位渲染本体**（`EntityRenderDispatcherMixin`）。关掉则本体视觉上消失。 |
| `disableOnDamage` | 布尔 `true` | — | 生存/冒险模式下本体受伤时自动回体（`LivingEntityMixin`）。 |

---

## 2. 公式语法参考（`[flight]` 5 个公式字符串）

自定义公式引擎（`flight/math/ExpressionParser`，无第三方依赖）：

- **四则运算**：`+ - * /`、一元负号、括号。
- **变量**（求值时注入）：

| 变量 | 含义 |
|---|---|
| `$roll` / `$pitch` / `$yaw` | 当前姿态角（度） |
| `$velocity_x` / `$velocity_y` / `$velocity_z` | 玩家速度矢量分量（米/秒） |
| `$velocity_length` | 速度矢量长度 |
| `$look_x` / `$look_y` / `$look_z` | 视线方向单位矢量分量 |
| `$banking_strength` | 配置值 `bankingStrength`（仅 banking 公式注入） |

- **常量**：`PI`、`E`、`TO_RAD`（π/180）、`TO_DEG`（180/π）。
- **单参函数**：`sqrt` `sin` `cos` `tan` `asin` `acos` `atan` `abs` `ceil` `floor` `log`（自然对数）`round` `randint`（0 ≤ 随机数 < 参数）。
- **双参函数**：`min` `max`。
- 三个效能公式默认值相同：`$velocity_x*$look_x + $velocity_y*$look_y + $velocity_z*$look_z`（速度在视线方向上的投影；直线飞行 ≈1，急转时衰减）——可作为缩放系数理解。

---

## 3. 调参速查

| 想实现的效果 | 改哪里 |
|---|---|
| 开启苦力怕不炸方块 | `[server]` `creeperNoBlockDamage = true` |
| 开启 MekaSuit 附魔台附魔（固定 30 级） | `[server]` `mekaSuitEnchantment = true` |
| 开启飞行控制 | `[flight]` `enabled = true` |
| 让任意鞘翅可用飞行控制 | `[flight]` `activationMode = "GLOBAL"` |
| 飞行手感调校 | `[flight]` 灵敏度 × 平滑：`sensitivity*`（响应快慢）+ `smoothing*`（惯性感） |
| 交换更省电 / 更贵 | `[machine.exchangeSwitch]` `uploadFePerSv` / `downloadFePerSv` |
| 交换更快 / 更慢 | `[machine.exchangeSwitch]` `*TicksPerSv`、`minTicks`、`maxTicks` |
| 控制交换速度上限 | `[machine.exchangeSwitch]` `maxTicks`（600 = 30 秒） |
| 修未知物品降价 / 涨价 | `[machine.restorationSwitch]` `fallbackSvCost` |
| 修已知物品更便宜 | 提高物品 SV 值（`data/meks/sv/*.json`）或调 `[machine.restorationSwitch]` `energyPerSv`/`ticksPerSv` |
| 关闭灵魂能量增长 | `[soulOut]` `costDoublingSeconds = 0` |

> 本模组另有两类"配置"不在 toml 里：**SV 预设表**（`data/meks/sv/*.json`，见 `docs/sv-values.md`）与**命令**（`/mek sv`、`/mek knowledge`，见 `PROJECT_OVERVIEW.md` §8）。