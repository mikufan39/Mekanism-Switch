package com.mikufan.meks;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.BiPredicate;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.RelativeSide;
import mekanism.api.Upgrade;
import mekanism.api.functions.ConstantPredicates;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.security.SecurityMode;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.holder.ConfigHolder;
import mekanism.common.capabilities.holder.energy.EnergyContainerHelper;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.slot.ContainerSlotType;
import mekanism.common.inventory.container.sync.SyncableBoolean;
import mekanism.common.inventory.container.sync.SyncableInt;
import mekanism.common.inventory.container.sync.SyncableLong;
import mekanism.common.inventory.slot.BasicInventorySlot;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.TileComponentSecurity;
import mekanism.common.tile.component.config.slot.InventorySlotInfo;
import mekanism.common.tile.prefab.TileEntityConfigurableMachine;
import mekanism.common.util.MekanismUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RestorationSwitchTile extends TileEntityConfigurableMachine {


    private MachineEnergyContainer<RestorationSwitchTile> energyContainer;
    private BasicInventorySlot repairSlot;
    private EnergyInventorySlot energySlot;
    private final RepairJob job = new RepairJob();
    private ItemStack lastSeenStack = ItemStack.EMPTY;
    private boolean repairCancelled;

    public RestorationSwitchTile(BlockPos pos, BlockState state) {
        super(MeksRegistries.RESTORATION_SWITCH_BLOCK, pos, state);
        configComponent.setupItemIOConfig(List.of((IInventorySlot) repairSlot), List.of((IInventorySlot) repairSlot), energySlot, false);
        configComponent.setupInputConfig(TransmissionType.ENERGY, energyContainer);
        configComponent.addDisabledSides(RelativeSide.FRONT);
        configComponent.getConfig(TransmissionType.ITEM).setEjecting(true);
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
        RestorationInventoryHolder holder = new RestorationInventoryHolder();
        repairSlot = new SingleItemSlot(this::canInsertItem, this::canExtractItem, this::isDamageableItem, contentsListener(listener), 80, 20);
        repairSlot.setSlotType(ContainerSlotType.NORMAL);
        holder.addSlot(repairSlot);
        holder.addSlot(energySlot = EnergyInventorySlot.fillOrConvert(energyContainer, this::getLevel, listener, 80, 84));
        return holder;
    }

    @Override
    protected boolean onUpdateServer() {
        boolean sendUpdatePacket = super.onUpdateServer();
        energySlot.fillContainerOrConvert();
        if (canFunction()) {
            process();
        } else {
            setActive(false);
        }
        return sendUpdatePacket;
    }

    private void process() {
        Player owner = getOwnerPlayer();
        if (owner == null) {
            setActive(false);
            return;
        }
        ItemStack current = repairSlot.getStack();
        if (current.isEmpty() || !isDamageableItem(current)) {
            resetJob();
            clearStats();
            setActive(false);
            return;
        }
        if (itemChanged(current)) {
            resetJob();
            clearStats();
            lastSeenStack = current.copy();
        } else if (damageChanged(current)) {
            resetJob();
            lastSeenStack = current.copy();
        }
        if (repairCancelled) {
            setActive(false);
            return;
        }
        if (!job.active && !beginAttempt(owner)) {
            setActive(false);
            return;
        }
        tickAttempt(owner);
        setActive(job.active);
    }

    private boolean beginAttempt(Player owner) {
        ItemStack stack = repairSlot.getStack();
        if (stack.isEmpty() || !canRepairItem(stack)) {
            resetJob();
            return false;
        }
        long value = MeksValues.getValue(stack.getItem());
        int minTicks = Config.RESTORATION_MIN_TICKS.get();
        int maxTicks = Math.max(minTicks, Config.RESTORATION_MAX_TICKS.get());
        long svCost = value > 0 ? Math.max(1, (value + 99) / 100) : Config.RESTORATION_FALLBACK_SV_COST.get();
        if (getData(owner).getSv() < svCost) {
            job.active = false;
            setActive(false);
            return false;
        }
        long energyTotal = svCost * Config.RESTORATION_ENERGY_PER_SV.get();
        int baseTicks = (int) Math.min(maxTicks, Math.max(minTicks, 20 + (long) (svCost * Config.RESTORATION_TICKS_PER_SV.get())));
        long baseEnergyPerTick = Math.max(1, energyTotal / Math.max(1, baseTicks));
        job.active = true;
        job.svCost = svCost;
        job.energyTotal = energyTotal;
        job.ticksRequired = MekanismUtils.getTicks(this, baseTicks);
        job.energyPerTick = MekanismUtils.getEnergyPerTick(this, baseEnergyPerTick);
        job.operatingTicks = 0;
        job.svRemaining = svCost;
        job.energyRemaining = energyTotal;
        job.chance = computeChance(stack, job.failBoost);
        setActive(true);
        markForSave();
        return true;
    }

    private void tickAttempt(Player owner) {
        if (job.ticksRequired <= 0) {
            return;
        }
        long svPerTick = proportional(job.svCost, job.svRemaining, job.operatingTicks, job.ticksRequired);
        long proportionalEnergy = proportional(job.energyTotal, job.energyRemaining, job.operatingTicks, job.ticksRequired);
        long energyPerTick = Math.max(0, proportionalEnergy);
        PlayerExchangeData data = getData(owner);
        if (data.getSv() < svPerTick || energyContainer.getEnergy() < energyPerTick) {
            setActive(false);
            return;
        }
        data.consumeSv(svPerTick);
        setData(owner, data);
        energyContainer.extract(energyPerTick, Action.EXECUTE, AutomationType.INTERNAL);
        job.svRemaining -= svPerTick;
        job.energyRemaining -= energyPerTick;
        job.operatingTicks++;
        if (job.operatingTicks >= job.ticksRequired) {
            completeAttempt(owner);
        }
        markForSave();
    }

    private void completeAttempt(Player owner) {
        ItemStack stack = repairSlot.getStack();
        if (stack.isEmpty() || !canRepairItem(stack)) {
            resetJob();
            clearStats();
            return;
        }
        if (level.random.nextInt(100) < job.chance) {
            ItemStack repaired = stack.copy();
            repaired.setDamageValue(Math.max(0, repaired.getDamageValue() - 1));
            repairSlot.setStack(repaired);
            lastSeenStack = repaired.copy();
            job.failBoost = 0;
            job.successCount++;
            if (repaired.getDamageValue() <= 0) {
                resetJob();
                setActive(false);
                markForSave();
                return;
            }
        } else {
            job.failBoost = Math.min(100, job.failBoost + 1);
            job.failCount++;
        }
        job.operatingTicks = 0;
        job.svRemaining = 0;
        job.energyRemaining = 0;
        job.active = false;
        beginAttempt(owner);
        markForSave();
    }

    private static long proportional(long total, long remaining, int done, int ticks) {
        long expected = total * (done + 1L) / ticks;
        long consumed = total - remaining;
        return Math.max(0, expected - consumed);
    }

    private static int computeChance(ItemStack stack, int boost) {
        int maxDamage = stack.getMaxDamage();
        if (maxDamage <= 0) {
            return 100;
        }
        int remaining = Math.max(0, maxDamage - stack.getDamageValue());
        int base = (remaining * 100 + maxDamage - 1) / maxDamage;
        return Math.min(100, base + boost);
    }

    private boolean stackChanged(ItemStack current) {
        return lastSeenStack.isEmpty()
              || !ItemStack.isSameItemSameComponents(lastSeenStack, current)
              || lastSeenStack.getDamageValue() != current.getDamageValue();
    }

    private boolean itemChanged(ItemStack current) {
        return lastSeenStack.isEmpty() || !ItemStack.isSameItemSameComponents(lastSeenStack, current);
    }

    private boolean damageChanged(ItemStack current) {
        return !lastSeenStack.isEmpty() && lastSeenStack.getDamageValue() != current.getDamageValue();
    }

    private void resetJob() {
        job.active = false;
        job.operatingTicks = 0;
        job.ticksRequired = 0;
        job.svCost = 0;
        job.energyTotal = 0;
        job.energyPerTick = 0;
        job.svRemaining = 0;
        job.energyRemaining = 0;
        job.chance = 0;
        job.failBoost = 0;
    }

    private void clearStats() {
        job.successCount = 0;
        job.failCount = 0;
    }

    private boolean canRepairItem(ItemStack stack) {
        return !stack.isEmpty() && stack.isDamageableItem() && stack.getDamageValue() > 0;
    }

    private boolean canInsertItem(ItemStack stack, AutomationType type) {
        if (stack.isEmpty() || !stack.isDamageableItem()) {
            return false;
        }
        return type == AutomationType.MANUAL || stack.getDamageValue() > 0;
    }

    private boolean isDamageableItem(ItemStack stack) {
        return !stack.isEmpty() && stack.isDamageableItem();
    }

    private boolean canExtractItem(ItemStack stack, AutomationType type) {
        return type == AutomationType.INTERNAL || repairCancelled || stack.getDamageValue() <= 0;
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

    @Override
    public void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putBoolean("repairCancelled", repairCancelled);
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        repairCancelled = tag.getBoolean("repairCancelled");
    }

    @Override
    public void open(Player player) {
        TileComponentSecurity security = getSecurity();
        if (security == null || !Objects.equals(security.getOwnerUUID(), player.getUUID())) {
            return;
        }
        super.open(player);
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
    public void recalculateUpgrades(Upgrade upgrade) {
        super.recalculateUpgrades(upgrade);
        if (job.active) {
            int minTicks = Config.RESTORATION_MIN_TICKS.get();
            int maxTicks = Math.max(minTicks, Config.RESTORATION_MAX_TICKS.get());
            int baseTicks = (int) Math.min(maxTicks, Math.max(minTicks, 20 + (long) (job.svCost * Config.RESTORATION_TICKS_PER_SV.get())));
            job.ticksRequired = MekanismUtils.getTicks(this, baseTicks);
            job.energyPerTick = MekanismUtils.getEnergyPerTick(this,
                  Math.max(1, (job.svCost * Config.RESTORATION_ENERGY_PER_SV.get()) / Math.max(1, baseTicks)));
        }
        energyContainer.updateMaxEnergy();
    }

    @Override
    public void addContainerTrackers(MekanismContainer container) {
        super.addContainerTrackers(container);
        RestorationSwitchContainer restorationContainer = (RestorationSwitchContainer) container;
        container.track(SyncableBoolean.create(() -> job.active, value -> job.active = value));
        container.track(SyncableInt.create(() -> job.operatingTicks, value -> job.operatingTicks = value));
        container.track(SyncableInt.create(() -> job.ticksRequired, value -> job.ticksRequired = value));
        container.track(SyncableLong.create(() -> job.svCost, value -> job.svCost = value));
        container.track(SyncableInt.create(() -> job.chance, value -> job.chance = value));
        container.track(SyncableInt.create(() -> job.successCount, value -> job.successCount = value));
        container.track(SyncableInt.create(() -> job.failCount, value -> job.failCount = value));
        container.track(SyncableLong.create(() -> restorationContainer.getPlayer().getData(MeksAttachments.EXCHANGE_DATA).getSv(), restorationContainer::setSv));
    }

    public double getScaledProgress() {
        return job.ticksRequired <= 0 ? 0 : Math.min(1, job.operatingTicks / (double) job.ticksRequired);
    }

    public long getSvCost() {
        return job.svCost;
    }

    public int getRepairChance() {
        return job.chance;
    }

    public int getSuccessCount() {
        return job.successCount;
    }

    public int getFailCount() {
        return job.failCount;
    }

    public boolean isRepairing() {
        return job.active;
    }

    public void cancelRepair() {
        if (level == null || level.isClientSide) {
            return;
        }
        resetJob();
        clearStats();
        repairCancelled = true;
        setActive(false);
        markForSave();
    }

    public MachineEnergyContainer<RestorationSwitchTile> getEnergyContainer() {
        return energyContainer;
    }

    public ItemStack getRepairSlotStack() {
        return repairSlot == null ? ItemStack.EMPTY : repairSlot.getStack();
    }

    private IContentsListener contentsListener(IContentsListener outer) {
        return () -> {
            ItemStack stack = repairSlot == null ? ItemStack.EMPTY : repairSlot.getStack();
            if (stack.isEmpty() || stackChanged(stack)) {
                repairCancelled = false;
            }
            outer.onContentsChanged();
        };
    }

    private class RestorationInventoryHolder extends ConfigHolder<IInventorySlot> implements IInventorySlotHolder {

        private RestorationInventoryHolder() {
            super(RestorationSwitchTile.this);
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
                return slots;
            }
            return getSlots(direction, slotInfo -> {
                if (slotInfo instanceof InventorySlotInfo inventorySlotInfo) {
                    return inventorySlotInfo.getSlots();
                }
                return List.of();
            });
        }
    }

    private static class SingleItemSlot extends BasicInventorySlot {

        private SingleItemSlot(BiPredicate<ItemStack, AutomationType> canInsert, BiPredicate<ItemStack, AutomationType> canExtract,
              Predicate<ItemStack> validator, IContentsListener listener, int x, int y) {
            super(1, canInsert, canExtract, validator, listener, x, y);
        }
    }

    private static class RepairJob {

        private boolean active;
        private int operatingTicks;
        private int ticksRequired;
        private long svCost;
        private long energyTotal;
        private long energyPerTick;
        private long svRemaining;
        private long energyRemaining;
        private int chance;
        private int failBoost;
        private int successCount;
        private int failCount;
    }
}
