package com.pvpai;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/** Crystal PvP: place obsidian and end crystals next to the enemy, break crystals at a safe distance. */
final class Crystal {
    private Crystal() { }
    private static int cd = 0;

    static void face(ClientPlayerEntity p, double x, double y, double z) {
        double dx = x - p.getX(), dy = y - p.getEyeY(), dz = z - p.getZ();
        p.setYaw((float) (Math.toDegrees(Math.atan2(dz, dx)) - 90));
        p.setPitch((float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz))));
    }

    /** Returns true if it did something this tick. want[0] is set to the hotbar slot that should stay selected. */
    static boolean run(MinecraftClient mc, ClientPlayerEntity p, LivingEntity t, int[] want) {
        if (cd > 0) cd--;
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof EndCrystalEntity)) continue;
            double dm = p.distanceTo(e);
            if (dm >= 2.8 && dm <= 4.5 && e.distanceTo(t) <= 5.5) {
                face(p, e.getX(), e.getY() + 0.8, e.getZ());
                mc.interactionManager.attackEntity(p, e);
                p.swingHand(Hand.MAIN_HAND);
                return true;
            }
        }
        if (cd > 0) return false;
        int cs = Inv.hotbar(p, s -> s.isOf(Items.END_CRYSTAL));
        int os = Inv.hotbar(p, s -> s.isOf(Items.OBSIDIAN));
        if (cs < 0) return false;

        BlockPos tb = t.getBlockPos(), best = null;
        boolean bestObs = false;
        double bd = 99;
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) for (int dy = -1; dy <= 0; dy++) {
            if (dx == 0 && dz == 0) continue;
            BlockPos b = tb.add(dx, dy, dz);
            double ex = b.getX() + .5 - p.getX(), ez = b.getZ() + .5 - p.getZ(), ey = b.getY() + 1 - p.getEyeY();
            if (Math.sqrt(ex * ex + ey * ey + ez * ez) > 4.4 || Math.hypot(ex, ez) < 2.5) continue;
            var st = mc.world.getBlockState(b);
            boolean base = st.isOf(Blocks.OBSIDIAN) || st.isOf(Blocks.BEDROCK);
            boolean air = st.isAir() && os >= 0 && !mc.world.getBlockState(b.down()).isAir();
            if (!base && !air) continue;
            if (!mc.world.getBlockState(b.up()).isAir() || !mc.world.getBlockState(b.up(2)).isAir()) continue;
            double d = Math.hypot(b.getX() + .5 - t.getX(), b.getZ() + .5 - t.getZ());
            if (d > 3.5 || d >= bd) continue;
            bd = d; best = b; bestObs = !base;
        }
        if (best == null) return false;

        BlockPos click = bestObs ? best.down() : best;
        int slot = bestObs ? os : cs;
        Inv.select(p, slot);
        want[0] = slot;
        Vec3d hit = new Vec3d(click.getX() + .5, click.getY() + 1.0, click.getZ() + .5);
        face(p, hit.x, hit.y, hit.z);
        mc.interactionManager.interactBlock(p, Hand.MAIN_HAND, new BlockHitResult(hit, Direction.UP, click, false));
        p.swingHand(Hand.MAIN_HAND);
        cd = 3;
        return true;
    }
}
