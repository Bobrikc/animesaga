package com.example.animesaga;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Hero gifts powered by "Star Energy" (0..100, regenerates). Unlocked by the story.
 * 0 Echo of Thoughts (hear what people nearby think), 1 Shadow Rush, 2 Time Echo (rewind 5s), 3 Shadow Step (blink).
 */
public final class AbilityManager {
    public static final String[] NAMES = {"Эхо мыслей", "Рывок Тени", "Эхо времени", "Шаг сквозь тень"};
    private static final int[] COST = {10, 25, 50, 35};
    private static final int COUNT = 4;
    private static final Map<UUID, Float> ENERGY = new HashMap<>();
    private static final Map<UUID, Integer> MODE = new HashMap<>();
    private static final Map<UUID, ArrayDeque<Snap>> HISTORY = new HashMap<>();

    private record Snap(double x, double y, double z, float yaw, float pitch, float health, RegistryKey<World> dim) {}

    public static boolean unlocked(ServerPlayerEntity p, int m) {
        return p.getCommandTags().contains("animesaga_ab" + m);
    }

    public static void unlock(ServerPlayerEntity p, int m) {
        if (p.addCommandTag("animesaga_ab" + m)) {
            p.sendMessage(Text.literal("✦ Новый дар: " + NAMES[m]).formatted(Formatting.GOLD, Formatting.BOLD));
        }
    }

    public static void forget(UUID id) { ENERGY.remove(id); MODE.remove(id); HISTORY.remove(id); }

    private static void bar(ServerPlayerEntity p, String s) {
        p.sendMessage(Text.literal(s).formatted(Formatting.AQUA), true);
    }

    public static void use(ServerPlayerEntity p) {
        UUID id = p.getUuid();
        int m = MODE.getOrDefault(id, 0);
        if (p.isSneaking()) {
            for (int i = 1; i <= COUNT; i++) {
                int n = (m + i) % COUNT;
                if (unlocked(p, n)) { MODE.put(id, n); bar(p, "✦ Дар: " + NAMES[n]); return; }
            }
            bar(p, "Других даров пока нет");
            return;
        }
        if (!unlocked(p, m)) { bar(p, "Этот дар ещё не открыт (Shift+ПКМ — сменить)"); return; }
        float e = ENERGY.getOrDefault(id, 100f);
        if (e < COST[m]) { bar(p, "Недостаточно звёздной энергии"); return; }

        boolean ok;
        if (m == 0) ok = thoughts(p);
        else if (m == 1) ok = dash(p);
        else if (m == 2) ok = rewind(p);
        else ok = step(p);
        if (!ok) return;

        ENERGY.put(id, e - COST[m]);
        p.getItemCooldownManager().set(AnimeSaga.HERO_CHARM, 15);
    }

    // ---- 0: Echo of Thoughts ----
    private static boolean thoughts(ServerPlayerEntity p) {
        ServerWorld w = p.getServerWorld();
        List<VillagerEntity> list = new java.util.ArrayList<>(w.getEntitiesByClass(VillagerEntity.class,
                p.getBoundingBox().expand(14),
                e -> e.getCommandTags().contains(Town.TAG) && !e.isInvisible()));
        if (list.isEmpty()) { bar(p, "Рядом нет чьих-то мыслей..."); return false; }
        list.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(p)));
        int stage = p.getCommandTags().contains("animesaga_done2") ? 2
                : (p.getCommandTags().contains("animesaga_done1") ? 1 : 0);

        w.spawnParticles(ParticleTypes.ENCHANT, p.getX(), p.getY() + 1.8, p.getZ(), 50, 0.8, 0.5, 0.8, 0.6);
        w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BEACON_ACTIVATE,
                SoundCategory.PLAYERS, 0.6f, 2.0f);
        p.sendMessage(Text.literal("✦ Эхо мыслей — ты слышишь тихие голоса...").formatted(Formatting.DARK_PURPLE, Formatting.ITALIC));
        int n = Math.min(4, list.size());
        for (int i = 0; i < n; i++) {
            VillagerEntity v = list.get(i);
            String name = v.getCustomName() == null ? "?" : v.getCustomName().getString();
            String t = Town.thought(name, stage, new java.util.Random(w.random.nextLong()));
            p.sendMessage(Text.literal("  💭 " + name + ": ").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD)
                    .append(Text.literal("«" + t + "»").formatted(Formatting.GRAY, Formatting.ITALIC)));
            v.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 80, 0, false, false));
        }
        return true;
    }

    // ---- 1: Shadow Rush ----
    private static boolean dash(ServerPlayerEntity p) {
        ServerWorld w = p.getServerWorld();
        Vec3d look = p.getRotationVec(1.0f);
        w.spawnParticles(ParticleTypes.SMOKE, p.getX(), p.getY() + 1, p.getZ(), 25, 0.3, 0.6, 0.3, 0.02);
        p.addVelocity(look.x * 2.2, 0.25, look.z * 2.2);
        p.velocityModified = true;
        p.fallDistance = 0;
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 30, 4));
        w.spawnParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 30, 0.4, 0.6, 0.4, 0.05);
        w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
                SoundCategory.PLAYERS, 1.0f, 1.4f);
        return true;
    }

    // ---- 2: Time Echo ----
    private static boolean rewind(ServerPlayerEntity p) {
        ArrayDeque<Snap> h = HISTORY.get(p.getUuid());
        if (h == null || h.size() < 40) { bar(p, "Время ещё не накопилось"); return false; }
        Snap s = h.peekFirst();
        ServerWorld w = p.getServerWorld();
        if (!s.dim().equals(w.getRegistryKey())) { bar(p, "Эхо потеряно: ты сменил мир"); return false; }
        w.spawnParticles(ParticleTypes.REVERSE_PORTAL, p.getX(), p.getY() + 1, p.getZ(), 40, 0.4, 0.8, 0.4, 0.1);
        p.networkHandler.requestTeleport(s.x(), s.y(), s.z(), s.yaw(), s.pitch());
        p.setHealth(Math.min(p.getMaxHealth(), Math.max(p.getHealth(), s.health())));
        p.fallDistance = 0;
        p.extinguish();
        w.spawnParticles(ParticleTypes.REVERSE_PORTAL, s.x(), s.y() + 1, s.z(), 40, 0.4, 0.8, 0.4, 0.1);
        w.playSound(null, s.x(), s.y(), s.z(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 0.6f);
        h.clear();
        bar(p, "✦ Эхо времени: ты вернулся на 5 секунд назад");
        return true;
    }

    // ---- 3: Shadow Step (blink to where you look) ----
    private static boolean step(ServerPlayerEntity p) {
        ServerWorld w = p.getServerWorld();
        Vec3d look = p.getRotationVec(1.0f);
        HitResult hit = p.raycast(12.0, 1.0f, false);
        Vec3d target;
        if (hit instanceof BlockHitResult b && hit.getType() == HitResult.Type.BLOCK) {
            Direction side = b.getSide();
            Vec3d pos = b.getPos().add(Vec3d.of(side.getVector()).multiply(0.6));
            if (side == Direction.UP) target = new Vec3d(pos.x, pos.y + 0.05, pos.z);
            else if (side == Direction.DOWN) target = new Vec3d(pos.x, pos.y - 2.2, pos.z);
            else target = new Vec3d(pos.x, pos.y - 1.5, pos.z);
        } else {
            Vec3d end = p.getEyePos().add(look.multiply(12.0));
            target = new Vec3d(end.x, end.y - 1.62, end.z);
        }
        w.spawnParticles(ParticleTypes.SMOKE, p.getX(), p.getY() + 1, p.getZ(), 30, 0.3, 0.7, 0.3, 0.03);
        p.networkHandler.requestTeleport(target.x, target.y, target.z, p.getYaw(), p.getPitch());
        p.fallDistance = 0;
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 60, 0, false, false));
        w.spawnParticles(ParticleTypes.PORTAL, target.x, target.y + 1, target.z, 40, 0.4, 0.7, 0.4, 0.2);
        w.playSound(null, target.x, target.y, target.z, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 0.8f, 1.5f);
        return true;
    }

    public static void tick(MinecraftServer server) {
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            UUID id = p.getUuid();
            float e = Math.min(100f, ENERGY.getOrDefault(id, 100f) + 0.1f);
            ENERGY.put(id, e);

            ArrayDeque<Snap> h = HISTORY.computeIfAbsent(id, k -> new ArrayDeque<>());
            h.addLast(new Snap(p.getX(), p.getY(), p.getZ(), p.getYaw(), p.getPitch(), p.getHealth(),
                    p.getServerWorld().getRegistryKey()));
            while (h.size() > 100) h.pollFirst();

            if (server.getTicks() % 10 == 0
                    && (p.getMainHandStack().isOf(AnimeSaga.HERO_CHARM) || p.getOffHandStack().isOf(AnimeSaga.HERO_CHARM))) {
                int m = MODE.getOrDefault(id, 0);
                String lock = unlocked(p, m) ? "" : " (закрыто)";
                bar(p, "✦ " + NAMES[m] + lock + " | Энергия " + (int) e + "/100 | Shift+ПКМ: смена");
            }
        }
    }
}
