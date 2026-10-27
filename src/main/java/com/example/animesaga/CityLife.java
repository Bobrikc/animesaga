package com.example.animesaga;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Scripted daily routines: ambient townsfolk walk between points in a loop and "enter" houses
 * (they vanish inside for a while, then come out again). Positions are relative to the town center.
 */
public final class CityLife {
    private enum Phase { MOVE, WAIT, INSIDE }

    private record Stop(int x, int z, int delay, int ix, int iz, boolean enter) {}

    private static Stop walk(int x, int z, int wait) { return new Stop(x, z, wait, 0, 0, false); }
    private static Stop enter(int x, int z, int ix, int iz, int wait) { return new Stop(x, z, wait, ix, iz, true); }

    private static final class Walker {
        final ServerWorld world; final UUID id; final BlockPos center; final Stop[] route;
        int idx = 0; Phase phase = Phase.MOVE; int timer = 0; int frozen = 0;
        Walker(ServerWorld w, UUID id, BlockPos c, Stop[] r) { world = w; this.id = id; center = c; route = r; }
    }

    private static final double SPEED = 0.1;
    private static final Map<UUID, Walker> WALKERS = new HashMap<>();
    private static final Map<String, Stop[]> ROUTES = new HashMap<>();

    static {
        // houses: NW door (-11,-5)/in(-11,-8), NE (11,-5)/(11,-8), SW (-11,5)/(-11,8), SE (11,5)/(11,8), Academy (0,-11)/(0,-15)
        ROUTES.put("Пекарша Мика", new Stop[]{walk(-6, 3, 60), walk(-4, -4, 40), enter(-11, -5, -11, -8, 300),
                walk(-6, 3, 60), walk(-2, 5, 40), enter(-11, 5, -11, 8, 300)});
        ROUTES.put("Торговец Хиро", new Stop[]{walk(6, -3, 60), walk(10, 3, 40), enter(11, -5, 11, -8, 260),
                walk(6, -3, 60), walk(4, 5, 40), enter(11, 5, 11, 8, 260)});
        ROUTES.put("Старик Дайки", new Stop[]{walk(-12, -3, 100), walk(-6, -3, 60), enter(-11, -5, -11, -8, 400),
                walk(-3, 4, 80)});
        ROUTES.put("Девочка Юка", new Stop[]{walk(4, 8, 60), walk(2, 4, 40), walk(8, 3, 40), enter(11, 5, 11, 8, 250)});
        ROUTES.put("Стражник Тацуя", new Stop[]{walk(0, 12, 80), walk(-8, 3, 40), walk(8, 3, 40), walk(3, -3, 0),
                enter(0, -11, 0, -15, 200), walk(3, -5, 0), walk(3, 5, 0)});
        ROUTES.put("Рыбак Сабуро", new Stop[]{walk(12, 3, 80), walk(16, 3, 40), walk(8, 3, 40),
                enter(11, -5, 11, -8, 300)});
    }

    /** Called for every entity loaded into a world (new spawn or chunk load after restart). */
    public static void onLoad(Entity e, ServerWorld w) {
        if (!(e instanceof VillagerEntity v) || !v.getCommandTags().contains(Town.TAG) || v.getCustomName() == null) return;
        Stop[] route = ROUTES.get(v.getCustomName().getString());
        BlockPos c = Town.centerOf(v);
        if (route == null || c == null) return;

        v.setAiDisabled(true);
        v.setNoGravity(true);
        if (v.isInvisible()) {
            Stop s0 = route[0];
            v.refreshPositionAndAngles(c.getX() + s0.x() + 0.5, c.getY(), c.getZ() + s0.z() + 0.5, v.getYaw(), 0f);
        }
        v.setInvisible(false);
        v.setCustomNameVisible(true);
        v.setSilent(false);
        WALKERS.put(v.getUuid(), new Walker(w, v.getUuid(), c, route));
    }

    public static void freeze(UUID id, int ticks) {
        Walker k = WALKERS.get(id);
        if (k != null) k.frozen = ticks;
    }

    public static void tick(MinecraftServer server) {
        Iterator<Walker> it = WALKERS.values().iterator();
        while (it.hasNext()) {
            Walker k = it.next();
            Entity e = k.world.getEntity(k.id);
            if (!(e instanceof VillagerEntity v) || v.isRemoved()) { it.remove(); continue; }
            if (k.frozen > 0) { k.frozen--; continue; }

            Stop s = k.route[k.idx];
            double y = k.center.getY();
            int cx = k.center.getX(), cz = k.center.getZ();
            switch (k.phase) {
                case MOVE -> {
                    double tx = cx + s.x() + 0.5, tz = cz + s.z() + 0.5;
                    double dx = tx - v.getX(), dz = tz - v.getZ();
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist <= 0.15) {
                        if (s.enter()) {
                            v.refreshPositionAndAngles(cx + s.ix() + 0.5, y, cz + s.iz() + 0.5, v.getYaw(), 0f);
                            v.setInvisible(true);
                            v.setCustomNameVisible(false);
                            v.setSilent(true);
                            k.phase = Phase.INSIDE;
                        } else {
                            k.phase = Phase.WAIT;
                        }
                        k.timer = s.delay();
                    } else {
                        double st = Math.min(SPEED, dist);
                        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
                        v.refreshPositionAndAngles(v.getX() + dx / dist * st, y, v.getZ() + dz / dist * st, yaw, 0f);
                        v.setHeadYaw(yaw);
                        v.setBodyYaw(yaw);
                    }
                }
                case WAIT -> {
                    if (--k.timer <= 0) { k.idx = (k.idx + 1) % k.route.length; k.phase = Phase.MOVE; }
                }
                case INSIDE -> {
                    if (--k.timer <= 0) {
                        v.refreshPositionAndAngles(cx + s.x() + 0.5, y, cz + s.z() + 0.5, v.getYaw(), 0f);
                        v.setInvisible(false);
                        v.setCustomNameVisible(true);
                        v.setSilent(false);
                        k.idx = (k.idx + 1) % k.route.length;
                        k.phase = Phase.MOVE;
                    }
                }
            }
        }
    }
}
