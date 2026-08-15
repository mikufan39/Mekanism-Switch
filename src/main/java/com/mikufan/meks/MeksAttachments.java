package com.mikufan.meks;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class MeksAttachments {

    private MeksAttachments() {
    }

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MekanismSwitch.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerExchangeData>> EXCHANGE_DATA =
            ATTACHMENT_TYPES.register("exchange_data",
                  () -> AttachmentType.builder(PlayerExchangeData::new)
                        .serialize(PlayerExchangeData.CODEC)
                        .copyHandler((data, holder, provider) -> data.copy())
                        .copyOnDeath()
                        .build()
            );
}
