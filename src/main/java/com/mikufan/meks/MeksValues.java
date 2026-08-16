package com.mikufan.meks;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
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

    private static final String[] PRESET_FILES = {
            "emc_preset.json",
            "cataclysm_preset.json",
            "twilightforest_preset.json",
            "iceandfire_preset.json"
    };

    private static final Map<ResourceLocation, Long> BASE_VALUES = new HashMap<>();
    private static final Map<ResourceLocation, Long> DERIVED_VALUES = new HashMap<>();
    private static final Set<ResourceLocation> MAPPED_ITEMS = new HashSet<>();
    private static boolean initialized;

    static {
        loadPreset();
    }

    /**
     * Loads all SV preset files shipped with the mod. Component-specific entries are skipped
     * because SV values are keyed by item id only.
     */
    private static void loadPreset() {
        for (String file : PRESET_FILES) {
            try (InputStream in = MeksValues.class.getResourceAsStream("/data/meks/sv/" + file)) {
                if (in == null) {
                    continue;
                }
                try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    JsonArray entries = JsonParser.parseReader(reader).getAsJsonArray();
                    for (JsonElement element : entries) {
                        JsonObject entry = element.getAsJsonObject();
                        if (entry.has("data")) {
                            continue;
                        }
                        ResourceLocation key = ResourceLocation.tryParse(entry.get("item").getAsString());
                        long value = entry.get("emc").getAsLong();
                        if (key != null && value > 0) {
                            BASE_VALUES.put(key, value);
                        }
                    }
                }
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load SV preset: " + file, e);
            }
        }
        MAPPED_ITEMS.addAll(BASE_VALUES.keySet());
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
