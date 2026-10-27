# PROJECT_STATE — animesaga (story mod)
Minecraft Java 1.21.1, Fabric, Java 21. NEVER compiled (no network in Claude sandbox) — expect small API errors.

## Done
- Story engine (scenes, choices, flags, branching, timed dialogue), Story Book, /story commands.
- Town «Хоси»: built ONCE on the player's first join (tag animesaga_started) + /story town to rebuild manually.
- Living town (CityLife): 6 ambient villagers walk loops and "enter" houses (vanish inside, then exit).
  NPCs are tagged animesaga_npc + animesaga_c_<x>_<y>_<z>; routines re-attach on chunk load (ENTITY_LOAD).
- Gifts (AbilityManager, Star Energy): 0 Echo of Thoughts (L1), 1 Shadow Rush (L1), 2 Time Echo (L2), 3 Shadow Step (L3).
  Thoughts depend on story progress (tags animesaga_done1/2/3).
- Line 1 «Пробуждение звезды», Line 2 «Лунная тень», Line 3 «Фестиваль звёзд» (adds festival stalls at start).

## TODO / ideas
- Save story progress across restarts, music, real cutscene camera, boss fights, more NPC thoughts, more lines.

## Build
IntelliJ IDEA -> Open folder -> Gradle -> Tasks -> build -> build; jar in build/libs/animesaga-0.1.0.jar
Use a NEW world. Test commands: /story unlock, /story start N, /story skip, /story town
