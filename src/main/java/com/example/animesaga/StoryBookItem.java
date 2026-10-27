package com.example.animesaga;

import com.example.animesaga.story.StoryManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class StoryBookItem extends Item {
    public StoryBookItem(Settings settings) { super(settings); }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient && user instanceof ServerPlayerEntity sp) {
            StoryManager.menu(sp);
        }
        return TypedActionResult.success(user.getStackInHand(hand), world.isClient());
    }
}
