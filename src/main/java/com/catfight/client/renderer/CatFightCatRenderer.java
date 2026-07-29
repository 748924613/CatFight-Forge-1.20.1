package com.catfight.client.renderer;

import com.catfight.client.CatFightClientPose;
import com.catfight.client.model.CatFightCatModel;
import com.catfight.client.model.CatFightModelLayers;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.CatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.animal.Cat;

/**
 * Keeps every vanilla cat-renderer feature (textures, scale and collar layer)
 * but substitutes the model whose animation has the confrontation pose.
 */
public final class CatFightCatRenderer extends CatRenderer {
    public CatFightCatRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new CatFightCatModel(context.bakeLayer(CatFightModelLayers.ARCHING_CAT));
    }

    @Override
    protected void scale(Cat cat, PoseStack poseStack, float partialTick) {
        super.scale(cat, poseStack, partialTick);
        if (CatFightClientPose.isPancaked(cat)) {
            // Renderer-level scaling keeps the vanilla cat's texture and every model part intact,
            // producing a broad, low cat pancake without the missing texture blocks caused by extra cubes.
            poseStack.scale(1.45F, 0.20F, 1.45F);
        }
    }
}
