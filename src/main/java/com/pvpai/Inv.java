package com.pvpai;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;

import java.util.function.Predicate;

/** Inventory helpers. */
final class Inv {
    private Inv() { }
    static boolean isFood(ItemStack s) { return s.isOf(Items.GOLDEN_APPLE) || s.isOf(Items.ENCHANTED_GOLDEN_APPLE); }
    static boolean isPearl(ItemStack s) { return s.isOf(Items.ENDER_PEARL); }

    static int hotbar(ClientPlayerEntity p, Predicate<ItemStack> f) {
        for (int i = 0; i < 9; i++) if (f.test(p.getInventory().getStack(i))) return i;
        return -1;
    }
    static boolean has(ClientPlayerEntity p, Predicate<ItemStack> f) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.size(); i++) if (f.test(inv.getStack(i))) return true;
        return false;
    }
    static int sword(ClientPlayerEntity p) {
        int s = hotbar(p, x -> x.isIn(ItemTags.SWORDS));
        return s >= 0 ? s : hotbar(p, x -> x.isIn(ItemTags.AXES));
    }
    static int selected(ClientPlayerEntity p) { return p.getInventory().getSelectedSlot(); }
    static void select(ClientPlayerEntity p, int i) {
        if (i >= 0 && i < 9 && selected(p) != i) p.getInventory().setSelectedSlot(i);
    }
}
