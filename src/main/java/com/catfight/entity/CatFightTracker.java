package com.catfight.entity;

import com.catfight.sound.ModSounds;
import com.catfight.network.CatFightNetwork;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public final class CatFightTracker {
    private static final Map<UUID, FightData> FIGHTS = new HashMap<>();
    private static final Map<UUID, Integer> FORBIDDEN = new HashMap<>();
    private static final Random RANDOM = new Random();

    private static final double FIGHT_RANGE = 5.0D;
    private static final int ATTACK_INTERVAL = 60;
    private static final int ATTACK_COOLDOWN = 40;
    private static final double ATTACK_CHANCE = 0.22D;
    private static final float HEAL_TRIGGER_HEALTH = 2.0F;
    private static final float HEAL_AMOUNT = 1.0F;
    private static final int HEAL_INTERVAL = 20;

    private CatFightTracker() {
    }

    /** Shared state for both sides of one confrontation. */
    private static final class PairState {
        private boolean healing;
        private long nextHealGameTime;
    }

    private static final class FightData {
        private final Cat fighter;
        private final Cat target;
        private final PairState pair;
        private final boolean restoreAiAfterFight;
        private int attackCooldown;
        private int soundCooldown;
        private int lastSoundIndex = -1;
        private boolean restoreSilentAfterHealing;

        private FightData(Cat fighter, Cat target, PairState pair) {
            this.fighter = fighter;
            this.target = target;
            this.pair = pair;
            this.restoreAiAfterFight = !fighter.isNoAi();
            if (this.restoreAiAfterFight) {
                fighter.setNoAi(true);
            }
            fighter.getNavigation().stop();
            fighter.setDeltaMovement(Vec3.ZERO);
        }

        private void startHealing() {
            this.restoreSilentAfterHealing = !this.fighter.isSilent();
            this.soundCooldown = 0;
            CatFightNetwork.stopSoundForTracking(this.fighter);
            if (this.restoreSilentAfterHealing) {
                this.fighter.setSilent(true);
            }
        }

        private void healStep() {
            if (this.fighter.getHealth() < this.fighter.getMaxHealth()) {
                this.fighter.heal(HEAL_AMOUNT);
            }
        }

        private boolean isFullyHealed() {
            return this.fighter.getHealth() >= this.fighter.getMaxHealth();
        }

        private void finishHealing() {
            if (this.restoreSilentAfterHealing && this.fighter.isAlive()) {
                this.fighter.setSilent(false);
            }
            this.restoreSilentAfterHealing = false;
            this.soundCooldown = 0;
        }

        private void release() {
            this.finishHealing();
            CatFightNetwork.stopSoundForTracking(this.fighter);
            if (this.restoreAiAfterFight && this.fighter.isAlive()) {
                this.fighter.setNoAi(false);
            }
        }
    }

    public static void tick(ServerLevel level) {
        List<Cat> cats = new ArrayList<>();
        Set<UUID> seenCats = new HashSet<>();
        level.players().forEach(player -> level.getEntitiesOfClass(Cat.class,
                        new AABB(player.blockPosition()).inflate(40.0D), cat -> true)
                .forEach(cat -> {
                    if (seenCats.add(cat.getUUID())) {
                        cats.add(cat);
                    }
                }));

        for (Cat cat : cats) {
            tickForbidden(cat);
        }

        long gameTime = level.getGameTime();
        for (Cat cat : cats) {
            if (!canFight(cat)) {
                removeFromFight(cat);
                continue;
            }

            FightData data = FIGHTS.get(cat.getUUID());
            if (data == null) {
                findOpponent(level, cat);
                continue;
            }

            Cat target = data.target;
            if (!canFight(target) || cat.distanceToSqr(target) > FIGHT_RANGE * FIGHT_RANGE) {
                removeFromFight(cat);
                continue;
            }
            FightData targetData = FIGHTS.get(target.getUUID());
            if (targetData == null || targetData.target != cat || targetData.pair != data.pair) {
                removeFromFight(cat);
                continue;
            }

            cat.getNavigation().stop();
            cat.setDeltaMovement(Vec3.ZERO);
            cat.lookAt(EntityAnchorArgument.Anchor.FEET, target.position());

            PairState pair = data.pair;
            if (!pair.healing
                    && (cat.getHealth() <= HEAL_TRIGGER_HEALTH || target.getHealth() <= HEAL_TRIGGER_HEALTH)) {
                pair.healing = true;
                pair.nextHealGameTime = gameTime;
                data.startHealing();
                targetData.startHealing();
            }
            if (pair.healing) {
                if (cat.getUUID().compareTo(target.getUUID()) < 0) {
                    if (gameTime >= pair.nextHealGameTime) {
                        data.healStep();
                        targetData.healStep();
                        pair.nextHealGameTime = gameTime + HEAL_INTERVAL;
                    }
                    if (data.isFullyHealed() && targetData.isFullyHealed()) {
                        pair.healing = false;
                        data.finishHealing();
                        targetData.finishHealing();
                    }
                }
                continue;
            }

            if (data.soundCooldown > 0) {
                data.soundCooldown--;
            }
            if (cat.getUUID().compareTo(target.getUUID()) < 0 && data.soundCooldown == 0) {
                int clipTicks = RANDOM.nextBoolean() ? 200 : 300;
                ModSounds.SelectedArgueSound nextSound = ModSounds.nextArgue(data.lastSoundIndex);
                CatFightNetwork.playSoundForTracking(cat, nextSound.sound(),
                        1.0F, 0.9F + RANDOM.nextFloat() * 0.2F, clipTicks);
                // The next clip begins one tick early; the client replaces the old clip so there is no overlap or gap.
                data.soundCooldown = clipTicks - 1;
                data.lastSoundIndex = nextSound.index();
            }

            if (data.attackCooldown > 0) {
                data.attackCooldown--;
            }
            if (gameTime % ATTACK_INTERVAL == 0 && data.attackCooldown == 0 && RANDOM.nextDouble() < ATTACK_CHANCE) {
                target.hurt(cat.damageSources().mobAttack(cat), 1.0F);
                data.attackCooldown = ATTACK_COOLDOWN;
            }
        }
    }

    private static void tickForbidden(Cat cat) {
        Integer remaining = FORBIDDEN.get(cat.getUUID());
        if (remaining == null) {
            return;
        }
        if (remaining <= 1) {
            FORBIDDEN.remove(cat.getUUID());
            cat.setSilent(false);
        } else if (remaining != Integer.MAX_VALUE) {
            FORBIDDEN.put(cat.getUUID(), remaining - 1);
        }
    }

    private static void findOpponent(ServerLevel level, Cat cat) {
        if (!canFight(cat) || FIGHTS.containsKey(cat.getUUID())) {
            return;
        }
        List<Cat> nearby = level.getEntitiesOfClass(Cat.class,
                new AABB(cat.blockPosition()).inflate(FIGHT_RANGE),
                other -> other != cat
                        && canFight(other)
                        && !FIGHTS.containsKey(other.getUUID())
                        && cat.distanceToSqr(other) <= FIGHT_RANGE * FIGHT_RANGE);
        if (!nearby.isEmpty()) {
            Cat opponent = nearby.get(0);
            PairState pair = new PairState();
            FIGHTS.put(cat.getUUID(), new FightData(cat, opponent, pair));
            FIGHTS.put(opponent.getUUID(), new FightData(opponent, cat, pair));
        }
    }

    public static void setForbidFight(Cat cat, int ticks) {
        removeFromFight(cat);
        FORBIDDEN.put(cat.getUUID(), ticks);
        cat.setSilent(true);
        if (cat.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    cat.getX(), cat.getY() + 0.8D, cat.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            serverLevel.broadcastEntityEvent(cat, (byte) 7);
        }
    }

    public static void removeFromFight(Cat cat) {
        endFight(cat);
        List<UUID> opponents = FIGHTS.entrySet().stream()
                .filter(entry -> entry.getValue().target.getUUID().equals(cat.getUUID()))
                .map(Map.Entry::getKey)
                .toList();
        for (UUID opponentId : opponents) {
            FightData data = FIGHTS.remove(opponentId);
            if (data != null) {
                data.release();
            }
        }
        cat.setYHeadRot(cat.getYRot());
    }

    public static void clearData() {
        FIGHTS.values().forEach(FightData::release);
        FIGHTS.clear();
        FORBIDDEN.clear();
    }

    private static void endFight(Cat cat) {
        FightData data = FIGHTS.remove(cat.getUUID());
        if (data != null) {
            data.release();
        }
    }

    /** Tamed cats are safe from the confrontation behavior; only wild cats may fight. */
    private static boolean canFight(Cat cat) {
        return cat.isAlive()
                && !cat.isRemoved()
                && !cat.isTame()
                && !cat.isPassenger()
                && !CatPancakeTracker.isPancaked(cat)
                && !CatPancakeTracker.isChasingVehicle(cat)
                && !FORBIDDEN.containsKey(cat.getUUID());
    }

    /** Used by the sound event hook to suppress vanilla cat sounds only while fighting. */
    public static boolean isFighting(Cat cat) {
        FightData data = FIGHTS.get(cat.getUUID());
        return data != null
                && data.fighter == cat
                && canFight(cat)
                && canFight(data.target)
                && cat.distanceToSqr(data.target) <= FIGHT_RANGE * FIGHT_RANGE;
    }

    /** Matches vanilla coordinate-based cat sounds to the cat that emitted them. */
    public static boolean isFightSoundPosition(Vec3 position) {
        for (FightData data : FIGHTS.values()) {
            if (isFighting(data.fighter) && data.fighter.distanceToSqr(position) <= 0.04D) {
                return true;
            }
        }
        return false;
    }
}
