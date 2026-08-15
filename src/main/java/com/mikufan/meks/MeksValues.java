package com.mikufan.meks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

public final class MeksValues {

    private MeksValues() {
    }

    private static final int MAX_DERIVATION_PASSES = 64;

    private static final Map<ResourceLocation, Long> BASE_VALUES = new HashMap<>();
    private static final Map<ResourceLocation, Long> DERIVED_VALUES = new HashMap<>();
    private static final Set<ResourceLocation> MAPPED_ITEMS = new HashSet<>();
    private static boolean initialized;

    static {
        add("minecraft:coal", 128);
        add("minecraft:charcoal", 128);
        add("minecraft:coal_block", 1152);
        add("minecraft:iron_ingot", 256);
        add("minecraft:iron_nugget", 28);
        add("minecraft:iron_block", 2304);
        add("minecraft:raw_iron", 192);
        add("minecraft:iron_ore", 384);
        add("minecraft:deepslate_iron_ore", 384);
        add("minecraft:gold_ingot", 512);
        add("minecraft:gold_nugget", 57);
        add("minecraft:gold_block", 4608);
        add("minecraft:raw_gold", 384);
        add("minecraft:gold_ore", 768);
        add("minecraft:deepslate_gold_ore", 768);
        add("minecraft:diamond", 8192);
        add("minecraft:diamond_block", 73728);
        add("minecraft:diamond_ore", 12288);
        add("minecraft:deepslate_diamond_ore", 12288);
        add("minecraft:emerald", 16384);
        add("minecraft:emerald_block", 147456);
        add("minecraft:redstone", 64);
        add("minecraft:redstone_block", 576);
        add("minecraft:lapis_lazuli", 864);
        add("minecraft:lapis_block", 7776);
        add("minecraft:quartz", 256);
        add("minecraft:quartz_block", 2304);
        add("minecraft:copper_ingot", 85);
        add("minecraft:copper_block", 765);
        add("minecraft:raw_copper", 64);
        add("minecraft:copper_ore", 128);
        add("minecraft:deepslate_copper_ore", 128);
        add("minecraft:netherite_scrap", 4096);
        add("minecraft:netherite_ingot", 73728);
        add("minecraft:netherite_block", 663552);
        add("minecraft:stick", 4);
        add("minecraft:oak_planks", 8);
        add("minecraft:spruce_planks", 8);
        add("minecraft:birch_planks", 8);
        add("minecraft:jungle_planks", 8);
        add("minecraft:acacia_planks", 8);
        add("minecraft:dark_oak_planks", 8);
        add("minecraft:mangrove_planks", 8);
        add("minecraft:cherry_planks", 8);
        add("minecraft:bamboo_planks", 8);
        add("minecraft:oak_log", 32);
        add("minecraft:spruce_log", 32);
        add("minecraft:birch_log", 32);
        add("minecraft:jungle_log", 32);
        add("minecraft:acacia_log", 32);
        add("minecraft:dark_oak_log", 32);
        add("minecraft:mangrove_log", 32);
        add("minecraft:cherry_log", 32);
        add("minecraft:bamboo_block", 32);
        add("minecraft:cobblestone", 1);
        add("minecraft:stone", 1);
        add("minecraft:dirt", 1);
        add("minecraft:sand", 1);
        add("minecraft:gravel", 1);
        add("minecraft:wheat", 24);
        add("minecraft:bread", 64);
        add("minecraft:apple", 128);
        add("minecraft:carrot", 64);
        add("minecraft:potato", 64);
        add("minecraft:beef", 128);
        add("minecraft:cooked_beef", 512);
        add("minecraft:porkchop", 128);
        add("minecraft:cooked_porkchop", 512);
        add("minecraft:chicken", 128);
        add("minecraft:cooked_chicken", 512);
        add("minecraft:mutton", 128);
        add("minecraft:cooked_mutton", 512);
        add("minecraft:cod", 128);
        add("minecraft:cooked_cod", 256);
        add("minecraft:salmon", 128);
        add("minecraft:cooked_salmon", 256);
        add("minecraft:ender_pearl", 1024);
        add("minecraft:blaze_rod", 1536);
        add("minecraft:ghast_tear", 4096);
        add("mekanism:ingot_osmium", 512);
        add("mekanism:ingot_copper", 85);
        add("mekanism:ingot_tin", 256);
        add("mekanism:ingot_lead", 512);
        add("mekanism:ingot_uranium", 2048);
        add("mekanism:ingot_bronze", 597);
        add("mekanism:ingot_steel", 1280);
        add("mekanism:ingot_refined_obsidian", 4096);
        add("mekanism:alloy_infused", 320);
        add("mekanism:alloy_reinforced", 2048);
        add("mekanism:alloy_atomic", 8192);
        add("mekanism:control_circuit", 1280);
        add("mekanism:advanced_control_circuit", 5120);
        add("mekanism:elite_control_circuit", 20480);
        add("mekanism:ultimate_control_circuit", 81920);
        add("mekanism:teleportation_core", 16384);
        add("mekanism:quantum_entangloporter", 73728);
        MAPPED_ITEMS.addAll(BASE_VALUES.keySet());
    }

    private static void add(String id, long value) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key != null) {
            BASE_VALUES.put(key, value);
        }
    }


    public static long getValue(ItemLike item) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item.asItem());
        Long base = BASE_VALUES.get(key);
        if (base != null) {
            return base;
        }
        Long derived = DERIVED_VALUES.get(key);
        return derived == null ? 0L : derived;
    }

    public static boolean hasValue(ItemLike item) {
        return getValue(item) > 0;
    }

    public static Set<ResourceLocation> getMappedItems() {
        return new HashSet<>(MAPPED_ITEMS);
    }

    public static void ensureInitialized(Level level) {
        if (level == null || initialized) {
            return;
        }
        initialized = true;
        computeDerivedValues(level.registryAccess(), level.getRecipeManager());
    }

    public static void onServerStarted(ServerStartedEvent event) {
        ensureInitialized(event.getServer().overworld());
    }

    public static void onServerStopping(ServerStoppingEvent event) {
        DERIVED_VALUES.clear();
        MAPPED_ITEMS.clear();
        MAPPED_ITEMS.addAll(BASE_VALUES.keySet());
        initialized = false;
    }

    private static void computeDerivedValues(HolderLookup.Provider registries, RecipeManager recipeManager) {
        List<RecipeHolder<CraftingRecipe>> recipes = recipeManager.getAllRecipesFor(RecipeType.CRAFTING).stream()
              .filter(holder -> holder.value() instanceof ShapedRecipe || holder.value() instanceof ShapelessRecipe)
              .toList();
        Map<ResourceLocation, Long> values = new HashMap<>();
        for (int pass = 0; pass < MAX_DERIVATION_PASSES; pass++) {
            boolean changed = false;
            for (RecipeHolder<CraftingRecipe> holder : recipes) {
                CraftingRecipe recipe = holder.value();
                ItemStack output = recipe.getResultItem(registries);
                if (output.isEmpty()) {
                    continue;
                }
                ResourceLocation outKey = BuiltInRegistries.ITEM.getKey(output.getItem());
                if (BASE_VALUES.containsKey(outKey) || values.containsKey(outKey)) {
                    continue;
                }
                long total = 0;
                boolean resolved = true;
                for (Ingredient ingredient : recipe.getIngredients()) {
                    if (ingredient.isEmpty()) {
                        continue;
                    }
                    long ingredientValue = firstKnownValue(ingredient, values);
                    if (ingredientValue <= 0) {
                        resolved = false;
                        break;
                    }
                    total += ingredientValue;
                }
                if (resolved) {
                    long perItem = total / Math.max(1, output.getCount());
                    if (perItem > 0) {
                        values.put(outKey, perItem);
                        changed = true;
                    }
                }
            }
            if (!changed) {
                break;
            }
        }
        DERIVED_VALUES.putAll(values);
        MAPPED_ITEMS.addAll(values.keySet());
    }

    private static long firstKnownValue(Ingredient ingredient, Map<ResourceLocation, Long> values) {
        for (ItemStack stack : ingredient.getItems()) {
            long value = getKnownValue(stack.getItem(), values);
            if (value > 0) {
                return value;
            }
        }
        return 0;
    }

    private static long getKnownValue(ItemLike item, Map<ResourceLocation, Long> values) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item.asItem());
        Long base = BASE_VALUES.get(key);
        if (base != null) {
            return base;
        }
        Long derived = values.get(key);
        return derived == null ? 0L : derived;
    }
}
