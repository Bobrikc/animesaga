package com.example.animesaga.story;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.server.network.ServerPlayerEntity;

public class Scene {
    public record Choice(String label, String next, BiConsumer<ServerPlayerEntity, PlayerStory> action) {}

    public final String id;
    public final String speaker; // null = narrator
    public final String text;
    public String next;
    public Function<PlayerStory, String> branch;
    public BiConsumer<ServerPlayerEntity, PlayerStory> action;
    public final List<Choice> choices = new ArrayList<>();
    public String chapterTitle, chapterSub;

    public Scene(String id, String speaker, String text) {
        this.id = id; this.speaker = speaker; this.text = text;
    }

    public Scene to(String next) { this.next = next; return this; }
    public Scene opt(String label, String next) { choices.add(new Choice(label, next, null)); return this; }
    public Scene opt(String label, String next, BiConsumer<ServerPlayerEntity, PlayerStory> a) {
        choices.add(new Choice(label, next, a)); return this;
    }
    public Scene then(BiConsumer<ServerPlayerEntity, PlayerStory> a) { this.action = a; return this; }
    public Scene branch(Function<PlayerStory, String> f) { this.branch = f; return this; }
    public Scene chapter(String title, String sub) { this.chapterTitle = title; this.chapterSub = sub; return this; }

    /** Reading time in ticks (20 ticks = 1 second). */
    public int pause() { return 60 + (int) (text.length() * 3.5); }
}
