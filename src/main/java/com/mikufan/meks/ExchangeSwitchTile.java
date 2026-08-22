package com.mikufan.meks;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.Upgrade;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.functions.ConstantPredicates;
import mekanism.api.security.SecurityMode;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.holder.energy.EnergyContainerHelper;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.capabilities.holder.ConfigHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.slot.ContainerSlotType;
import mekanism.common.inventory.container.sync.SyncableEnum;
import mekanism.common.inventory.container.sync.SyncableInt;
import mekanism.common.inventory.container.sync.SyncableItemStack;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.inventory.slot.BasicInventorySlot;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.TileComponentSecurity;
import mekanism.common.tile.component.config.slot.InventorySlotInfo;
import mekanism.common.tile.prefab.TileEntityConfigurableMachine;
import mekanism.common.util.MekanismUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExchangeSwitchTile extends TileEntityConfigurableMachine {

    private MachineEnergyContainer<ExchangeSwitchTile> energyContainer;
    private BasicInventorySlot processSlot;
    private BasicInventorySlot channelSlot;
    private BasicInventorySlot forgetSlot;
    private EnergyInventorySlot energySlot;

    private ChannelJob processJob;
    private ChannelJob channelJob;
    private ChannelJob forgetJob;
    private boolean channelUpgrade;

    public ExchangeSwitchTile(BlockPos pos, BlockState state) {
        super(MeksRegistries.EXCHANGE_SWITCH_BLOCK, pos, state);
        configComponent.setupItemIOConfig(List.of((IInventorySlot) processSlot, (IInventorySlot) channelSlot),
              List.of((IInventorySlot) processSlot, (IInventorySlot) channelSlot), energySlot, false);
        configComponent.setupInputConfig(TransmissionType.ENERGY, energyContainer);
        ejectorComponent = new TileComponentEjector(this);
        ejectorComponent.setOutputData(configComponent, TransmissionType.ITEM);
    }

    @Override
    protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener) {
        EnergyContainerHelper builder = EnergyContainerHelper.forSideWithConfig(this);
        builder.addContainer(energyContainer = MachineEnergyContainer.input(this, listener));
        return builder.build();
    }

    @NotNull
    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        ExchangeInventoryHolder holder = new ExchangeInventoryHolder();
        processJob = new ChannelJob();
        channelJob = new ChannelJob();
        forgetJob = new ChannelJob();
        holder.addSlot(processSlot = new ChannelInventorySlot(
              (stack, type) -> canExchangeExtract(processJob, processSlot, stack, type),
              (stack, type) -> canExchangeInsert(processJob, processSlot, stack, type),
              this::canProcessItem, processContentsListener(listener, processJob), 29, 117));
        processSlot.setSlotType(ContainerSlotType.INPUT);
        holder.addSlot(channelSlot = new ChannelInventorySlot(
              (stack, type) -> canExchangeExtract(channelJob, channelSlot, stack, type),
              (stack, type) -> canExchangeInsert(channelJob, channelSlot, stack, type),
              this::canProcessItem, processContentsListener(listener, channelJob), 50, 117));
        channelSlot.setSlotType(ContainerSlotType.INPUT);
        holder.addSlot(forgetSlot = BasicInventorySlot.at(ConstantPredicates.manualOnly(), ConstantPredicates.internalOnly(),
              ConstantPredicates.alwaysTrue(), listener, 8, 117));
        forgetSlot.setSlotType(ContainerSlotType.OUTPUT);
        holder.addSlot(energySlot = EnergyInventorySlot.fillOrConvert(energyContainer, this::getLevel, listener, 152, 117));
        return holder;
    }

    @Override
    protected boolean onUpdateServer() {
        boolean sendUpdatePacket = super.onUpdateServer();
        energySlot.fillContainerOrConvert();
        if (canFunction()) {
            process();
        }
        return sendUpdatePacket;
    }

    private void process() {
        Player owner = getOwnerPlayer();
        if (owner == null) {
            setActive(false);
            return;
        }
        checkForInterrupt(processJob, processSlot);
        checkForInterrupt(channelJob, channelSlot);
        autoStartUpload(processJob, processSlot, owner);
        autoStartUpload(channelJob, channelSlot, owner);

        boolean anyActive = processJob.operation != ExchangeOperation.NONE
              || channelJob.operation != ExchangeOperation.NONE
              || forgetJob.operation != ExchangeOperation.NONE;
        if (!anyActive) {
            setActive(false);
            return;
        }
        long totalEnergyPerTick = 0;
        if (processJob.operation != ExchangeOperation.NONE) {
            totalEnergyPerTick += processJob.energyPerTick;
        }
        if (channelJob.operation != ExchangeOperation.NONE) {
            totalEnergyPerTick += channelJob.energyPerTick;
        }
        if (forgetJob.operation != ExchangeOperation.NONE) {
            totalEnergyPerTick += forgetJob.energyPerTick;
        }
        if (energyContainer.getEnergy() < totalEnergyPerTick) {
            setActive(false);
            return;
        }
        energyContainer.extract(totalEnergyPerTick, Action.EXECUTE, AutomationType.INTERNAL);
        energyContainer.setEnergyPerTick(totalEnergyPerTick);

        boolean active = false;
        if (processJob.operation != ExchangeOperation.NONE) {
            active |= tickJob(processJob, processSlot, owner);
        }
        if (channelJob.operation != ExchangeOperation.NONE) {
            active |= tickJob(channelJob, channelSlot, owner);
        }
        if (forgetJob.operation != ExchangeOperation.NONE) {
            active |= tickJob(forgetJob, forgetSlot, owner);
        }
        setActive(active);
        markForSave();
    }

    private boolean tickJob(ChannelJob job, BasicInventorySlot slot, Player owner) {
        long value = getTargetValue(job, slot);
        if (value <= 0 || (job.operation != ExchangeOperation.UPLOAD && job.pendingCount <= 0)) {
            resetJob(job, slot);
            return false;
        }
        if (job.operation != ExchangeOperation.UPLOAD && getData(owner).getSv() < value) {
            resetJob(job, slot);
            return false;
        }
        if (job.operation != ExchangeOperation.UPLOAD
              && !slot.insertItem(job.createTargetStack(), Action.SIMULATE, AutomationType.INTERNAL).isEmpty()) {
            return false;
        }
        job.operatingTicks++;
        if (job.operatingTicks >= job.ticksRequired) {
            completeOperation(job, slot, owner, value);
            job.operatingTicks = 0;
            if (job.operation != ExchangeOperation.NONE) {
                recalculateRequirements(job);
            }
            return job.operation != ExchangeOperation.NONE;
        }
        return true;
    }

    private void completeOperation(ChannelJob job, BasicInventorySlot slot, Player owner, long value) {
        PlayerExchangeData data = getData(owner);
        switch (job.operation) {
            case UPLOAD -> {
                ItemStack stack = slot.getStack();
                if (stack.isEmpty()) {
                    resetJob(job, slot);
                    return;
                }
                ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
                data.addSv(value);
                data.learn(key);
                slot.extractItem(1, Action.EXECUTE, AutomationType.INTERNAL);
                if (slot.isEmpty()) {
                    resetJob(job, slot);
                }
            }
            case DOWNLOAD -> {
                if (!data.consumeSv(value)) {
                    resetJob(job, slot);
                    setData(owner, data);
                    syncToOwner(owner);
                    return;
                }
                ItemStack remainder = slot.insertItem(job.createTargetStack(), Action.EXECUTE, AutomationType.INTERNAL);
                if (!remainder.isEmpty()) {
                    data.addSv(value);
                    return;
                }
                job.pendingCount--;
                if (job.pendingCount <= 0) {
                    resetJob(job, slot);
                    job.suppressAutoStart = true;
                }
            }
            case FORGET -> {
                if (!data.consumeSv(value)) {
                    resetJob(job, slot);
                    setData(owner, data);
                    syncToOwner(owner);
                    return;
                }
                ItemStack remainder = slot.insertItem(job.createTargetStack(), Action.EXECUTE, AutomationType.INTERNAL);
                if (!remainder.isEmpty()) {
                    data.addSv(value);
                    return;
                }
                data.forget(job.target);
                resetJob(job, slot);
            }
            case NONE -> {
            }
        }
        setData(owner, data);
        syncToOwner(owner);
    }

    public void startExchange(Player player, ResourceLocation requested, int count, boolean forget, int slotIndex) {
        if (level == null || level.isClientSide) {
            return;
        }
        TileComponentSecurity security = getSecurity();
        if (security == null || !Objects.equals(security.getOwnerUUID(), player.getUUID())) {
            return;
        }
        PlayerExchangeData data = getData(player);
        if (!data.hasKnowledge(requested)) {
            return;
        }
        long value = MeksValues.getValue(BuiltInRegistries.ITEM.get(requested));
        if (value <= 0 || data.getSv() < value) {
            return;
        }
        if (forget) {
            if (forgetJob.operation != ExchangeOperation.NONE || !forgetSlot.isEmpty()) {
                return;
            }
            forgetJob.operation = ExchangeOperation.FORGET;
            forgetJob.target = requested;
            forgetJob.pendingCount = 1;
            recalculateRequirements(forgetJob);
            markForSave();
            syncToOwner(player);
            return;
        }
        ChannelJob job = slotIndex == 1 ? channelJob : processJob;
        if (slotIndex == 1 && !channelUpgrade) {
            return;
        }
        BasicInventorySlot slot = slotIndex == 1 ? channelSlot : processSlot;
        if (job.operation != ExchangeOperation.NONE || !slot.isEmpty()) {
            return;
        }
        job.operation = ExchangeOperation.DOWNLOAD;
        job.target = requested;
        job.pendingCount = Math.max(1, Math.min(count, job.createTargetStack().getMaxStackSize()));
        recalculateRequirements(job);
        markForSave();
        syncToOwner(player);
    }

    private void recalculateRequirements(ChannelJob job) {
        BasicInventorySlot slot = job == processJob ? processSlot : job == channelJob ? channelSlot : forgetSlot;
        long value = Math.max(1, getTargetValue(job, slot));
        long totalEnergy;
        int baseTicks;
        int minTicks = Config.EXCHANGE_MIN_TICKS.get();
        int maxTicks = Math.max(minTicks, Config.EXCHANGE_MAX_TICKS.get());
        if (job.operation == ExchangeOperation.UPLOAD) {
            baseTicks = (int) Math.min(maxTicks, Math.max(minTicks, 20 + (long) (value * Config.EXCHANGE_UPLOAD_TICKS_PER_SV.get())));
            totalEnergy = value * Config.EXCHANGE_UPLOAD_FE_PER_SV.get();
        } else {
            baseTicks = (int) Math.min(maxTicks, Math.max(minTicks, 20 + (long) (value * Config.EXCHANGE_DOWNLOAD_TICKS_PER_SV.get())));
            totalEnergy = value * Config.EXCHANGE_DOWNLOAD_FE_PER_SV.get();
        }
        long baseEnergyPerTick = Math.max(1, totalEnergy / Math.max(1, baseTicks));
        job.ticksRequired = MekanismUtils.getTicks(this, baseTicks);
        job.energyPerTick = MekanismUtils.getEnergyPerTick(this, baseEnergyPerTick);
        energyContainer.updateMaxEnergy();
    }

    @Override
    public void recalculateUpgrades(Upgrade upgrade) {
        super.recalculateUpgrades(upgrade);
        boolean anyActive = processJob.operation != ExchangeOperation.NONE
              || channelJob.operation != ExchangeOperation.NONE
              || forgetJob.operation != ExchangeOperation.NONE;
        if (anyActive) {
            if (processJob.operation != ExchangeOperation.NONE) {
                recalculateRequirements(processJob);
            }
            if (channelJob.operation != ExchangeOperation.NONE) {
                recalculateRequirements(channelJob);
            }
            if (forgetJob.operation != ExchangeOperation.NONE) {
                recalculateRequirements(forgetJob);
            }
        } else {
            energyContainer.updateMaxEnergy();
        }
    }

    private long getTargetValue(ChannelJob job, BasicInventorySlot slot) {
        if (job.operation == ExchangeOperation.UPLOAD) {
            ItemStack stack = slot.getStack();
            return stack.isEmpty() ? 0 : MeksValues.getValue(stack.getItem());
        }
        return job.target == null ? 0 : MeksValues.getValue(BuiltInRegistries.ITEM.get(job.target));
    }

    private void checkForInterrupt(ChannelJob job, BasicInventorySlot slot) {
        if (job.operation != ExchangeOperation.DOWNLOAD || slot.isEmpty() || job.target == null) {
            return;
        }
        if (!ItemStack.isSameItemSameComponents(slot.getStack(), job.createTargetStack())) {
            resetJob(job, slot);
            job.suppressAutoStart = true;
            markForSave();
        }
    }

    private void autoStartUpload(ChannelJob job, BasicInventorySlot slot, Player owner) {
        if (job.operation != ExchangeOperation.NONE || job.suppressAutoStart || slot.isEmpty()) {
            return;
        }
        ItemStack stack = slot.getStack();
        if (MeksValues.hasValue(stack.getItem())) {
            job.operation = ExchangeOperation.UPLOAD;
            job.operatingTicks = 0;
            recalculateRequirements(job);
        }
    }

    private void resetJob(ChannelJob job, BasicInventorySlot slot) {
        job.operation = ExchangeOperation.NONE;
        job.target = null;
        job.pendingCount = 0;
        job.operatingTicks = 0;
    }

    private void onProcessSlotChanged(ChannelJob job, BasicInventorySlot slot) {
        if (level == null || level.isClientSide) {
            return;
        }
        if (slot.isEmpty()) {
            job.suppressAutoStart = false;
        }
    }

    /**
     * Cancels only the job of the item slot the player right-clicked, so the two channels
     * (and the forget slot) stay independent: interrupting one never touches the others.
     * Slot ids mirror {@link #startExchange}: 0 = process slot, 1 = channel slot,
     * 2 = forget slot (the forget slot has no dedicated start payload, so it uses a distinct id).
     */
    public void cancelOperation(Player player, int slot) {
        if (level == null || level.isClientSide) {
            return;
        }
        TileComponentSecurity security = getSecurity();
        if (security == null || !Objects.equals(security.getOwnerUUID(), player.getUUID())) {
            return;
        }
        ChannelJob job = processJob;
        BasicInventorySlot physicalSlot = processSlot;
        if (slot == 1) {
            if (!channelUpgrade) {
                return;
            }
            job = channelJob;
            physicalSlot = channelSlot;
        } else if (slot == 2) {
            job = forgetJob;
            physicalSlot = forgetSlot;
        }
        cancelJob(job, physicalSlot);
        markForSave();
    }

    private void cancelJob(ChannelJob job, BasicInventorySlot slot) {
        if (job.operation != ExchangeOperation.NONE) {
            job.suppressAutoStart = true;
        }
        resetJob(job, slot);
    }

    private boolean canExchangeInsert(ChannelJob job, BasicInventorySlot slot, ItemStack stack, AutomationType type) {
        return type == AutomationType.INTERNAL || (job.operation == ExchangeOperation.NONE && slot.isEmpty());
    }

    private boolean canExchangeExtract(ChannelJob job, BasicInventorySlot slot, ItemStack stack, AutomationType type) {
        return type == AutomationType.INTERNAL || (job.operation == ExchangeOperation.NONE && !slot.isEmpty());
    }

    private boolean canProcessItem(ItemStack stack) {
        return !stack.isEmpty() && MeksValues.hasValue(stack.getItem());
    }

    @Nullable
    private Player getOwnerPlayer() {
        TileComponentSecurity security = getSecurity();
        UUID owner = security == null ? null : security.getOwnerUUID();
        if (owner == null || level == null || level.getServer() == null) {
            return null;
        }
        return level.getServer().getPlayerList().getPlayer(owner);
    }

    private static PlayerExchangeData getData(Player player) {
        return player.getData(MeksAttachments.EXCHANGE_DATA);
    }

    private static void setData(Player player, PlayerExchangeData data) {
        player.setData(MeksAttachments.EXCHANGE_DATA, data);
    }

    public void syncToOwner(Player owner) {
        if (owner instanceof ServerPlayer serverPlayer) {
            PlayerExchangeData data = getData(owner);
            PacketDistributor.sendToPlayer(serverPlayer,
                  new MeksPayloads.SyncExchangePayload(new ArrayList<>(data.getKnowledge()), data.getSv()));
        }
    }

    @Override
    public void open(Player player) {
        super.open(player);
        if (player instanceof ServerPlayer) {
            syncToOwner(player);
        }
    }

    @Override
    public void onAdded() {
        super.onAdded();
        MeksValues.ensureInitialized(level);
        TileComponentSecurity security = getSecurity();
        if (security != null && security.getMode() != SecurityMode.PRIVATE) {
            security.setMode(SecurityMode.PRIVATE);
            markForSave();
        }
    }

    @Override
    public void addContainerTrackers(MekanismContainer container) {
        super.addContainerTrackers(container);
        container.track(SyncableEnum.create(ExchangeOperation.BY_ID, ExchangeOperation.NONE, () -> processJob.operation, value -> processJob.operation = value));
        container.track(SyncableInt.create(() -> processJob.operatingTicks, value -> processJob.operatingTicks = value));
        container.track(SyncableInt.create(() -> processJob.ticksRequired, value -> processJob.ticksRequired = value));
        container.track(SyncableItemStack.create(processJob::createTargetStack, stack -> processJob.targetStackSync = stack));
        container.track(SyncableEnum.create(ExchangeOperation.BY_ID, ExchangeOperation.NONE, () -> channelJob.operation, value -> channelJob.operation = value));
        container.track(SyncableInt.create(() -> channelJob.operatingTicks, value -> channelJob.operatingTicks = value));
        container.track(SyncableInt.create(() -> channelJob.ticksRequired, value -> channelJob.ticksRequired = value));
        container.track(SyncableItemStack.create(channelJob::createTargetStack, stack -> channelJob.targetStackSync = stack));
        container.track(SyncableEnum.create(ExchangeOperation.BY_ID, ExchangeOperation.NONE, () -> forgetJob.operation, value -> forgetJob.operation = value));
        container.track(SyncableInt.create(() -> forgetJob.operatingTicks, value -> forgetJob.operatingTicks = value));
        container.track(SyncableInt.create(() -> forgetJob.ticksRequired, value -> forgetJob.ticksRequired = value));
        container.track(SyncableItemStack.create(forgetJob::createTargetStack, stack -> forgetJob.targetStackSync = stack));
    }

    public double getScaledProgress() {
        ChannelJob job = processJob.operation != ExchangeOperation.NONE ? processJob
              : channelJob.operation != ExchangeOperation.NONE ? channelJob : forgetJob;
        return job.ticksRequired <= 0 ? 0 : job.operatingTicks / (double) job.ticksRequired;
    }

    public MachineEnergyContainer<ExchangeSwitchTile> getEnergyContainer() {
        return energyContainer;
    }

    public ExchangeOperation getProcessOperation() {
        return processJob.operation;
    }

    public int getProcessOperatingTicks() {
        return processJob.operatingTicks;
    }

    public int getProcessTicksRequired() {
        return processJob.ticksRequired;
    }

    public ItemStack getProcessTargetStack() {
        return processJob.targetStackSync;
    }

    public ExchangeOperation getChannelOperation() {
        return channelJob.operation;
    }

    public int getChannelOperatingTicks() {
        return channelJob.operatingTicks;
    }

    public int getChannelTicksRequired() {
        return channelJob.ticksRequired;
    }

    public ItemStack getChannelTargetStack() {
        return channelJob.targetStackSync;
    }

    public ExchangeOperation getForgetOperation() {
        return forgetJob.operation;
    }

    public int getForgetOperatingTicks() {
        return forgetJob.operatingTicks;
    }

    public int getForgetTicksRequired() {
        return forgetJob.ticksRequired;
    }

    public ItemStack getForgetTargetStack() {
        return forgetJob.targetStackSync;
    }

    public ItemStack getProcessSlotStack() {
        return processSlot == null ? ItemStack.EMPTY : processSlot.getStack();
    }

    public ItemStack getChannelSlotStack() {
        return channelSlot == null ? ItemStack.EMPTY : channelSlot.getStack();
    }

    public ItemStack getForgetSlotStack() {
        return forgetSlot == null ? ItemStack.EMPTY : forgetSlot.getStack();
    }

    public boolean hasChannelUpgrade() {
        return channelUpgrade;
    }

    public boolean tryInstallChannelUpgrade(Player player) {
        if (level == null || level.isClientSide || channelUpgrade) {
            return false;
        }
        TileComponentSecurity security = getSecurity();
        if (security == null || !Objects.equals(security.getOwnerUUID(), player.getUUID())) {
            return false;
        }
        channelUpgrade = true;
        markForSave();
        sendUpdatePacket();
        return true;
    }

    private IContentsListener processContentsListener(IContentsListener outer, ChannelJob job) {
        return () -> {
            if (job != null) {
                onProcessSlotChanged(job, job == processJob ? processSlot : channelSlot);
            }
            outer.onContentsChanged();
        };
    }

    private boolean isSlotActive(IInventorySlot slot) {
        return slot == processSlot || slot == forgetSlot || slot == energySlot || (slot == channelSlot && channelUpgrade);
    }

    private class ExchangeInventoryHolder extends ConfigHolder<IInventorySlot> implements IInventorySlotHolder {

        private ExchangeInventoryHolder() {
            super(ExchangeSwitchTile.this);
        }

        private void addSlot(IInventorySlot slot) {
            slots.add(slot);
        }

        @Override
        protected TransmissionType getTransmissionType() {
            return TransmissionType.ITEM;
        }

        @Override
        public List<IInventorySlot> getInventorySlots(@Nullable Direction direction) {
            if (direction == null) {
                return slots.stream().filter(ExchangeSwitchTile.this::isSlotActive).toList();
            }
            return getSlots(direction, slotInfo -> {
                if (slotInfo instanceof InventorySlotInfo inventorySlotInfo) {
                    return inventorySlotInfo.getSlots().stream().filter(ExchangeSwitchTile.this::isSlotActive).toList();
                }
                return List.of();
            });
        }
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        saveJob(tag, "", processJob);
        saveJob(tag, "channel", channelJob);
        saveJob(tag, "forget", forgetJob);
        tag.putBoolean("channelUpgrade", channelUpgrade);
    }

    private static void saveJob(CompoundTag tag, String prefix, ChannelJob job) {
        tag.putInt(prefix + "operation", job.operation.ordinal());
        tag.putInt(prefix + "pending", job.pendingCount);
        tag.putInt(prefix + "progress", job.operatingTicks);
        tag.putBoolean(prefix + "suppress", job.suppressAutoStart);
        if (job.target != null) {
            tag.putString(prefix + "target", job.target.toString());
        }
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        // MUST be read before super.loadAdditional(): the item container attachment (ContainerType.ITEM)
        // saves/restores slots through getInventorySlots(null), whose membership depends on channelUpgrade
        // (ExchangeInventoryHolder.isSlotActive). If the flag is still false here, the loaded slot list
        // is one entry shorter than the saved one (channel slot missing), every subsequent slot index
        // shifts, and the energy-slot item (last index) is silently dropped by DataHandlerUtils.readContents.
        channelUpgrade = tag.getBoolean("channelUpgrade");
        super.loadAdditional(tag, provider);
        loadJob(tag, "", processJob);
        loadJob(tag, "channel", channelJob);
        loadJob(tag, "forget", forgetJob);
        if (processJob.operation != ExchangeOperation.NONE) {
            recalculateRequirements(processJob);
        }
        if (channelJob.operation != ExchangeOperation.NONE) {
            recalculateRequirements(channelJob);
        }
        if (forgetJob.operation != ExchangeOperation.NONE) {
            recalculateRequirements(forgetJob);
        }
    }

    private static void loadJob(CompoundTag tag, String prefix, ChannelJob job) {
        ExchangeOperation[] values = ExchangeOperation.values();
        int id = tag.getInt(prefix + "operation");
        job.operation = id >= 0 && id < values.length ? values[id] : ExchangeOperation.NONE;
        job.pendingCount = tag.getInt(prefix + "pending");
        job.operatingTicks = tag.getInt(prefix + "progress");
        job.suppressAutoStart = tag.getBoolean(prefix + "suppress");
        job.target = null;
        if (tag.contains(prefix + "target")) {
            job.target = ResourceLocation.tryParse(tag.getString(prefix + "target"));
        }
    }

    @Override
    public CompoundTag getReducedUpdateTag(@NotNull HolderLookup.Provider provider) {
        CompoundTag tag = super.getReducedUpdateTag(provider);
        tag.putBoolean("channelUpgrade", channelUpgrade);
        return tag;
    }

    @Override
    public void handleUpdateTag(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.handleUpdateTag(tag, provider);
        channelUpgrade = tag.getBoolean("channelUpgrade");
    }

    private static class ChannelInventorySlot extends BasicInventorySlot {

        private ChannelInventorySlot(BiPredicate<ItemStack, AutomationType> canExtract, BiPredicate<ItemStack, AutomationType> canInsert,
              Predicate<ItemStack> validator, IContentsListener listener, int x, int y) {
            super(canExtract, canInsert, validator, listener, x, y);
        }

        @Override
        public ItemStack insertItem(ItemStack stack, Action action, AutomationType automationType) {
            if (automationType != AutomationType.INTERNAL && stack.getCount() > 1) {
                // Each channel only accepts one item at a time from automation/manual input so the surplus
                // is routed to the other open channel instead of entirely filling the first slot.
                ItemStack single = stack.copyWithCount(1);
                if (super.insertItem(single, action, automationType).isEmpty()) {
                    return stack.copyWithCount(stack.getCount() - 1);
                }
                return stack;
            }
            return super.insertItem(stack, action, automationType);
        }
    }

    private static class ChannelJob {

        private ExchangeOperation operation = ExchangeOperation.NONE;
        private ResourceLocation target;
        private int pendingCount;
        private int operatingTicks;
        private int ticksRequired;
        private long energyPerTick;
        private boolean suppressAutoStart;
        private ItemStack targetStackSync = ItemStack.EMPTY;

        private ItemStack createTargetStack() {
            return target == null ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.get(target));
        }
    }
}
