package com.example.animesaga.story;

import java.util.LinkedHashMap;
import java.util.Map;

public class StoryLine {
    public final String title;
    public final String start;
    public final Map<String, Scene> scenes = new LinkedHashMap<>();

    public StoryLine(String title, String start) { this.title = title; this.start = start; }

    public Scene add(String id, String speaker, String text) {
        Scene s = new Scene(id, speaker, text);
        scenes.put(id, s);
        return s;
    }
}
