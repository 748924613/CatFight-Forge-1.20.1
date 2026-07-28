package com.catfight.network;

import com.catfight.CatFightMod;
import com.catfight.network.packet.PlayCatFightSoundPacket;
import com.catfight.network.packet.StopCatFightSoundPacket;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.animal.Cat;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** Common network channel for client effects that must follow a specific cat. */
public final class CatFightNetwork {
    private static final String PROTOCOL_VERSION = "2";
    private static int nextPacketId;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CatFightMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private CatFightNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(nextPacketId++, PlayCatFightSoundPacket.class,
                PlayCatFightSoundPacket::encode,
                PlayCatFightSoundPacket::decode,
                PlayCatFightSoundPacket::handle);
        CHANNEL.registerMessage(nextPacketId++, StopCatFightSoundPacket.class,
                StopCatFightSoundPacket::encode,
                StopCatFightSoundPacket::decode,
                StopCatFightSoundPacket::handle);
    }

    public static void playSoundForTracking(Cat cat, SoundEvent sound, float volume, float pitch, int playbackTicks) {
        ResourceLocation soundId = BuiltInRegistries.SOUND_EVENT.getKey(sound);
        if (soundId != null) {
            CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> cat),
                    new PlayCatFightSoundPacket(cat.getId(), soundId, volume, pitch, playbackTicks));
        }
    }

    public static void stopSoundForTracking(Cat cat) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> cat),
                new StopCatFightSoundPacket(cat.getId()));
    }
}
