package com.example.animesaga;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/** Builds the small living town «Хоси» (41x41) around a point and fills it with NPCs. */
public final class Town {
    public static final String TAG = "animesaga_npc";
    private static final int R = 20;

    private static final Map<String, String[]> LINES = new HashMap<>();
    /** thoughts[name][stage 0..2][variant] */
    private static final Map<String, String[][]> THOUGHTS = new HashMap<>();

    static {
        LINES.put("Рин", new String[]{"Если нужна помощь, просто скажи.", "Город кажется тихим, но я чувствую: что-то приближается."});
        LINES.put("Кэндзи", new String[]{"Тренируешься? Правильно. Враг не станет ждать.", "Не расслабляйся, новенький."});
        LINES.put("Сэйдзи", new String[]{"Книга Сюжета у тебя? Продолжай свой путь.", "Звёзды помнят каждого, кто к ним прикоснулся."});
        LINES.put("Пекарша Мика", new String[]{"Свежие булочки! Для героев — за полную цену.", "Говорят, ночью над крышами бродят тени. Я закрываю ставни."});
        LINES.put("Торговец Хиро", new String[]{"Фонари для Фестиваля Звёзд раскупают быстрее, чем я их делаю.", "Купи что-нибудь, и я расскажу тебе секрет. Шучу, секретов у меня нет."});
        LINES.put("Старик Дайки", new String[]{"В моё время звёзды падали прямо на площадь.", "Фонтан старше этого города. Верь мне."});
        LINES.put("Девочка Юка", new String[]{"Мой брат Хару обещал сделать самый большой фонарик.", "Ты правда из Академии? Покажешь фокус?"});
        LINES.put("Стражник Тацуя", new String[]{"Город под защитой. Не задавай лишних вопросов.", "Ночью лучше не гулять. Это совет, не приказ."});
        LINES.put("Рыбак Сабуро", new String[]{"Рыба сегодня не клюёт. Наверное, чует грозу.", "У реки по ночам светятся огоньки. Красиво и жутко."});

        THOUGHTS.put("Рин", new String[][]{
                {"Он выглядит растерянным... совсем как я когда-то.", "Надо не показать, как мне страшно."},
                {"Если Охотники нашли его так быстро, найдут и остальных.", "Мне нельзя бояться. Но я боюсь."},
                {"Почему сердце бьётся так странно, когда я смотрю на звёзды?", "Только бы он не узнал мой секрет."}});
        THOUGHTS.put("Кэндзи", new String[][]{
                {"Слабак? Нет... Но проверить стоит.", "Я стану лучшим бойцом Академии. Любой ценой."},
                {"Я не смог ничего сделать. В следующий раз буду быстрее.", "Он сильнее меня? Нет. Пока нет."},
                {"Если что-то случится на празднике, я прикрою всех.", "Почему у Рин такие испуганные глаза?"}});
        THOUGHTS.put("Сэйдзи", new String[][]{
                {"Три Звезды... Только бы я не опоздал.", "Он напоминает мне её. Ту, что исчезла шестнадцать лет назад."},
                {"Охотники стали смелее. Кто-то направляет их.", "Печать готова ко второму дару."},
                {"Фестиваль — идеальная ночь для нападения. Но отменить его нельзя.", "Звёздный свет сегодня слишком яркий."}});
        THOUGHTS.put("Пекарша Мика", new String[][]{
                {"Тесто опять не поднялось. Это из-за погоды.", "Новенький голодный? Надо угостить."},
                {"Ставни на замок. Тени не любят запах хлеба... надеюсь.", "Скорей бы закончилась эта неделя."},
                {"Звёздный крем должен получиться идеально!", "Если все придут, распродам всё до полуночи."}});
        THOUGHTS.put("Торговец Хиро", new String[][]{
                {"Продам три фонаря — и неделя окупилась.", "Этот новичок похож на героя. Героям скидки не положены."},
                {"Покупатели боятся выходить после заката. Убытки!", "Может, закрыть лавку пораньше?"},
                {"Столько фонарей! Руки не успевают считать монеты.", "Загадаю удачную торговлю."}});
        THOUGHTS.put("Старик Дайки", new String[][]{
                {"Когда-то я тоже мечтал об Академии...", "Фонтан шумит, как в молодости."},
                {"Эти тени... Я видел такие в детстве. Тогда город чуть не пал.", "Надо рассказать директору. Хотя он и так знает."},
                {"Помню Фестиваль шестнадцать лет назад. Тогда тоже упала звезда.", "Хорошо, что город снова улыбается."}});
        THOUGHTS.put("Девочка Юка", new String[][]{
                {"Хару обещал вернуться к обеду. Я подожду.", "Интересно, он правда умеет фокусы?"},
                {"Хару не вернулся... Я не буду плакать. Нет.", "Если попросить героев, они найдут его."},
                {"Хару рядом, всё хорошо! Запустим самый большой фонарик!", "Почему мне кажется, что я светюсь изнутри?"}});
        THOUGHTS.put("Стражник Тацуя", new String[][]{
                {"Опять никаких происшествий. И слава небесам.", "Новенький без значка... Надо присмотреть."},
                {"Я видел тень у моста и промолчал. Сочтут трусом.", "Руки дрожат. Спрячь их, Тацуя."},
                {"Теперь я не буду молчать. Что бы ни случилось.", "Фестиваль под охраной. Под моей охраной."}});
        THOUGHTS.put("Рыбак Сабуро", new String[][]{
                {"Рыба не клюёт. Наверное, чует грозу.", "У реки огоньки... Красиво. И страшно."},
                {"Огоньки у реки были похожи на глаза. Ночью туда больше не пойду.", "Лучше без улова, чем без головы."},
                {"Фестиваль! Сегодня улов не важен.", "Запущу фонарик над рекой, пусть звёзды увидят."}});
    }

    public static String thought(String name, int stage, Random rnd) {
        String[][] t = THOUGHTS.get(name);
        if (t == null) return "...";
        String[] row = t[Math.max(0, Math.min(2, stage))];
        return row[rnd.nextInt(row.length)];
    }

    public static void timeCmd(ServerPlayerEntity p, String time) {
        var server = p.getServer();
        server.getCommandManager().executeWithPrefix(server.getCommandSource().withSilent(), "time set " + time);
    }

    public static BlockPos centerOf(Entity e) {
        for (String t : e.getCommandTags()) {
            if (t.startsWith("animesaga_c_")) {
                String[] s = t.split("_");
                try {
                    return new BlockPos(Integer.parseInt(s[2]), Integer.parseInt(s[3]), Integer.parseInt(s[4]));
                } catch (Exception ignored) { }
            }
        }
        return null;
    }

    public static ActionResult talk(PlayerEntity player, World world, Hand hand, Entity entity) {
        if (hand != Hand.MAIN_HAND || !(entity instanceof VillagerEntity v) || !v.getCommandTags().contains(TAG)) {
            return ActionResult.PASS;
        }
        if (!world.isClient && v.getCustomName() != null) {
            String name = v.getCustomName().getString();
            String[] ls = LINES.getOrDefault(name, new String[]{"..."});
            String line = ls[world.random.nextInt(ls.length)];
            player.sendMessage(Text.literal("[" + name + "] ").formatted(Formatting.GOLD, Formatting.BOLD)
                    .append(Text.literal(line).formatted(Formatting.WHITE)));
            CityLife.freeze(v.getUuid(), 100);
        }
        return ActionResult.SUCCESS;
    }

    /** Festival decorations: 4 market stalls with striped roofs. Used at the start of Line 3. */
    public static void festival(ServerPlayerEntity p) {
        ServerWorld w = p.getServerWorld();
        List<VillagerEntity> l = w.getEntitiesByClass(VillagerEntity.class,
                new Box(p.getBlockPos()).expand(64), e -> e.getCommandTags().contains(TAG));
        BlockPos c = null;
        for (VillagerEntity e : l) { c = centerOf(e); if (c != null) break; }
        if (c == null) return;
        int[][] stalls = {{-6, -8}, {6, -8}, {-17, -3}, {17, -3}};
        for (int[] s : stalls) stall(w, c, s[0], s[1]);
        timeCmd(p, "12500");
    }

    private static void stall(ServerWorld w, BlockPos c, int dx, int dz) {
        int x = c.getX() + dx, z = c.getZ() + dz, y = c.getY();
        for (int i = -1; i <= 1; i++) set(w, new BlockPos(x, y, z + i), Blocks.SPRUCE_PLANKS.getDefaultState());
        for (int a = -1; a <= 1; a += 2) {
            for (int b = -1; b <= 1; b += 2) {
                for (int h = 0; h < 3; h++) set(w, new BlockPos(x + a, y + h, z + b), Blocks.OAK_FENCE.getDefaultState());
            }
        }
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                set(w, new BlockPos(x + a, y + 3, z + b),
                        ((a + b) & 1) == 0 ? Blocks.RED_WOOL.getDefaultState() : Blocks.WHITE_WOOL.getDefaultState());
            }
        }
        set(w, new BlockPos(x, y + 2, z), Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, true));
    }

    public static void build(ServerPlayerEntity player) {
        ServerWorld w = player.getServerWorld();
        BlockPos c = player.getBlockPos();
        int y0 = c.getY() - 1;
        int cx = c.getX(), cz = c.getZ();
        Random rnd = new Random(c.asLong());

        List<VillagerEntity> old = w.getEntitiesByClass(VillagerEntity.class,
                new Box(c).expand(R + 10), e -> e.getCommandTags().contains(TAG));
        for (VillagerEntity e : old) e.discard();

        BlockState[] flowers = {Blocks.POPPY.getDefaultState(), Blocks.DANDELION.getDefaultState(),
                Blocks.CORNFLOWER.getDefaultState(), Blocks.AZURE_BLUET.getDefaultState(),
                Blocks.SHORT_GRASS.getDefaultState(), Blocks.SHORT_GRASS.getDefaultState()};

        for (int dx = -R; dx <= R; dx++) {
            for (int dz = -R; dz <= R; dz++) {
                BlockPos g = new BlockPos(cx + dx, y0, cz + dz);
                boolean plaza = Math.abs(dx) <= 5 && Math.abs(dz) <= 5;
                boolean road = (Math.abs(dz) <= 1) || (Math.abs(dx) <= 1 && dz >= -10);
                for (int y = 1; y <= 3; y++) set(w, g.down(y), Blocks.DIRT.getDefaultState());
                for (int y = 1; y <= 10; y++) set(w, g.up(y), Blocks.AIR.getDefaultState());
                if (plaza) {
                    set(w, g, ((dx + dz) & 1) == 0 ? Blocks.STONE_BRICKS.getDefaultState() : Blocks.POLISHED_ANDESITE.getDefaultState());
                } else if (road) {
                    set(w, g, Blocks.DIRT_PATH.getDefaultState());
                } else {
                    set(w, g, Blocks.GRASS_BLOCK.getDefaultState());
                    if (rnd.nextInt(100) < 12) set(w, g.up(), flowers[rnd.nextInt(flowers.length)]);
                }
            }
        }

        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++)
                set(w, new BlockPos(cx + dx, y0 + 1, cz + dz),
                        (dx == 0 && dz == 0) ? Blocks.WATER.getDefaultState() : Blocks.STONE_BRICKS.getDefaultState());

        int[][] lamps = {{-5, -5}, {5, -5}, {-5, 5}, {5, 5}, {-12, 2}, {12, 2}, {-18, 2}, {18, 2}, {2, 10}, {2, 16}, {-2, -10}};
        for (int[] l : lamps) lamp(w, new BlockPos(cx + l[0], y0 + 1, cz + l[1]));

        house(w, cx, cz, y0, -14, -12, 7, 7, 4, Direction.SOUTH, Blocks.BIRCH_PLANKS.getDefaultState(), Blocks.SPRUCE_PLANKS.getDefaultState(), Blocks.SPRUCE_SLAB.getDefaultState());
        house(w, cx, cz, y0, 8, -12, 7, 7, 4, Direction.SOUTH, Blocks.SPRUCE_PLANKS.getDefaultState(), Blocks.DARK_OAK_PLANKS.getDefaultState(), Blocks.DARK_OAK_SLAB.getDefaultState());
        house(w, cx, cz, y0, -14, 6, 7, 7, 4, Direction.NORTH, Blocks.OAK_PLANKS.getDefaultState(), Blocks.SPRUCE_PLANKS.getDefaultState(), Blocks.SPRUCE_SLAB.getDefaultState());
        house(w, cx, cz, y0, 8, 6, 7, 7, 4, Direction.NORTH, Blocks.BIRCH_PLANKS.getDefaultState(), Blocks.OAK_PLANKS.getDefaultState(), Blocks.OAK_SLAB.getDefaultState());
        house(w, cx, cz, y0, -5, -19, 11, 8, 5, Direction.SOUTH, Blocks.STONE_BRICKS.getDefaultState(), Blocks.DEEPSLATE_BRICKS.getDefaultState(), Blocks.DEEPSLATE_BRICK_SLAB.getDefaultState());

        int fy = y0 + 1;
        String ct = "animesaga_c_" + cx + "_" + fy + "_" + cz;
        double y = fy;
        npc(w, "Рин", cx + 2.5, y, cz + 4.5, ct);
        npc(w, "Кэндзи", cx - 3.5, y, cz - 8.5, ct);
        npc(w, "Сэйдзи", cx + 0.5, y, cz - 15.5, ct);
        npc(w, "Пекарша Мика", cx - 6 + 0.5, y, cz + 3 + 0.5, ct);
        npc(w, "Торговец Хиро", cx + 6 + 0.5, y, cz - 3 + 0.5, ct);
        npc(w, "Старик Дайки", cx - 12 + 0.5, y, cz - 3 + 0.5, ct);
        npc(w, "Девочка Юка", cx + 4 + 0.5, y, cz + 8 + 0.5, ct);
        npc(w, "Стражник Тацуя", cx + 0.5, y, cz + 12 + 0.5, ct);
        npc(w, "Рыбак Сабуро", cx + 12 + 0.5, y, cz + 3 + 0.5, ct);

        timeCmd(player, "day");
        player.networkHandler.requestTeleport(cx + 0.5, y, cz + 7.5, 180f, 0f);
    }

    private static void set(ServerWorld w, BlockPos p, BlockState s) { w.setBlockState(p, s, 2); }

    private static void lamp(ServerWorld w, BlockPos base) {
        for (int i = 0; i < 3; i++) set(w, base.up(i), Blocks.OAK_FENCE.getDefaultState());
        set(w, base.up(3), Blocks.LANTERN.getDefaultState());
    }

    private static void house(ServerWorld w, int cx, int cz, int y0, int ox, int oz, int wd, int dp, int ht,
                              Direction door, BlockState wall, BlockState floorRoof, BlockState slab) {
        int x1 = cx + ox, z1 = cz + oz, x2 = x1 + wd - 1, z2 = z1 + dp - 1;
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
                boolean corner = (x == x1 || x == x2) && (z == z1 || z == z2);
                set(w, new BlockPos(x, y0, z), Blocks.OAK_PLANKS.getDefaultState());
                for (int h = 1; h <= ht; h++) {
                    BlockPos p = new BlockPos(x, y0 + h, z);
                    if (!edge) set(w, p, Blocks.AIR.getDefaultState());
                    else if (corner) set(w, p, Blocks.OAK_LOG.getDefaultState());
                    else if (h >= 2 && h < ht && ((x + z) % 3 == 0)) set(w, p, Blocks.GLASS.getDefaultState());
                    else set(w, p, wall);
                }
            }
        }
        for (int x = x1 - 1; x <= x2 + 1; x++) {
            for (int z = z1 - 1; z <= z2 + 1; z++) {
                boolean over = x < x1 || x > x2 || z < z1 || z > z2;
                set(w, new BlockPos(x, y0 + ht + 1, z), over ? slab : floorRoof);
            }
        }
        int dx = (x1 + x2) / 2;
        int dz = door == Direction.SOUTH ? z2 : z1;
        BlockPos d = new BlockPos(dx, y0 + 1, dz);
        set(w, d, Blocks.AIR.getDefaultState());
        set(w, d.up(), Blocks.AIR.getDefaultState());
        BlockState ds = Blocks.OAK_DOOR.getDefaultState().with(DoorBlock.FACING, door);
        set(w, d, ds.with(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        set(w, d.up(), ds.with(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        int step = door == Direction.SOUTH ? 1 : -1;
        for (int i = 1; i <= 12; i++) {
            BlockPos pp = new BlockPos(dx, y0, dz + step * i);
            if (Math.abs(pp.getZ() - cz) <= 1) break;
            set(w, pp, Blocks.DIRT_PATH.getDefaultState());
        }
        set(w, new BlockPos(x1 + 1, y0 + 1, z1 + 1), Blocks.CRAFTING_TABLE.getDefaultState());
        set(w, new BlockPos(x1 + 1, y0 + 2, z1 + 1), Blocks.LANTERN.getDefaultState());
        set(w, new BlockPos(x2 - 1, y0 + 1, z1 + 1), Blocks.BOOKSHELF.getDefaultState());
    }

    private static void npc(ServerWorld w, String name, double x, double y, double z, String centerTag) {
        VillagerEntity v = new VillagerEntity(EntityType.VILLAGER, w);
        v.refreshPositionAndAngles(x, y, z, 0f, 0f);
        v.setCustomName(Text.literal(name));
        v.setCustomNameVisible(true);
        v.setPersistent();
        v.setInvulnerable(true);
        v.setAiDisabled(true);
        v.setNoGravity(true);
        v.addCommandTag(TAG);
        v.addCommandTag(centerTag);
        w.spawnEntity(v);
    }
}
