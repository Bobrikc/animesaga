package com.example.animesaga.story;

import java.util.HashMap;
import java.util.Map;

public class PlayerStory {
    public final int line;
    public String scene;
    public boolean waiting;
    public int waitUntil;
    public final Map<String, Integer> flags = new HashMap<>();

    public PlayerStory(int line) { this.line = line; }

    public int get(String key) { return flags.getOrDefault(key, 0); }
    public void add(String key, int v) { flags.merge(key, v, Integer::sum); }
}
