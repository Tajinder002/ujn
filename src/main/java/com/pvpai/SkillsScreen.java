package com.pvpai;

import net.minecraft.client.gui.DrawContext;

public class SkillsScreen extends Base {
    private static final String[] NAMES = {"Aim speed", "Strafe period", "Fight range", "Crit chance", "Swing timing"};
    public SkillsScreen() { super("Skills and my style"); }

    @Override
    protected void build() {
        int x = width / 2 - 150, y = 34;
        for (int i = 0; i < 5; i++) {
            final int idx = i;
            btn("-", x + 230, y + i * 22, 32, () -> adjust(idx, -1));
            btn("+", x + 268, y + i * 22, 32, () -> adjust(idx, 1));
        }
        btn("Copy my own style (watch me)", x, y + 124, 300, Brain.I::applyMyStyle);
        btn(() -> "Train neural net on " + Brain.I.samples.size() + " samples", x, y + 148, 300, () -> Brain.I.trainNow(1000));
        back();
    }

    private void adjust(int i, int d) {
        Genome g = Brain.I.current;
        switch (i) {
            case 0 -> g.aimSpeed += 2 * d;
            case 1 -> g.strafePeriod += d;
            case 2 -> g.range += 0.1 * d;
            case 3 -> g.critChance += 0.05 * d;
            default -> g.hitDelay += 0.02 * d;
        }
        g.clamp();
        Brain.I.save.best = g.copy();
    }

    @Override
    public void render(DrawContext c, int mx, int my, float d) {
        super.render(c, mx, my, d);
        int x = width / 2 - 150, y = 34;
        Genome g = Brain.I.current;
        String[] v = {String.format("%.0f deg/tick", g.aimSpeed), String.format("%.0f ticks", g.strafePeriod),
                String.format("%.1f blocks", g.range), String.format("%.0f%%", g.critChance * 100), String.format("%.0f%%", g.hitDelay * 100)};
        for (int i = 0; i < 5; i++) txt(c, NAMES[i] + ": " + v[i], x, y + 5 + i * 22, 0xFFFFFFFF);
        Brain.Obs m = Brain.I.me;
        txt(c, m.ticks < 100 ? "Your style: keep fighting with the menu closed so I can watch (" + m.ticks + "/100)"
                : String.format("Your style: aim %.1f deg/tick, strafe every %.0f ticks (%d ticks)", m.avgAim(), m.ticksPerFlip(), m.ticks), x, y + 178, 0xFF88DDFF);
        txt(c, "While AI is off or in Skills mode, the net learns from you and from itself.", x, y + 192, 0xFF999999);
    }
}
