package com.catfight;

import com.catfight.entity.CatFightTracker;
import com.catfight.entity.CatPancakeTracker;
import com.catfight.item.ModItems;
import com.catfight.network.CatFightNetwork;
import com.catfight.sound.ModSounds;
import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.PlayLevelSoundEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(CatFightMod.MOD_ID)
public class CatFightMod {
    public static final String MOD_ID = "catfight";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CatFightMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        ModItems.ITEMS.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);
        CatFightNetwork.register();
        modEventBus.addListener(ModItems::addCreative);
        MinecraftForge.EVENT_BUS.register(this);
        LOGGER.info("CatFight Forge loaded");
    }

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel serverLevel) {
            CatFightTracker.tick(serverLevel);
            CatPancakeTracker.finishLevelTick(serverLevel);
        }
    }

    @SubscribeEvent
    public void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel) {
            CatFightTracker.clearData();
        }
    }

    @SubscribeEvent
    public void onCatTick(LivingEvent.LivingTickEvent event) {
        if (!event.getEntity().level().isClientSide() && event.getEntity() instanceof Cat cat) {
            CatPancakeTracker.tick(cat);
        }
    }

    @SubscribeEvent
    public void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getTarget() instanceof Cat cat) {
            CatPancakeTracker.syncToPlayer(player, cat);
        }
    }

    @SubscribeEvent
    public void onEmptyBottleUse(PlayerInteractEvent.EntityInteract event) {
        if (tryRescuePancake(event, event.getTarget())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    @SubscribeEvent
    public void onEmptyBottleUseSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (tryRescuePancake(event, event.getTarget())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    @SubscribeEvent
    public void onPlayCatSound(PlayLevelSoundEvent.AtEntity event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Cat cat
                && cat.isAlive() && CatFightTracker.isFighting(cat) && isVanillaCatSound(event.getSound())) {
            // Custom client-bound voices continue to play; only vanilla entity sounds are cancelled.
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onPlayCatSoundAtPosition(PlayLevelSoundEvent.AtPosition event) {
        if (!event.getLevel().isClientSide() && isVanillaCatSound(event.getSound())
                && CatFightTracker.isFightSoundPosition(event.getPosition())) {
            // Entity#playSound uses the position overload, which has no source entity attached.
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onCatDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide() && event.getEntity() instanceof Cat cat) {
            CatFightTracker.removeFromFight(cat);
        }
    }

    @SubscribeEvent
    public void onCatLeaveLevel(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Cat cat) {
            CatFightTracker.removeFromFight(cat);
            CatPancakeTracker.forgetRuntime(cat);
        }
    }

    private static boolean isVanillaCatSound(Holder<SoundEvent> sound) {
        if (sound == null) {
            return false;
        }
        ResourceLocation soundId = BuiltInRegistries.SOUND_EVENT.getKey(sound.value());
        return soundId != null
                && "minecraft".equals(soundId.getNamespace())
                && soundId.getPath().startsWith("entity.cat.");
    }

    private static boolean tryRescuePancake(PlayerInteractEvent event, net.minecraft.world.entity.Entity target) {
        return !event.getLevel().isClientSide()
                && event.getItemStack().is(Items.GLASS_BOTTLE)
                && target instanceof Cat cat
                && CatPancakeTracker.rescue(cat);
    }
}
