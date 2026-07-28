package com.catfight.item;

import com.catfight.entity.CatFightTracker;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class SuperDriedFishItem extends Item {
    public SuperDriedFishItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Cat cat) || !cat.isTame()) {
            return InteractionResult.PASS;
        }
        if (!player.level().isClientSide) {
            stack.shrink(1);
            CatFightTracker.setForbidFight(cat, Integer.MAX_VALUE);
            cat.setOrderedToSit(true);
            cat.spawnAtLocation(new ItemStack(Items.GOLDEN_APPLE));
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }
}
