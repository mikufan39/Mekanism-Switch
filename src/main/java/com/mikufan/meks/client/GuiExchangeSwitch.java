package com.mikufan.meks.client;

import com.mikufan.meks.ExchangeOperation;
import com.mikufan.meks.MeksValues;
import com.mikufan.meks.ExchangeSwitchContainer;
import com.mikufan.meks.ExchangeSwitchContainer.KnowledgeEntry;
import com.mikufan.meks.ExchangeSwitchTile;
import com.mojang.blaze3d.systems.RenderSystem;
import mekanism.client.gui.GuiConfigurableTile;
import mekanism.client.gui.element.tab.GuiSecurityTab;
import mekanism.client.gui.element.text.BackgroundType;
import mekanism.client.gui.element.text.GuiTextField;
import mekanism.common.util.text.TextUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public class GuiExchangeSwitch extends GuiConfigurableTile<ExchangeSwitchTile, ExchangeSwitchContainer> {

    private static final int FORGET_SLOT_X = 8;
    private static final int FORGET_SLOT_Y = 117;
    private static final int PROCESS_SLOT_X = 29;
    private static final int PROCESS_SLOT_Y = 117;
    private static final int CHANNEL_SLOT_X = 50;
    private static final int CHANNEL_SLOT_Y = 117;

    private GuiTextField searchField;
    private GuiKnowledgeScroll knowledgeScroll;
    private KnowledgeEntry dragSource;
    private boolean dragBatch;

    public GuiExchangeSwitch(ExchangeSwitchContainer container, Inventory inv, Component title) {
        super(container, inv, title);
        imageWidth = 176;
        imageHeight = 224;
        inventoryLabelY = 138;
        titleLabelY = 6;
        dynamicSlots = true;
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        renderables.removeIf(element -> element instanceof GuiSecurityTab);
        children().removeIf(element -> element instanceof GuiSecurityTab);
        knowledgeScroll = addRenderableWidget(new GuiKnowledgeScroll(this, 8, 30, 8, 4, () -> menu.getKnowledgeList()));
        MeksValues.ensureInitialized(Minecraft.getInstance().level);
        searchField = addRenderableWidget(new GuiTextField(this, knowledgeScroll.getRelativeX(), 18,
              knowledgeScroll.getScrollBarRightEdge() - knowledgeScroll.getRelativeX(), 10));
        searchField.setOffset(0, -1);
        searchField.setBackground(BackgroundType.ELEMENT_HOLDER);
        searchField.setMaxLength(100);
        searchField.setTextColor(0xFFFFFF);
        searchField.setVisible(true);
        searchField.setResponder(text -> knowledgeScroll.setFilter(text));
        addRenderableWidget(new GuiSwitchPowerBar(this, tile.getEnergyContainer(), 100, 127, 48));
        menu.requestSync();
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(guiGraphics, partialTick, mouseX, mouseY);
    }

    @Override
    protected void renderTitleText(@NotNull GuiGraphics guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();
        Component title = Component.translatable("gui.meks.exchange_switch.title", minecraft.player.getDisplayName());
        int x = imageWidth / 2 - font.width(title) / 2;
        guiGraphics.drawString(font(), title, x, titleLabelY, 0x404040, false);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderProcessFill(guiGraphics);
        if (dragSource == null) {
            return;
        }
        ItemStack stack = dragSource.getInternalStack().copy();
        stack.setCount(dragBatch ? stack.getMaxStackSize() : 1);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos, topPos, 0.0F);
        guiGraphics.pose().translate(0.0F, 0.0F, 500.0F);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        int x = mouseX - leftPos - 8;
        int y = mouseY - topPos - 8;
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.66F);
        guiGraphics.renderItem(stack, x, y);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.renderItemDecorations(font, stack, x, y);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        guiGraphics.pose().popPose();
    }

    @Override
    protected void drawForegroundText(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderTitleText(guiGraphics);
        String svText = "SV: " + TextUtils.format(menu.getSv());
        guiGraphics.drawString(font(), Component.literal(svText), 149 - font.width(svText), 117, 0x55D8A8);
        super.drawForegroundText(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && dragSource != null) {
            dragSource = null;
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT
              && (isOverSlot(mouseX, mouseY, PROCESS_SLOT_X, PROCESS_SLOT_Y)
                  || (tile.hasChannelUpgrade() && isOverSlot(mouseX, mouseY, CHANNEL_SLOT_X, CHANNEL_SLOT_Y)))) {
            menu.cancelExchange();
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            KnowledgeEntry entry = knowledgeScroll.getEntryAt(mouseX, mouseY);
            if (entry != null) {
                dragSource = entry;
                dragBatch = Screen.hasShiftDown();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragSource != null) {
            KnowledgeEntry entry = dragSource;
            dragSource = null;
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                int count = dragBatch ? entry.getInternalStack().getMaxStackSize() : 1;
                if (isOverSlot(mouseX, mouseY, PROCESS_SLOT_X, PROCESS_SLOT_Y)
                      || (tile.hasChannelUpgrade() && isOverSlot(mouseX, mouseY, CHANNEL_SLOT_X, CHANNEL_SLOT_Y))) {
                    int slotIndex = isOverSlot(mouseX, mouseY, CHANNEL_SLOT_X, CHANNEL_SLOT_Y) && tile.hasChannelUpgrade() ? 1 : 0;
                    menu.startExchange(entry.key(), count, false, slotIndex);
                } else if (isOverSlot(mouseX, mouseY, FORGET_SLOT_X, FORGET_SLOT_Y)) {
                    menu.startExchange(entry.key(), 1, true, 0);
                }
            }
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void renderProcessFill(GuiGraphics guiGraphics) {
        renderSlotProgress(guiGraphics, PROCESS_SLOT_X, PROCESS_SLOT_Y, tile.getProcessOperation(),
              tile.getProcessTargetStack(), tile.getProcessOperatingTicks(), tile.getProcessTicksRequired(),
              tile.getProcessSlotStack());
        if (tile.hasChannelUpgrade()) {
            renderSlotProgress(guiGraphics, CHANNEL_SLOT_X, CHANNEL_SLOT_Y, tile.getChannelOperation(),
                  tile.getChannelTargetStack(), tile.getChannelOperatingTicks(), tile.getChannelTicksRequired(),
                  tile.getChannelSlotStack());
        }
        renderSlotProgress(guiGraphics, FORGET_SLOT_X, FORGET_SLOT_Y, tile.getForgetOperation(),
              tile.getForgetTargetStack(), tile.getForgetOperatingTicks(), tile.getForgetTicksRequired(),
              tile.getForgetSlotStack());
    }

    private void renderSlotProgress(GuiGraphics guiGraphics, int slotX, int slotY, ExchangeOperation operation,
          ItemStack targetStack, int operatingTicks, int ticksRequired, ItemStack displayStack) {
        if (operation == ExchangeOperation.NONE || (operation != ExchangeOperation.UPLOAD && targetStack.isEmpty())) {
            return;
        }
        int x = getGuiLeft() + slotX;
        int y = getGuiTop() + slotY;
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        if (operation != ExchangeOperation.UPLOAD && displayStack.isEmpty()) {
            guiGraphics.renderItem(targetStack, x, y);
        }
        if (ticksRequired > 0) {
            float progress = Math.min(1.0F, operatingTicks / (float) ticksRequired);
            int height = Math.round(16.0F * progress);
            if (height > 0) {
                if (operation == ExchangeOperation.UPLOAD) {
                    guiGraphics.enableScissor(x, y + 16 - height, x + 16, y + 16);
                } else {
                    guiGraphics.enableScissor(x, y, x + 16, y + height);
                }
                guiGraphics.fill(x + 1, y + 1, x + 17, y + 17, 0x80FFFFFF);
                guiGraphics.disableScissor();
            }
        }
        if (!displayStack.isEmpty()) {
            guiGraphics.renderItemDecorations(font, displayStack, x, y);
        } else if (operation != ExchangeOperation.UPLOAD) {
            guiGraphics.renderItemDecorations(font, targetStack, x, y);
        }
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private boolean isOverSlot(double mouseX, double mouseY, int slotX, int slotY) {
        int x = getGuiLeft() + slotX;
        int y = getGuiTop() + slotY;
        return mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
    }
}
