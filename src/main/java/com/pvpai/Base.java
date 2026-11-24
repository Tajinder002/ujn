package com.pvpai;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Shared menu helpers. */
public abstract class Base extends Screen {
    private final List<ButtonWidget> bs = new ArrayList<>();
    private final List<Supplier<String>> ls = new ArrayList<>();

    protected Base(String t) { super(Text.literal(t)); }
    protected abstract void build();

    @Override protected void init() { bs.clear(); ls.clear(); build(); }

    protected ButtonWidget btn(Supplier<String> label, int x, int y, int w, Runnable r) {
        ButtonWidget b = ButtonWidget.builder(Text.literal(label.get()), bb -> r.run()).dimensions(x, y, w, 20).build();
        addDrawableChild(b); bs.add(b); ls.add(label);
        return b;
    }
    protected ButtonWidget btn(String label, int x, int y, int w, Runnable r) { return btn(() -> label, x, y, w, r); }
    protected void back() { btn("Back", width / 2 - 50, height - 28, 100, () -> client.setScreen(new MainScreen())); }
    protected static String on(boolean b) { return b ? "ON" : "OFF"; }
    protected void txt(DrawContext c, String s, int x, int y, int col) { c.drawTextWithShadow(textRenderer, Text.literal(s), x, y, col); }

    protected List<String> wrap(String s, int n) {
        List<String> out = new ArrayList<>();
        while (!s.isEmpty()) {
            int cut = Math.min(s.length(), n);
            if (cut < s.length()) { int sp = s.lastIndexOf(' ', cut); if (sp > n / 3) cut = sp; }
            out.add(s.substring(0, cut).trim());
            s = s.substring(cut).trim();
        }
        return out;
    }

    @Override
    public void render(DrawContext c, int mx, int my, float d) {
        for (int i = 0; i < bs.size(); i++) bs.get(i).setMessage(Text.literal(ls.get(i).get()));
        super.render(c, mx, my, d);
        c.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFFFF);
    }

    @Override public boolean shouldPause() { return false; }
}
