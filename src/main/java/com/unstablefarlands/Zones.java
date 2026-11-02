package com.unstablefarlands;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.state.property.Properties;

import java.util.LinkedHashMap;
import java.util.Map;

/** All terrain zones. Each is a column function plus optional tree/decoration pass. */
public final class Zones {
    private Zones() {}

    public static final Map<String, Zone> ALL = new LinkedHashMap<>();

    private static Zone reg(Zone z) { ALL.put(z.id(), z); return z; }

    // ---------- helpers ----------
    static BlockState b(Block x) { return x.getDefaultState(); }
    static BlockState leaves(Block x) { return x.getDefaultState().with(Properties.PERSISTENT, true); }

    static int hgt(Noise n, int x, int z, int base, double amp, double scale, int salt) {
        return base + (int) Math.round((n.fbm(x / scale, z / scale, 3, salt) - 0.5) * 2 * amp);
    }

    static BlockState pick(Noise n, int x, int z, int salt, BlockState... l) {
        return l[(int) (n.hash(x, z, salt) * l.length)];
    }

    interface Maker { void make(Deco d, int x, int y, int z, double r); }

    /** Calls maker at hashed positions; scans 4 blocks beyond the chunk so trees cross borders cleanly. */
    static void scatter(Deco d, int salt, double prob, Maker m) {
        for (int x = d.x0 - 4; x < d.x0 + 20; x++) {
            for (int z = d.z0 - 4; z < d.z0 + 20; z++) {
                double r = d.n.hash(x, z, salt);
                if (r < prob) m.make(d, x, d.surface(x, z), z, r / prob);
            }
        }
    }

    static void tree(Deco d, int x, int y, int z, int h, BlockState log, BlockState leaf, int r) {
        for (int i = 1; i <= h; i++) d.set(x, y + i, z, log);
        for (int dy = h - 2; dy <= h + 1; dy++) {
            int rr = dy >= h ? r - 1 : r;
            for (int dx = -rr; dx <= rr; dx++)
                for (int dz = -rr; dz <= rr; dz++) {
                    if (dx * dx + dz * dz > rr * rr + 1) continue;
                    if (dx == 0 && dz == 0 && dy <= h) continue;
                    d.set(x + dx, y + dy, z + dz, leaf);
                }
        }
    }

    // ---------- 1. Red Plains ----------
    public static final Zone RED_PLAINS = reg(new Zone() {
        public String id() { return "red_plains"; }
        public void column(int x, int z, Col c, Noise n) {
            int h = hgt(n, x, z, 70, 8, 48, 1);
            c.fill(c.minY, h - 1, b(Blocks.NETHERRACK));
            c.set(h, b(Blocks.CRIMSON_NYLIUM));
        }
        public void decorate(Deco d) {
            scatter(d, 11, 0.004, (dd, x, y, z, r) ->
                tree(dd, x, y, z, 5 + (int) (r * 4), b(Blocks.BIRCH_LOG), leaves(Blocks.BIRCH_LEAVES), 2));
        }
    });

    // ---------- 2. Wavy Lands ----------
    public static final Zone WAVY_LANDS = reg(new Zone() {
        public String id() { return "wavy_lands"; }
        public void column(int x, int z, Col c, Noise n) {
            int h = 90 + (int) Math.round(10 * Math.sin(x / 9.0)
                    + 8 * Math.sin(z / 13.0 + x / 17.0)
                    + 6 * Math.sin((x + z) / 6.0));
            c.fill(c.minY, h - 4, b(Blocks.STONE));
            c.fill(h - 3, h - 1, b(Blocks.DIRT));
            c.set(h, b(Blocks.GRASS_BLOCK));
        }
    });

    // ---------- 3. Dark Forest (players get Darkness) ----------
    public static final Zone DARK_FOREST = reg(new Zone() {
        public String id() { return "dark_forest"; }
        public boolean dark() { return true; }
        public void column(int x, int z, Col c, Noise n) {
            int h = hgt(n, x, z, 72, 6, 40, 2);
            c.fill(c.minY, h - 4, b(Blocks.STONE));
            c.fill(h - 3, h - 1, b(Blocks.DIRT));
            c.set(h, n.hash(x / 4, z / 4, 3) < 0.3 ? b(Blocks.PODZOL) : b(Blocks.GRASS_BLOCK));
        }
        public void decorate(Deco d) {
            scatter(d, 12, 0.03, (dd, x, y, z, r) ->
                tree(dd, x, y, z, 5 + (int) (r * 4), b(Blocks.DARK_OAK_LOG), leaves(Blocks.DARK_OAK_LEAVES), 3));
        }
    });

    // ---------- 4. The Fabric (dark open space, stone staircases) ----------
    public static final Zone FABRIC = reg(new Zone() {
        public String id() { return "fabric"; }
        public boolean dark() { return true; }
        public void column(int x, int z, Col c, Noise n) {
            int cx = Math.floorDiv(x, 16), cz = Math.floorDiv(z, 16);
            int gx = Math.floorMod(x, 16), gz = Math.floorMod(z, 16);
            if (Math.abs(Math.floorMod(x, 96) - 48) <= 2) {
                int d = Math.floorMod(z, 128) / 2;           // 0..63 stepping down
                c.fill(c.minY, 59 - d, b(Blocks.STONE));
                c.set(60 - d, b(Blocks.COBBLESTONE));
            } else {
                c.fill(c.minY, 59, b(Blocks.STONE));
                c.set(60, b(Blocks.STONE_BRICKS));
            }
            if (n.hash(cx, cz, 4) < 0.1 && gx >= 6 && gx <= 9 && gz >= 6 && gz <= 9) {
                c.fill(61, 80 + (int) (n.hash(cx, cz, 5) * 50), b(Blocks.STONE_BRICKS));
            }
        }
    });

    // ---------- 5. Extreme Skyscrapers ----------
    private static boolean tower(Noise n, int cx, int cz) { return n.hash(cx, cz, 6) < 0.7; }

    public static final Zone SKYSCRAPERS = reg(new Zone() {
        public String id() { return "skyscrapers"; }
        public void column(int x, int z, Col c, Noise n) {
            int cx = Math.floorDiv(x, 32), cz = Math.floorDiv(z, 32);
            int lx = Math.floorMod(x, 32), lz = Math.floorMod(z, 32);
            int g = 60 + (int) (n.fbm(x / 50.0, z / 50.0, 2, 3) * 6);
            c.fill(c.minY, g, b(Blocks.STONE));
            if (n.hash(x, z, 4) < 0.015) {                    // dripstone spikes
                c.fill(g + 1, g + 6 + (int) (n.hash(x, z, 5) * 18), b(Blocks.DRIPSTONE_BLOCK));
            }
            if (lx >= 8 && lx < 24 && lz >= 8 && lz < 24 && tower(n, cx, cz)) {
                int H = 100 + (int) (n.hash(cx, cz, 7) * 110);
                BlockState p = pick(n, cx, cz, 12, b(Blocks.OAK_PLANKS), b(Blocks.SPRUCE_PLANKS),
                        b(Blocks.DARK_OAK_PLANKS), b(Blocks.BIRCH_PLANKS));
                c.fill(g + 1, H, p);
                c.set(H + 1, b(Blocks.GRASS_BLOCK));
                return;
            }
            if (lz >= 14 && lz < 18) {                         // bridge along +X
                int bx = lx < 8 ? cx - 1 : (lx >= 24 ? cx : Integer.MIN_VALUE);
                if (bx != Integer.MIN_VALUE && tower(n, bx, cz) && tower(n, bx + 1, cz) && n.hash(bx, cz, 8) < 0.5)
                    c.set(80 + (int) (n.hash(bx, cz, 9) * 15), b(Blocks.STONE_BRICKS));
            }
            if (lx >= 14 && lx < 18) {                         // bridge along +Z
                int bz = lz < 8 ? cz - 1 : (lz >= 24 ? cz : Integer.MIN_VALUE);
                if (bz != Integer.MIN_VALUE && tower(n, cx, bz) && tower(n, cx, bz + 1) && n.hash(cx, bz, 10) < 0.5)
                    c.set(80 + (int) (n.hash(cx, bz, 11) * 15), b(Blocks.STONE_BRICKS));
            }
        }
    });

    // ---------- 6. Rainbow Forest ----------
    public static final Zone RAINBOW_FOREST = reg(new Zone() {
        public String id() { return "rainbow_forest"; }
        public void column(int x, int z, Col c, Noise n) {
            int h = hgt(n, x, z, 75, 9, 55, 4);
            c.fill(c.minY, h - 4, b(Blocks.STONE));
            c.fill(h - 3, h - 1, b(Blocks.DIRT));
            c.set(h, b(Blocks.GRASS_BLOCK));
            if (n.hash(x, z, 13) < 0.07) {
                c.set(h + 1, pick(n, x, z, 14, b(Blocks.POPPY), b(Blocks.DANDELION), b(Blocks.ALLIUM),
                        b(Blocks.AZURE_BLUET), b(Blocks.CORNFLOWER), b(Blocks.OXEYE_DAISY),
                        b(Blocks.PINK_TULIP), b(Blocks.BLUE_ORCHID)));
            }
        }
        public void decorate(Deco d) {
            scatter(d, 15, 0.008, (dd, x, y, z, r) -> {
                BlockState[] logs = { b(Blocks.OAK_LOG), b(Blocks.SPRUCE_LOG), b(Blocks.BIRCH_LOG), b(Blocks.JUNGLE_LOG),
                        b(Blocks.ACACIA_LOG), b(Blocks.DARK_OAK_LOG), b(Blocks.CHERRY_LOG) };
                BlockState[] lvs = { leaves(Blocks.OAK_LEAVES), leaves(Blocks.SPRUCE_LEAVES), leaves(Blocks.BIRCH_LEAVES),
                        leaves(Blocks.JUNGLE_LEAVES), leaves(Blocks.ACACIA_LEAVES), leaves(Blocks.DARK_OAK_LEAVES),
                        leaves(Blocks.CHERRY_LEAVES), leaves(Blocks.AZALEA_LEAVES), leaves(Blocks.FLOWERING_AZALEA_LEAVES) };
                int i = (int) (r * 1000) % logs.length, j = (int) (r * 7919) % lvs.length;
                tree(dd, x, y, z, 5 + (int) (r * 5), logs[i], lvs[j], 3);
            });
        }
    });

    // ---------- 7. Ravager Desert (sand pillars grow until they blot out the sky) ----------
    public static final Zone RAVAGER_DESERT = reg(new Zone() {
        public String id() { return "ravager_desert"; }
        public void column(int x, int z, Col c, Noise n) {
            double t = Config.progress(z);
            int h = hgt(n, x, z, 70, 6, 60, 5);
            c.fill(c.minY, h - 4, b(Blocks.RED_SANDSTONE));
            c.fill(h - 3, h, b(Blocks.RED_SAND));
            int px = Math.floorDiv(x, 3), pz = Math.floorDiv(z, 3);
            if (n.hash(px, pz, 16) < 0.002 + 0.04 * t) {
                int top = h + 8 + (int) ((20 + 220 * t) * n.hash(px, pz, 17));
                c.fill(h + 1, top, b(Blocks.CUT_RED_SANDSTONE));
            }
        }
    });

    // ---------- 8. Mythical Forest ----------
    public static final Zone MYTHICAL_FOREST = reg(new Zone() {
        public String id() { return "mythical_forest"; }
        public void column(int x, int z, Col c, Noise n) {
            int h = hgt(n, x, z, 70, 3, 40, 6);
            c.fill(c.minY, h - 1, b(Blocks.NETHERRACK));
            c.set(h, n.fbm(x / 20.0, z / 20.0, 2, 18) > 0.68 ? b(Blocks.SCULK) : b(Blocks.WARPED_NYLIUM));
            if (n.hash(x, z, 19) < 0.02) c.set(h + 1, b(Blocks.WARPED_FUNGUS));

            int cx = Math.floorDiv(x, 24), cz = Math.floorDiv(z, 24);
            int tx = cx * 24 + 4 + (int) (n.hash(cx, cz, 20) * 16);
            int tz = cz * 24 + 4 + (int) (n.hash(cx, cz, 21) * 16);
            int dx = x - tx, dz = z - tz;
            if (dx * dx + dz * dz <= 5) {
                Block[] logs = { Blocks.OAK_LOG, Blocks.SPRUCE_LOG, Blocks.BIRCH_LOG, Blocks.JUNGLE_LOG,
                        Blocks.ACACIA_LOG, Blocks.DARK_OAK_LOG, Blocks.CHERRY_LOG, Blocks.MANGROVE_LOG, Blocks.WARPED_STEM };
                for (int y = h + 1; y <= c.maxY(); y++) {
                    boolean vein = n.fbm((x + y * 0.7) / 5.0, (z + y * 1.3) / 5.0, 2, 22) > 0.72;
                    c.set(y, vein ? b(Blocks.SEA_LANTERN)
                            : b(logs[(int) (n.hash(x * 31 + y, z, 23) * logs.length)]));
                }
            }
        }
    });

    // ---------- 9. Decayed Labyrinth ----------
    public static final Zone DECAYED_LABYRINTH = reg(new Zone() {
        public String id() { return "decayed_labyrinth"; }
        public void column(int x, int z, Col c, Noise n) {
            int cx = Math.floorDiv(x, 4), cz = Math.floorDiv(z, 4);
            int gx = Math.floorMod(x, 4), gz = Math.floorMod(z, 4);
            c.fill(c.minY, 63, b(Blocks.STONE));
            c.set(64, n.hash(x, z, 24) < 0.3 ? b(Blocks.MOSSY_COBBLESTONE) : b(Blocks.STONE_BRICKS));
            boolean wall = Maze.wall(n, x, z, 25);
            if (wall) {
                BlockState w = n.hash(x, z, 27) < 0.5 ? b(Blocks.MOSSY_COBBLESTONE) : b(Blocks.MOSSY_STONE_BRICKS);
                c.fill(65, 71, w);
                c.set(72, b(Blocks.SCULK));
            }
        }
    });

    // ---------- 11. Natural Maze (overgrown, always-solvable maze: hedges and mossy stone) ----------
    public static final Zone NATURAL_MAZE = reg(new Zone() {
        public String id() { return "natural_maze"; }
        public void column(int x, int z, Col c, Noise n) {
            int h = 64;
            c.fill(c.minY, h - 4, b(Blocks.STONE));
            c.fill(h - 3, h - 1, b(Blocks.DIRT));
            c.set(h, n.hash(x, z, 80) < 0.15 ? b(Blocks.MOSS_BLOCK) : b(Blocks.GRASS_BLOCK));

            if (Maze.wall(n, x, z, 81)) {
                if (Maze.pillar(x, z)) {
                    c.fill(h + 1, h + 7, b(Blocks.OAK_LOG));
                } else if (n.fbm(x / 48.0, z / 48.0, 2, 82) > 0.45) {          // hedge areas
                    c.fill(h + 1, h + 6, n.hash(x, z, 83) < 0.15 ? leaves(Blocks.AZALEA_LEAVES) : leaves(Blocks.OAK_LEAVES));
                } else {                                                         // ruined mossy stone areas
                    double r = n.hash(x, z, 84);
                    c.fill(h + 1, h + 6, r < 0.5 ? b(Blocks.MOSSY_STONE_BRICKS)
                            : (r < 0.8 ? b(Blocks.STONE_BRICKS) : b(Blocks.MOSSY_COBBLESTONE)));
                }
            } else if (n.hash(x, z, 85) < 0.10) {
                c.set(h + 1, pick(n, x, z, 86, b(Blocks.SHORT_GRASS), b(Blocks.FERN), b(Blocks.POPPY),
                        b(Blocks.DANDELION), b(Blocks.AZURE_BLUET)));
            }
        }
    });
}
