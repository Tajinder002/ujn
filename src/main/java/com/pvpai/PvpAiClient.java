package com.pvpai;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class PvpAiClient implements ClientModInitializer {
    private static KeyBinding guiKey, toggleKey;

    @Override
    public void onInitializeClient() {
        Brain.I.load();
        KeyBinding.Category cat = KeyBinding.Category.create(Identifier.of("pvpai", "main"));
        guiKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.pvpai.gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, cat));
        toggleKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.pvpai.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, cat));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (guiKey.wasPressed()) mc.setScreen(new MainScreen());
            while (toggleKey.wasPressed()) {
                Brain.I.enabled = !Brain.I.enabled;
                if (mc.player != null)
                    mc.player.sendMessage(Text.literal("PvP AI: " + (Brain.I.enabled ? "ON" : "OFF")), true);
            }
            Brain.I.tick(mc);
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> Brain.I.saveToDisk());
    }
}
