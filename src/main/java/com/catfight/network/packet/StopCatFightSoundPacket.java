package com.catfight.network.packet;

import com.catfight.client.sound.CatFightClientSounds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client-bound request to stop the custom voice bound to one cat immediately. */
public record StopCatFightSoundPacket(int entityId) {
    public static void encode(StopCatFightSoundPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.entityId);
    }

    public static StopCatFightSoundPacket decode(FriendlyByteBuf buffer) {
        return new StopCatFightSoundPacket(buffer.readInt());
    }

    public static void handle(StopCatFightSoundPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isClient()) {
                CatFightClientSounds.stop(packet.entityId());
            }
        });
        context.setPacketHandled(true);
    }
}
