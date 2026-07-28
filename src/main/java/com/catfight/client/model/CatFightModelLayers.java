package com.catfight.client.model;

import com.catfight.CatFightMod;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

/** Layer containing the additional two-piece body used only for the arched fight pose. */
public final class CatFightModelLayers {
    public static final ModelLayerLocation ARCHING_CAT = new ModelLayerLocation(
            new ResourceLocation(CatFightMod.MOD_ID, "arching_cat"), "main");

    private CatFightModelLayers() {
    }
}
