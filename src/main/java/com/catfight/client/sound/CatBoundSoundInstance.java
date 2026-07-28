package com.catfight.client.sound;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.Cat;

/** A non-looping long-form sound that follows its cat and stops as soon as it dies or despawns. */
public final class CatBoundSoundInstance extends AbstractTickableSoundInstance {
    private final Cat cat;
    private int remainingTicks;

    public CatBoundSoundInstance(Cat cat, SoundEvent sound, float volume, float pitch, int playbackTicks) {
        super(sound, SoundSource.NEUTRAL, RandomSource.create());
        this.cat = cat;
        this.volume = volume;
        this.pitch = pitch;
        this.remainingTicks = Math.max(1, playbackTicks);
        this.attenuation = SoundInstance.Attenuation.LINEAR;
        updatePosition();
    }

    @Override
    public void tick() {
        if (!this.cat.isAlive() || this.cat.isRemoved()) {
            this.stop();
            return;
        }
        if (--this.remainingTicks <= 0) {
            this.stop();
            return;
        }
        updatePosition();
    }

    public void stopNow() {
        this.stop();
    }

    private void updatePosition() {
        this.x = this.cat.getX();
        this.y = this.cat.getY();
        this.z = this.cat.getZ();
    }
}
