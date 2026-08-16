# SV 预设表说明

`src/main/resources/data/meks/sv/` 下的 JSON 都是 SV 预设表。核心的 `emc_preset.json` 内容完整来自 ProjectE 的 EMC 值；其余预设为可选模组提供直接可用的 SV。

## 来源

- ProjectE：`1.21.1-PE1.1.0`
- Mekanism：`1.21.1-10.7.19.85`
- 生成方式：在同时安装 ProjectE 与 Mekanism 的 NeoForge 1.21.1 服务端中，把 `config/ProjectE/mapping.toml` 的 `usePregenerated` 设为 `true`，进入世界后执行 `/reload`，ProjectE 会写出 `config/ProjectE/pregenerated_emc.json`。
- 过滤规则：只保留 `minecraft:*` 与 `mekanism:*` 的基础物品条目；带 `data`（NBT 组件）的条目会被跳过，因为本模组的 SV 只按物品 ID 记录。
- 共 1415 条：1151 个 Minecraft 物品、264 个 Mekanism 物品。

## Mekanism 的 EMC 从哪来

- ProjectE 自带的 `data/projecte/pe_custom_conversions/*.json`（如 `c:ingots/osmium` 等标签换算）会把基础 EMC 传播到 Mekanism 的标签物品。
- Mekanism 内置 ProjectE 集成（`mekanism.common.integration.projecte.mappers.*`）把自己的机器配方（粉碎、富集、冶炼、冶金灌注、化学转化等）注册成 EMC 换算。
- Mekanism 自带 `pe_custom_conversions/defaults.json`，给盐、氟石、HDPE 颗粒等设置基础值。
- ProjectE 在世界加载时用图算法算出最终 EMC 并写入预生成文件。

## 重新生成

1. 把 ProjectE 与 Mekanism 放入运行目录，启动一次服务端生成配置。
2. 编辑 `config/ProjectE/mapping.toml`，设置 `usePregenerated = true`。
3. 启动服务端并执行 `/reload`，等待 `pregenerated_emc.json` 生成。
4. 用 `design/generate_sv_preset.py` 重新生成 `emc_preset.json`。

## 运行时行为

`MeksValues` 启动时依次读取 `data/meks/sv/` 下的所有预设文件作为基础 SV；不在表中的物品若所有合成原料都有 SV，则按 `原料 SV 之和 ÷ 输出数量` 自动推导。

## 附属模组预设

- `cataclysm_preset.json`：L_Ender's Cataclysm 1.21.1-3.32，共 151 条。生成器：`design/cataclysm_sv/generate_cataclysm_sv_xlsx.py`。
- `twilightforest_preset.json`：The Twilight Forest 1.21.1（官方 1.21.1 分支），共 151 条。生成器：`design/mod_sv_pricing/generate_mod_xlsx.py` + `design/twilightforest_sv/price.json`。
- `iceandfire_preset.json`：Ice and Fire Community Edition 2.0（1.21.1），共 381 条。生成器：`design/mod_sv_pricing/generate_mod_xlsx.py` + `design/iceandfire_sv/price.json`。

三个附属预设都遵守同一规则：可重复合成品不高于原料成本；锻造、龙锻、自定义配方等运行时不会自动推导的产物显式写值；刷怪蛋、生物桶、创造专属物品、WIP 与纯装饰物品不定价。

## 工具提示

客户端悬停物品时，若该物品有 SV 值，工具提示末尾会像 ProjectE 的 EMC 一样显示一行金色 `SV：值`；没有 SV 值的物品不显示。
