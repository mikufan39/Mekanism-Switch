package com.mikufan.meks.client;

import com.mikufan.meks.MekanismSwitch;
import com.mikufan.meks.MeksValues;
import com.mikufan.meks.RestorationSwitchContainer;
import com.mikufan.meks.RestorationSwitchTile;
import mekanism.client.gui.GuiConfigurableTile;
import org.joml.Quaternionf;
import mekanism.client.gui.element.progress.ProgressType;
import mekanism.client.gui.element.tab.GuiSecurityTab;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.util.text.TextUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class GuiRestorationSwitch extends GuiConfigurableTile<RestorationSwitchTile, RestorationSwitchContainer> {

    private static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(MekanismSwitch.MODID, "gui/restoration_switch.png");

    private static final int SLOT_X = 80;
    private static final int SLOT_Y = 20;
    private static final int ENERGY_SLOT_Y = 84;
    private static final int PROGRESS_X = 85;
    private static final int PROGRESS_Y = 53;
    private static final int PROGRESS_WIDTH = 8;
    private static final int PROGRESS_HEIGHT = 17;
    private static final ResourceLocation PROGRESS_TEXTURE = ProgressType.BAR.getTexture();

    public GuiRestorationSwitch(RestorationSwitchContainer container, Inventory inv, Component title) {
        super(container, inv, title);
        imageWidth = 176;
        imageHeight = 200;
        inventoryLabelY = 114;
        titleLabelY = 6;
        dynamicSlots = true;
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        renderables.removeIf(element -> element instanceof GuiSecurityTab);
        children().removeIf(element -> element instanceof GuiSecurityTab);
        addRenderableWidget(new GuiRestorationEnergyBar(this, tile.getEnergyContainer(), 158, 20, 82));
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        MekanismRenderer.resetColor(guiGraphics);
        guiGraphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        renderVerticalProgress(guiGraphics);
    }

    @Override
    protected void drawForegroundText(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderTitleText(guiGraphics);
        ItemStack stack = tile.getRepairSlotStack();
        if (!stack.isEmpty() && stack.isDamageableItem()) {
            int centerX = SLOT_X / 2;
            int slotBottom = SLOT_Y + 18;
            Component name = stack.getHoverName();
            guiGraphics.drawString(font(), name, centerX - font.width(name) / 2, SLOT_Y, 0x000000, false);
            int maxDamage = stack.getMaxDamage();
            int remaining = Math.max(0, maxDamage - stack.getDamageValue());
            String durability = remaining + "/" + maxDamage;
            guiGraphics.drawString(font(), Component.literal(durability),
                  centerX - font.width(durability) / 2, slotBottom - font.lineHeight, 0x000000, false);
            if (stack.getDamageValue() > 0) {
                drawRepairInfo(guiGraphics, stack);
            }
            drawStats(guiGraphics);
        }
        drawSvText(guiGraphics);
        super.drawForegroundText(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTitleText(@NotNull GuiGraphics guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();
        Component title = Component.translatable("gui.meks.restoration_switch.title", minecraft.player.getDisplayName());
        int x = imageWidth / 2 - font.width(title) / 2;
        guiGraphics.drawString(font(), title, x, titleLabelY, 0x404040, false);
    }

    private void drawRepairInfo(GuiGraphics guiGraphics, ItemStack stack) {
        long svCost = tile.isRepairing() ? tile.getSvCost() : computeSvCost(stack);
        int chance = tile.isRepairing() ? tile.getRepairChance() : computeChance(stack);
        String svText = Long.toString(svCost);
        String chanceText = chance + "%";
        int totalWidth = font.width(svText) + 3 + font.width(chanceText);
        int startX = PROGRESS_X / 2 - totalWidth / 2;
        int textY = PROGRESS_Y + (PROGRESS_HEIGHT - font.lineHeight) / 2;
        guiGraphics.drawString(font(), Component.literal(svText), startX, textY, 0x000000, false);
        int x = startX + font.width(svText) + 3;
        guiGraphics.drawString(font(), Component.literal(chanceText), x, textY, 0x000000, false);
    }

    private void drawSvText(GuiGraphics guiGraphics) {
        String svText = "SV: " + TextUtils.format(menu.getSv());
        int centerX = SLOT_X / 2;
        int y = ENERGY_SLOT_Y + (18 - font.lineHeight) / 2;
        guiGraphics.drawString(font(), Component.literal(svText), centerX - font.width(svText) / 2, y, 0x55D8A8, false);
    }

    private void drawStats(GuiGraphics guiGraphics) {
        int centerX = PROGRESS_X + PROGRESS_WIDTH + (imageWidth - (PROGRESS_X + PROGRESS_WIDTH)) / 2;
        String success = "成功" + tile.getSuccessCount() + "次";
        String fail = "失败" + tile.getFailCount() + "次";
        guiGraphics.drawString(font(), Component.literal(success), centerX - font.width(success) / 2, PROGRESS_Y, 0x000000, false);
        guiGraphics.drawString(font(), Component.literal(fail), centerX - font.width(fail) / 2, PROGRESS_Y + 9, 0x000000, false);
    }

    private static long computeSvCost(ItemStack stack) {
        long value = MeksValues.getValue(stack.getItem());
        return value > 0 ? Math.max(1, (value + 99) / 100) : 616L;
    }

    private static int computeChance(ItemStack stack) {
        int maxDamage = stack.getMaxDamage();
        if (maxDamage <= 0) {
            return 100;
        }
        int remaining = Math.max(0, maxDamage - stack.getDamageValue());
        return Math.min(100, (remaining * 100 + maxDamage - 1) / maxDamage);
    }

    private void renderVerticalProgress(GuiGraphics guiGraphics) {
        int x = leftPos + PROGRESS_X;
        int y = topPos + PROGRESS_Y;
        guiGraphics.fill(x - 1, y - 1, x + PROGRESS_WIDTH + 1, y + PROGRESS_HEIGHT + 1, 0xFF111111);
        guiGraphics.fill(x, y, x + PROGRESS_WIDTH, y + PROGRESS_HEIGHT, 0xFF3A3A3A);
        double progress = tile.getScaledProgress();
        if (progress > 0) {
            int scaledHeight = Math.max(1, (int) Math.round(PROGRESS_HEIGHT * progress));
            guiGraphics.enableScissor(x, y + PROGRESS_HEIGHT - scaledHeight, x + PROGRESS_WIDTH, y + PROGRESS_HEIGHT);
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(x + PROGRESS_WIDTH / 2.0F, y + PROGRESS_HEIGHT / 2.0F, 0.0F);
            guiGraphics.pose().mulPose(new Quaternionf().rotateZ((float) Math.toRadians(90)));
            guiGraphics.pose().scale(PROGRESS_HEIGHT / 25.0F, PROGRESS_WIDTH / 9.0F, 1.0F);
            guiGraphics.blit(PROGRESS_TEXTURE, -12, -4, 0, 0, 25, 9, 25, 27);
            guiGraphics.pose().popPose();
            guiGraphics.disableScissor();
        }
    }
}
