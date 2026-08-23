# Mekanism-Switch

Mekanism-Switch 是 Minecraft 1.21.1 / NeoForge 的 Mekanism 附属模组，新增“交换机”与“复位机”两台 Mekanism 风格机器和“便携式交换机”手持设备，并附带 MekaSuit 增强功能。

当前版本 SV 值、配方与平衡性仍在实验阶段。

## 特性

- 交换机：上传物品获得 SV，下载已解锁知识的物品，或遗忘物品释放知识。
- **便携式交换机**：手持右键打开，无需电力即时上传/下载：点击背包物品立即入库，点击库条目直接扣除 SV 生成真实物品，右键下载成功后遗忘知识（防误操作）。详见 [便携式交换机说明](docs/portable-exchange-switch.md)。
- 复位机：消耗 SV 与电力，按概率逐点修复工具/武器/盔甲耐久，支持自动化与升级。
- SV 值系统：预设表完整采用 ProjectE EMC 值，并根据合成配方自动推导衍生物品价值。详见 [SV 值说明](docs/sv-values.md)。
- SV 值显示：悬停物品时，工具提示会像 ProjectE 的 EMC 一样显示 SV 值，仅显示有值的物品。
- 知识列表：带搜索与滚动的物品知识面板，支持单件或整组拖拽操作。
- 通道升级：潜行右键交换机安装后增加一个上传/下载通道。
- MekaSuit 附魔：附魔台第三格可对 MekaSuit 附魔保护 V，消耗 30 级经验。
- MekaSuit 飞行控制：鞘翅飞行时启用三轴飞行控制（俯仰/偏航/横滚），无能量消耗；默认仅 MekaSuit 胸甲+鞘翅单元激活，可配置为全局（任意鞘翅飞行玩家）激活。多人服务器可见横滚。基于 Do a Barrel Roll 的飞行代码集成实现。详见 [飞行控制说明](docs/flight-controls.md)。
- 灵魂出窍：穿戴 MekaSuit 头盔时按 F4 让灵魂离开身体，相机可穿墙自由飞行；能量消耗随时间指数增长，受伤或能量耗尽自动回体。详见 [灵魂出窍说明](docs/soul-out.md)。
- 苦力怕保护：默认开启，苦力怕爆炸不破坏方块（实体伤害保留）。

## 依赖

- Minecraft 1.21.1
- NeoForge 21.1.248+
- Mekanism 1.21.1 10.7.19+（必需）

## 安装

1. 安装 NeoForge 1.21.1 并启动一次游戏。
2. 将 Mekanism 1.21.1 放入 `mods` 目录。
3. 将 `meks-0.3.2.jar` 放入 `mods` 目录。
4. 启动游戏，创意模式物品栏“Mekanism 交换”分类中可找到交换机、通道升级与便携式交换机。

## 使用

- 交换机需要能量才能工作，放入物品后自动上传并增加 SV。
- 打开交换机界面，从知识列表拖动物品到上传/下载格即可交换。
- **便携式交换机**：手持右键打开。点击背包物品立即上传（Shift 上传整组）；点击库条目扣除 SV 生成真实物品：左键 1 个、Shift+左键整组、Shift+右键半组；右键下载 1 个成功后遗忘该知识。无需电力。
- 潜行右键交换机使用通道升级，可安装一次。
- 穿戴 MekaSuit 头盔后按 F4（可在控制设置中改键）切换灵魂出窍；WASD/空格/Shift 控制灵魂飞行。
- 鞘翅飞行时鼠标 X 控制横滚，鼠标 Y 控制俯仰，A/D 控制偏航；默认需穿 MekaSuit 胸甲（装鞘翅单元）才激活，配置 `activationMode = "GLOBAL"` 后任意鞘翅飞行可用。不满足条件时自动回到原版操控；开关仅由配置 `enabled` 控制（无游戏内切换键）。
- 附魔台与 MekaSuit 相关的配置见 `config/Mekanism/meks-common.toml`。

## 配置

配置文件位于 `config/Mekanism/`，**只有一个 `meks-common.toml`**（原 `meks-client.toml` 已并入）。配置为**服务端权威**：客户端连接服务器后使用服务端同步下来的值，本地无独立客户端配置；单人游戏即本地 config 目录。修改后需重启（服务端）生效。

```toml
[server]
mekaSuitEnchantment = false
creeperNoBlockDamage = false

[machine.exchangeSwitch]
uploadFePerSv = 2
downloadFePerSv = 2
uploadTicksPerSv = 0.1
downloadTicksPerSv = 0.1
minTicks = 20
maxTicks = 600

[machine.restorationSwitch]
fallbackSvCost = 616
energyPerSv = 2
ticksPerSv = 0.1
minTicks = 20
maxTicks = 600

[flight]
enabled = false
activationMode = "ELYTRA_UNIT"
switchRollAndYaw = false
invertPitch = false
momentumBasedMouse = false
momentumMouseDeadzone = 0.2
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
bankingXFormula = "sin($roll * TO_RAD) * cos($pitch * TO_RAD) * 10 * $banking_strength"
bankingYFormula = "(-1 + cos($roll * TO_RAD)) * cos($pitch * TO_RAD) * 10 * $banking_strength"
elevatorEfficacyFormula = "$velocity_x * $look_x + $velocity_y * $look_y + $velocity_z * $look_z"
aileronEfficacyFormula = "$velocity_x * $look_x + $velocity_y * $look_y + $velocity_z * $look_z"
rudderEfficacyFormula = "$velocity_x * $look_x + $velocity_y * $look_y + $velocity_z * $look_z"

[soulOut]
enabled = true
baseCostPerTick = 500
costDoublingSeconds = 45
horizontalSpeed = 1.0
verticalSpeed = 1.0
freezeBody = true
showBody = true
disableOnDamage = true
```

飞行控制参数在 `[flight]` 段（`enabled`、`activationMode`、灵敏度/平滑/公式等，见 [飞行控制说明](docs/flight-controls.md)）。

机器参数用于调整交换机与复位机的能耗、耗时和复位机对无 SV 值物品的兜底成本；灵魂出窍参数在 `[soulOut]` 段。所有配置项逐一详解见 [配置说明](docs/config.md)。

## 命令

- `/mek sv get [player]`：查询 SV；查询其他玩家需要权限等级 2。
- `/mek sv value <item>`：查询物品的 SV 值。
- `/mek sv set <amount> [player]`：设置 SV，权限等级 2。
- `/mek sv add <amount> [player]`：增减 SV（负数表示扣除），权限等级 2。
- `/mek knowledge list [player]`：列出已解锁知识；查询其他玩家需要权限等级 2。
- `/mek knowledge add <item> [player]`：解锁指定物品知识，权限等级 2。
- `/mek knowledge remove <item> [player]`：移除指定物品知识，权限等级 2。
- `/mek knowledge clear [player]`：清空知识，权限等级 2。
- `/mek knowledge unlock-all [player]`：解锁全部知识，权限等级 2。

省略 `[player]` 时作用于执行命令的玩家。`<item>` 使用物品 ID，例如 `minecraft:diamond`。

## 已知问题

- 预览版 SV 数值与配方未最终平衡。
- 部分物品可能因配方结构无法推导出价值。
- 飞行控制需要服务端与客户端都安装本模组才能让其他玩家看到你的滚转；服务端 `[flight] enabled=false` 时服务端停止转发横滚状态（他人不可见，本地效果仍由客户端配置决定）。
- 灵魂出窍为纯客户端功能：其他玩家看不到灵魂；`freezeBody` 在部分服务器上可能被服务端位置校正拉回。

## 许可

MIT License。作者：Mikufan。
