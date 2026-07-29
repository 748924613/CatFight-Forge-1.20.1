package com.catfight.entity;

import com.catfight.network.CatFightNetwork;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * High-speed vehicles lure cats into a chase.  A real impact turns the cat
 * into a locked cat pancake until a player rescues it with an empty bottle;
 * after ten minutes without a rescue, it dies.
 */
public final class CatPancakeTracker {
    private static final double MIN_HORIZONTAL_SPEED = 0.20D;
    private static final double MIN_HORIZONTAL_SPEED_SQR = MIN_HORIZONTAL_SPEED * MIN_HORIZONTAL_SPEED;
    private static final double MAX_SWEPT_DISTANCE_SQR = 64.0D;
    private static final double IMPACT_MARGIN = 0.10D;
    private static final double SEARCH_RADIUS = 8.0D;
    private static final double CHASE_RADIUS = 16.0D;
    private static final double CHASE_SPEED = 1.30D;
    private static final int PANCAKE_TIMEOUT_TICKS = 20 * 60 * 10;
    private static final int RESCUE_GRACE_TICKS = 40;

    private static final String DEATH_TIME_TAG = "catfight_pancake_death_time";
    private static final String X_TAG = "catfight_pancake_x";
    private static final String Y_TAG = "catfight_pancake_y";
    private static final String Z_TAG = "catfight_pancake_z";
    private static final String Y_ROT_TAG = "catfight_pancake_y_rot";
    private static final String Y_HEAD_ROT_TAG = "catfight_pancake_y_head_rot";
    private static final String Y_BODY_ROT_TAG = "catfight_pancake_y_body_rot";
    private static final String X_ROT_TAG = "catfight_pancake_x_rot";
    private static final String RESTORE_AI_TAG = "catfight_pancake_restore_ai";
    private static final String RESCUE_GRACE_TAG = "catfight_pancake_rescue_grace";

    private static final Map<UUID, PancakeState> PANCAKED = new HashMap<>();
    private static final Map<UUID, UUID> CHASING_TARGETS = new HashMap<>();

    private CatPancakeTracker() {
    }

    /** Called once per server tick for every loaded cat. */
    public static void tick(Cat cat) {
        if (!(cat.level() instanceof ServerLevel level) || cat.isRemoved() || !cat.isAlive()) {
            return;
        }

        long gameTime = gameTime(level);
        PancakeState pancake = getPancakeState(cat);
        if (pancake != null) {
            if (gameTime >= pancake.deathGameTime()) {
                clearPancakeState(cat);
                cat.kill();
                return;
            }
            lockPancakedCat(cat, pancake);
            return;
        }

        if (hasRescueGrace(cat, gameTime)) {
            endChase(cat);
            return;
        }

        if (hasFastImpact(cat)) {
            startPancake(cat, gameTime);
            return;
        }

        Entity pursuitTarget = findPursuitTarget(cat);
        if (pursuitTarget != null) {
            beginChase(cat, pursuitTarget);
        } else {
            endChase(cat);
        }
    }

    /** Pancaked cats are temporarily ineligible for a confrontation. */
    public static boolean isPancaked(Cat cat) {
        return getPancakeState(cat) != null;
    }

    /** Cats chasing a fast vehicle should not be frozen into a nearby cat fight. */
    public static boolean isChasingVehicle(Cat cat) {
        return CHASING_TARGETS.containsKey(cat.getUUID());
    }

    /**
     * Rescues a cat pancake.  The empty glass bottle is intentionally not
     * consumed: it is the rescue tool, not a crafting ingredient.
     */
    public static boolean rescue(Cat cat) {
        PancakeState pancake = getPancakeState(cat);
        if (pancake == null) {
            return false;
        }

        clearPancakeState(cat);
        if (pancake.restoreAiAfterRescue() && cat.isAlive()) {
            cat.setNoAi(false);
        }
        cat.setDeltaMovement(Vec3.ZERO);
        if (cat.level() instanceof ServerLevel serverLevel) {
            cat.getPersistentData().putLong(RESCUE_GRACE_TAG, gameTime(serverLevel) + RESCUE_GRACE_TICKS);
        }
        CatFightNetwork.clearPancakedForTracking(cat);
        if (cat.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    cat.getX(), cat.getY() + 0.35D, cat.getZ(), 5,
                    0.25D, 0.05D, 0.25D, 0.02D);
            serverLevel.broadcastEntityEvent(cat, (byte) 7);
        }
        return true;
    }

    /** Sends the remaining pancake lifetime to a player who begins tracking this cat. */
    public static void syncToPlayer(ServerPlayer player, Cat cat) {
        if (!(cat.level() instanceof ServerLevel level)) {
            return;
        }
        PancakeState pancake = getPancakeState(cat);
        long remaining = pancake == null ? 0L : pancake.deathGameTime() - gameTime(level);
        CatFightNetwork.setPancakedForPlayer(player, cat, (int) Math.max(0L,
                Math.min(Integer.MAX_VALUE, remaining)));
        CatFightNetwork.setChasingForPlayer(player, cat, pancake == null && isChasingVehicle(cat));
    }

    /** Removes only runtime bookkeeping; persistent pancake data survives chunk unload/reload. */
    public static void forgetRuntime(Cat cat) {
        PANCAKED.remove(cat.getUUID());
        CHASING_TARGETS.remove(cat.getUUID());
    }

    /** Used when the cat dies, is rescued, or otherwise truly leaves pancake state. */
    public static void clearPancakeState(Cat cat) {
        PANCAKED.remove(cat.getUUID());
        CHASING_TARGETS.remove(cat.getUUID());
        CompoundTag data = cat.getPersistentData();
        data.remove(DEATH_TIME_TAG);
        data.remove(X_TAG);
        data.remove(Y_TAG);
        data.remove(Z_TAG);
        data.remove(Y_ROT_TAG);
        data.remove(Y_HEAD_ROT_TAG);
        data.remove(Y_BODY_ROT_TAG);
        data.remove(X_ROT_TAG);
        data.remove(RESTORE_AI_TAG);
        data.remove(RESCUE_GRACE_TAG);
    }

    /** Level unload clears maps only; cats keep their ten-minute deadline if the world is reloaded. */
    public static void clearRuntimeData() {
        PANCAKED.clear();
        CHASING_TARGETS.clear();
    }

    /** A second lock at LevelTick END blocks movement from water, pushes, and late mod hooks. */
    public static void finishLevelTick(ServerLevel level) {
        long gameTime = gameTime(level);
        for (UUID catId : PANCAKED.keySet().toArray(UUID[]::new)) {
            Entity entity = level.getEntity(catId);
            if (!(entity instanceof Cat cat) || !cat.isAlive() || cat.isRemoved()) {
                continue;
            }
            PancakeState pancake = getPancakeState(cat);
            if (pancake == null) {
                continue;
            }
            if (gameTime >= pancake.deathGameTime()) {
                clearPancakeState(cat);
                cat.kill();
            } else {
                lockPancakedCat(cat, pancake);
            }
        }
    }

    private static void startPancake(Cat cat, long gameTime) {
        CatFightTracker.removeFromFight(cat);
        endChase(cat);
        cat.stopRiding();

        PancakeState pancake = new PancakeState(gameTime + PANCAKE_TIMEOUT_TICKS,
                cat.getX(), cat.getY(), cat.getZ(), cat.getYRot(), cat.getYHeadRot(), cat.yBodyRot, cat.getXRot(),
                !cat.isNoAi());
        PANCAKED.put(cat.getUUID(), pancake);
        savePancakeState(cat, pancake);
        lockPancakedCat(cat, pancake);
        CatFightNetwork.setPancakedForTracking(cat, PANCAKE_TIMEOUT_TICKS);
    }

    private static void lockPancakedCat(Cat cat, PancakeState pancake) {
        cat.setNoAi(true);
        cat.stopRiding();
        cat.getNavigation().stop();
        cat.setTarget(null);
        cat.setPos(pancake.x(), pancake.y(), pancake.z());
        cat.setDeltaMovement(Vec3.ZERO);
        cat.setYRot(pancake.yRot());
        cat.setYHeadRot(pancake.yHeadRot());
        cat.setYBodyRot(pancake.yBodyRot());
        cat.setXRot(pancake.xRot());
    }

    private static void beginChase(Cat cat, Entity pursuitTarget) {
        UUID catId = cat.getUUID();
        UUID targetId = pursuitTarget.getUUID();
        UUID previousTarget = CHASING_TARGETS.put(catId, targetId);
        if (!targetId.equals(previousTarget)) {
            CatFightTracker.removeFromFight(cat);
            CatFightNetwork.setChasingForTracking(cat, true);
        }
        cat.getNavigation().moveTo(pursuitTarget.getX(), pursuitTarget.getY(), pursuitTarget.getZ(), CHASE_SPEED);
        cat.lookAt(EntityAnchorArgument.Anchor.EYES, pursuitTarget.position());
    }

    private static void endChase(Cat cat) {
        if (CHASING_TARGETS.remove(cat.getUUID()) != null) {
            cat.getNavigation().stop();
            CatFightNetwork.setChasingForTracking(cat, false);
        }
    }

    private static Entity findPursuitTarget(Cat cat) {
        AABB searchBox = cat.getBoundingBox().inflate(CHASE_RADIUS);
        return cat.level().getEntities(cat, searchBox, candidate -> isImpactCandidate(cat, candidate))
                .stream()
                .filter(candidate -> isFastHorizontalMotion(tickMotion(candidate)))
                .min(Comparator.comparingDouble(cat::distanceToSqr))
                .orElse(null);
    }

    private static boolean hasFastImpact(Cat cat) {
        // Minecarts can scoop a cat up as a passenger, after which their AABBs
        // no longer overlap.  A fast vehicle still counts as the impact.
        Entity vehicle = cat.getVehicle();
        if (vehicle != null && isFastHorizontalMotion(tickMotion(vehicle))) {
            return true;
        }

        Vec3 catMotion = tickMotion(cat);
        AABB catBox = cat.getBoundingBox();
        AABB catSweep = catBox.minmax(catBox.move(-catMotion.x, -catMotion.y, -catMotion.z))
                .inflate(IMPACT_MARGIN);
        AABB searchBox = catBox.inflate(SEARCH_RADIUS);
        return cat.level().getEntities(cat, searchBox, candidate -> isImpactCandidate(cat, candidate))
                .stream()
                .anyMatch(candidate -> sweptImpact(catMotion, catSweep, candidate));
    }

    private static boolean isImpactCandidate(Cat cat, Entity candidate) {
        return !(candidate instanceof Cat)
                && !(candidate instanceof Projectile)
                && !(candidate instanceof ItemEntity)
                && !(candidate instanceof ExperienceOrb)
                && !candidate.isRemoved()
                && candidate.isAlive()
                && !candidate.isSpectator()
                && candidate != cat.getVehicle();
    }

    private static boolean sweptImpact(Vec3 catMotion, AABB catSweep, Entity candidate) {
        Vec3 candidateMotion = tickMotion(candidate);
        // The impacting object itself must be fast.  A cat running into a
        // stationary cow or player must not turn itself into a pancake.
        if (!isFastHorizontalMotion(candidateMotion)) {
            return false;
        }

        AABB candidateBox = candidate.getBoundingBox();
        AABB candidateSweep = candidateBox.minmax(candidateBox.move(-candidateMotion.x,
                -candidateMotion.y, -candidateMotion.z)).inflate(IMPACT_MARGIN);
        return candidateSweep.intersects(catSweep);
    }

    private static PancakeState getPancakeState(Cat cat) {
        PancakeState inMemory = PANCAKED.get(cat.getUUID());
        if (inMemory != null) {
            return inMemory;
        }

        CompoundTag data = cat.getPersistentData();
        if (!data.contains(DEATH_TIME_TAG)) {
            return null;
        }
        PancakeState restored = new PancakeState(data.getLong(DEATH_TIME_TAG), data.getDouble(X_TAG),
                data.getDouble(Y_TAG), data.getDouble(Z_TAG), data.getFloat(Y_ROT_TAG), data.getFloat(Y_HEAD_ROT_TAG),
                data.getFloat(Y_BODY_ROT_TAG), data.getFloat(X_ROT_TAG), data.getBoolean(RESTORE_AI_TAG));
        PANCAKED.put(cat.getUUID(), restored);
        return restored;
    }

    private static void savePancakeState(Cat cat, PancakeState pancake) {
        CompoundTag data = cat.getPersistentData();
        data.putLong(DEATH_TIME_TAG, pancake.deathGameTime());
        data.putDouble(X_TAG, pancake.x());
        data.putDouble(Y_TAG, pancake.y());
        data.putDouble(Z_TAG, pancake.z());
        data.putFloat(Y_ROT_TAG, pancake.yRot());
        data.putFloat(Y_HEAD_ROT_TAG, pancake.yHeadRot());
        data.putFloat(Y_BODY_ROT_TAG, pancake.yBodyRot());
        data.putFloat(X_ROT_TAG, pancake.xRot());
        data.putBoolean(RESTORE_AI_TAG, pancake.restoreAiAfterRescue());
    }

    private static Vec3 tickMotion(Entity entity) {
        Vec3 movedSinceLastTick = new Vec3(entity.getX() - entity.xo,
                entity.getY() - entity.yo, entity.getZ() - entity.zo);
        Vec3 velocity = entity.getDeltaMovement();
        return movedSinceLastTick.lengthSqr() >= velocity.lengthSqr() ? movedSinceLastTick : velocity;
    }

    private static boolean hasRescueGrace(Cat cat, long gameTime) {
        CompoundTag data = cat.getPersistentData();
        long graceUntil = data.getLong(RESCUE_GRACE_TAG);
        if (graceUntil > gameTime) {
            return true;
        }
        data.remove(RESCUE_GRACE_TAG);
        return false;
    }

    /** Uses the Overworld clock so a dimension transfer cannot reset the ten-minute rescue deadline. */
    private static long gameTime(ServerLevel level) {
        return level.getServer().overworld().getGameTime();
    }

    private static boolean isFastHorizontalMotion(Vec3 motion) {
        return motion.x * motion.x + motion.z * motion.z >= MIN_HORIZONTAL_SPEED_SQR
                && motion.lengthSqr() <= MAX_SWEPT_DISTANCE_SQR;
    }

    private record PancakeState(long deathGameTime, double x, double y, double z, float yRot, float yHeadRot,
                                float yBodyRot, float xRot, boolean restoreAiAfterRescue) {
    }
}
