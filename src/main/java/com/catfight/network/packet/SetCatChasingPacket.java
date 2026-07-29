package com.catfight.network.packet;

import com.catfight.client.CatFightClientPose;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** Keeps the client from drawing a chasing cat as if it were still in a cat fight. */
public record SetCatChasingPacket(int entityId, UUID catId, boolean chasing) {
    public static void encode(SetCatChasingPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeUUID(packet.catId);
        buffer.writeBoolean(packet.chasing);
    }

    public static SetCatChasingPacket decode(FriendlyByteBuf buffer) {
        return new SetCatChasingPacket(buffer.readVarInt(), buffer.readUUID(), buffer.readBoolean());
    }

    public static void handle(SetCatChasingPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isClient()) {
                CatFightClientPose.setChasing(packet.entityId(), packet.catId(), packet.chasing());
            }
        });
        context.setPacketHandled(true);
    }
}
