# MekaSuit 飞行控制说明

本文档介绍 Mekanism-Switch 新增的 MekaSuit 飞行控制功能：启用条件、操作方式、能量消耗、实现原理与已知限制。

## 1. 功能简介

MekaSuit 飞行控制为玩家在“穿着 MekaSuit 胸甲 + 鞘翅飞行”状态下提供三轴飞行控制：

- 俯仰（Pitch）：上下看，控制爬升/俯冲。
- 偏航（Yaw）：左右转向。
- 横滚（Roll）：围绕飞行方向旋转视角，可做出翻滚、盘旋等机动。

该功能为模组原创实现，只参考了同类模组的交互思路，没有使用或嵌入任何外部模组代码，项目保持 MIT 许可。功能完全运行在客户端，不修改服务端飞行物理。

## 2. 启用条件

同时满足以下条件时飞行控制激活：

1. 配置项 `mekaSuitFlightControls` 为 `true`（默认开启）。
2. 玩家身穿 MekaSuit 胸甲（`ItemMekaSuitArmor`）。
3. 玩家处于鞘翅飞行状态，例如使用 MekaSuit 喷射背包的鞘翅模式。
4. MekaSuit 胸甲能量容器中有足够能量。

任一条件不满足（例如脱下胸甲、停止飞行、能量耗尽）时，自动回到原版鞘翅操控，横滚角度也会平滑回正。

## 3. 操作方式

| 输入 | 效果 |
| --- | --- |
| 鼠标 X（左右移动） | 横滚 |
| 鼠标 Y（上下移动） | 俯仰（沿用原版灵敏度和“反转鼠标”设置） |
| A / D | 偏航转向 |

横滚使用固定灵敏度，与原版鼠标灵敏度设置解耦：原始鼠标位移每单位约 0.15 度（常量 `ROLL_SENSITIVITY = 1.0`，与参考实现 Do a Barrel Roll 的默认横滚灵敏度一致），A/D 偏航速度约为每帧 1.8 度（常量 `KEY_YAW_STEP`）。横滚平滑系数为 0.45/游戏刻，停止飞行后的自动回正系数为 0.22/游戏刻。

## 4. 能量消耗

- 飞行控制激活时，每个游戏刻从 MekaSuit 胸甲的能量容器抽取 `100 J`（常量 `ENERGY_PER_TICK`），即约 2000 J/秒。
- 能量不足一个 tick 的消耗量时，本次 tick 不扣能量，并立即退回原版操控。
- 消耗使用 Mekanism 的 `IStrictEnergyHandler.extractEnergy`（`Action.EXECUTE`），先通过 `Action.SIMULATE` 预检。

## 5. 配置文件

配置文件路径：`config/meks-common.toml`。

```toml
mekaSuitFlightControls = true
```

- `true`：启用 MekaSuit 飞行控制。
- `false`：禁用，鞘翅飞行完全保持原版操作。

配置修改后需要重启游戏（NeoForge 通用配置在游戏启动时加载）。

## 6. 实现原理

功能由三个客户端 Mixin 和一个状态接口组成，全部位于 `src/main/java/com/mikufan/meks/` 下。

### LocalPlayerMixin

注入 `net.minecraft.client.player.LocalPlayer#tick` 的 HEAD：

- 维护玩家的横滚状态：`prevRoll`、`roll`、`targetRoll`、`rolling`。
- 每个游戏刻结算启用条件，并在激活时抽取胸甲能量。
- 激活时让 `roll` 向 `targetRoll` 平滑靠拢；未激活时让 `roll` 逐渐回正到 0。

### MouseHandlerMixin

重定向 `net.minecraft.client.MouseHandler#turnPlayer` 中对 `LocalPlayer.turn(DD)V` 的调用：

- 飞行控制未激活时，直接调用原版 `turn`，行为不变。
- 激活时：
  - 鼠标 X 先还原为原始位移（除以原版 8 × (0.6 × 灵敏度 + 0.2)³ 的缩放），再按 0.15 系数转为 `targetRoll` 增量，横滚速度不受原版灵敏度影响。
  - 鼠标 Y 继续调用 `turn(0, deltaPitch)`，保持原版俯仰与反转设置。
  - A/D 按键调用 `turn(yaw, 0)` 完成偏航转向。

### CameraMixin

注入 `net.minecraft.client.Camera#setup` 的 TAIL：

- 使用 `Mth.lerp(partialTick, prevRoll, roll)` 做渲染插值。
- 对相机四元数调用 `rotateLocalZ` 施加横滚；第三人称反转视角（面朝玩家的视角）会取反，保证滚转方向一致。

### 状态接口

`MeksRollState` 是挂在本地玩家上的客户端横滚状态接口，`MeksFlightController` 负责全部结算逻辑，常量集中在该类顶部，便于后续调参。

## 7. 设计取舍与限制

- 纯客户端功能：不修改服务端物理，服务器无需安装本模组，也不会收到任何相关数据包。
- 其他玩家看不到你的模型横滚（没有做实体渲染同步），多人游玩时只有本地视角有滚转效果。
- 第三人称视角下，世界会随镜头滚转，但玩家模型本身仍保持直立。
- 横滚只改变相机朝向，不改变鞘翅飞行轨迹与速度；需要转弯时请配合 A/D。
- 横滚角度不设上限，可以连续翻滚。

## 8. 与 Do a Barrel Roll 的关系

本功能在行为上参考了 Do a Barrel Roll（GPL-3.0）的三轴飞行交互思路，但实现完全原创：

- 未复制、移植或链接其源码、类或资源。
- 不依赖 Forgified Fabric API、YACL 等额外库。
- 项目继续以 MIT 许可发布。

若同时安装 Do a Barrel Roll，两者都会尝试接管飞行输入，可能出现操作冲突；建议二选一，或把其中一个的飞行功能关闭。

## 9. 常见问题

**飞行控制没有生效**

按顺序检查：

1. `config/meks-common.toml` 中 `mekaSuitFlightControls = true`。
2. 是否穿着 MekaSuit 胸甲（不是普通鞘翅）。
3. 是否真的处于鞘翅飞行状态。
4. 胸甲是否有能量（查看 HUD 能量显示）。

**能量消耗太快**

把 `MeksFlightController.ENERGY_PER_TICK` 调小（例如 50），重新编译即可。

**横滚方向相反**

调整 `CameraMixin` 中 `rotateLocalZ` 的符号，或修改 `MeksFlightController.ROLL_SENSITIVITY` 的正负。

**和 Do a Barrel Roll 一起装会怎样**

两个模组都会拦截鼠标转向输入，横滚和偏航会互相叠加，体验异常；关闭其中一个即可。

## 10. 开发信息

相关文件：

- `src/main/java/com/mikufan/meks/flight/MeksFlightController.java`
- `src/main/java/com/mikufan/meks/flight/MeksRollState.java`
- `src/main/java/com/mikufan/meks/mixin/client/LocalPlayerMixin.java`
- `src/main/java/com/mikufan/meks/mixin/client/MouseHandlerMixin.java`
- `src/main/java/com/mikufan/meks/mixin/client/CameraMixin.java`
- `src/main/resources/meks.mixins.json`（`client` 段注册）

编译：

```bash
gradlew build
```

功能自 `0.2.0` 起提供，手感参数（灵敏度、偏航速度、平滑/回正系数、能量消耗）仍在预览调整阶段。
