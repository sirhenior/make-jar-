package com.unstablefarlands;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;

/**
 * All cross-dimension teleporting lives here, because this API changes most between versions.
 * If this file fails to compile, only this one method needs fixing.
 */
public final class Warp {
    private Warp() {}

    public static void to(ServerPlayerEntity p, ServerWorld world, double x, double y, double z) {
        TeleportTarget t = new TeleportTarget(world, new Vec3d(x, y, z), Vec3d.ZERO,
                p.getYaw(), p.getPitch(), TeleportTarget.NO_OP);
        p.teleportTo(t);
        p.fallDistance = 0;
    }
}
