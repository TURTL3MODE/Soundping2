package com.example.soundping;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

public class SoundPingScreen extends Screen {
    public SoundPingScreen() {
        super(Component.literal("Sound Ping"));
    }

    private static Component state(String label, boolean on) {
        return Component.literal(label + ": ")
                .append(Component.literal(on ? "ON" : "OFF")
                        .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    private void addToggle(int x, int y, int w, String label, BooleanSupplier get, Runnable toggle) {
        Button b = Button.builder(state(label, get.getAsBoolean()), btn -> {
            toggle.run();
            Config.save();
            btn.setMessage(state(label, get.getAsBoolean()));
        }).bounds(x, y, w, 20).build();
        addRenderableWidget(b);
    }

    @Override
    protected void init() {
        int bw = 98, gap = 4, cols = 3;
        int full = cols * bw + (cols - 1) * gap;
        int left = this.width / 2 - full / 2;
        int y = 8;

        // master switch
        addToggle(left, y, full, "Sound Ping", () -> Config.enabled, () -> Config.enabled = !Config.enabled);
        y += 24;

        // sound pings + scan range
        int half = (full - gap) / 2;
        addToggle(left, y, half, "Sound pings", () -> Config.pings, () -> Config.pings = !Config.pings);
        Button range = Button.builder(Component.literal("Range: " + Config.radius + " chunks"), btn -> {
            Config.cycleRadius();
            Config.save();
            btn.setMessage(Component.literal("Range: " + Config.radius + " chunks"));
        }).bounds(left + half + gap, y, half, 20).build();
        addRenderableWidget(range);
        y += 26;

        // one toggle per block group
        BlockKind[] kinds = BlockKind.values();
        for (int i = 0; i < kinds.length; i++) {
            BlockKind k = kinds[i];
            int cx = left + (i % cols) * (bw + gap);
            int cy = y + (i / cols) * 22;
            addToggle(cx, cy, bw, k.label, () -> Config.isOn(k), () -> Config.toggle(k));
        }

        int rows = (kinds.length + cols - 1) / cols;
        addRenderableWidget(Button.builder(Component.literal("Done"), btn -> onClose())
                .bounds(left, y + rows * 22 + 4, full, 20).build());
    }
}
