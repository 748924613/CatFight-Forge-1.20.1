package com.catfight.client.renderer;

import com.catfight.client.model.CatFightCatModel;
import com.catfight.client.model.CatFightModelLayers;
import net.minecraft.client.renderer.entity.CatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * Keeps every vanilla cat-renderer feature (textures, scale and collar layer)
 * but substitutes the model whose animation has the confrontation pose.
 */
public final class CatFightCatRenderer extends CatRenderer {
    public CatFightCatRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new CatFightCatModel(context.bakeLayer(CatFightModelLayers.ARCHING_CAT));
    }
}
