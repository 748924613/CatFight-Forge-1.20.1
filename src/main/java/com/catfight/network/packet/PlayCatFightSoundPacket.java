package com.catfight.network.packet;

import com.catfight.client.sound.CatFightClientSounds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client-bound request to play a sound that follows, and stops with, one cat. */
public record PlayCatFightSoundPacket(int entityId, ResourceLocation soundId, float volume, float pitch,
                                      int playbackTicks) {
    public static void encode(PlayCatFightSoundPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.entityId);
        buffer.writeResourceLocation(packet.soundId);
        buffer.writeFloat(packet.volume);
        buffer.writeFloat(packet.pitch);
        buffer.writeInt(packet.playbackTicks);
    }

    public static PlayCatFightSoundPacket decode(FriendlyByteBuf buffer) {
        return new PlayCatFightSoundPacket(buffer.readInt(), buffer.readResourceLocation(),
                buffer.readFloat(), buffer.readFloat(), buffer.readInt());
    }

    public static void handle(PlayCatFightSoundPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isClient()) {
                CatFightClientSounds.play(packet);
            }
        });
        context.setPacketHandled(true);
    }
}
