# Mekanism-Switch

Mekanism-Switch 是 Minecraft 1.21.1 / NeoForge 的 Mekanism 附属模组，新增“交换机”与“复位机”两台 Mekanism 风格机器，并附带 MekaSuit 增强功能。

当前版本 SV 值、配方与平衡性仍在实验阶段。

## 特性

- 交换机：上传物品获得 SV，下载已解锁知识的物品，或遗忘物品释放知识。
- 复位机：消耗 SV 与电力，按概率逐点修复工具/武器/盔甲耐久，支持自动化与升级。
- SV 值系统：内置基础物品价值，并根据合成配方自动推导衍生物品价值。
- 知识列表：带搜索与滚动的物品知识面板，支持单件或整组拖拽操作。
- 通道升级：潜行右键交换机安装后增加一个上传/下载通道。
- MekaSuit 附魔：附魔台第三格可对 MekaSuit 附魔保护 V，消耗 30 级经验。
- MekaSuit 飞行控制：穿着 MekaSuit 胸甲鞘翅飞行时启用三轴飞行控制（俯仰/偏航/横滚），消耗胸甲能量。详见 [飞行控制说明](docs/flight-controls.md)。
- 灵魂出窍：穿戴 MekaSuit 头盔时按 F4 让灵魂离开身体，相机可穿墙自由飞行；能量消耗随时间指数增长，受伤或能量耗尽自动回体。详见 [灵魂出窍说明](docs/soul-out.md)。
- 苦力怕保护：默认开启，苦力怕爆炸不破坏方块（实体伤害保留）。

## 依赖

- Minecraft 1.21.1
- NeoForge 21.1.248+
- Mekanism 1.21.1 10.7.19+（必需）

## 安装

1. 安装 NeoForge 1.21.1 并启动一次游戏。
2. 将 Mekanism 1.21.1 放入 `mods` 目录。
3. 将 `meks-0.2.0.jar` 放入 `mods` 目录。
4. 启动游戏，创意模式物品栏“Mekanism 交换”分类中可找到交换机与通道升级。

## 使用

- 交换机需要能量才能工作，放入物品后自动上传并增加 SV。
- 打开交换机界面，从知识列表拖动物品到上传/下载格即可交换。
- 潜行右键交换机使用通道升级，可安装一次。
- 穿戴 MekaSuit 头盔后按 F4（可在控制设置中改键）切换灵魂出窍；WASD/空格/Shift 控制灵魂飞行。
- MekaSuit 胸甲进入鞘翅飞行后，鼠标 X 控制横滚，鼠标 Y 控制俯仰，A/D 控制偏航；能量耗尽或未穿胸甲时自动回到原版操控。
- 附魔台与 MekaSuit 相关的配置见 `config/meks-common.toml`。

## 配置

- `mekaSuitEnchantment`：是否启用 MekaSuit 附魔台功能，默认 `true`。
- `mekaSuitFlightControls`：是否启用 MekaSuit 飞行控制，默认 `true`。
- `creeperNoBlockDamage`：是否禁止苦力怕爆炸破坏方块，默认 `true`。
- `soulOut`：灵魂出窍配置菜单（`enabled`、`baseCostPerTick`、`costDoublingSeconds`、`horizontalSpeed`、`verticalSpeed`、`freezeBody`、`showBody`、`disableOnDamage`）。

## 开发者命令

- `/mek dev`：仅限权限等级 2 的玩家，设置 SV 为 9999999999 并解锁全部知识。

## 已知问题

- 预览版 SV 数值与配方未最终平衡。
- 部分物品可能因配方结构无法推导出价值。
- 飞行控制仅影响本地视角：其他玩家看不到你的模型滚转，第三人称视角下玩家模型本身也不随镜头滚转。
- 灵魂出窍为纯客户端功能：其他玩家看不到灵魂；`freezeBody` 在部分服务器上可能被服务端位置校正拉回。

## 许可

MIT License。作者：Mikufan。
