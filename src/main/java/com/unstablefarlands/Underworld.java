package com.unstablefarlands;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

import static com.unstablefarlands.Zones.b;

/**
 * The Underworld (own dimension, see data/unstable_farlands/dimension/underworld.json).
 *
 * Layout along +Z:
 *   z < fractureStart                : standard land (soul sand/soil, deep chasms, green crystals, lantern posts),
 *                                      with Fire areas (lava lakes, red crystals) and Snow areas (massive spruces)
 *   fractureStart .. voidStart       : land breaks apart into floating islands, fewer and fewer
 *   z >= voidStart                   : pure void with escape-room style castles linked by glass bridges
 *
 * Everything must stay inside Y 0..255 (it uses the Nether dimension type).
 */
public final class Underworld implements Zone {
    public static final Underworld I = new Underworld();
    private Underworld() {}

    private static final int CASTLE_SPACING = 192;
    private static final int FLOOR = 100;

    @Override public String id() { return "underworld"; }
    @Override public void column(int x, int z, Col c, Noise n) {
        if (z >= Config.voidStart()) voidZone(x, z, c, n);
        else if (z >= Config.fractureStart()) fractured(x, z, c, n);
        else land(x, z, c, n);
    }

    // ---------------------------------------------------------------- land

    /** 0 = standard, 1 = fire, 2 = snow, 3 = massive maze */
    static int region(Noise n, int x, int z) {
        if (n.fbm(x / 1600.0, z / 1600.0, 2, 90) > 0.60) return 3;
        double r = n.fbm(x / 1200.0, z / 1200.0, 2, 40);
        return r < 0.42 ? 1 : (r > 0.58 ? 2 : 0);
    }

    static int surfaceHeight(Noise n, int x, int z, int reg) {
        int h = Zones.hgt(n, x, z, reg == 1 ? 58 : 64, reg == 2 ? 5 : 11, 70, 41);
        if (reg == 1) {                                         // lava basins
            double lake = n.fbm(x / 90.0, z / 90.0, 2, 44);
            if (lake > 0.56) h = Math.min(h, 54 - (int) ((lake - 0.56) * 90));
        }
        double ridge = Math.abs(n.fbm(x / 110.0, z / 110.0, 3, 42) * 2 - 1);
        if (ridge < 0.05) {                                     // deep chasms
            double k = Math.min(1.0, (1 - ridge / 0.05) * 2);
            h -= (int) (52 * k);
        }
        return Math.max(h, 8);
    }

    private static BlockState topBlock(Noise n, int x, int z) {
        double v = n.fbm(x / 14.0, z / 14.0, 2, 43);
        if (v < 0.30) return b(Blocks.BLACKSTONE);
        if (v < 0.65) return b(Blocks.SOUL_SAND);
        return b(Blocks.SOUL_SOIL);
    }

    private void land(int x, int z, Col c, Noise n) {
        int reg = region(n, x, z);
        if (reg == 3) { mazeLand(x, z, c, n); return; }
        int h = surfaceHeight(n, x, z, reg);

        c.fill(c.minY, c.minY, b(Blocks.BEDROCK));
        c.fill(c.minY + 1, h - 4, b(Blocks.DEEPSLATE));
        if (reg == 2) {
            c.fill(h - 3, h, b(Blocks.SNOW_BLOCK));
        } else {
            c.fill(h - 3, h - 1, b(Blocks.SOUL_SOIL));
            c.set(h, topBlock(n, x, z));
        }
        if (reg == 1 && h < 55) c.fill(h + 1, 55, b(Blocks.LAVA));

        if (reg != 2) {
            crystals(x, z, h, reg, c, n);
            lanternPost(x, z, h, c, n);
        } else {
            snowTree(x, z, h, c, n);
        }
    }

    private void crystals(int x, int z, int h, int reg, Col c, Noise n) {
        int cx = Math.floorDiv(x, 36), cz = Math.floorDiv(z, 36);
        if (n.hash(cx, cz, 52) >= 0.30) return;
        int px = cx * 36 + 6 + (int) (n.hash(cx, cz, 50) * 24);
        int pz = cz * 36 + 6 + (int) (n.hash(cx, cz, 51) * 24);
        double r = 3 + n.hash(cx, cz, 53) * 4;
        double tall = 14 + n.hash(cx, cz, 54) * 40;
        double d = Math.hypot(x - px, z - pz);
        if (d >= r) return;
        int hh = (int) (tall * (1 - d / r));
        if (hh <= 0) return;
        BlockState glass = reg == 1 ? b(Blocks.RED_STAINED_GLASS) : b(Blocks.GREEN_STAINED_GLASS);
        BlockState core = reg == 1 ? b(Blocks.SHROOMLIGHT) : b(Blocks.VERDANT_FROGLIGHT);
        c.fill(h + 1, h + hh, d < 1.6 ? core : glass);
    }

    private void lanternPost(int x, int z, int h, Col c, Noise n) {
        int cx = Math.floorDiv(x, 40), cz = Math.floorDiv(z, 40);
        if (n.hash(cx, cz, 48) >= 0.3) return;
        int px = cx * 40 + (int) (n.hash(cx, cz, 46) * 40);
        int pz = cz * 40 + (int) (n.hash(cx, cz, 47) * 40);
        if (x != px || z != pz || h < 40) return;
        c.fill(h + 1, h + 4, b(Blocks.DEEPSLATE_BRICK_WALL));
        c.set(h + 5, b(Blocks.SEA_LANTERN));
    }

    private void snowTree(int x, int z, int h, Col c, Noise n) {
        int cx = Math.floorDiv(x, 48), cz = Math.floorDiv(z, 48);
        if (n.hash(cx, cz, 62) >= 0.6) return;
        int tx = cx * 48 + 16 + (int) (n.hash(cx, cz, 60) * 16);
        int tz = cz * 48 + 16 + (int) (n.hash(cx, cz, 61) * 16);
        int height = 50 + (int) (n.hash(cx, cz, 63) * 50);
        int base = surfaceHeight(n, tx, tz, 2);
        int dx = x - tx, dz = z - tz;
        int d2 = dx * dx + dz * dz;
        if (d2 <= 7) {
            c.fill(Math.min(h, base) + 1, base + height, b(Blocks.SPRUCE_LOG));
            return;
        }
        double d = Math.sqrt(d2);
        if (d > 14.5) return;
        BlockState leaf = Zones.leaves(Blocks.SPRUCE_LEAVES);
        double start = base + height / 4.0;
        for (int y = (int) start; y <= base + height; y++) {
            double frac = (y - start) / (height * 0.75);
            if (d <= 13 * (1 - frac) + 1.5) c.set(y, leaf);
        }
    }


    // ------------------------------------------------------- massive mazes

    private static final int MAZE_CELL = 24, MAZE_WALL = 4, MAZE_FLOOR = 64, MAZE_HEIGHT = 60;

    private void mazeLand(int x, int z, Col c, Noise n) {
        c.fill(c.minY, c.minY, b(Blocks.BEDROCK));
        c.fill(c.minY + 1, MAZE_FLOOR - 2, b(Blocks.DEEPSLATE));
        double f = n.hash(x / 2, z / 2, 91);
        c.fill(MAZE_FLOOR - 1, MAZE_FLOOR,
                f < 0.35 ? b(Blocks.BLACKSTONE) : (f < 0.7 ? b(Blocks.DEEPSLATE_TILES) : b(Blocks.POLISHED_BLACKSTONE_BRICKS)));

        if (!Maze.wall(n, x, z, 92, MAZE_CELL, MAZE_WALL)) return;
        for (int y = MAZE_FLOOR + 1; y <= MAZE_FLOOR + MAZE_HEIGHT; y++) {
            double r = n.hash(x * 31 + y, z, 93);
            BlockState st;
            if (r < 0.30) st = b(Blocks.BLACKSTONE);
            else if (r < 0.55) st = b(Blocks.DEEPSLATE_BRICKS);
            else if (r < 0.75) st = b(Blocks.POLISHED_BLACKSTONE_BRICKS);
            else if (r < 0.88) st = b(Blocks.DEEPSLATE_TILES);
            else if (r < 0.96) st = b(Blocks.CRACKED_DEEPSLATE_BRICKS);
            else st = b(Blocks.COBBLED_DEEPSLATE);
            if (n.hash(x * 17 + y, z, 94) < 0.003) st = b(Blocks.VERDANT_FROGLIGHT);   // faint green glow
            c.set(y, st);
        }
        c.set(MAZE_FLOOR + MAZE_HEIGHT + 1, ((x / 2 + z / 2) & 1) == 0 ? b(Blocks.DEEPSLATE_TILES) : b(Blocks.AIR));
    }

    // ------------------------------------------------------------- fractures

    private void fractured(int x, int z, Col c, Noise n) {
        double t = (z - Config.fractureStart()) / (double) Config.fractureLength();
        double v = n.fbm(x / 45.0, z / 45.0, 3, 45);
        double thresh = 0.25 + 0.70 * t;                 // more and more void as t -> 1
        if (v < thresh) return;
        double over = v - thresh;
        int top = 68 + (int) (over * 30);
        int thick = 5 + (int) (over * 60);
        c.fill(top - thick, top - 1, b(Blocks.DEEPSLATE));
        c.set(top, n.hash(x / 3, z / 3, 49) < 0.5 ? b(Blocks.SOUL_SAND) : b(Blocks.SOUL_SOIL));
    }

    // -------------------------------------------------------- void + castles

    private void voidZone(int x, int z, Col c, Noise n) {
        int rel = z - Config.voidStart();
        int k = Math.floorDiv(rel, CASTLE_SPACING);
        int dz = rel - k * CASTLE_SPACING - CASTLE_SPACING / 2;
        int dx = x;
        if (Math.abs(dx) <= 18 && Math.abs(dz) <= 18) {
            castle(dx, dz, c);
        } else if (Math.abs(dx) <= 1 && n.hash(x, z, 70) > 0.04) {   // glass bridge, a few missing panes
            c.set(FLOOR, b(Blocks.GLASS));
        }
    }

    private void castle(int dx, int dz, Col c) {
        int ax = Math.abs(dx), az = Math.abs(dz), m = Math.max(ax, az);
        BlockState brick = b(Blocks.DEEPSLATE_BRICKS);
        BlockState tile = b(Blocks.DEEPSLATE_TILES);
        BlockState glow = b(Blocks.VERDANT_FROGLIGHT);

        c.fill(FLOOR - 5, FLOOR, brick);                       // foundation / courtyard floor

        if (m == 17 || m == 18) {                              // curtain wall, gates on the +/-Z sides
            boolean gate = az >= 17 && ax <= 2;
            if (!gate) {
                c.fill(FLOOR + 1, FLOOR + 18, brick);
                if (((dx + dz) & 1) == 0) c.set(FLOOR + 19, tile);
            } else {
                c.fill(FLOOR + 9, FLOOR + 18, brick);
            }
        }

        if (m <= 6) {                                          // central keep with a spire
            if (m >= 5) c.fill(FLOOR + 1, FLOOR + 40, brick);
            c.fill(FLOOR + 40, FLOOR + 40 + (7 - m) * 6, brick);
            if (m == 6 && ((az == 6 && (dx & 3) == 0) || (ax == 6 && (dz & 3) == 0)))
                c.fill(FLOOR + 6, FLOOR + 32, glow);           // glowing green strips
        }

        int tx = Math.abs(dx - (dx < 0 ? -15 : 15));           // four corner towers
        int tz = Math.abs(dz - (dz < 0 ? -15 : 15));
        int tm = Math.max(tx, tz);
        if (tm <= 2 && ax >= 12 && az >= 12) {
            c.fill(FLOOR + 1, FLOOR + 28, brick);
            c.fill(FLOOR + 29, FLOOR + 28 + (3 - tm) * 5, brick);
        }

        if (ax == 10 && az == 10) {                            // lantern posts in the courtyard
            c.fill(FLOOR + 1, FLOOR + 4, b(Blocks.DEEPSLATE_BRICK_WALL));
            c.set(FLOOR + 5, b(Blocks.SEA_LANTERN));
        }
    }
}
