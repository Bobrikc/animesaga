package com.example.animesaga;

import com.example.animesaga.story.StoryManager;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class AnimeSaga implements ModInitializer {
    public static final String MOD_ID = "animesaga";
    private static final Map<UUID, Integer> PENDING_START = new HashMap<>();

    private static Identifier id(String path) { return Identifier.of(MOD_ID, path); }

    public static final Item STORY_BOOK = Registry.register(Registries.ITEM, id("story_book"),
            new StoryBookItem(new Item.Settings().maxCount(1)));
    public static final Item HERO_CHARM = Registry.register(Registries.ITEM, id("hero_charm"),
            new HeroCharmItem(new Item.Settings().maxCount(1)));

    @Override
    public void onInitialize() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(e -> {
            e.add(STORY_BOOK);
            e.add(HERO_CHARM);
        });

        // Story starts right away on the player's very first join: build the town, then Line 1.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity p = handler.getPlayer();
            if (p.getInventory().count(STORY_BOOK) == 0) p.giveItemStack(new ItemStack(STORY_BOOK));
            if (p.addCommandTag("animesaga_started")) PENDING_START.put(p.getUuid(), 80);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            StoryManager.stop(handler.getPlayer(), false);
            AbilityManager.forget(handler.getPlayer().getUuid());
            PENDING_START.remove(handler.getPlayer().getUuid());
        });

        ServerTickEvents.END_SERVER_TICK.register(this::tick);
        ServerEntityEvents.ENTITY_LOAD.register(CityLife::onLoad);
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> Town.talk(player, world, hand, entity));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("story")
                        .then(CommandManager.literal("menu").executes(ctx -> {
                            StoryManager.menu(ctx.getSource().getPlayerOrThrow());
                            return 1;
                        }))
                        .then(CommandManager.literal("start").then(
                                CommandManager.argument("line", IntegerArgumentType.integer(1, 3)).executes(ctx -> {
                                    StoryManager.start(ctx.getSource().getPlayerOrThrow(),
                                            IntegerArgumentType.getInteger(ctx, "line"));
                                    return 1;
                                })))
                        .then(CommandManager.literal("choose").then(
                                CommandManager.argument("n", IntegerArgumentType.integer(1, 9)).executes(ctx -> {
                                    StoryManager.choose(ctx.getSource().getPlayerOrThrow(),
                                            IntegerArgumentType.getInteger(ctx, "n"));
                                    return 1;
                                })))
                        .then(CommandManager.literal("skip").executes(ctx -> {
                            StoryManager.skip(ctx.getSource().getPlayerOrThrow());
                            return 1;
                        }))
                        .then(CommandManager.literal("stop").executes(ctx -> {
                            StoryManager.stop(ctx.getSource().getPlayerOrThrow(), true);
                            return 1;
                        }))
                        .then(CommandManager.literal("charm").executes(ctx -> {
                            ctx.getSource().getPlayerOrThrow().giveItemStack(new ItemStack(HERO_CHARM));
                            return 1;
                        }))
                        .then(CommandManager.literal("unlock").executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                            for (int i = 0; i < 4; i++) AbilityManager.unlock(p, i);
                            return 1;
                        }))
                        .then(CommandManager.literal("town").executes(ctx -> {
                            Town.build(ctx.getSource().getPlayerOrThrow());
                            return 1;
                        }))));
    }

    private void tick(MinecraftServer server) {
        AbilityManager.tick(server);
        StoryManager.tick(server);
        CityLife.tick(server);
        for (UUID uuid : new ArrayList<>(PENDING_START.keySet())) {
            int left = PENDING_START.get(uuid) - 1;
            if (left > 0) { PENDING_START.put(uuid, left); continue; }
            PENDING_START.remove(uuid);
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(uuid);
            if (p != null) {
                Town.build(p);
                StoryManager.start(p, 1);
            }
        }
    }
}
