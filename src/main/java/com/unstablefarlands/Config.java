package com.unstablefarlands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Config file: config/unstable_farlands.json
 * Zones run along +Z, one after another, starting at startZ. Reorder or remove ids in "order".
 */
public final class Config {
    private Config() {}

    private static class Data {
        long startZ = 12_550_821L;
        int zoneLength = 50_000;
        int underworldFractureStart = 6000;   // underworld z where the land starts breaking into void
        int underworldFractureLength = 2000;  // how long the breaking-up stretch is before pure void + castles
        List<String> order = new ArrayList<>(Zones.ALL.keySet());
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static long startZ;
    private static int zoneLength;
    private static int fractureStart = 6000;
    private static int fractureLength = 2000;
    private static Zone[] zones = new Zone[0];

    public static void load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("unstable_farlands.json");
        Data d = new Data();
        try {
            if (Files.exists(file)) {
                Data read = GSON.fromJson(Files.readString(file), Data.class);
                if (read != null) d = read;
            } else {
                Files.writeString(file, GSON.toJson(d));
            }
        } catch (IOException | RuntimeException e) {
            UnstableFarlands.LOG.error("Could not read config, using defaults", e);
        }
        startZ = d.startZ;
        zoneLength = Math.max(1000, d.zoneLength);
        fractureStart = d.underworldFractureStart;
        fractureLength = Math.max(100, d.underworldFractureLength);
        List<Zone> list = new ArrayList<>();
        for (String id : d.order) {
            Zone z = Zones.ALL.get(id);
            if (z != null) list.add(z); else UnstableFarlands.LOG.warn("Unknown zone id in config: {}", id);
        }
        zones = list.toArray(new Zone[0]);
    }

    public static long startZ() { return startZ; }
    public static int zoneLength() { return zoneLength; }
    public static int fractureStart() { return fractureStart; }
    public static int voidStart() { return fractureStart + fractureLength; }
    public static int fractureLength() { return fractureLength; }
    public static int zoneCount() { return zones.length; }
    public static Zone zone(int i) { return zones[i]; }

    public static int index(int wz) {
        long rel = (long) wz - startZ;
        if (rel < 0) return -1;
        long i = rel / zoneLength;
        return i < zones.length ? (int) i : -1;
    }

    public static Zone zoneAt(int wz) {
        int i = index(wz);
        return i < 0 ? null : zones[i];
    }

    /** 0..1 progress through the zone containing wz. */
    public static double progress(int wz) {
        long rel = (long) wz - startZ;
        if (rel < 0) return 0;
        return (rel % zoneLength) / (double) zoneLength;
    }

    public static List<String> ids() {
        List<String> out = new ArrayList<>();
        for (Zone z : zones) out.add(z.id());
        return out;
    }
}
