# Mekanism-Switch

Mekanism-Switch 是 Minecraft 1.21.1 / NeoForge 的 Mekanism 附属模组，新增一台 Mekanism 风格的“交换机”（Exchange Switch）机器。

当前为首个预览版，SV 值、配方与平衡性仍在实验阶段。

## 特性

- 交换机：上传物品获得 SV，下载已解锁知识的物品，或遗忘物品释放知识。
- SV 值系统：内置基础物品价值，并根据合成配方自动推导衍生物品价值。
- 知识列表：带搜索与滚动的物品知识面板，支持单件或整组拖拽操作。
- 通道升级：潜行右键交换机安装后增加一个上传/下载通道。
- MekaSuit 附魔：附魔台第三格可对 MekaSuit 附魔保护 V，消耗 60 级经验。
- 苦力怕保护：默认开启，苦力怕爆炸不破坏方块（实体伤害保留）。

## 依赖

- Minecraft 1.21.1
- NeoForge 21.1.248+
- Mekanism 1.21.1 10.7.19+（必需）

## 安装

1. 安装 NeoForge 1.21.1 并启动一次游戏。
2. 将 Mekanism 1.21.1 放入 `mods` 目录。
3. 将 `meks-0.1.0-preview.1.jar` 放入 `mods` 目录。
4. 启动游戏，创意模式物品栏“Mekanism 交换”分类中可找到交换机与通道升级。

## 使用

- 交换机需要能量才能工作，放入物品后自动上传并增加 SV。
- 打开交换机界面，从知识列表拖动物品到上传/下载格即可交换。
- 潜行右键交换机使用通道升级，可安装一次。
- 附魔台与 MekaSuit 相关的配置见 `config/meks-common.toml`。

## 配置

- `mekaSuitEnchantment`：是否启用 MekaSuit 附魔台功能，默认 `true`。
- `creeperNoBlockDamage`：是否禁止苦力怕爆炸破坏方块，默认 `true`。

## 开发者命令

- `/mek dev`：仅限权限等级 2 的玩家，设置 SV 为 9999999999 并解锁全部知识。

## 已知问题

- 预览版 SV 数值与配方未最终平衡。
- 部分物品可能因配方结构无法推导出价值。

## 许可

MIT License。作者：Mikufan。