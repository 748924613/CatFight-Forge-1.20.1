package com.catfight.client;

import com.catfight.CatFightMod;
import com.catfight.client.model.CatFightCatModel;
import com.catfight.client.model.CatFightModelLayers;
import com.catfight.client.renderer.CatFightCatRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client registrations are isolated here so dedicated servers never load rendering classes. */
@Mod.EventBusSubscriber(modid = CatFightMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CatFightClientEvents {
    private CatFightClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityType.CAT, CatFightCatRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(CatFightModelLayers.ARCHING_CAT, CatFightCatModel::createBodyLayer);
    }
}
