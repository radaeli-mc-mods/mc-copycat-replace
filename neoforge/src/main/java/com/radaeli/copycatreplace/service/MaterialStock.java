package com.radaeli.copycatreplace.service;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

/** Snapshot budgets exclude armor, the other hand and items returned during this action. */
public final class MaterialStock {
    private final List<Source> sources = new ArrayList<>();
    private final ItemStack creativeStack;
    private int cursor;
    public MaterialStock(Player player, ItemStack held) {
        creativeStack = player.isCreative() ? held : ItemStack.EMPTY;
        if (held.isEmpty()) return;
        sources.add(new Source(held));
        for (ItemStack stack : player.getInventory().items) {
            if (stack != held && !stack.isEmpty() && ItemStack.isSameItemSameComponents(held, stack))
                sources.add(new Source(stack));
        }
    }
    public int count() {
        return sources.stream().mapToInt(s -> Math.min(s.remaining, s.stack.getCount())).sum();
    }
    public ItemStack next() {
        if (!creativeStack.isEmpty()) return creativeStack;
        while (cursor < sources.size()) {
            Source source = sources.get(cursor);
            if (source.remaining > 0 && !source.stack.isEmpty()) return source.stack;
            cursor++;
        }
        return ItemStack.EMPTY;
    }
    public void consumed(int amount) {
        if (creativeStack.isEmpty() && cursor < sources.size()) sources.get(cursor).remaining -= amount;
    }
    private static final class Source {
        final ItemStack stack;
        int remaining;
        Source(ItemStack stack) { this.stack = stack; remaining = stack.getCount(); }
    }
}
