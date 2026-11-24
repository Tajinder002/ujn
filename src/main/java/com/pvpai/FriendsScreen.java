package com.pvpai;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class FriendsScreen extends Base {
    private TextFieldWidget field;
    public FriendsScreen() { super("Friends (the AI never attacks them)"); }

    @Override
    protected void build() {
        int x = width / 2 - 150;
        field = new TextFieldWidget(textRenderer, x, 34, 300, 20, Text.literal("name"));
        field.setMaxLength(32);
        addDrawableChild(field);
        btn("Add friend", x, 60, 148, () -> { Brain.I.addFriend(field.getText()); field.setText(""); });
        btn("Remove friend", x + 152, 60, 148, () -> { Brain.I.removeFriend(field.getText()); field.setText(""); });
        back();
    }

    @Override
    public void render(DrawContext c, int mx, int my, float d) {
        super.render(c, mx, my, d);
        int x = width / 2 - 150, y = 92, i = 0;
        txt(c, "Type a player name, then press Add. Training mode lets the AI spar with friends.", x, 86 - 12, 0xFF999999);
        for (String f : Brain.I.save.friends) { if (i >= 12) break; txt(c, "- " + f, x, y + i * 12, 0xFFFFFFFF); i++; }
        if (Brain.I.save.friends.isEmpty()) txt(c, "No friends yet.", x, y, 0xFFAAAAAA);
    }
}
