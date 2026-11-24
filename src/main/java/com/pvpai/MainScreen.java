package com.pvpai;

public class MainScreen extends Base {
    public MainScreen() { super("PvP Learner AI"); }

    @Override
    protected void build() {
        Brain b = Brain.I;
        int L = width / 2 - 155, R = width / 2 + 5, y = 28, s = 24;
        btn(() -> "AI control: " + on(b.enabled), L, y, 150, () -> b.enabled = !b.enabled);
        btn(() -> "Learning: " + on(b.learning), R, y, 150, () -> b.learning = !b.learning);
        btn(() -> "Brain: " + (b.useNet ? "Neural net" : "Skills"), L, y + s, 150, () -> b.setNeural(!b.useNet));
        btn(() -> "Style: " + (b.crystalMode ? "Crystal" : "Melee"), R, y + s, 150, () -> b.crystalMode = !b.crystalMode);
        btn(() -> "Training mode: " + on(b.training), L, y + 2 * s, 150, () -> b.training = !b.training);
        btn(() -> "Watch me play: " + on(b.watchMe), R, y + 2 * s, 150, () -> b.watchMe = !b.watchMe);
        btn(() -> "Auto respawn: " + on(b.autoRespawn), L, y + 3 * s, 150, () -> b.autoRespawn = !b.autoRespawn);
        btn(() -> "Recap on death: " + on(b.autoRecap), R, y + 3 * s, 150, () -> b.autoRecap = !b.autoRecap);
        btn("Skills and my style", L, y + 4 * s + 6, 150, () -> client.setScreen(new SkillsScreen()));
        btn("Friends", R, y + 4 * s + 6, 150, () -> client.setScreen(new FriendsScreen()));
        btn("Brain slots", L, y + 5 * s + 6, 150, () -> client.setScreen(new SlotsScreen()));
        btn("Last death recap", R, y + 5 * s + 6, 150, () -> client.setScreen(new RecapScreen()));
        btn("Reset brain (forget everything)", L, y + 6 * s + 6, 310, b::resetBrain);
    }

    @Override
    public void render(net.minecraft.client.gui.DrawContext c, int mx, int my, float d) {
        super.render(c, mx, my, d);
        Brain.Save s = Brain.I.save;
        int x = width / 2 - 155, y = 28 + 7 * 24 + 12;
        double kd = s.deaths == 0 ? s.kills : (double) s.kills / s.deaths;
        txt(c, String.format("Generation %d  Kills %d  Deaths %d  K/D %.2f  Hit rate %.0f%%", s.generation, s.kills, s.deaths, kd, Brain.I.hitRate()), x, y, 0xFFFFFF55);
        txt(c, "Why I lost last time:", x, y + 13, 0xFFFFAA55);
        int i = 0;
        for (String line : wrap(s.lastLoss, 58)) { if (i >= 3) break; txt(c, line, x, y + 25 + i * 11, 0xFFFFFFFF); i++; }
        txt(c, "Keys: Right Shift = menu, K = AI on/off", x, height - 14, 0xFF999999);
    }
}
