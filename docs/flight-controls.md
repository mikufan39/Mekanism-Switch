# MekaSuit 飞行控制说明

本文档介绍 Mekanism-Switch 的 MekaSuit 飞行控制功能：启用条件、操作方式、能量消耗、实现原理与已知限制。

## 1. 功能简介

MekaSuit 飞行控制在“穿着 MekaSuit 胸甲 + 鞘翅单元”滑翔时为玩家提供完整的三轴飞行控制：

- 俯仰（Pitch）：上下看，控制爬升/俯冲。
- 偏航（Yaw）：左右转向。
- 横滚（Roll）：围绕飞行方向旋转视角，可做出翻滚、盘旋等机动。
- 附带：方向舵平滑、倾斜补偿（banking）、自动回正、可选动量鼠标与人工地平线 HUD。

飞行实现代码集成自 [Do a Barrel Roll](https://github.com/enjarai/do-a-barrel-roll)（tag `3.7.3+1.21-neoforge`，GPL-3.0），按本模组需要做了裁剪与改造，详见第 6 节。

## 2. 启用条件

同时满足以下条件时飞行控制激活：

1. 配置项 `enabled` 为 `true`（默认开启）。
2. 玩家身穿 MekaSuit 胸甲，且已安装并启用鞘翅单元（进入滑翔由 Mekanism 判定）。
3. 玩家处于鞘翅飞行状态（`isFallFlying()`）。
4. MekaSuit 胸甲能量容器中有足够能量（见第 4 节）。

任一条件不满足（例如脱下胸甲、停止飞行、能量耗尽）时，横滚角度会自动回正。默认模式下入水也会临时禁用飞行控制（`disableWhenSubmerged`）。

## 3. 操作方式

| 输入 | 效果 |
| --- | --- |
| 鼠标 X（左右移动） | 横滚（可与偏航互换，见配置 `switchRollAndYaw`） |
| 鼠标 Y（上下移动） | 俯仰（沿用原版灵敏度与“反转鼠标”设置） |
| A / D | 偏航转向 |
| I（默认，可在控制设置中改键） | 切换飞行控制开关 |

默认按键与移动键（A/D）不冲突：NeoForge 的 `KeyMappingLookup` 会把按键同时派发给原版移动键与飞行键，飞行键只在滑翔时被读取。

## 4. 能量消耗

- 飞行控制激活时，每个游戏刻从 MekaSuit 胸甲的能量容器抽取 `flightEnergyPerTick` 配置值（默认 100 J），即默认约 2000 J/秒。
- 能量不足一个 tick 的消耗量时，本次 tick 不扣能量，并立即退回原版操控。
- 消耗使用 Mekanism 的 `IEnergyContainer.extract`（`Action.EXECUTE`），先通过 `Action.SIMULATE` 预检。

## 5. 配置文件

配置文件路径：`config/Mekanism/meks-flight-client.toml`。

常用配置：

```toml
[flight]
enabled = true
flightEnergyPerTick = 100
switchRollAndYaw = false
invertPitch = false
momentumBasedMouse = false
momentumMouseDeadzone = 0.2
showMomentumWidget = true
disableWhenSubmerged = true
sensitivityPitch = 1.0
sensitivityYaw = 0.4
sensitivityRoll = 1.0
smoothingPitch = 1.0
smoothingYaw = 2.5
smoothingRoll = 1.0
enableBanking = true
bankingStrength = 20.0
simulateControlSurfaceEfficacy = false
automaticRighting = false
rightingStrength = 50.0
showHorizon = false
bankingXFormula = "sin($roll * TO_RAD) * cos($pitch * TO_RAD) * 10 * $banking_strength"
bankingYFormula = "(-1 + cos($roll * TO_RAD)) * cos($pitch * TO_RAD) * 10 * $banking_strength"
elevatorEfficacyFormula = "$velocity_x * $look_x + $velocity_y * $look_y + $velocity_z * $look_z"
aileronEfficacyFormula = "$velocity_x * $look_x + $velocity_y * $look_y + $velocity_z * $look_z"
rudderEfficacyFormula = "$velocity_x * $look_x + $velocity_y * $look_y + $velocity_z * $look_z"
```

- `enabled`：是否启用飞行控制；也可在游戏中按 I 切换。
- `flightEnergyPerTick`：飞行控制激活时每游戏刻消耗的胸甲能量（J/t）。
- 表达式支持 `$pitch/$yaw/$roll/$velocity_x/...` 等变量与 `sin/cos/sqrt/...` 函数，语法与 Do a Barrel Roll 一致。
- 客户端配置在游戏启动时加载，修改后需要重启游戏。

## 6. 实现原理与代码来源

功能由以下部分组成（均位于 `com.mikufan.meks` 下）：

- **修饰器管线**（`flight/api/event/*`、`flight/RotationModifiers.java`）：旋转增量依次经过 EARLY/LATE 两组修饰器——按键控制、灵敏度、方向舵效能表达式、平滑、倾斜补偿与自动回正。
- **旋转数学**（`flight/api/rotation/*`、`flight/math/*`）：`RotationInstant` 与表达式解析引擎。
- **roll 状态**（`flight/api/RollEntity.java` + `mixin/flight/roll/entity/*`）：挂在 Entity/LivingEntity/Player 上的滚转状态，客户端每 tick 结算。
- **输入接线**（`mixin/flight/client/roll/MouseMixin.java`、`entity/ClientPlayerEntityMixin.java`）：滚转时把鼠标增量改道进修饰器管线；`ClientPlayerEntityMixin` 覆写 `changeElytraLook` 完成真正的三轴旋转数学。
- **渲染**（`mixin/flight/client/roll/CameraMixin.java`、`PlayerEntityRendererMixin.java`）：相机滚转与第三人称模型滚转；F3 面板显示 roll 值。
- **键位**（`flight/MeksFlightKeybinds.java`、`mixin/flight/client/key/*`）：NeoForge `KeyMapping` + 键位上下文（`InputContext`）。
- **网络**（`flight/net/FlightNetworking.java` + `MeksPayloads` 的 `RollSyncPayload/RollSyncS2CPayload`）：本地玩家的滚转状态上报服务器并转发给追踪者，**其他玩家可以看到你的模型滚转**（与旧实现不同）。
- **配置**（`flight/config/MeksFlightConfig.java`）：NeoForge `ModConfigSpec`。

代码由 Do a Barrel Roll（tag `3.7.3+1.21-neoforge`，GPL-3.0）的飞行实现移植改造而来。集成时做了以下裁剪：删除服务端配置同步/握手与权限系统（改为纯客户端配置）、删除 thrust（火箭加速）功能、删除 YACL 配置界面与兼容层（ModMenu/Controlify/Cicada）、删除激活模式（固定为 VANILLA）、删除动能伤害与彩蛋；所有 Fabric API 依赖改写为 NeoForge 原生 API（网络、键位、事件）；Yarn 映射全量重映射为 Mojang 官方映射。方法前缀改为 `meksFlight$`，与 DABR 本体共存时不冲突。

## 7. 已知限制

- 需要服务器安装本模组才能看到其他玩家的滚转；仅客户端安装时功能退化为纯本地视角效果（与本模组旧实现相同）。
- 若同时安装 Do a Barrel Roll 本体，两者的飞行控制会同时接管输入，建议二选一。
- 灵魂出窍激活期间身体被冻结，飞行控制不会响应（与灵魂出窍的设计一致）。
- thrust（火箭推进加速）功能未集成，使用原版烟花火箭即可。

## 8. 开发信息

相关文件：

- `src/main/java/com/mikufan/meks/flight/`（全部飞行代码）
- `src/main/java/com/mikufan/meks/mixin/flight/`（飞行 mixin）
- `src/main/java/com/mikufan/meks/MeksPayloads.java`（roll 同步网络包）
- `src/main/resources/meks.mixins.json`（`mixins`/`client` 段注册）
- `design/dabr-integration.md`（集成设计文档）

编译：

```bash
gradlew build
```

功能自 0.2.0 起提供；0.3 起改用 Do a Barrel Roll 的飞行实现。
