package com.catfight.item;

import com.catfight.entity.CatFightTracker;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class TrainingStickItem extends Item {
    public static final int FORBID_FIGHT_DURATION = 3_600;

    public TrainingStickItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (target instanceof Cat cat && attacker instanceof Player && cat.isTame()) {
            CatFightTracker.setForbidFight(cat, FORBID_FIGHT_DURATION);
            return true;
        }
        return super.hurtEnemy(stack, target, attacker);
    }
}
