package com.example.soundping;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;
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

    private static final int CHUNK_RADIUS = 8;      // chunks scanned in each direction
    private static final int MAX_RARE = 3000;       // cap for chests, spawners, etc.
    private static final int MAX_BLACKSTONE = 1500; // cap so bastions don't lag the game

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (++tickCounter % 20 == 0) scan(mc);
        });

        LevelRenderEvents.BEFORE_GIZMOS.register(ctx -> {
            for (Found f : found) {
                if (f.fill()) Gizmos.cuboid(f.box(), GizmoStyle.fill((0x55 << 24) | f.rgb())).setAlwaysOnTop();
                Gizmos.cuboid(f.box(), GizmoStyle.stroke(0xFF000000 | f.rgb())).setAlwaysOnTop();
            }

            // sound pings (pistons moving, chests opening) fade after 30 seconds
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
        if (level == null || player == null) {
            found = List.of();
            return;
        }
        int pcx = player.blockPosition().getX() >> 4;
        int pcz = player.blockPosition().getZ() >> 4;

        List<Found> rare = new ArrayList<>();
        List<Found> bulk = new ArrayList<>();

        for (int dx = -CHUNK_RADIUS; dx <= CHUNK_RADIUS; dx++) {
            for (int dz = -CHUNK_RADIUS; dz <= CHUNK_RADIUS; dz++) {
                int cx = pcx + dx, cz = pcz + dz;
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;

                LevelChunkSection[] sections = chunk.getSections();
                for (int i = 0; i < sections.length; i++) {
                    LevelChunkSection s = sections[i];
                    if (s == null || s.hasOnlyAir()) continue;
                    if (!s.maybeHas(st -> colorFor(st) >= 0)) continue; // skip sections with none of our blocks

                    int baseY = chunk.getSectionYFromSectionIndex(i) << 4;
                    for (int x = 0; x < 16; x++) {
                        for (int y = 0; y < 16; y++) {
                            for (int z = 0; z < 16; z++) {
                                BlockState st = s.getBlockState(x, y, z);
                                int rgb = colorFor(st);
                                if (rgb < 0) continue;

                                AABB box = new AABB(new BlockPos((cx << 4) + x, baseY + y, (cz << 4) + z));
                                if (st.is(Blocks.BLACKSTONE)) {
                                    if (bulk.size() < MAX_BLACKSTONE) bulk.add(new Found(box, rgb, false));
                                } else if (rare.size() < MAX_RARE) {
                                    rare.add(new Found(box, rgb, true));
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

    /** Returns an RGB color for blocks we care about, or -1 for everything else. */
    private static int colorFor(BlockState st) {
        if (st.is(Blocks.ENDER_CHEST)) return 0xB060FF;
        if (st.is(Blocks.CHEST)) return 0xE0A030;
        if (st.is(Blocks.TRAPPED_CHEST)) return 0xFF4040;
        if (st.is(Blocks.BARREL)) return 0xC08040;
        if (st.getBlock() instanceof ShulkerBoxBlock) return 0xD080FF;
        if (st.is(Blocks.GILDED_BLACKSTONE)) return 0xFFD700;
        if (st.is(Blocks.BLACKSTONE)) return 0x8080B0;
        if (st.is(Blocks.PISTON) || st.is(Blocks.STICKY_PISTON)) return 0x60C0FF;
        if (st.is(Blocks.OBSERVER)) return 0x40FF80;
        if (st.is(Blocks.HOPPER)) return 0xA0A0A0;
        if (st.is(Blocks.BEACON)) return 0x40FFFF;
        if (st.is(Blocks.SPAWNER)) return 0xFF2020;
        return -1;
    }

    // kept so GameRendererMixin still compiles
    public static void close() {}
}
