package com.catfight.network.packet;

import com.catfight.client.CatFightClientPose;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** Client-bound state for the flattened cat appearance; zero duration clears it after rescue. */
public record SetCatPancakePacket(int entityId, UUID catId, int durationTicks) {
    public static void encode(SetCatPancakePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeUUID(packet.catId);
        buffer.writeVarInt(packet.durationTicks);
    }

    public static SetCatPancakePacket decode(FriendlyByteBuf buffer) {
        return new SetCatPancakePacket(buffer.readVarInt(), buffer.readUUID(), buffer.readVarInt());
    }

    public static void handle(SetCatPancakePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isClient()) {
                CatFightClientPose.setPancaked(packet.entityId(), packet.catId(), packet.durationTicks());
            }
        });
        context.setPacketHandled(true);
    }
}
