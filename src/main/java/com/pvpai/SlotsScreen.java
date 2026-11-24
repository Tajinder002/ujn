package com.pvpai;

import net.minecraft.client.gui.DrawContext;

public class SlotsScreen extends Base {
    public SlotsScreen() { super("Brain slots"); }

    @Override
    protected void build() {
        int x = width / 2 - 150;
        for (int n = 1; n <= 3; n++) {
            final int slot = n;
            btn("Save to slot " + n, x, 40 + (n - 1) * 44, 148, () -> Brain.I.saveSlot(slot));
            btn("Load slot " + n, x + 152, 40 + (n - 1) * 44, 148, () -> Brain.I.loadSlot(slot));
        }
        back();
    }

    @Override
    public void render(DrawContext c, int mx, int my, float d) {
        super.render(c, mx, my, d);
        int x = width / 2 - 150;
        for (int n = 1; n <= 3; n++) txt(c, "Slot " + n + ": " + Brain.I.slotInfo(n), x, 64 + (n - 1) * 44, 0xFFFFFF55);
        txt(c, "To share a brain, send the file config/pvpai_slotN.json and drop it in the same folder.", x, 180, 0xFF999999);
    }
}
