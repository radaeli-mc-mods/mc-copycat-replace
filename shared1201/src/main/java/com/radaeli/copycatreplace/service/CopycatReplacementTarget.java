package com.radaeli.copycatreplace.service;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** A copycat, or one copycat part, whose material can be replaced. */
@FunctionalInterface
public interface CopycatReplacementTarget {
    void replaceMaterial(Player player, InteractionHand materialHand, ItemStack materialStack);
}
