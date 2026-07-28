package com.catfight.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Random;

public class DriedFishItem extends Item {
    private static final Random RANDOM = new Random();

    public DriedFishItem(Properties properties) {
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
            int roll = RANDOM.nextInt(100);
            if (roll < 30) {
                cat.spawnAtLocation(new ItemStack(Items.GOLD_NUGGET, 1 + RANDOM.nextInt(3)));
            } else if (roll < 50) {
                cat.spawnAtLocation(new ItemStack(Items.COD));
            } else if (roll < 65) {
                cat.spawnAtLocation(new ItemStack(Items.STRING, 1 + RANDOM.nextInt(2)));
            } else if (roll < 78) {
                cat.spawnAtLocation(new ItemStack(Items.FEATHER, 1 + RANDOM.nextInt(2)));
            } else if (roll < 88) {
                cat.spawnAtLocation(new ItemStack(Items.GOLD_INGOT));
            } else if (roll < 95) {
                cat.spawnAtLocation(new ItemStack(Items.IRON_INGOT));
            } else if (roll < 98) {
                cat.spawnAtLocation(new ItemStack(Items.EMERALD));
            } else {
                cat.spawnAtLocation(new ItemStack(Items.DIAMOND));
            }
            cat.level().broadcastEntityEvent(cat, (byte) 7);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }
}
