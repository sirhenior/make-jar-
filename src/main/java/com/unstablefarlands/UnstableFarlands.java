package com.unstablefarlands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnstableFarlands implements ModInitializer {
    public static final String MOD_ID = "unstable_farlands";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    /** The Underworld dimension, defined in data/unstable_farlands/dimension/underworld.json */
    public static final RegistryKey<World> UNDERWORLD =
            RegistryKey.of(RegistryKeys.WORLD, Identifier.of(MOD_ID, "underworld"));

    @Override
    public void onInitialize() {
        Config.load();
        LOG.info("Unstable Farlands: {} overworld zones along +Z from z={}", Config.zoneCount(), Config.startZ());

        // Rewrite chunks right after they are generated.
        ServerChunkEvents.CHUNK_GENERATE.register((world, chunk) -> {
            RegistryKey<World> key = world.getRegistryKey();
            if (key == World.OVERWORLD) ZoneManager.apply(world, chunk);
            else if (key == UNDERWORLD) ZoneManager.applyWhole(world, chunk, Underworld.I);
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            int tick = server.getTicks();
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                // NOTE: if getEntityWorld() errors, swap it for getWorld() (or vice versa).
                RegistryKey<World> key = p.getEntityWorld().getRegistryKey();

                // Falling out of the bottom of the Nether drops you into the Underworld.
                if (key == World.NETHER && tick % 2 == 0 && p.getY() < -20) {
                    sendToUnderworld(server, p, p.getBlockX(), p.getBlockZ());
                    continue;
                }
                if (tick % 20 != 0) continue;

                boolean dark = false;
                if (key == World.OVERWORLD) {
                    Zone z = Config.zoneAt(p.getBlockZ());
                    dark = z != null && z.dark();
                } else if (key == UNDERWORLD) {
                    dark = p.getBlockZ() >= Config.fractureStart();   // gets darker toward the void
                }
                if (dark) {
                    p.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 80, 0, false, false, false));
                }
            }
        });

        // /unstablefarlands list | tp <zone> | underworld [fracture|void]
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) ->
            dispatcher.register(CommandManager.literal("unstablefarlands")
                .then(CommandManager.literal("list").executes(ctx -> {
                    ctx.getSource().sendFeedback(() -> Text.literal("Zones (along +Z): " + String.join(", ", Config.ids())), false);
                    return 1;
                }))
                .then(CommandManager.literal("tp")
                    .then(CommandManager.argument("zone", StringArgumentType.word())
                        .suggests((c, b) -> CommandSource.suggestMatching(Config.ids(), b))
                        .executes(UnstableFarlands::teleport)))
                .then(CommandManager.literal("underworld")
                    .executes(ctx -> warpUnderworld(ctx, 0))
                    .then(CommandManager.literal("fracture").executes(ctx -> warpUnderworld(ctx, Config.fractureStart() + 100)))
                    .then(CommandManager.literal("void").executes(ctx -> warpUnderworld(ctx, Config.voidStart() + 40))))));
    }

    // ------------------------------------------------------------------ helpers

    private static void sendToUnderworld(MinecraftServer server, ServerPlayerEntity p, int x, int z) {
        ServerWorld uw = server.getWorld(UNDERWORLD);
        if (uw == null) {
            LOG.warn("Underworld dimension is not loaded (is the datapack part of this world?)");
            return;
        }
        Warp.to(p, uw, x + 0.5, underworldY(uw, x, z), z + 0.5);
    }

    private static double underworldY(ServerWorld uw, int x, int z) {
        Col col = new Col(uw.getBottomY(), uw.getHeight());
        Underworld.I.column(x, z, col, ZoneManager.noise(uw.getSeed()));
        int top = col.top();
        return (top < col.minY ? 110 : top) + 3;
    }

    private static int warpUnderworld(CommandContext<ServerCommandSource> ctx, int z) {
        ServerCommandSource src = ctx.getSource();
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) { src.sendError(Text.literal("Players only.")); return 0; }
        ServerWorld uw = src.getServer().getWorld(UNDERWORLD);
        if (uw == null) {
            src.sendError(Text.literal("Underworld dimension not found. Make a NEW world with the mod installed."));
            return 0;
        }
        Warp.to(player, uw, 0.5, underworldY(uw, 0, z), z + 0.5);
        src.sendFeedback(() -> Text.literal("Welcome to the Underworld (z=" + z + ")"), false);
        return 1;
    }

    private static int teleport(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        String id = StringArgumentType.getString(ctx, "zone");
        int idx = Config.ids().indexOf(id);
        if (idx < 0) { src.sendError(Text.literal("Unknown zone: " + id)); return 0; }
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) { src.sendError(Text.literal("Players only.")); return 0; }
        ServerWorld world = src.getServer().getOverworld();
        int x = 0;
        int z = (int) Math.min(29_999_000L, Config.startZ() + (long) idx * Config.zoneLength() + 200);
        Col col = new Col(world.getBottomY(), world.getHeight());
        Config.zone(idx).column(x, z, col, ZoneManager.noise(world.getSeed()));
        int top = col.top();
        double y = (top < col.minY ? 80 : top) + 3;
        Warp.to(player, world, x + 0.5, y, z + 0.5);
        src.sendFeedback(() -> Text.literal("Teleported to " + id + " at z=" + z), false);
        return 1;
    }
}
