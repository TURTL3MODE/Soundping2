package com.example.soundping;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class SoundPings {
    public record Ping(BlockPos pos, float r, float g, float b, long createdMs) {}

    public static final List<Ping> PINGS = new CopyOnWriteArrayList<>();
    public static final long LIFETIME_MS = 30_000;

    private record Info(int rgb) {}
    private static final Map<SoundEvent, Info> WATCHED = new HashMap<>();
    static {
        watch(SoundEvents.CHEST_OPEN, 0xE0A030);
        watch(SoundEvents.CHEST_CLOSE, 0xE0A030);
        watch(SoundEvents.ENDER_CHEST_OPEN, 0xB060FF);
        watch(SoundEvents.BARREL_OPEN, 0xC08040);
        watch(SoundEvents.SHULKER_BOX_OPEN, 0xD080FF);
        watch(SoundEvents.PISTON_EXTEND, 0x60C0FF);
        watch(SoundEvents.PISTON_CONTRACT, 0x60C0FF);
    }

    private static void watch(SoundEvent e, int rgb) { WATCHED.put(e, new Info(rgb)); }

    public static void onSound(Holder<SoundEvent> holder, double x, double y, double z) {
        Info info = WATCHED.get(holder.value());
        if (info == null) return;
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            var level = mc.level;
            if (level == null) return;

            BlockPos base = BlockPos.containing(x, y, z);
            List<BlockPos> candidates = new ArrayList<>(List.of(base));
            if (x == Math.floor(x)) candidates.add(base.west());
            if (z == Math.floor(z)) candidates.add(base.north());

            List<BlockPos> hits = new ArrayList<>();
            for (BlockPos p : candidates) if (!level.getBlockState(p).isAir()) hits.add(p);
            if (hits.isEmpty()) hits.add(base);

            long now = System.currentTimeMillis();
            float r = ((info.rgb() >> 16) & 255) / 255f;
            float g = ((info.rgb() >> 8) & 255) / 255f;
            float b = (info.rgb() & 255) / 255f;
            for (BlockPos p : hits) {
                PINGS.removeIf(o -> o.pos().equals(p));
                PINGS.add(new Ping(p.immutable(), r, g, b, now));
            }
        });
    }
}
