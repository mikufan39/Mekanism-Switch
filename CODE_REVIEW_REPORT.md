# Mekanism-Switch 完整代码审查报告

> 审查日期：2026-10-11 · 基线：`e2aea4f chore: release 0.3.2`（工作区干净，仅本报告与 AI_TOOLS.md 未提交）
> 方法：7 个模块由并行子代理深度审查（每个文件全文通读，关键结论经字节码/反编译交叉核实），Lead 对全部 high 级发现逐一亲验源码；`gradlew build` 验证编译通过。
> 范围：全部 92 个 Java 文件（约 7000 行）+ 全部资源 JSON + 构建配置。

---

## 0. 总览

| 严重度 | 数量 | 说明 |
|---|---|---|
| critical | 0 | 无崩溃级/存档级问题 |
| **high** | **3** | ✅ 全部已修复并验证 |
| medium | 11 | ✅ 9 已修（3 项部分修）/ **2 项开放**：SV 推导溢出、容器内容物策略 |
| low | 15 | ✅ 6 已修（批次 C：缓存/降频/冷却/死键/硬编码文案）/ 9 开放 |
| info | 20+ | ✅ 4 已修（死代码、注释、缓存）/ 其余为备注或待决策 |

> 状态更新（2026-10-11）：批次 A（5 项安全/健壮性）、批次 B（SV 耐久折旧 + 移除灵魂出窍能耗）、批次 C（性能缓存 4 项 + 顺手小项 4 项）均已实施，`gradlew build` BUILD SUCCESSFUL。C7 GPL 合规按用户决定**不做**，该项仍为发布前必办。

**三个 high（按修复优先级）：** ✅ 全部已于 2026-10-11 修复并编译验证（`gradlew compileJava` BUILD SUCCESSFUL）

1. **复位机"取消"无归属校验——任意玩家可远程冻结他人复位机**（[MeksPayloads.java:365](src/main/java/com/mikufan/meks/MeksPayloads.java#L365) + [RestorationSwitchTile.java:384](src/main/java/com/mikufan/meks/RestorationSwitchTile.java#L384)）✅ 已修复
   `handleCancelRepair` 只检查 tile 类型即调用 `tile.cancelRepair()`，而 `cancelRepair()` 本体不做任何 owner 比对（对比 `ExchangeSwitchTile.cancelOperation(player, slot)` 有完整校验）。恶意客户端遍历 BlockPos 发包即可把他人复位机置 `repairCancelled=true` 并清空统计，属纯 griefing 向量。**已修复：`cancelRepair(Player)` 增加 `security.getOwnerUUID()` 比对（与 `cancelOperation` 同模式），payload handler 传入玩家。**

2. **下载作业因 SV 不足中断后，机器会"吃回"玩家刚下载的物品**（[ExchangeSwitchTile.java:166](src/main/java/com/mikufan/meks/ExchangeSwitchTile.java#L166)）✅ 已修复
   `tickJob` 的 SV 不足分支直接 `resetJob` 不设 `suppressAutoStart`（对照 ：218/:342/:407 三条路径都设了），下一 tick `autoStartUpload`(:347) 见槽内有刚产出的物品即启动 UPLOAD，把玩家已付费下载的物品逐件吃回 SV。**已修复：SV 不足与异常重置（value≤0/pendingCount≤0）两个分支均补 `job.suppressAutoStart = true`，物品留在槽中待玩家取走（取走后标志自动复位）。**

3. **公式引擎深递归 `StackOverflowError` 不被捕获，坏公式可崩溃客户端**（[flight/math/ExpressionParser.java:25](src/main/java/com/mikufan/meks/flight/math/ExpressionParser.java#L25)）✅ 已修复
   递归下降解析每层括号约 3 帧，仅 `catch (RuntimeException)` 接不住 `Error`；且配置 getter 每次调用都 `new ExpressionParser`，实例级错误缓存形同虚设。数百层嵌套括号的配置串会在飞行激活后首个鼠标事件上抛未捕获的 StackOverflowError。**已修复（四层防护）：① 输入长度上限 1024；② 解析递归深度上限 256（超限抛 RuntimeException → 走既有降级为默认公式路径）；③ `randint` 参数钳制 ≥1，根除 eval 期 `IllegalArgumentException`；④ `RotationModifiers.banking`/`applyControlSurfaceEfficacy` 求值边界捕获 `Throwable` 并按默认值兜底（日志每次会话只打一次防刷屏）。批次 C 追加：公式 getter 改为按配置字符串缓存解析器（`MeksFlightConfig.FORMULA_CACHE`），实例级错误缓存从此真正生效。**

---

## 1. 核心数据 / 命令 / 主类（MekanismSwitch, Config, MeksValues, MeksAttachments, PlayerExchangeData, MeksCommands, KnowledgeEntry, ExchangeOperation, MeksLang, MekaSuitEnchantmentHandler, CreeperExplosionHandler）

| 严重度 | 位置 | 问题 |
|---|---|---|
| medium | [PlayerExchangeData.java:18](src/main/java/com/mikufan/meks/PlayerExchangeData.java#L18) | knowledge 集合反序列化不校验注册表：卸载某模组后存档残留 id，`/mek knowledge remove/list` 与 `KnowledgeEntry.getInternalStack` 对未注册 id 会 NPE。~~修复：反序列化后 `removeIf(!BuiltInRegistries.ITEM.containsKey)`。~~ ✅ 已按此修复（2026-10-11，CODEC 反序列化经 `filterRegisteredItems` 过滤）。 |
| medium | [MeksValues.java:152](src/main/java/com/mikufan/meks/MeksValues.java#L152) | SV 推导 `total += ingredientValue` 无溢出防护：预设允许单条 emc 达 Long.MAX，跨配方链回绕后写入错误派生值（下游 `value * FE_PER_SV` 溢出为负会把 energyPerTick 钳成 1 FE/t，近乎免电）。修复：`Math.addExact` 或派生值钳制保守上限（如 1e15）。 |
| low | [PlayerExchangeData.java:50](src/main/java/com/mikufan/meks/PlayerExchangeData.java#L50) | `addSv/consumeSv` 无溢出/负数防护，payload 侧 `value * count` 未用 `multiplyExact`——与命令层（已用 `addExact`）防护标准不一致。溢出为负时 `consumeSv` 反向加 SV（理论"印钱"向量）。 |
| low | [MeksValues.java:47](src/main/java/com/mikufan/meks/MeksValues.java#L47) | 静态表（HashMap/普通 boolean）被客户端渲染线程（tooltip）与服务端线程共同读写，退出存档清表瞬间存在数据竞争窗口。建议 synchronized 或 volatile 不可变 Map 整体替换。 |
| info | [KnowledgeEntry.java:19](src/main/java/com/mikufan/meks/KnowledgeEntry.java#L19) | `item()/getInternalStack()` 每次调用新建 ItemStack/HashedItem/UUID，GUI 悬停按帧分配。✅ 已修复（批次 C：静态 `STACK_CACHE`/`UUID_CACHE` computeIfAbsent，缓存对象只读共享）。 |

**做得好**：命令权限（操作他人全 level 2）、`svAdd` 的 `addExact`+负数拒绝、事件总线挂接/物理侧全部正确、MekaSuit 附魔槽位与费用逻辑核实无零级白嫖、苦力怕保护覆盖带电变体且时机正确、attachment Codec/STREAM_CODEC/copyHandler 全套健全。

## 2. 方块机器 + 注册（ExchangeSwitchTile, RestorationSwitchTile, Block*, Container*, ChannelUpgradeItem, MeksRegistries）

| 严重度 | 位置 | 问题 |
|---|---|---|
| high | 见 §0.1 / §0.2 | CancelRepair 无归属校验；下载中断回收物品。（跨模块交叉确认，两处独立审查均发现） |
| low | [ExchangeSwitchTile.java:139](src/main/java/com/mikufan/meks/ExchangeSwitchTile.java#L139) | `extract` 先于 `tickJob`：DOWNLOAD 槽满 simulate 失败的 tick 无进展也扣电。可触发面窄，属结构性隐患。 |
| low | [ExchangeSwitchTile.java:157](src/main/java/com/mikufan/meks/ExchangeSwitchTile.java#L157) / [RestorationSwitchTile.java:180](src/main/java/com/mikufan/meks/RestorationSwitchTile.java#L180) | 运行中每 tick `markForSave()`，区块持续置脏。✅ 已修复（批次 C：`saveThrottle` 降为每 20 tick 一次；启动/完成/取消处仍即时置脏）。 |
| low | [ExchangeSwitchTile.java:475](src/main/java/com/mikufan/meks/ExchangeSwitchTile.java#L475) | GUI 打开期间每 tick 4 次 `createTargetStack()` 新分配（SyncableItemStack getter 无条件调用）。 |
| info | [RestorationSwitchTile.java:301](src/main/java/com/mikufan/meks/RestorationSwitchTile.java#L301) | `RepairJob` 完全不持久化（对比交换机作业全量持久化），重启丢进行中状态；两机策略不一致值得留意。 |
| info | [ExchangeSwitchTile.java:63](src/main/java/com/mikufan/meks/ExchangeSwitchTile.java#L63) | 构造器在 `setupItemIOConfig` 引用槽位字段，依赖 Mekanism 基类构造顺序（字节码确认当前安全，升级需留意）。 |
| info | [ExchangeSwitchTile.java:221](src/main/java/com/mikufan/meks/ExchangeSwitchTile.java#L221) | FORGET 完成时不重验 `hasKnowledge`：任务期间经便携机遗忘后仍扣 SV 发货（非复制漏洞，语义瑕疵）。 |

**做得好**：`channelUpgrade` 先于 `super.loadAdditional()` 读取的关键顺序正确且注释完整（经 `ContainerType.readFrom→DataHandlerUtils.readContents` 字节码核实依赖真实）；`proportional()` 纯整数比例分摊无浮点误差；作业中槽位锁定（含 ejector/侧配置路径）推演无绕过；MeksRegistries 注册一一对应齐全。

## 3. 网络层（MeksPayloads, FlightNetworking）

| 严重度 | 位置 | 问题 |
|---|---|---|
| high | 见 §0.1 | CancelRepair 无所有权校验（同上）。 |
| medium | [MeksPayloads.java:382](src/main/java/com/mikufan/meks/MeksPayloads.java#L382) | roll 字段接受 NaN/Infinity：`roll != lastRoll` 对 NaN 恒真 → 该实体每 tick 触发全员广播（转发风暴）；NaN 经 S2C 污染观察者相机矩阵。~~修复：`Float.isFinite` 校验。~~ ✅ 已按此修复（2026-10-11，非有限值按 0 处理）。 |
| low | [MeksPayloads.java:214](src/main/java/com/mikufan/meks/MeksPayloads.java#L214) / :347 / :356 | 交换机类 C2S 只查 tile 类型，不校验 `containerMenu` 对应关系与距离——owner 可跨维度盲操作自己的机器（owner 比对兜底了他人操作，影响限于防御深度）。 |
| info | [MeksPayloads.java:201](src/main/java/com/mikufan/meks/MeksPayloads.java#L201) | `versioned("2").optional()` 静默降级：旧客户端连上新服无报错，仅 GUI 永远空数据。安全但排障困难，可考虑 GUI 层首次同步超时提示。 |
| info | [MeksPayloads.java:374](src/main/java/com/mikufan/meks/MeksPayloads.java#L374) | RollSync 不校验飞行资格/频率，仅 `modEnabled` 门控——自我外观欺骗，危害有限。 |

**做得好**：10 个 handler 全部 `enqueueWork`，线程模型零违例；便携/交换机的数量、槽位统一钳制（slot 0-35、count 双向 clamp）；所有权校验经 `TileComponentSecurity` UUID 严格比对；S2C 数据范围严格限定请求者自身；`addPairing` TAIL 首帧推送与 play 通道 FIFO 时序正确；畸形字节由原生 StreamCodec 解码层断连，无手工解析风险。

## 4. 便携式交换机 + GUI（Portable*, MekanismSwitchClient, client/*）

| 严重度 | 位置 | 问题 |
|---|---|---|
| medium | [MeksPayloads.java:251](src/main/java/com/mikufan/meks/MeksPayloads.java#L251) | SV 结算不感知组件：受损钻石剑按满值上传、下载得全新满耐久物品 → 无限免费"修复"，架空复位机；上传带内容物的潜影盒时内容物被静默销毁且无确认。✅ **耐久维度已修复**（2026-10-11 批次 B：`MeksValues.getValue(ItemStack)` 按 `满值×剩余耐久/最大耐久` 折旧，交换机与便携上传、tooltip 三处同步；下载仍按满值 → 免费修复闭环关闭）。**内容物维度仍开放**（潜影盒等容器物品内容物照旧静默销毁，需后续决策：拒绝上传/递归计价/确认提示）。 |
| low | [MeksPayloads.java:309](src/main/java/com/mikufan/meks/MeksPayloads.java#L309) | `RequestPortableSync`（及 `RequestExchangeSync`）无频率限制：每 tick 发包即让服务器全量拷贝 knowledge 下发，带宽放大向量。✅ 已修复（批次 C：两 handler 共用 `SYNC_COOLDOWNS`，<10 tick 丢弃；`PlayerLoggedOutEvent` 清理；操作触发的同步不受限）。 |
| low | [GuiKnowledgeScroll.java:63](src/main/java/com/mikufan/meks/client/GuiKnowledgeScroll.java#L63) | 过滤态每帧重建过滤列表 + 每条目每 token `toLowerCase`，同帧滚动条又调一次双倍开销。✅ 已修复（批次 C：结果按 (filter, 容器 `syncVersion`) 缓存；搜索数据预存小写，消除每帧 `toLowerCase`）。 |
| low | [GuiRestorationSwitch.java:128](src/main/java/com/mikufan/meks/client/GuiRestorationSwitch.java#L128) | 统计文案 `"成功…次"` 硬编码中文未走 translatable。✅ 已修复（批次 C：`gui.meks.restoration.success_count`/`fail_count` 双语键；实际是成功/失败两行，故拆为两个键）。 |
| info | [GuiRestorationSwitch.java:107](src/main/java/com/mikufan/meks/client/GuiRestorationSwitch.java#L107) | 客户端复刻服务端费用/成功率公式（含魔数 616 fallback），两端公式一旦漂移显示即错。建议由 tile 同步预估值。 |
| info | [MekanismSwitchClient.java:53](src/main/java/com/mikufan/meks/MekanismSwitchClient.java#L53) | 断开专用服后客户端派生表不重置，tooltip 沿用上一服派生值（结算仍正确，仅显示）。建议 LoggingOut 复位。 |
| info | [GuiPortableExchangeSwitch.java:75](src/main/java/com/mikufan/meks/client/GuiPortableExchangeSwitch.java#L75) | 注释写 "280 px tall (8-row)"，实际 imageHeight=212、5 行。✅ 已修复（批次 C：注释改为 212 px/5 行，并修正原先"比 256 px 基础背景更高"的错误理由）。 |

**做得好**：遗忘"先下载成功后遗忘"顺序严格正确（任何失败路径不删知识）；GUI 全链路只读+发包，无客户端直写权威状态路径；滚动/搜索/点击索引映射经反编译核实无越界错位；背包空间预检避免"扣了 SV 物品掉地上"的边界不一致。

## 5. 灵魂出窍 + 客户端 mixin（soul/*, mixin/client/* 12 个）

| 严重度 | 位置 | 问题 |
|---|---|---|
| medium | [ClientPacketListenerMixin.java:13](src/main/java/com/mikufan/meks/mixin/client/ClientPacketListenerMixin.java#L13) | 只拦 `handleRespawn` 不拦 `handleLogin`：经 BungeeCord/Velocity 跨服后 active 状态残留，新服交互被永久锁死且持续对新玩家扣能量，直到手动按 F4。~~修复：handleLogin HEAD（或 LoggingIn 事件）同样重置。~~ ✅ 已修复（2026-10-11，handleLogin HEAD 注入复用 `onRespawn()`）。 |
| medium | [SoulOutController.java:165](src/main/java/com/mikufan/meks/soul/SoulOutController.java#L165) | 能量扣除只发生在客户端 ItemStack 上，服务端永不感知：多人环境成本形同虚设，与配置"server-authoritative"语义不符。✅ **已按方案 C 修复**（2026-10-11 批次 B：整体移除能量消耗——删除 `canAfford`/`drainEnergy`/`currentCost` 与 `elapsedTicks`，删除 `[soulOut] baseCostPerTick`/`costDoublingSeconds` 配置项与 `msg.meks.soul.no_energy` 文本，README/配置/灵魂出窍/项目速览四份文档同步更新）。 |
| low | [LivingEntityMixin.java:19](src/main/java/com/mikufan/meks/mixin/client/LivingEntityMixin.java#L19) | `disableOnDamage` 以 setHealth 下降为信号：全套 MekaSuit 用能量抵伤时血量不降，玩家可被打而不回体，与配置语义不符。 |
| low | [GuiMixin.java:27](src/main/java/com/mikufan/meks/mixin/client/GuiMixin.java#L27) | `renderTextureOverlay` 一刀切隐藏：连细雪冻结轮廓等危险提示也被屏蔽，与"HUD 始终描述真实玩家"设计矛盾，掩盖死亡风险。 |
| info | [SoulCamera.java:41](src/main/java/com/mikufan/meks/soul/SoulCamera.java#L41) | 固定假实体 id -420 有与其他假实体 mod 冲突风险；:66 只覆写一参 `send`，两参重载会向未连接 Connection 排队积累。 |
| info | [MinecraftMixin.java:14](src/main/java/com/mikufan/meks/mixin/client/MinecraftMixin.java#L14) | 灵魂期间 Q 丢物/容器点击未拦截（服务端正常结算，无 reach 利用面，仅语义不完整）。 |
| info | [SoulOutController.java:142](src/main/java/com/mikufan/meks/soul/SoulOutController.java#L142) | `disable()` 无条件 `smartCull=true`，覆盖其他 mod 的修改；建议记录原值还原。 |

**做得好**：交互拦截覆盖面基本完整，未发现可被反作弊位置校验命中的新包路径；能量公式经 double 计算饱和到 Long.MAX 不回绕（该实现已于批次 B 随能量消耗一并移除）；`handleRespawn` 同时覆盖死亡重生与同服换维度（despawn 用自身 level() 定位正确）；disable() 状态恢复顺序正确避开自身拦截；12 个 mixin 全 HEAD 注入，无 ordinal/局部捕获依赖，冲突风险低。

## 6. 飞行系统（flight/* 全部 + mixin/flight/* 12 个）

| 严重度 | 位置 | 问题 |
|---|---|---|
| high | 见 §0.3 | ExpressionParser 深递归 StackOverflowError 未捕获，坏公式崩客户端。 |
| medium | [ExpressionParser.java:30](src/main/java/com/mikufan/meks/flight/math/ExpressionParser.java#L30) | `'-'` 是变量名字符：`$velocity-5` 解析成变量 `"velocity-5"` 静默按 0 处理（自带 TODO），减法语义与变量名冲突且无提示。✅ 已修复（批次 C：`Parser.isVariableChar()` 移除 `'-'`，`$velocity-5` 现正确解析为减法；未知变量改为首次使用时日志告警一次）。 |
| medium | [ExpressionParser.java:134](src/main/java/com/mikufan/meks/flight/math/ExpressionParser.java#L134) | `randint` 参数 ≤0（或 NaN→0）抛 `IllegalArgumentException`，eval 直接在鼠标事件中调用，无 try/catch → 客户端崩溃。✅ 已修复（2026-10-11：单/双参 `randint` 边界钳制 ≥1，并在求值边界捕获 `Throwable` 兜底）。 |
| medium | [RotationModifiers.java:75](src/main/java/com/mikufan/meks/flight/RotationModifiers.java#L75) | banking/fixNaN 只查 `isNaN` 不查 Infinity：公式如 `1/$velocity_length` 速度为 0 时产生 Infinity 写入视角，pitch/yaw 永久 NaN 直到重登。~~统一改 `!Double.isFinite`。~~ ✅ 已按此修复（2026-10-11，banking 与 fixNaN 四处检查全部改判 isFinite）。 |
| medium | [RotationModifiers.java:65](src/main/java/com/mikufan/meks/flight/RotationModifiers.java#L65) | 热路径逐帧重建最多 5 棵 AST（每次 `new ExpressionParser`）+ 双括号 HashMap + `EventImpl.getListeners()` 每次 sort+ImmutableList。1kHz 鼠标下开销可观。✅ 已修复（批次 C：`MeksFlightConfig` 按配置字符串缓存 AST，事件监听器改注册时排序）；**残留**：`RotationModifiers.getVars()` 的双括号匿名 HashMap 与 `Sensitivity` 每帧新建仍在，属低收益项。 |
| medium | [DebugHudMixin.java:31](src/main/java/com/mikufan/meks/mixin/flight/client/roll/DebugHudMixin.java#L31) | 防御分支 `return null` 传入 `String.format` 会 NPE（同文件另一处正确返回原 args，处理不一致）；`ordinal = 7` 定位对上游改动脆弱。✅ NPE 已修复（2026-10-11，`return null` 改为 `return format`）；ordinal 脆弱性保留待后续加固。 |
| low | [ServerEntityMixin.java:44](src/main/java/com/mikufan/meks/mixin/flight/ServerEntityMixin.java#L44) | 中继开关关闭时只重置服务端缓存，已看到 rolling=true 的客户端收不到关闭通知，永久显示翻滚。 |
| low | [ClientPlayerEntityMixin.java:118](src/main/java/com/mikufan/meks/mixin/flight/roll/entity/ClientPlayerEntityMixin.java#L118) | 直接 `roll += delta` 绕过 `setRoll` 的跨 ±90° 插值修正，快速翻滚可能一帧反向毛刺。 |
| low | [RotationModifiers.java:47](src/main/java/com/mikufan/meks/flight/RotationModifiers.java#L47) | 注释与实际执行顺序矛盾（键盘 2000 晚于换轴 1000），误导后续改优先级的人。 |
| info | flight/ 与 mixin/flight/ 全部 | **GPL 来源声明不完整**：20 余个移植文件仅 3 处行内注释提及 DABR，无版权/许可文件头、无"已修改"声明。GPL-3.0 要求保留；建议统一头注（来源项目+版本+许可+修改说明）。 |
| info | [InputContextImpl.java:77](src/main/java/com/mikufan/meks/flight/impl/key/InputContextImpl.java#L77) | `bindingsByKey` 等为无调用方死代码。✅ 已删除（批次 C：连同接口中无法独立存活的 `getKeyBinding`/`updateKeysByCode` 声明及其空置导入；`KeyBindingAccessor` mixin 因此失去引用，保留但已无用）。 |

**做得好**：激活门槛集中单一 `isFallFlying()`，非飞行时 `clearValues()` 重置平滑器避免视角跳变；NaN 防护在 4 个阶段设检查点（除 Infinity 盲区外）；观察者同步链路（变化广播+首帧推送+`sendPosition` TAIL 时序+不含自身）设计周密；按键 mixin 最小侵入；坏公式降级为无 banking 而非禁飞。

## 7. 资源 JSON + 构建配置

**核对结论（全量校验）**：
- meks.mixins.json 27 条与 27 个 mixin 类**一一对应**，无缺失/多余，client 包全部正确放 client 区块，专用服务器可安全加载；
- zh_cn/en_us 键集合**完全对称**（审查时各 50 键；批次 B 删 `no_energy`、批次 C 增 success/fail 两键后为各 49 键，仍对称且 JSON 有效）；代码 lang key 引用**全部命中**；
- blockstate→model→贴图、item 模型、配方、loot_table、SV 预设表（1415/151/381/151 条，schema 统一无重复）**全部闭环，缺失 0**；23 个 JSON 无语法错误（含对照 Mekanism jar 内父模型验证纹理链）；
- 发布 jar 策略正确：Mekanism/mixin 均 compileOnly，不缺类不误打包；gradle.properties 无敏感信息，CI 未使用 secrets。

| 严重度 | 位置 | 问题 |
|---|---|---|
| low | [lang/zh_cn.json:6](src/main/resources/assets/meks/lang/zh_cn.json#L6) | `item.meks.channel_upgrade.desc` 为死键（代码无引用）。✅ 已修复（批次 C：`ChannelUpgradeItem.appendHoverText` 启用该键，与 `PortableExchangeSwitchItem` 同写法）。 |
| info | [recipe/restoration_switch.json:10](src/main/resources/data/meks/recipe/restoration_switch.json#L10) | key 符号 A 与 F 指向同一物品，可合并提升可读性。 |
| info | [.github/workflows/build.yml:29](.github/workflows/build.yml#L29) | 建议补 `gradle/actions/wrapper-validation@v4` 防止 gradlew 被篡改。 |
| info | assets/meks/gui/ | GUI 贴图不在 `textures/gui/` 惯例路径（按字面 RL 引用，自洽可用），接 datagen 时注意。 |

---

## 8. 修复优先级建议

1. ~~**立即（安全/体验硬伤）**：§0.1 / §0.2 / §0.3~~ ✅ 已完成（2026-10-11，`compileJava` 通过；§0.3 的 `randint` 兜底已随本次一并修复，从下表移除）。
2. ~~**下版本**：roll NaN/Infinity 校验；knowledge 注册表过滤；灵魂出窍 handleLogin 重置；Infinity 视角防护~~ ✅ 已完成（2026-10-11 批次 A）。~~SV 结算组件感知；灵魂出窍能量结算语义~~ ✅ 已完成（2026-10-11 批次 B：耐久折旧 + 移除能量消耗，`compileJava` 通过）。**剩余**：容器物品内容物静默销毁（潜影盒等，需决策）；批次 C 性能/工程项。
3. ~~**择机**：RequestSync 冷却、热路径缓存（KnowledgeEntry/过滤列表/表达式 AST）、markForSave 降频~~ ✅ 已完成（2026-10-11 批次 C，`gradlew build` 通过）。**剩余**：payload 容器/距离校验（防御深度，低危）、GPL 合规包（C7，按用户决定不做，发布前必办）、容器内容物策略、`RotationModifiers` 残余每帧分配、`MeksValues` 跨线程同步。
4. **已知接受的取舍（本次未列入问题）**：无单元测试；SV 推导仅工作台配方/64 轮/tag 低估；复位机不退费；便携机即时无能耗；`versioned("2").optional()` 静默降级。

## 9. 总体评价

架构质量显著高于一般附属 mod：服务端权威贯彻彻底（owner UUID 比对 + 输入钳制 + GUI 只读发包三重防线），线程模型零违例，最关键的 NBT 顺序依赖处理正确且有充分注释，资源闭环零缺失。三个 high 均为局部改动即可修复（两处一行级、一处 ~10 行），无结构性返工。`flight/` 的公式引擎健壮性曾是当前最薄弱环节，已由公式长度/深度限制、`randint` 钳制、求值边界兜底与 AST 缓存加固；灵魂出窍的"客户端自嗨"边界中，能量消耗已于批次 B 整体移除，剩余待决项为伤害信号（MekaSuit 能量抵伤不触发回体）与跨服状态（已补 `handleLogin` 重置）。
审查后共完成三批修复（A：5 项安全/健壮性；B：SV 耐久折旧 + 移除能耗；C：性能缓存 4 项 + 顺手小项 4 项），全部 `gradlew build` 通过、资源校验通过、变更未提交。剩余开放项见 §8。
