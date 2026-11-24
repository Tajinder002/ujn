package com.pvpai;

import net.minecraft.client.gui.DrawContext;

import java.util.List;

/** Shows the last 5 seconds before you died. */
public class RecapScreen extends Base {
    public RecapScreen() { super("Death recap (last 5 seconds)"); }

    @Override
    protected void build() { back(); }

    private static int actColor(int a) {
        return switch (a) {
            case 1 -> 0xFFFFFF55; case 2 -> 0xFF55FF55; case 4 -> 0xFFFFAA00; case 5 -> 0xFFFF55FF; case 6 -> 0xFF55FFFF;
            default -> 0xFF444444;
        };
    }

    @Override
    public void render(DrawContext c, int mx, int my, float d) {
        super.render(c, mx, my, d);
        List<float[]> r = Brain.I.recap;
        int x0 = width / 2 - 150;
        for (int i = 0; i < Math.min(2, wrap(Brain.I.recapInfo, 60).size()); i++) txt(c, wrap(Brain.I.recapInfo, 60).get(i), x0, 28 + i * 11, 0xFFFFFF55);
        if (r.isEmpty()) { txt(c, "No fight recorded yet.", x0, 70, 0xFFAAAAAA); return; }
        int n = r.size(), h = 56, y1 = 70, y2 = y1 + h + 26, y3 = y2 + 36 + 18;
        c.fill(x0 - 1, y1 - 1, x0 + 301, y1 + h + 1, 0x66000000);
        c.fill(x0 - 1, y2 - 1, x0 + 301, y2 + 31, 0x66000000);
        for (int i = 0; i < n; i++) {
            float[] f = r.get(i);
            int x = x0 + (i + 100 - n) * 3;
            int mh = (int) (Math.min(f[1], 20) / 20f * h);
            c.fill(x, y1 + h - mh, x + 2, y1 + h, 0xFF55FF55);
            int eh = (int) (Math.min(f[2], 20) / 20f * h);
            c.fill(x, y1 + h - eh - 1, x + 2, y1 + h - eh + 1, 0xFFFF5555);
            int dh = (int) (Math.min(f[0], 10) / 10f * 30);
            c.fill(x, y2 + 30 - dh, x + 2, y2 + 30, 0xFF5599FF);
            c.fill(x, y3, x + 2, y3 + 8, actColor((int) f[3]));
        }
        txt(c, "Green = your health, red dot = enemy health", x0, y1 - 11, 0xFFFFFFFF);
        txt(c, "Blue = distance to enemy (taller = farther)", x0, y2 - 11, 0xFFFFFFFF);
        txt(c, "Actions: yellow hit, green eat, orange retreat, pink crystal, cyan bow", x0, y3 + 12, 0xFFFFFFFF);
        txt(c, "What it blamed:", x0, y3 + 28, 0xFFFFAA55);
        int i = 0;
        for (String line : wrap(Brain.I.save.lastLoss, 58)) { if (i >= 3) break; txt(c, line, x0, y3 + 40 + i * 11, 0xFFFFFFFF); i++; }
    }
}
