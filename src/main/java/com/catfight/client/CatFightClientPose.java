package com.catfight.client;

import net.minecraft.world.entity.animal.Cat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Client-only approximation of the server-side fight pairing.
 *
 * <p>The server keeps a fight active only while both cats remain within five
 * blocks, so the visual state uses the same exact range.
 * A cat that has been calmed by the mod is silent on both sides and is
 * therefore deliberately excluded. A tamed cat that is currently sitting is
 * still eligible, matching the server-side confrontation rule.</p>
 */
public final class CatFightClientPose {
    private static final double START_RANGE_SQR = 25.0D;
    private static final double LEAVE_RANGE_SQR = 25.0D;
    private static final Map<UUID, UUID> ACTIVE_FIGHTS = new HashMap<>();

    private CatFightClientPose() {
    }

    public static boolean isFighting(Cat cat) {
        if (!isEligible(cat)) {
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

    private static boolean isEligible(Cat cat) {
        return cat.isAlive()
                && !cat.isSilent()
                && !cat.isPassenger();
    }

    private static void removeFight(Cat cat) {
        UUID catId = cat.getUUID();
        UUID targetId = ACTIVE_FIGHTS.remove(catId);
        if (targetId != null && catId.equals(ACTIVE_FIGHTS.get(targetId))) {
            ACTIVE_FIGHTS.remove(targetId);
        }
    }
}
