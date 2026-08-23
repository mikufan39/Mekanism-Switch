# DABR 飞行代码集成设计（do-a-barrel-roll → Mekanism-Switch）

来源：enjarai/do-a-barrel-roll @ tag `3.7.3+1.21-neoforge`（Minecraft 1.21/1.21.1，GPL-3.0，经用户确认本次暂不考虑协议问题）。
目标：`com.mikufan.meks.flight` 包树，NeoForge 1.21.1，Mojang 映射，ModDevGradle。

## 1. 范围

**集成（保留）**：DABR 核心三轴飞行控制管线——
- 相机修饰器管线：`RollEvents`（EARLY/LATE_CAMERA_MODIFIERS）、`RotationModifiers`（键位控制、鼠标转向、平滑、banking、自动回正、舵面效能表达式）
- 旋转数学：`RotationInstant`、`ModMath`、`MagicNumbers`、`Smoother`
- 表达式引擎：`math/Expression`、`ExpressionParser`、`Parser`（配置中的灵敏度/banking 公式）
- roll 状态：`RollEntity` 接口 + `roll.entity.*` mixin 链（Entity/LivingEntity/Player）
- 渲染：相机横滚（CameraMixin）、他人模型横滚（PlayerEntityRendererMixin）、HUD 组件（动量准星/地平线）
- 键位系统：`InputContext` 上下文键位 + `KeyBindingMixin` 的 4 个注入（让 A/D 等键与 vanilla 键共存不冲突，原为 fabric 专属，移植为 meks 通用 mixin）
- 网络：roll 同步 C2S/S2C（其他玩家可见横滚），用 meks 的 NeoForge payload 体系重写

**丢弃（简化）**：
- 服务端配置同步/握手（Handshake*/ServerConfigHolder/配置更新包/权限系统）→ 客户端本地配置
- thrust（火箭加速）功能及 ThrustEvents
- YACL 配置界面、ModMenu、Controlify 兼容层、Cicada
- mixinsquared / DABRMixinCanceller
- ActivationBehaviour 的 HYBRID 模式（保留 VANILLA：fall flying 即激活）
- ToastUtil、ServerEvents（仅配置同步用）、StarFox64 事件（按需）

## 2. 激活条件（meks 专属改造）

原 `isFallFlying()` 末尾追加 meks 门控：
1. 配置 `flightControls` 开启
2. 玩家处于 `isFallFlying()`（Mekanism 的 `ItemMekaSuitArmor.canElytraFly` 已保证只有装鞘翅单元才进入滑翔）
3. 胸甲是 `ItemMekaSuitArmor`（与 meks 原功能一致）
4. 每 tick 从胸甲抽能量 `flightEnergyPerTick`，不足则视为未激活（沿用原 meks 能量消耗行为）

## 3. 包结构

```
com.mikufan.meks.flight
├── MeksFlight.java            (common 初始化：roll 网络服务端)
├── MeksFlightClient.java      (client 初始化：修饰器注册、键位、isFallFlying 门控)
├── MeksFlightKeybinds.java    (KeyMapping 注册，RegisterKeyMappingsEvent)
├── api/ (RollEntity, RollCamera, RollMouse, RotationInstant, RollContext, RollGroup, RollEvents, ClientEvents, Event, InputContext, TriState)
├── config/ MeksFlightConfig.java (ModConfigSpec 重写 ModConfig 客户端部分)
├── flight/ RotationModifiers.java
├── impl/ (EventImpl, RollContextImpl, RollGroupImpl, RotationInstantImpl, InputContextImpl)
├── math/ (Expression, ExpressionParser, Parser, MagicNumbers, ModMath)
├── mixin/ (roll.entity.*, ServerEntityMixin, key.*, client/roll/*, render/*)
├── net/ (RollSyncPayload C2S + RollSyncS2CPayload, FlightNetworking)
├── render/ (HorizonLineWidget, MomentumCrosshairWidget, RenderHelper)
└── util/ (DelayedRunnable, Value, MixinHooks(按需), ContextualKeyBinding)
```

## 4. 映射转换约定（Yarn → Mojang）

- `Identifier` → `ResourceLocation`；`PacketByteBuf` → `FriendlyByteBuf`
- `MinecraftClient` → `Minecraft`；`ServerPlayerEntity` → `ServerPlayer`；`PlayerEntity` → `Player`；`ClientPlayerEntity` → `LocalPlayer`；`ServerPlayNetworkHandler` → `ServerGamePacketListenerImpl`
- `EntityTrackerEntry` → `ServerEntity`；`PlayerManager` → `PlayerList`
- `MathHelper` → `Mth`；`Vec3d` → `Vec3`；`Box` → `AABB`；`Text` → `Component`；`DrawContext` → `GuiGraphics`
- `getPitch/getYaw` → `getXRot/getYRot`；`setPitch/setYaw` → `setXRot/setYRot`；`prevPitch/prevYaw` → `xRotO/yRotO`；`headYaw/bodyYaw` → `yHeadRot/yBodyRot`
- `getVelocity/setVelocity/addVelocity` → `getDeltaMovement/setDeltaMovement/addDeltaMovement`；`getRotationVector` → `getViewVector(1.0F)`；`getRotationVecClient` → `getViewVector(1.0F)`；`getLerpedPos` → `getPosition`
- `getMainHandStack` → `getMainHandItem`；`getStackInHand` → `getItemInHand`；`sendMessage(...,true)` → `displayClientMessage(...,true)`
- `KeyBinding` → `KeyMapping`；`isPressed()` → `isDown()`；`wasPressed()` → `consumeClick()`；`InputUtil` → `InputConstants`
- `Mouse` → `MouseHandler`；`updateMouse` → `turnPlayer`；`changeLookDirection(DD)` → `Entity.turn(DD)`（调用点 owner 为 `LocalPlayer`，即 `Lnet/minecraft/client/player/LocalPlayer;turn(DD)V`）
- `InGameHud` → `Gui`；`renderCrosshair(DrawContext, RenderTickCounter)` → `renderCrosshair(GuiGraphics, DeltaTracker)`（1.21.1 是 `net.minecraft.client.DeltaTracker`）
- `DebugHud.getLeftText` → `DebugScreenOverlay.getGameInformation`
- `PlayerEntityRenderer.setupTransforms` → `PlayerRenderer.setupRotations`
- `ClientPlayerEntity.sendMovementPackets` → `LocalPlayer.sendPosition`（private）
- `Camera.update` → `Camera.setup`；`updateEyeHeight`+`cameraY` 字段 → `setup` 内 `eyeHeight` 字段（FIELD target `Lnet/minecraft/client/Camera;eyeHeight:F`）；NeoForge 补丁提供 `Camera.setRotation(FFF)` 三参版与 `roll` 字段，DABR neoforge 分支的两个 ModifyArg 以它为 target
- `world.getEntityById` → `level.getEntity`；`world.getPlayers` → `level.players()`
- mixin 统一 `remap = false`（meks 约定）；目标类与字段描述符用 mojmap 名；`@Share` 命名加 `meksFlight$` 前缀避免与 soul mixin 冲突
- 验证参考：`D:\Mikufan\Documents\.gradle\neoforge-src-ref\`（neoforge-21.1.248-sources.jar 解压，mojmap+NeoForge 补丁源码）

## 4b. 跨层 mixin 继承契约

`flight.client.roll.entity.ClientPlayerEntityMixin` extends `flight.roll.entity.PlayerEntityMixin`（mixin 继承，与 DABR 同构）：
- `PlayerEntityMixin` 必须是 public abstract class；`meksFlight$roll`/`meksFlight$prevRoll`/`meksFlight$isRolling` 字段 protected；`meksFlight$baseTickTail2()` protected 覆写点
- `LivingEntityMixin`（roll.entity）的 `meksFlight$baseTickTail()` 调用 `meksFlight$baseTickTail2()`

## 5. 键位（meks 键位类别 key.categories.meks）

- toggle_enabled（默认 I）、pitch_up/pitch_down（未绑定）、yaw_left（A）、yaw_right（D）、roll_left/roll_right（未绑定）
- 丢弃：toggle_thrust、open_config、thrust_forward/backward

## 6. 网络（MeksPayloads 扩展）

- C2S `RollSyncPayload(rolling, roll)`：客户端限频发送（原 DABR 逻辑）
- S2C `RollSyncS2CPayload(entityId, rolling, roll)`：服务端转发给追踪该实体的玩家
- 无需握手：服务端无条件接受（简化）

## 7. 依赖变化

- 无需新增外部模组依赖（无 FFAPI/mixinsquared）
- MixinExtras：NeoForge 运行时自带；编译期若报缺类，添加 `compileOnly io.github.llamalad7:mixinextras-neoforge:0.4.1`
- 访问转换器：无（DABR 的 accesswidener 为空）

## 8. 执行分层

- L1 基础层：api/impl/math/config/key 基础设施（无 MC mixin）
- L2 服务端层：roll api + roll.entity mixin 链 + ServerEntity 转发
- L3 客户端层：飞行管线 mixin（changeElytraLook/Mouse/Camera/Renderer/HUD）+ render 组件 + 资源
- L4 胶水层：MeksFlight/MeksFlightClient/键位注册/payload/语言/meks.mixins.json/构建脚本
- L5 构建修复 + 文档 + 提交

每层完成后 `gradlew compileJava` 验证。

## 9. 后续 meks 调整（相对本设计文档）

- **移除 I 键运行时开关**：`TOGGLE_ENABLED`、`clientTick` 切换逻辑、`setModEnabled` 与启用/禁用消息删除，`enabled` 仅读配置。
- **移除飞行能量消耗**：`flightEnergyPerTick` 配置、`MeksFlightClient.updateFlightState/drainEnergy` 删除；激活判定不再含能量检查。
- **配置并入通用文件**：全部飞行选项移入 `Config.java` 的 COMMON spec（`meks-common.toml [flight]`），`meks-flight-client.toml` 注册与文件删除；`MeksFlightConfig` 变为取值门面（双端必装，单一配置文件）。
- **移除 HUD 组件**：`HorizonLineWidget`/`MomentumCrosshairWidget`/`RenderHelper`/`ModMath`/`GuiMixin` 删除，`showMomentumWidget`/`showHorizon` 配置删除，`RollMouse.meksFlight$getMouseTurnVec` 移除。
- **新增 `activationMode`**（默认 `ELYTRA_UNIT`）：仅 MekaSuit 胸甲+鞘翅单元激活；`GLOBAL` 时任意鞘翅飞行玩家（含原版鞘翅）激活，胸甲检查跳过。
- **roll 同步加固**：服务端中继受自身 `[flight] enabled` 门控（`handleRollSync` 拒收 + `ServerEntity.sendChanges` 不广播；客户端本地行为仍随客户端配置）；`ServerEntity.addPairing` TAIL 注入首帧推送，中途加入的追踪者立即获得当前 roll 状态（弥补"仅变化时广播"造成的初始态缺失）。
