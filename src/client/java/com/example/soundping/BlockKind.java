package com.example.soundping;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Predicate;

/** Every block group the mod can highlight. Add a new line here to add a new toggle. */
public enum BlockKind {
    //          menu label      color     on by default  outline only  matcher
    CHEST(      "Chests",       0xE0A030, true,  false, s -> s.is(Blocks.CHEST)),
    TRAPPED(    "Trapped",      0xFF4040, true,  false, s -> s.is(Blocks.TRAPPED_CHEST)),
    ENDER(      "Ender Chest",  0xB060FF, true,  false, s -> s.is(Blocks.ENDER_CHEST)),
    BARREL(     "Barrels",      0xC08040, true,  false, s -> s.is(Blocks.BARREL)),
    SHULKER(    "Shulkers",     0xD080FF, true,  false, s -> s.getBlock() instanceof ShulkerBoxBlock),
    SPAWNER(    "Spawners",     0xFF2020, true,  false, s -> s.is(Blocks.SPAWNER)),
    BED(        "Beds",         0xFF80C0, true,  false, s -> s.is(BlockTags.BEDS)),
    LECTERN(    "Lecterns",     0xE0E080, true,  false, s -> s.is(Blocks.LECTERN)),
    FURNACE(    "Furnaces",     0x909090, false, false, s -> s.is(Blocks.FURNACE) || s.is(Blocks.BLAST_FURNACE) || s.is(Blocks.SMOKER)),
    CRAFTING(   "Crafting",     0xB08050, false, false, s -> s.is(Blocks.CRAFTING_TABLE)),
    ENCHANTING( "Ench. Table",  0x4060FF, true,  false, s -> s.is(Blocks.ENCHANTING_TABLE)),
    BREWING(    "Brewing",      0xC0C040, true,  false, s -> s.is(Blocks.BREWING_STAND)),
    HOPPER(     "Hoppers",      0xA0A0A0, true,  false, s -> s.is(Blocks.HOPPER)),
    PISTON(     "Pistons",      0x60C0FF, true,  false, s -> s.is(Blocks.PISTON) || s.is(Blocks.STICKY_PISTON)),
    OBSERVER(   "Observers",    0x40FF80, true,  false, s -> s.is(Blocks.OBSERVER)),
    BEACON(     "Beacons",      0x40FFFF, true,  false, s -> s.is(Blocks.BEACON)),
    EMERALD(    "Emerald",  0x20E040, true,  false, s -> s.is(Blocks.EMERALD_ORE) || s.is(Blocks.DEEPSLATE_EMERALD_ORE)),
    DIAMOND(    "Diamond",  0x40E0FF, false, false, s -> s.is(Blocks.DIAMOND_ORE) || s.is(Blocks.DEEPSLATE_DIAMOND_ORE)),
    DEBRIS(     "Debris", 0x8B5A2B, false, false, s -> s.is(Blocks.ANCIENT_DEBRIS)),
    GILDED(     "Gilded BS",    0xFFD700, true,  false, s -> s.is(Blocks.GILDED_BLACKSTONE)),
    BLACKSTONE( "Blackstone",   0x8080B0, false, true,  s -> s.is(Blocks.BLACKSTONE));

    public final String label;
    public final int rgb;
    public final boolean defaultOn;
    public final boolean outlineOnly;
    private final Predicate<BlockState> test;

    BlockKind(String label, int rgb, boolean defaultOn, boolean outlineOnly, Predicate<BlockState> test) {
        this.label = label;
        this.rgb = rgb;
        this.defaultOn = defaultOn;
        this.outlineOnly = outlineOnly;
        this.test = test;
    }

    private static final BlockKind[] ALL = values();
    private static final Map<BlockState, BlockKind> CACHE = new IdentityHashMap<>();

    /** Which group a block state belongs to, or null. Cached so scanning stays fast. */
    public static BlockKind of(BlockState st) {
        BlockKind k = CACHE.get(st);
        if (k != null || CACHE.containsKey(st)) return k;
        for (BlockKind c : ALL) {
            if (c.test.test(st)) { k = c; break; }
        }
        CACHE.put(st, k);
        return k;
    }
}
