package com.example.animesaga.story;

import com.example.animesaga.story.Scene.Choice;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class StoryManager {
    public static final Map<Integer, StoryLine> LINES = new LinkedHashMap<>();
    private static final Map<UUID, PlayerStory> ACTIVE = new HashMap<>();

    static {
        LINES.put(1, Line1.build());
        LINES.put(2, Line2.build());
        LINES.put(3, Line3.build());
    }

    public static void menu(ServerPlayerEntity p) {
        p.sendMessage(Text.literal("=== КНИГА СЮЖЕТА ===").formatted(Formatting.GOLD, Formatting.BOLD));
        for (var e : LINES.entrySet()) {
            int n = e.getKey();
            p.sendMessage(Text.literal("  ▶ " + e.getValue().title).styled(s -> s
                    .withColor(Formatting.AQUA)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/story start " + n))));
        }
        p.sendMessage(Text.literal("(нажми на линию, чтобы начать)").formatted(Formatting.GRAY, Formatting.ITALIC));
    }

    public static void start(ServerPlayerEntity p, int n) {
        StoryLine line = LINES.get(n);
        if (line == null) return;
        PlayerStory st = new PlayerStory(n);
        ACTIVE.put(p.getUuid(), st);
        show(p, st, line.start);
    }

    public static void stop(ServerPlayerEntity p, boolean notify) {
        boolean had = ACTIVE.remove(p.getUuid()) != null;
        if (had && notify) p.sendMessage(Text.literal("Сюжет остановлен.").formatted(Formatting.GRAY));
    }

    public static void skip(ServerPlayerEntity p) {
        PlayerStory st = ACTIVE.get(p.getUuid());
        if (st != null && !st.waiting) advance(p, st);
    }

    public static void choose(ServerPlayerEntity p, int n) {
        PlayerStory st = ACTIVE.get(p.getUuid());
        if (st == null || !st.waiting) {
            p.sendMessage(Text.literal("Сейчас нечего выбирать.").formatted(Formatting.GRAY));
            return;
        }
        Scene sc = LINES.get(st.line).scenes.get(st.scene);
        if (n < 1 || n > sc.choices.size()) return;
        Choice ch = sc.choices.get(n - 1);
        st.waiting = false;
        if (ch.action() != null) ch.action().accept(p, st);
        p.sendMessage(Text.literal("Ты выбрал: " + ch.label()).formatted(Formatting.GREEN, Formatting.ITALIC));
        show(p, st, ch.next());
    }

    public static void tick(MinecraftServer server) {
        int now = server.getTicks();
        for (var e : new ArrayList<>(ACTIVE.entrySet())) {
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(e.getKey());
            if (p == null) { ACTIVE.remove(e.getKey()); continue; }
            PlayerStory st = e.getValue();
            if (!st.waiting && now >= st.waitUntil) advance(p, st);
        }
    }

    private static void advance(ServerPlayerEntity p, PlayerStory st) {
        Scene sc = LINES.get(st.line).scenes.get(st.scene);
        String nextId = sc.branch != null ? sc.branch.apply(st) : sc.next;
        show(p, st, nextId);
    }

    private static void show(ServerPlayerEntity p, PlayerStory st, String id) {
        Scene sc = LINES.get(st.line).scenes.get(id);
        if (sc == null) { ACTIVE.remove(p.getUuid()); return; }
        st.scene = id;
        st.waiting = false;
        if (sc.action != null) sc.action.accept(p, st);

        if (sc.chapterTitle != null) {
            p.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 70, 20));
            p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal(sc.chapterSub == null ? "" : sc.chapterSub)));
            p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal(sc.chapterTitle).formatted(Formatting.GOLD)));
        }
        print(p, sc);
        p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(),
                SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.3f, 1.6f);

        if (!sc.choices.isEmpty()) {
            st.waiting = true;
            for (int i = 0; i < sc.choices.size(); i++) {
                int n = i + 1;
                p.sendMessage(Text.literal("   [" + n + "] " + sc.choices.get(i).label()).styled(s -> s
                        .withColor(Formatting.AQUA)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/story choose " + n))));
            }
        } else if (sc.next == null && sc.branch == null) {
            ACTIVE.remove(p.getUuid());
            p.sendMessage(Text.literal("— Конец —").formatted(Formatting.GOLD, Formatting.BOLD));
        } else {
            st.waitUntil = p.getServer().getTicks() + sc.pause();
        }
    }

    private static void print(ServerPlayerEntity p, Scene sc) {
        String text = sc.text.replace("{p}", p.getName().getString());
        p.sendMessage(Text.literal(""));
        if (sc.speaker == null) {
            p.sendMessage(Text.literal(text).formatted(Formatting.GRAY, Formatting.ITALIC));
        } else {
            p.sendMessage(Text.literal("[" + sc.speaker + "] ").formatted(color(sc.speaker), Formatting.BOLD)
                    .append(Text.literal(text).formatted(Formatting.WHITE)));
        }
    }

    private static Formatting color(String speaker) {
        return switch (speaker) {
            case "Рин" -> Formatting.LIGHT_PURPLE;
            case "Кэндзи" -> Formatting.RED;
            case "Сэйдзи" -> Formatting.YELLOW;
            case "Охотник", "???" -> Formatting.DARK_PURPLE;
            case "Ты" -> Formatting.AQUA;
            default -> Formatting.GOLD;
        };
    }
}
