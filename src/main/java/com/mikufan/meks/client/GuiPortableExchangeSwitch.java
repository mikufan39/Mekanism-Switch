package com.mikufan.meks.client;

import com.mikufan.meks.KnowledgeEntry;
import com.mikufan.meks.MekanismSwitch;
import com.mikufan.meks.MeksValues;
import com.mikufan.meks.PortableExchangeSwitchContainer;
import mekanism.client.gui.GuiMekanism;
import mekanism.client.gui.element.text.BackgroundType;
import mekanism.client.gui.element.text.GuiTextField;
import mekanism.common.util.text.TextUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

/**
 * GUI of the Portable Exchange Switch — a stripped-down version of the block
 * Exchange Switch GUI: an 8x5 scrollable knowledge library (same slot size and
 * left alignment as the player inventory), no machine slots, no energy, no
 * side configuration and no upgrades. Left-clicking an inventory slot uploads
 * 1 item (shift: the whole stack) instantly; clicking a library entry
 * deducts SV and generates the real item into the inventory: 1 (left-click),
 * full stack (shift+left), half stack (shift+right), or 1 + forget the entry
 * (plain right-click, forgotten only after the download succeeded).
 */
public class GuiPortableExchangeSwitch extends GuiMekanism<PortableExchangeSwitchContainer> {

    private static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(MekanismSwitch.MODID, "gui/portable_exchange_switch.png");

    private static final int LIBRARY_X = 8;
    private static final int LIBRARY_Y = 30;
    private static final int LIBRARY_COLUMNS = 8;
    private static final int LIBRARY_ROWS = 5;
    /** Top y of the first player inventory slot row (must match container). */
    private static final int INVENTORY_SLOT_TOP = 136;

    private GuiTextField searchField;
    private GuiKnowledgeScroll knowledgeScroll;

    public GuiPortableExchangeSwitch(PortableExchangeSwitchContainer container, Inventory inv, Component title) {
        super(container, inv, title);
        imageWidth = 176;
        imageHeight = 212;
        inventoryLabelY = 126;
        titleLabelY = 6;
        dynamicSlots = true;
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        knowledgeScroll = addRenderableWidget(new GuiKnowledgeScroll(this, LIBRARY_X, LIBRARY_Y, LIBRARY_COLUMNS, LIBRARY_ROWS,
              () -> menu.getKnowledgeList()));
        MeksValues.ensureInitialized(Minecraft.getInstance().level);
        searchField = addRenderableWidget(new GuiTextField(this, knowledgeScroll.getRelativeX(), 18,
              knowledgeScroll.getScrollBarRightEdge() - knowledgeScroll.getRelativeX(), 10));
        searchField.setOffset(0, -1);
        searchField.setBackground(BackgroundType.ELEMENT_HOLDER);
        searchField.setMaxLength(100);
        searchField.setTextColor(0xFFFFFF);
        searchField.setVisible(true);
        searchField.setResponder(text -> knowledgeScroll.setFilter(text));
        menu.requestSync();
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // The GUI is 280 px tall (8-row library), taller than Mekanism's 256 px
        // base background, so a dedicated background texture is drawn directly.
        mekanism.client.render.MekanismRenderer.resetColor(guiGraphics);
        guiGraphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
    }

    @Override
    protected void renderTitleText(@NotNull GuiGraphics guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();
        Component title = Component.translatable("gui.meks.portable_exchange_switch.title", minecraft.player.getDisplayName());
        int x = imageWidth / 2 - font.width(title) / 2;
        guiGraphics.drawString(font(), title, x, titleLabelY, 0x404040, false);
    }

    @Override
    protected void drawForegroundText(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderTitleText(guiGraphics);
        // SV is vertically centered in the 16 px gap between the library
        // (bottom y=120) and the player inventory top slot (y=136), and
        // horizontally centered on the whole GUI. The vanilla "Inventory"
        // label is intentionally not rendered here (matches the block switch).
        String svText = "SV: " + TextUtils.format(menu.getSv());
        int libraryBottom = LIBRARY_Y + LIBRARY_ROWS * 18;
        int gap = INVENTORY_SLOT_TOP - libraryBottom;
        int y = libraryBottom + Math.max(0, (gap - font.lineHeight) / 2);
        guiGraphics.drawString(font(), Component.literal(svText), imageWidth / 2 - font.width(svText) / 2, y, 0x55D8A8, false);
        super.drawForegroundText(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            // Clicking a library entry downloads it into the inventory.
            KnowledgeEntry entry = knowledgeScroll.getEntryAt(mouseX, mouseY);
            if (entry != null) {
                int count = Screen.hasShiftDown() ? entry.getInternalStack().getMaxStackSize() : 1;
                menu.startDownload(entry.key(), count);
                return true;
            }
            // Clicking one of the player's own inventory slots uploads it to the library.
            Slot slot = findSlot(mouseX, mouseY);
            if (slot != null && isPlayerInventorySlot(slot) && !slot.getItem().isEmpty()) {
                ItemStack stack = slot.getItem();
                int count = Screen.hasShiftDown() ? stack.getCount() : 1;
                menu.startUpload(slot.getContainerSlot(), count);
                return true;
            }
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            // Right-click without shift downloads 1 item and forgets the entry
            // afterwards (the server forgets only after the download succeeds,
            // so failed downloads never delete knowledge).
            KnowledgeEntry entry = knowledgeScroll.getEntryAt(mouseX, mouseY);
            if (entry != null) {
                if (Screen.hasShiftDown()) {
                    // Shift + right-click downloads half the max stack (rounded
                    // up) like vanilla "take half", without forgetting.
                    int max = entry.getInternalStack().getMaxStackSize();
                    menu.startDownload(entry.key(), (max + 1) / 2, false);
                } else {
                    menu.startDownload(entry.key(), 1, true);
                }
                return true;
            }
            // Swallow right-clicks on inventory slots so vanilla "take half a
            // stack into the cursor" can not conflict with the instant upload.
            Slot slot = findSlot(mouseX, mouseY);
            if (slot != null && isPlayerInventorySlot(slot)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isPlayerInventorySlot(Slot slot) {
        // This container only ever adds the player's main inventory and hot-bar
        // slots; any slot that reached the GUI is one of those.
        return slot.getContainerSlot() < 36;
    }
}