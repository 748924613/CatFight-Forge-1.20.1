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

    private CatFightTracker() {
    }

    private static final class FightData {
        private final Cat fighter;
        private final Cat target;
        private final boolean restoreAiAfterFight;
        private int attackCooldown;
        private int soundCooldown;
        private int lastSoundIndex = -1;

        private FightData(Cat fighter, Cat target) {
            this.fighter = fighter;
            this.target = target;
            this.restoreAiAfterFight = !fighter.isNoAi();
            if (this.restoreAiAfterFight) {
                fighter.setNoAi(true);
            }
            fighter.getNavigation().stop();
            fighter.setDeltaMovement(Vec3.ZERO);
        }

        private void release() {
            CatFightNetwork.stopSoundForTracking(this.fighter);
            if (this.restoreAiAfterFight && this.fighter.isAlive()) {
                this.fighter.setNoAi(false);
            }
        }
    }

    public static void tick(ServerLevel level) {
        Set<UUID> seenCats = new HashSet<>();
        List<Cat> cats = new ArrayList<>();
        level.players().forEach(player -> cats.addAll(level.getEntitiesOfClass(Cat.class,
                new AABB(player.blockPosition()).inflate(40.0D), cat -> true)));

        for (Cat cat : cats) {
            if (seenCats.add(cat.getUUID())) {
                tickForbidden(cat);
            }
        }

        long gameTime = level.getGameTime();
        for (Cat cat : cats) {
            if (!cat.isAlive()) {
                removeFromFight(cat);
                continue;
            }
            if (FORBIDDEN.containsKey(cat.getUUID())) {
                removeFromFight(cat);
                continue;
            }

            FightData data = FIGHTS.get(cat.getUUID());
            if (data == null) {
                findOpponent(level, cat);
                continue;
            }

            Cat target = data.target;
            if (!target.isAlive() || target.isRemoved() || cat.distanceToSqr(target) > FIGHT_RANGE * FIGHT_RANGE
                    || FORBIDDEN.containsKey(target.getUUID())) {
                removeFromFight(cat);
                continue;
            }

            cat.getNavigation().stop();
            cat.setDeltaMovement(Vec3.ZERO);
            cat.lookAt(EntityAnchorArgument.Anchor.FEET, target.position());
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
        if (FIGHTS.containsKey(cat.getUUID())) {
            return;
        }
        List<Cat> nearby = level.getEntitiesOfClass(Cat.class,
                new AABB(cat.blockPosition()).inflate(FIGHT_RANGE),
                other -> other != cat
                        && !FIGHTS.containsKey(other.getUUID())
                        && !FORBIDDEN.containsKey(other.getUUID())
                        && cat.distanceToSqr(other) <= FIGHT_RANGE * FIGHT_RANGE);
        if (!nearby.isEmpty()) {
            Cat opponent = nearby.get(0);
            FIGHTS.put(cat.getUUID(), new FightData(cat, opponent));
            FIGHTS.put(opponent.getUUID(), new FightData(opponent, cat));
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

    /** Used by the sound event hook to suppress vanilla cat sounds only while fighting. */
    public static boolean isFighting(Cat cat) {
        FightData data = FIGHTS.get(cat.getUUID());
        return data != null
                && data.fighter == cat
                && !cat.isRemoved()
                && data.target.isAlive()
                && !data.target.isRemoved()
                && !FORBIDDEN.containsKey(cat.getUUID())
                && !FORBIDDEN.containsKey(data.target.getUUID())
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
