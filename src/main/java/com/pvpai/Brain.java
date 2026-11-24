package com.pvpai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Plays PvP, watches everyone (including you), and learns after every fight. */
public class Brain {
    public static final Brain I = new Brain();

    public static class Profile { public int kills, losses, ticks; public double aim, flip = 30; }

    public static class Save {
        public Genome best = new Genome();
        public double bestScore;
        public Net net = new Net(), netBest = new Net();
        public double netScore;
        public int kills, deaths, generation = 1, swings, hits;
        public String lastLoss = "No deaths yet.";
        public List<String> friends = new ArrayList<>();
        public Map<String, Profile> profiles = new HashMap<>();
    }

    public static class Obs {
        public String name = "?";
        public double lx, lz, aimSum; public float lyaw;
        public int ticks, flips, lastSign, last = -9;
        public double avgAim() { return ticks > 0 ? aimSum / ticks : 0; }
        public double ticksPerFlip() { return flips > 0 ? (double) ticks / flips : 30; }
    }

    // settings (toggled from the menu)
    public boolean enabled, learning = true, autoRespawn = true, useNet, crystalMode, training, watchMe = true, autoRecap = true;
    public Save save = new Save();
    public Genome current = new Genome();
    public final Map<UUID, Obs> obs = new HashMap<>();
    public final Obs me = new Obs();
    public List<float[]> recap = new ArrayList<>();
    public String recapInfo = "";
    public final List<float[][]> samples = new ArrayList<>();

    private final ArrayDeque<float[]> buf = new ArrayDeque<>();
    private final Random rnd = new Random();
    private LivingEntity target, killer, pendingSwing, countedDead;
    private boolean controlling, dead, critMode, eating, drawing, prevSwing;
    private int strafeTick, strafeDir = 1, tickCount, pendingTick, lastFightTick = -999, wtap, gearStep, gearCd, foodCd, pearlCd, eatStart, drawStart, lastTid = -1;
    private int lifeSwings, lifeHits, lifeTicks, lifeKills;
    private double lifeDamage, lastTX, lastTZ, enemySpeed;
    private float lastHp = 20, myPrevYaw;

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private Path file() { return FabricLoader.getInstance().getConfigDir().resolve("pvpai.json"); }
    private Path slotFile(int n) { return FabricLoader.getInstance().getConfigDir().resolve("pvpai_slot" + n + ".json"); }

    // ------------------------------------------------------------------ saving
    private void fix() {
        if (save == null) save = new Save();
        if (save.best == null) save.best = new Genome();
        if (save.net == null) save.net = new Net();
        if (save.netBest == null) save.netBest = save.net.copy();
        if (save.friends == null) save.friends = new ArrayList<>();
        if (save.profiles == null) save.profiles = new HashMap<>();
    }
    public void load() {
        try { if (Files.exists(file())) save = gson.fromJson(Files.readString(file()), Save.class); } catch (Exception e) { save = new Save(); }
        fix(); current = save.best.copy();
    }
    public void saveToDisk() { try { Files.writeString(file(), gson.toJson(save)); } catch (Exception ignored) { } }
    public void resetBrain() {
        List<String> fr = save.friends;
        save = new Save(); save.friends = fr; current = new Genome(); obs.clear(); samples.clear(); saveToDisk();
    }
    public void saveSlot(int n) { try { Files.writeString(slotFile(n), gson.toJson(save)); } catch (Exception ignored) { } }
    public boolean loadSlot(int n) {
        try {
            if (!Files.exists(slotFile(n))) return false;
            Save s = gson.fromJson(Files.readString(slotFile(n)), Save.class);
            if (s == null) return false;
            List<String> fr = save.friends;
            save = s; fix(); save.friends = fr; current = save.best.copy(); saveToDisk();
            return true;
        } catch (Exception e) { return false; }
    }
    public String slotInfo(int n) {
        try {
            if (!Files.exists(slotFile(n))) return "empty";
            Save s = gson.fromJson(Files.readString(slotFile(n)), Save.class);
            return "generation " + s.generation + ", kills " + s.kills + ", deaths " + s.deaths;
        } catch (Exception e) { return "unreadable"; }
    }

    // ------------------------------------------------------------------ small helpers
    public double hitRate() { return save.swings > 0 ? 100.0 * save.hits / save.swings : 0; }
    public boolean isFriend(String name) { return save.friends.contains(name.toLowerCase()); }
    public void addFriend(String n) { n = n.trim().toLowerCase(); if (!n.isEmpty() && !save.friends.contains(n)) save.friends.add(n); saveToDisk(); }
    public void removeFriend(String n) { save.friends.remove(n.trim().toLowerCase()); saveToDisk(); }
    public void setNeural(boolean b) { useNet = b; if (b && save.netScore <= 0) save.netBest = save.net.copy(); }
    public void applyMyStyle() {
        if (me.ticks < 100) return;
        current.aimSpeed = me.avgAim() * 3; current.strafePeriod = me.ticksPerFlip(); current.clamp();
        save.best = current.copy();
    }
    public void trainNow(int steps) {
        if (samples.size() < 20) return;
        for (int i = 0; i < steps; i++) { float[][] s = samples.get(rnd.nextInt(samples.size())); save.net.train(s[0], s[1], 0.02f); }
    }
    private Profile profile(String name) { return save.profiles.computeIfAbsent(name.toLowerCase(), k -> new Profile()); }
    private Profile profileOf(LivingEntity t) {
        return t instanceof PlayerEntity pe ? save.profiles.get(pe.getName().getString().toLowerCase()) : null;
    }
    private void record(float[] in, float[] out) {
        float[][] s = {in, out};
        if (samples.size() < 5000) samples.add(s); else samples.set(rnd.nextInt(samples.size()), s);
    }
    private void rec(double dist, float hp, float ehp, int act) {
        buf.addLast(new float[]{(float) dist, hp, ehp, act});
        if (buf.size() > 100) buf.removeFirst();
    }
    private void keys(MinecraftClient mc, boolean f, boolean b, boolean l, boolean r, boolean j, boolean s, boolean u) {
        var o = mc.options;
        o.forwardKey.setPressed(f); o.backKey.setPressed(b); o.leftKey.setPressed(l); o.rightKey.setPressed(r);
        o.jumpKey.setPressed(j); o.sprintKey.setPressed(s); o.useKey.setPressed(u);
    }
    private void stopControl() {
        if (controlling) { controlling = false; eating = false; drawing = false; KeyBinding.updatePressedStates(); }
    }
    private static float step(float cur, float want, double max) {
        float d = MathHelper.wrapDegrees(want - cur);
        return cur + (float) Math.max(-max, Math.min(max, d));
    }

    // ------------------------------------------------------------------ main tick
    public void tick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null) { controlling = false; return; }
        tickCount++;

        if (p.isDead() || p.getHealth() <= 0) {
            if (!dead) { dead = true; killer = target; onDeath(); }
            if (enabled && autoRespawn) p.requestRespawn();
            return;
        }
        if (dead) {
            dead = false;
            if (!enabled && autoRecap && !recap.isEmpty()) mc.setScreen(new RecapScreen());
        }

        observe(mc, p);
        trackCombat(p);
        if (!(enabled && useNet) && samples.size() >= 50) trainNow(16);
        if (tickCount % 1200 == 0) saveToDisk();

        if (!enabled) { stopControl(); watch(mc, p); return; }
        if (mc.currentScreen != null) { stopControl(); return; }
        fight(mc, p);
        if (tickCount % 10 == 0)
            p.sendMessage(Text.literal("AI " + (useNet ? "Neural" : "Skills") + (crystalMode ? " + Crystal" : "")
                    + " | HP " + (int) p.getHealth() + " | " + (target != null ? target.getName().getString() : "-")
                    + " | K/D " + save.kills + "/" + save.deaths), true);
    }

    // ------------------------------------------------------------------ watching
    private void lateral(Obs ob, double ox, double oz, double px, double pz) {
        double mx = ox - ob.lx, mz = oz - ob.lz, tx = px - ox, tz = pz - oz, tl = Math.hypot(tx, tz);
        if (tl > 0.1 && Math.hypot(mx, mz) > 0.04) {
            double side = (mx * -tz + mz * tx) / tl;
            int sg = side > 0.03 ? 1 : side < -0.03 ? -1 : 0;
            if (sg != 0) { if (ob.lastSign != 0 && sg != ob.lastSign) ob.flips++; ob.lastSign = sg; }
        }
    }

    private void observe(MinecraftClient mc, ClientPlayerEntity p) {
        if (obs.size() > 60) obs.clear();
        for (AbstractClientPlayerEntity o : mc.world.getPlayers()) {
            if (o == p || p.distanceTo(o) > 12) continue;
            Obs ob = obs.computeIfAbsent(o.getUuid(), k -> new Obs());
            ob.name = o.getName().getString();
            if (ob.last == tickCount - 1) {
                ob.aimSum += Math.abs(MathHelper.wrapDegrees(o.getYaw() - ob.lyaw));
                ob.ticks++;
                lateral(ob, o.getX(), o.getZ(), p.getX(), p.getZ());
            }
            ob.lx = o.getX(); ob.lz = o.getZ(); ob.lyaw = o.getYaw(); ob.last = tickCount;
        }
    }

    private void trackEnemy(LivingEntity t) {
        enemySpeed = t.getId() == lastTid ? Math.hypot(t.getX() - lastTX, t.getZ() - lastTZ) : 0;
        lastTid = t.getId(); lastTX = t.getX(); lastTZ = t.getZ();
    }

    /** While you play: learn your style and copy your actions into the neural net. */
    private void watch(MinecraftClient mc, ClientPlayerEntity p) {
        boolean sw = p.handSwinging, rise = sw && !prevSwing;
        prevSwing = sw;
        if (mc.currentScreen != null) return;
        LivingEntity t = pickTarget(mc, p);
        if (t == null || p.distanceTo(t) > 10) { myPrevYaw = p.getYaw(); me.last = -9; return; }
        target = t; lastFightTick = tickCount; lifeTicks++; trackEnemy(t);
        if (rise) { save.swings++; lifeSwings++; pendingSwing = t; pendingTick = tickCount; }
        if (watchMe) {
            if (me.last == tickCount - 1) {
                me.aimSum += Math.abs(MathHelper.wrapDegrees(p.getYaw() - myPrevYaw));
                me.ticks++;
                lateral(me, p.getX(), p.getZ(), t.getX(), t.getZ());
            }
            var o = mc.options;
            float fwd = (o.forwardKey.isPressed() ? 1 : 0) - (o.backKey.isPressed() ? 1 : 0);
            float str = (o.rightKey.isPressed() ? 1 : 0) - (o.leftKey.isPressed() ? 1 : 0);
            record(inputs(p, t), new float[]{fwd, str, o.jumpKey.isPressed() ? 1 : 0, rise ? 1 : 0, p.isBlocking() ? 1 : 0});
        }
        me.lx = p.getX(); me.lz = p.getZ(); me.last = tickCount; myPrevYaw = p.getYaw();
        rec(p.distanceTo(t), p.getHealth(), t.getHealth(), rise ? 1 : 0);
    }

    private void trackCombat(ClientPlayerEntity p) {
        float hp = p.getHealth();
        if (hp < lastHp) lifeDamage += lastHp - hp;
        lastHp = hp;
        if (pendingSwing != null) {
            if (tickCount - pendingTick > 8) pendingSwing = null;
            else if (pendingSwing.hurtTime > 0 && pendingSwing.hurtTime >= pendingSwing.maxHurtTime - 1) {
                save.hits++; lifeHits++; pendingSwing = null;
            }
        }
        if (target != null && target != countedDead && target.getHealth() <= 0 && tickCount - lastFightTick < 40) {
            countedDead = target; save.kills++; lifeKills++;
            if (target instanceof PlayerEntity pe) profile(pe.getName().getString()).kills++;
        }
    }

    // ------------------------------------------------------------------ choosing a target
    private boolean valid(ClientPlayerEntity p, LivingEntity e) {
        if (e.isSpectator() || e.getHealth() <= 0 || p.isTeammate(e)) return false;
        return !(e instanceof PlayerEntity pe && isFriend(pe.getName().getString()) && !training);
    }

    private LivingEntity pickTarget(MinecraftClient mc, ClientPlayerEntity p) {
        if (target != null && target.isAlive() && p.distanceTo(target) < 25 && valid(p, target)) return target;
        LivingEntity best = null; double bd = 25;
        for (AbstractClientPlayerEntity o : mc.world.getPlayers()) {
            if (o == p || !valid(p, o)) continue;
            double d = p.distanceTo(o);
            if (d < bd) { bd = d; best = o; }
        }
        if (training) for (Entity e : mc.world.getEntities()) {
            if (e instanceof HostileEntity h && h.isAlive()) {
                double d = p.distanceTo(h);
                if (d < bd) { bd = d; best = h; }
            }
        }
        return best;
    }

    /** Skills adjusted for this opponent: known players and their armor. */
    private Genome effective(LivingEntity t) {
        Genome e = current.copy();
        Profile pr = profileOf(t);
        if (pr != null && pr.losses > pr.kills) {
            e.aimSpeed *= 1.1; e.range -= 0.1;
            if (pr.flip < e.strafePeriod) e.strafePeriod = (e.strafePeriod + pr.flip) / 2;
        }
        int arm = t.getArmor();
        if (arm >= 14) e.critChance = Math.max(e.critChance, 0.7);
        else if (arm <= 4) { e.critChance *= 0.5; e.hitDelay -= 0.05; }
        e.clamp();
        return e;
    }

    private float[] inputs(ClientPlayerEntity p, LivingEntity t) {
        double dx = t.getX() - p.getX(), dz = t.getZ() - p.getZ(), dy = t.getY() + t.getHeight() * 0.65 - p.getEyeY();
        float wy = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90), wp = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
        float[] x = new float[Net.NIN];
        x[0] = (float) Math.min(2, p.distanceTo(t) / 6);
        x[1] = MathHelper.wrapDegrees(wy - p.getYaw()) / 180f;
        x[2] = MathHelper.wrapDegrees(wp - p.getPitch()) / 90f;
        x[3] = p.getHealth() / 20f; x[4] = t.getHealth() / 20f; x[5] = t.getArmor() / 20f;
        x[6] = (float) Math.max(-1, Math.min(1, p.getVelocity().y * 3));
        x[7] = (float) Math.min(1, enemySpeed * 4);
        x[8] = p.getAttackCooldownProgress(0.5f);
        x[9] = p.isOnGround() ? 1 : 0;
        x[10] = t.handSwinging ? 1 : 0;
        return x;
    }

    // ------------------------------------------------------------------ gear: totem / shield in offhand
    private void gear(MinecraftClient mc, ClientPlayerEntity p) {
        if (gearStep == 1) {
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
            gearStep = 0; return;
        }
        if (tickCount < gearCd || eating) return;
        var off = p.getOffHandStack();
        int s = -1;
        if (!off.isOf(Items.TOTEM_OF_UNDYING)) s = Inv.hotbar(p, x -> x.isOf(Items.TOTEM_OF_UNDYING));
        if (s < 0 && !off.isOf(Items.TOTEM_OF_UNDYING) && !off.isOf(Items.SHIELD)) s = Inv.hotbar(p, x -> x.isOf(Items.SHIELD));
        if (s >= 0) { Inv.select(p, s); gearStep = 1; gearCd = tickCount + 60; }
    }

    // ------------------------------------------------------------------ playing
    private void fight(MinecraftClient mc, ClientPlayerEntity p) {
        LivingEntity t = pickTarget(mc, p);
        if (t == null) { stopControl(); return; }
        target = t; controlling = true; lifeTicks++; lastFightTick = tickCount; trackEnemy(t);
        gear(mc, p);
        Genome g = effective(t);

        double dx = t.getX() - p.getX(), dz = t.getZ() - p.getZ(), dy = t.getY() + t.getHeight() * 0.65 - p.getEyeY();
        double dist = p.distanceTo(t);
        float hp = p.getHealth();
        float wantYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90);
        float wantPitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));

        int sword = Inv.sword(p);
        int want = sword >= 0 ? sword : Inv.selected(p);
        int food = Inv.hotbar(p, Inv::isFood);
        int bow = Inv.hotbar(p, x -> x.isOf(Items.BOW));
        boolean arrows = Inv.has(p, x -> x.isIn(ItemTags.ARROWS));

        // choose what to do: 1 eat, 2 retreat, 3 bow, 0 melee
        int mode = 0;
        if (eating) mode = 1;
        else if (hp <= 10 && food >= 0 && (dist > 3.5 || hp <= 6) && tickCount >= foodCd) {
            eating = true; eatStart = tickCount; foodCd = tickCount + 100; mode = 1;
        } else if (hp <= 6) mode = 2;
        else if (!crystalMode && bow >= 0 && arrows && dist > 9 && dist < 45) mode = 3;
        if (mode != 3) drawing = false;
        if (mode == 1 && food >= 0) want = food;
        if (mode == 3) { want = bow; wantPitch -= (float) (dist * 0.25); }

        p.setYaw(step(p.getYaw(), wantYaw, g.aimSpeed));
        p.setPitch(MathHelper.clamp(step(p.getPitch(), wantPitch, g.aimSpeed * 0.7), -90f, 90f));

        if (++strafeTick >= g.strafePeriod) { strafeTick = 0; strafeDir = rnd.nextBoolean() ? 1 : -1; }
        boolean sl = dist < 7 && strafeDir < 0, sr = dist < 7 && strafeDir > 0;
        int act = 0;

        switch (mode) {
            case 1 -> {
                act = 2;
                keys(mc, false, dist < 6, sl, sr, false, false, true);
                if ((tickCount - eatStart > 8 && !p.isUsingItem()) || hp >= 19 || tickCount - eatStart > 60 || food < 0) eating = false;
            }
            case 2 -> {
                act = 4;
                keys(mc, false, dist < 7, sl, sr, false, false, false);
                if (dist < 5 && tickCount >= pearlCd) {
                    int ps = Inv.hotbar(p, Inv::isPearl);
                    if (ps >= 0) {
                        Inv.select(p, ps); want = ps;
                        p.setYaw(wantYaw + 180); p.setPitch(-35f);
                        mc.interactionManager.interactItem(p, Hand.MAIN_HAND);
                        pearlCd = tickCount + 80;
                    }
                }
            }
            case 3 -> {
                act = 6;
                if (!drawing) { drawing = true; drawStart = tickCount; }
                else if (tickCount - drawStart >= 22) drawing = false;
                keys(mc, false, false, sl, sr, false, false, drawing);
            }
            default -> {
                boolean ready = p.getAttackCooldownProgress(0.5f) >= g.hitDelay;
                boolean aimed = Math.abs(MathHelper.wrapDegrees(wantYaw - p.getYaw())) < 12;
                boolean shield = p.getOffHandStack().isOf(Items.SHIELD);
                int[] wa = {want};
                boolean did = crystalMode && Crystal.run(mc, p, t, wa);
                want = wa[0];
                if (did) act = 5;
                if (useNet) {
                    float[] o = save.net.forward(inputs(p, t));
                    boolean f = o[0] > .3, b = o[0] < -.3, r = o[1] > .3, l = o[1] < -.3;
                    boolean j = o[2] > .3, atk = o[3] > .3, blk = o[4] > .3 && shield;
                    if (wtap > 0) { f = false; wtap--; }
                    keys(mc, f, b, l, r, j, f && dist > 4, blk);
                    if (!did && atk && ready && dist <= 3.0 && aimed) { swing(mc, p, t); act = 1; }
                } else {
                    boolean f = wtap <= 0 && dist > g.range, b = dist < g.range - 1.0;
                    if (wtap > 0) wtap--;
                    boolean jump = false, atk = false;
                    boolean blk = shield && t.handSwinging && !ready;
                    if (ready && dist <= 3.0 && aimed && !did) {
                        if (critMode) { if (p.isOnGround()) jump = true; else if (p.getVelocity().y < 0) atk = true; }
                        else atk = true;
                    }
                    keys(mc, f, b, sl, sr, jump, wtap <= 0 && dist > 4, blk);
                    record(inputs(p, t), new float[]{f ? 1 : b ? -1 : 0, sr ? 1 : sl ? -1 : 0, jump ? 1 : 0, atk ? 1 : 0, blk ? 1 : 0});
                    if (atk) { swing(mc, p, t); act = 1; }
                }
            }
        }
        if (gearStep != 1) Inv.select(p, want);
        rec(dist, hp, t.getHealth(), act);
    }

    private void swing(MinecraftClient mc, ClientPlayerEntity p, LivingEntity t) {
        mc.interactionManager.attackEntity(p, t);
        p.swingHand(Hand.MAIN_HAND);
        save.swings++; lifeSwings++;
        pendingSwing = t; pendingTick = tickCount; wtap = 2;
        critMode = rnd.nextDouble() < current.critChance;
    }

    // ------------------------------------------------------------------ learning
    private void onDeath() {
        Save s = save;
        s.deaths++;
        recap = new ArrayList<>(buf); buf.clear();
        double score = lifeHits + 5.0 * lifeKills - 0.4 * lifeDamage;
        if (score >= s.bestScore) { s.best = current.copy(); s.bestScore = score; } else s.bestScore *= 0.95;
        Genome next = s.best.copy();
        StringBuilder why = new StringBuilder();
        String kn = killer != null ? killer.getName().getString() : "?";
        Obs k = killer instanceof PlayerEntity ? obs.get(killer.getUuid()) : null;

        if (learning) {
            double hitRate = lifeSwings > 0 ? (double) lifeHits / lifeSwings : 0;
            if (lifeSwings >= 4 && hitRate < 0.4) {
                next.aimSpeed *= 1.15;
                why.append("Missed too many swings (").append(Math.round(hitRate * 100)).append("% hit rate), so aim is faster. ");
            }
            if (k != null && k.ticks > 30) {
                double theirAim = k.avgAim() * 3, theirFlip = k.ticksPerFlip();
                if (theirAim > next.aimSpeed) { next.aimSpeed = next.aimSpeed * 0.7 + theirAim * 0.3; why.append(kn).append(" aimed faster, copying part of their aim. "); }
                if (theirFlip < next.strafePeriod) { next.strafePeriod = next.strafePeriod * 0.7 + theirFlip * 0.3; why.append(kn).append(" strafed faster, copying part of their strafing. "); }
            }
            if (lifeTicks < 200 && lifeDamage > 6) { next.range -= 0.1; why.append("Died very fast, so it will fight closer. "); }
            if (lifeHits < 3 && lifeTicks > 100) { next.hitDelay -= 0.03; why.append("Barely landed hits, so it swings sooner. "); }
            if (why.length() == 0) why.append("No clear mistake found, trying a small random change. ");
            next.mutate(0.08, rnd);

            if (useNet) {
                if (score >= s.netScore) { s.netBest = s.net.copy(); s.netScore = score; } else s.netScore *= 0.95;
                Net n = s.netBest.copy();
                n.mutate(Math.max(0.03, 0.12 - s.generation * 0.001), rnd);
                s.net = n;
                why.append("Neural net: kept the best brain and mutated it. ");
            }
            if (killer instanceof PlayerEntity) {
                Profile pr = profile(kn);
                pr.losses++;
                if (k != null && k.ticks > 30) { pr.aim = k.avgAim() * 3; pr.flip = k.ticksPerFlip(); pr.ticks += k.ticks; }
            }
        } else why.append("Learning is off.");

        s.lastLoss = why.toString().trim();
        recapInfo = "Killed by " + kn + " after " + (lifeTicks / 20) + "s. Hits " + lifeHits + "/" + lifeSwings + ", damage taken " + (int) lifeDamage + ".";
        s.generation++;
        current = next;
        lifeSwings = lifeHits = lifeTicks = lifeKills = 0; lifeDamage = 0;
        target = null; pendingSwing = null; lastHp = 20; eating = false; drawing = false;
        saveToDisk();
    }
}
