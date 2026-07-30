package com.catfight.client.renderer;

import com.catfight.CatFightMod;
import com.catfight.client.CatFightClientPose;
import com.catfight.client.model.CatFightCatModel;
import com.catfight.client.model.CatFightModelLayers;
import net.minecraft.client.renderer.entity.CatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Cat;

/**
 * Keeps every vanilla cat-renderer feature (textures, scale and collar layer)
 * but substitutes the model whose animation has the confrontation pose.
 */
public final class CatFightCatRenderer extends CatRenderer {
    private static final ResourceLocation PANCAKE_TEXTURE = new ResourceLocation(
            CatFightMod.MOD_ID, "textures/entity/cat_pancake.png");

    public CatFightCatRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new CatFightCatModel(context.bakeLayer(CatFightModelLayers.ARCHING_CAT));
    }

    @Override
    public ResourceLocation getTextureLocation(Cat cat) {
        return CatFightClientPose.isPancaked(cat) ? PANCAKE_TEXTURE : super.getTextureLocation(cat);
    }
}
