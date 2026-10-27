package com.example.animesaga;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/** Seal of Awakening: right-click = use ability, Shift+right-click = switch ability. */
public class HeroCharmItem extends Item {
    public HeroCharmItem(Settings settings) { super(settings); }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient && user instanceof ServerPlayerEntity sp) {
            AbilityManager.use(sp);
        }
        return TypedActionResult.success(user.getStackInHand(hand), world.isClient());
    }
}
