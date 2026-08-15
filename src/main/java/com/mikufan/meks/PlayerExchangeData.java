package com.mikufan.meks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public class PlayerExchangeData {

    public static final Codec<PlayerExchangeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("sv").forGetter(data -> data.sv),
            ResourceLocation.CODEC.listOf()
                  .xmap(list -> (Set<ResourceLocation>) new HashSet<>(list), ArrayList::new)
                  .fieldOf("knowledge")
                  .forGetter(data -> data.knowledge)
    ).apply(instance, PlayerExchangeData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerExchangeData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, PlayerExchangeData::getSv,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.collection(HashSet::new)), PlayerExchangeData::getKnowledge,
            PlayerExchangeData::new
    );

    private long sv;
    private final Set<ResourceLocation> knowledge;

    public PlayerExchangeData() {
        this(0L, new HashSet<>());
    }

    private PlayerExchangeData(long sv, Set<ResourceLocation> knowledge) {
        this.sv = sv;
        this.knowledge = knowledge;
    }

    public long getSv() {
        return sv;
    }

    public void setSv(long value) {
        sv = value;
    }

    public void addSv(long amount) {
        sv += amount;
    }

    public boolean consumeSv(long amount) {
        if (sv < amount) {
            return false;
        }
        sv -= amount;
        return true;
    }

    public Set<ResourceLocation> getKnowledge() {
        return knowledge;
    }

    public boolean hasKnowledge(ResourceLocation key) {
        return knowledge.contains(key);
    }

    public boolean learn(ResourceLocation key) {
        return knowledge.add(key);
    }

    public boolean forget(ResourceLocation key) {
        return knowledge.remove(key);
    }

    public PlayerExchangeData copy() {
        return new PlayerExchangeData(sv, new HashSet<>(knowledge));
    }
}
