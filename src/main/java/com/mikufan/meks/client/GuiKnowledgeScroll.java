package com.mikufan.meks.client;

import com.mikufan.meks.KnowledgeEntry;
import com.mikufan.meks.MeksValues;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiElement;
import mekanism.client.gui.element.scroll.GuiScrollBar;
import mekanism.client.gui.element.slot.GuiSlot;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import mekanism.common.util.text.TextUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

public class GuiKnowledgeScroll extends GuiElement {

    private static final ResourceLocation SLOTS = MekanismUtils.getResource(ResourceType.GUI_SLOT, "slots.png");

    private final GuiScrollBar scrollBar;
    private final int xSlots;
    private final int ySlots;
    private final Supplier<List<KnowledgeEntry>> slotList;
    private final Map<ResourceLocation, SearchData> searchCache = new HashMap<>();
    private String filter = "";

    public GuiKnowledgeScroll(IGuiWrapper gui, int x, int y, int xSlots, int ySlots, Supplier<List<KnowledgeEntry>> slotList) {
        super(gui, x, y, xSlots * 18 + 18, ySlots * 18);
        this.xSlots = xSlots;
        this.ySlots = ySlots;
        this.slotList = slotList;
        scrollBar = addChild(new GuiScrollBar(gui, relativeX + xSlots * 18 + 4, y, ySlots * 18,
              () -> Mth.ceil((double) getFilteredList().size() / xSlots), () -> ySlots));
    }

    public void setFilter(String filter) {
        this.filter = filter == null ? "" : filter.toLowerCase(Locale.ROOT);
    }

    public int getScrollBarRightEdge() {
        return relativeX + xSlots * 18 + 18;
    }

    @Override
    public void drawBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.drawBackground(guiGraphics, mouseX, mouseY, partialTicks);
        guiGraphics.blit(SLOTS, relativeX, relativeY, 0, 0, xSlots * 18, ySlots * 18, 288, 288);
        List<KnowledgeEntry> list = getFilteredList();
        if (!list.isEmpty()) {
            int slotStart = scrollBar.getCurrentSelection() * xSlots;
            int max = xSlots * ySlots;
            for (int i = 0; i < max; i++) {
                int index = slotStart + i;
                if (index >= list.size()) {
                    break;
                }
                ItemStack stack = list.get(index).getInternalStack();
                if (!stack.isEmpty()) {
                    gui().renderItemWithOverlay(guiGraphics, stack, relativeX + 18 * (i % xSlots) + 1, relativeY + 18 * (i / xSlots) + 1, 1, "");
                }
            }
        }
    }

    @Override
    public void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderForeground(guiGraphics, mouseX, mouseY);
        int xAxis = mouseX - getGuiLeft();
        int yAxis = mouseY - getGuiTop();
        int slotX = (xAxis - relativeX) / 18;
        int slotY = (yAxis - relativeY) / 18;
        if (slotX >= 0 && slotY >= 0 && slotX < xSlots && slotY < ySlots && checkWindows(mouseX, mouseY)) {
            int startX = relativeX + slotX * 18 + 1;
            int startY = relativeY + slotY * 18 + 1;
            if (xAxis >= startX && xAxis < startX + 16 && yAxis >= startY && yAxis < startY + 16) {
                guiGraphics.fill(RenderType.guiOverlay(), startX, startY, startX + 16, startY + 16, GuiSlot.DEFAULT_HOVER_COLOR);
            }
        }
    }

    @Override
    public void renderToolTip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderToolTip(guiGraphics, mouseX, mouseY);
        KnowledgeEntry entry = getEntryAt(mouseX, mouseY);
        if (entry != null) {
            long value = MeksValues.getValue(entry.getInternalStack().getItem());
            gui().renderItemTooltipWithExtra(guiGraphics, entry.getInternalStack(), mouseX, mouseY,
                  List.of(Component.translatable("gui.meks.sv_value", TextUtils.format(value))));
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double xDelta, double yDelta) {
        return scrollBar.adjustScroll(yDelta) || super.mouseScrolled(mouseX, mouseY, xDelta, yDelta);
    }

    @Nullable
    public KnowledgeEntry getEntryAt(double mouseX, double mouseY) {
        List<KnowledgeEntry> list = getFilteredList();
        if (list.isEmpty()) {
            return null;
        }
        int slotX = (int) ((mouseX - getX()) / 18);
        int slotY = (int) ((mouseY - getY()) / 18);
        if (slotX < 0 || slotY < 0 || slotX >= xSlots || slotY >= ySlots) {
            return null;
        }
        int startX = getX() + slotX * 18 + 1;
        int startY = getY() + slotY * 18 + 1;
        if (mouseX < startX || mouseX >= startX + 16 || mouseY < startY || mouseY >= startY + 16) {
            return null;
        }
        int index = (slotY + scrollBar.getCurrentSelection()) * xSlots + slotX;
        return index >= 0 && index < list.size() ? list.get(index) : null;
    }

    private List<KnowledgeEntry> getFilteredList() {
        List<KnowledgeEntry> list = slotList.get();
        if (filter.isBlank()) {
            return list;
        }
        String[] tokens = filter.trim().split("\\s+");
        return list.stream()
              .filter(entry -> matches(entry, tokens))
              .toList();
    }

    private boolean matches(KnowledgeEntry entry, String[] tokens) {
        SearchData data = getSearchData(entry);
        for (String token : tokens) {
            if (!matchesToken(data, token)) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesToken(SearchData data, String token) {
        if (token.length() > 1) {
            String term = token.substring(1);
            return switch (token.charAt(0)) {
                case '@' -> fuzzyMatches(data.modId(), term) || fuzzyMatches(data.modName(), term);
                case '#' -> fuzzyMatches(data.tags(), term);
                case '^' -> fuzzyMatches(data.tooltip(), term);
                default -> fuzzyMatches(data.displayName(), token);
            };
        }
        return fuzzyMatches(data.displayName(), token);
    }

    private static boolean fuzzyMatches(String text, String term) {
        if (term.isEmpty()) {
            return true;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        int termIndex = 0;
        int termLength = term.length();
        for (int i = 0; i < lower.length() && termIndex < termLength; i++) {
            if (lower.charAt(i) == term.charAt(termIndex)) {
                termIndex++;
            }
        }
        return termIndex == termLength;
    }

    private SearchData getSearchData(KnowledgeEntry entry) {
        return searchCache.computeIfAbsent(entry.key(), key -> {
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(key));
            Item item = stack.getItem();
            String modId = key.getNamespace();
            String modName = ModList.get().getModContainerById(modId)
                  .map(container -> container.getModInfo().getDisplayName())
                  .orElse(modId);
            String tags = stack.getTags().map(tag -> tag.location().toString()).collect(Collectors.joining(" "));
            String tooltip = "";
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level != null && minecraft.player != null) {
                tooltip = stack.getTooltipLines(Item.TooltipContext.of(minecraft.level), minecraft.player, TooltipFlag.NORMAL)
                      .stream().map(Component::getString).collect(Collectors.joining(" "));
            }
            return new SearchData(stack.getHoverName().getString(), modId, modName, tags, tooltip);
        });
    }

    private record SearchData(String displayName, String modId, String modName, String tags, String tooltip) {
    }
}
