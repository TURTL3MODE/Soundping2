package com.example.soundping;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public class SoundPingMod implements ClientModInitializer {
    private record Found(AABB box, int rgb, boolean fill) {}

    private static volatile List<Found> found = List.of();
    private static int tickCounter = 0;
    private static KeyMapping menuKey;

    private static final int MAX_RARE = 3000; // cap for normal blocks
    private static final int MAX_BULK = 1500; // cap for outline-only blocks (blackstone)

    @Override
    public void onInitializeClient() {
        Config.load();

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("soundping", "main"));
        menuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.soundping.menu", InputConstants.Type.KEYBOARD, InputConstants.KEY_K, category));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (menuKey.consumeClick()) mc.gui.setScreen(new SoundPingScreen());
            if (++tickCounter % 20 == 0) scan(mc);
        });

        LevelRenderEvents.BEFORE_GIZMOS.register(ctx -> {
            if (!Config.enabled) return;

            for (Found f : found) {
                if (f.fill()) Gizmos.cuboid(f.box(), GizmoStyle.fill((0x55 << 24) | f.rgb())).setAlwaysOnTop();
                Gizmos.cuboid(f.box(), GizmoStyle.stroke(0xFF000000 | f.rgb())).setAlwaysOnTop();
            }

            if (!Config.pings) return;
            long now = System.currentTimeMillis();
            SoundPings.PINGS.removeIf(p -> now - p.createdMs() > SoundPings.LIFETIME_MS);
            for (SoundPings.Ping p : SoundPings.PINGS) {
                float age = (now - p.createdMs()) / (float) SoundPings.LIFETIME_MS;
                int alpha = (int) (255 * Math.max(0.15f, 0.40f * (1f - age)));
                int rgb = (((int) (p.r() * 255)) << 16)
                        | (((int) (p.g() * 255)) << 8)
                        | ((int) (p.b() * 255));
                AABB box = new AABB(p.pos());
                Gizmos.cuboid(box, GizmoStyle.fill((alpha << 24) | rgb)).setAlwaysOnTop();
                Gizmos.cuboid(box, GizmoStyle.stroke(0xFF000000 | rgb)).setAlwaysOnTop();
            }
        });
    }

    private static void scan(Minecraft mc) {
        var level = mc.level;
        var player = mc.player;
        if (!Config.enabled || level == null || player == null) {
            found = List.of();
            return;
        }
        int radius = Config.radius;
        int pcx = player.blockPosition().getX() >> 4;
        int pcz = player.blockPosition().getZ() >> 4;

        List<Found> rare = new ArrayList<>();
        List<Found> bulk = new ArrayList<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int cx = pcx + dx, cz = pcz + dz;
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;

                LevelChunkSection[] sections = chunk.getSections();
                for (int i = 0; i < sections.length; i++) {
                    LevelChunkSection s = sections[i];
                    if (s == null || s.hasOnlyAir()) continue;
                    if (!s.maybeHas(st -> {
                        BlockKind k = BlockKind.of(st);
                        return k != null && Config.isOn(k);
                    })) continue;

                    int baseY = chunk.getSectionYFromSectionIndex(i) << 4;
                    for (int x = 0; x < 16; x++) {
                        for (int y = 0; y < 16; y++) {
                            for (int z = 0; z < 16; z++) {
                                BlockState st = s.getBlockState(x, y, z);
                                BlockKind k = BlockKind.of(st);
                                if (k == null || !Config.isOn(k)) continue;

                                AABB box = new AABB(new BlockPos((cx << 4) + x, baseY + y, (cz << 4) + z));
                                if (k.outlineOnly) {
                                    if (bulk.size() < MAX_BULK) bulk.add(new Found(box, k.rgb, false));
                                } else if (rare.size() < MAX_RARE) {
                                    rare.add(new Found(box, k.rgb, true));
                                }
                            }
                        }
                    }
                }
            }
        }
        rare.addAll(bulk);
        found = rare;
    }

    // kept so GameRendererMixin still compiles
    public static void close() {}
}
