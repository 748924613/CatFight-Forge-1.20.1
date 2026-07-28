package com.catfight.sound;

import com.catfight.CatFightMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.Random;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, CatFightMod.MOD_ID);
    private static final Random RANDOM = new Random();

    public static final RegistryObject<SoundEvent> LAOWU_1 = register("laowu_1");
    public static final RegistryObject<SoundEvent> LAOWU_2 = register("laowu_2");
    public static final RegistryObject<SoundEvent> LAOWU_3 = register("laowu_3");
    private static final List<RegistryObject<SoundEvent>> ARGUE_SOUNDS = List.of(LAOWU_1, LAOWU_2, LAOWU_3);

    private ModSounds() {
    }

    private static RegistryObject<SoundEvent> register(String name) {
        ResourceLocation id = new ResourceLocation(CatFightMod.MOD_ID, name);
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    /** Picks a different clip from the previous one, so a switch is always audible. */
    public static SelectedArgueSound nextArgue(int previousIndex) {
        int size = ARGUE_SOUNDS.size();
        int index = previousIndex < 0 || previousIndex >= size
                ? RANDOM.nextInt(size)
                : (previousIndex + 1 + RANDOM.nextInt(size - 1)) % size;
        return new SelectedArgueSound(ARGUE_SOUNDS.get(index).get(), index);
    }

    public record SelectedArgueSound(SoundEvent sound, int index) {
    }
}
