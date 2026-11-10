package com.example.soundping;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Properties;
import java.util.Set;

/** Menu settings, saved to .minecraft/config/soundping.properties */
public class Config {
    public static volatile boolean enabled = true;
    public static volatile boolean pings = true;
    public static volatile int radius = 8;

    private static final int[] RADII = {4, 6, 8, 10, 12};
    private static final Set<BlockKind> ON = Collections.synchronizedSet(EnumSet.noneOf(BlockKind.class));

    static {
        for (BlockKind k : BlockKind.values()) if (k.defaultOn) ON.add(k);
    }

    public static boolean isOn(BlockKind k) { return ON.contains(k); }

    public static void toggle(BlockKind k) {
        if (!ON.remove(k)) ON.add(k);
    }

    public static void cycleRadius() {
        int next = RADII[0];
        for (int i = 0; i < RADII.length; i++) {
            if (RADII[i] == radius) { next = RADII[(i + 1) % RADII.length]; break; }
        }
        radius = next;
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("soundping.properties");
    }

    public static void load() {
        Path f = file();
        if (!Files.exists(f)) return;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(f)) {
            p.load(in);
        } catch (IOException e) {
            return;
        }
        enabled = Boolean.parseBoolean(p.getProperty("enabled", "true"));
        pings = Boolean.parseBoolean(p.getProperty("pings", "true"));
        try { radius = Integer.parseInt(p.getProperty("radius", "8")); } catch (NumberFormatException ignored) {}
        for (BlockKind k : BlockKind.values()) {
            String v = p.getProperty("kind." + k.name());
            if (v == null) continue;
            if (Boolean.parseBoolean(v)) ON.add(k); else ON.remove(k);
        }
    }

    public static void save() {
        Properties p = new Properties();
        p.setProperty("enabled", String.valueOf(enabled));
        p.setProperty("pings", String.valueOf(pings));
        p.setProperty("radius", String.valueOf(radius));
        for (BlockKind k : BlockKind.values()) p.setProperty("kind." + k.name(), String.valueOf(isOn(k)));
        try (OutputStream out = Files.newOutputStream(file())) {
            p.store(out, "Sound Ping settings");
        } catch (IOException ignored) {}
    }
}
