package com.catfight.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Cat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Client-only approximation of the server-side fight pairing.
 *
 * <p>The server keeps a fight active only while both cats remain within five
 * blocks, so the visual state uses the same exact range.
 * A cat that has been calmed by the mod is silent on both sides and is
 * therefore deliberately excluded. Tamed cats are also deliberately excluded,
 * matching the server-side safe-cat rule.</p>
 */
public final class CatFightClientPose {
    private static final double START_RANGE_SQR = 25.0D;
    private static final double LEAVE_RANGE_SQR = 25.0D;
    private static final Map<UUID, UUID> ACTIVE_FIGHTS = new HashMap<>();
    private static final Map<UUID, Long> PANCAKED_UNTIL = new HashMap<>();
    private static final Set<UUID> CHASING_CATS = new HashSet<>();

    private CatFightClientPose() {
    }

    public static boolean isFighting(Cat cat) {
        if (isPancaked(cat) || isChasing(cat) || !isEligible(cat)) {
            removeFight(cat);
            return false;
        }

        UUID targetId = ACTIVE_FIGHTS.get(cat.getUUID());
        if (targetId != null) {
            Cat target = cat.level().getEntitiesOfClass(Cat.class, cat.getBoundingBox().inflate(5.0D),
                    other -> targetId.equals(other.getUUID())).stream().findFirst().orElse(null);
            if (target != null && isEligible(target) && cat.distanceToSqr(target) <= LEAVE_RANGE_SQR) {
                return true;
            }
            removeFight(cat);
        }

        // Mirror the server tracker: a pair only fights inside five blocks.
        Cat opponent = cat.level().getEntitiesOfClass(Cat.class, cat.getBoundingBox().inflate(5.0D),
                other -> other != cat && isEligible(other) && cat.distanceToSqr(other) <= START_RANGE_SQR)
                .stream().findFirst().orElse(null);
        if (opponent == null) {
            return false;
        }

        ACTIVE_FIGHTS.put(cat.getUUID(), opponent.getUUID());
        ACTIVE_FIGHTS.put(opponent.getUUID(), cat.getUUID());
        return true;
    }

    /** Gives each cat a stable, mirrored-looking lean rather than changing side every frame. */
    public static float leanDirection(Cat cat) {
        return (cat.getUUID().getLeastSignificantBits() & 1L) == 0L ? 1.0F : -1.0F;
    }

    /** Receives the server-authoritative lifetime for the locked cat-pancake appearance. */
    public static void setPancaked(int entityId, UUID catId, int durationTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        if (durationTicks <= 0) {
            PANCAKED_UNTIL.remove(catId);
        } else {
            long until = minecraft.level.getGameTime() + durationTicks;
            PANCAKED_UNTIL.put(catId, until);
        }
        Entity entity = minecraft.level.getEntity(entityId);
        if (entity instanceof Cat cat && cat.getUUID().equals(catId)) {
            removeFight(cat);
        }
    }

    public static boolean isPancaked(Cat cat) {
        long until = PANCAKED_UNTIL.getOrDefault(cat.getUUID(), 0L);
        if (until > cat.level().getGameTime()) {
            return true;
        }
        PANCAKED_UNTIL.remove(cat.getUUID());
        return false;
    }

    /** Tracks the server-authoritative vehicle chase state for visual consistency. */
    public static void setChasing(int entityId, UUID catId, boolean chasing) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        if (chasing) {
            CHASING_CATS.add(catId);
        } else {
            CHASING_CATS.remove(catId);
        }
        Entity entity = minecraft.level.getEntity(entityId);
        if (entity instanceof Cat cat && cat.getUUID().equals(catId)) {
            removeFight(cat);
        }
    }

    private static boolean isChasing(Cat cat) {
        return CHASING_CATS.contains(cat.getUUID());
    }

    private static boolean isEligible(Cat cat) {
        return cat.isAlive()
                && !cat.isSilent()
                && !cat.isTame()
                && !cat.isPassenger()
                && !isPancaked(cat)
                && !isChasing(cat);
    }

    private static void removeFight(Cat cat) {
        UUID catId = cat.getUUID();
        UUID targetId = ACTIVE_FIGHTS.remove(catId);
        if (targetId != null && catId.equals(ACTIVE_FIGHTS.get(targetId))) {
            ACTIVE_FIGHTS.remove(targetId);
        }
    }
}
