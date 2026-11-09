package com.example.soundping;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.AABB;

public class SoundPingMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LevelRenderEvents.BEFORE_GIZMOS.register(ctx -> {
            long now = System.currentTimeMillis();
            SoundPings.PINGS.removeIf(p -> now - p.createdMs() > SoundPings.LIFETIME_MS);

            for (SoundPings.Ping p : SoundPings.PINGS) {
                float age = (now - p.createdMs()) / (float) SoundPings.LIFETIME_MS;
                int fillAlpha = (int) (255 * Math.max(0.15f, 0.40f * (1f - age)));

                int rgb = (((int) (p.r() * 255)) << 16)
                        | (((int) (p.g() * 255)) << 8)
                        | ((int) (p.b() * 255));

                AABB box = new AABB(p.pos());
                Gizmos.cuboid(box, GizmoStyle.fill((fillAlpha << 24) | rgb)).setAlwaysOnTop();
                Gizmos.cuboid(box, GizmoStyle.stroke(0xFF000000 | rgb)).setAlwaysOnTop();
            }
        });
    }

    // kept so GameRendererMixin still compiles
    public static void close() {}
}
