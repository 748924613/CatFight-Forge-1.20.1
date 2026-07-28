package com.catfight.client.sound;

import com.catfight.network.packet.PlayCatFightSoundPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Cat;

import java.util.HashMap;
import java.util.Map;

/** Handles client-bound cat sounds without creating a detached world-position sound. */
public final class CatFightClientSounds {
    private static final Map<Integer, CatBoundSoundInstance> ACTIVE_SOUNDS = new HashMap<>();

    private CatFightClientSounds() {
    }

    public static void play(PlayCatFightSoundPacket packet) {
        ACTIVE_SOUNDS.entrySet().removeIf(entry -> entry.getValue().isStopped());
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        Entity entity = minecraft.level.getEntity(packet.entityId());
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(packet.soundId());
        if (entity instanceof Cat cat && cat.isAlive() && sound != null) {
            stop(packet.entityId());
            CatBoundSoundInstance instance = new CatBoundSoundInstance(cat, sound, packet.volume(), packet.pitch(),
                    packet.playbackTicks());
            ACTIVE_SOUNDS.put(packet.entityId(), instance);
            minecraft.getSoundManager().play(instance);
        }
    }

    /** Stops a server-owned fight voice immediately, including when its opponent dies. */
    public static void stop(int entityId) {
        CatBoundSoundInstance instance = ACTIVE_SOUNDS.remove(entityId);
        if (instance != null) {
            instance.stopNow();
            Minecraft.getInstance().getSoundManager().stop(instance);
        }
    }
}
