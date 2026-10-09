package net.livingworld.world.gen;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.livingworld.LivingWorld;
import net.minecraft.world.World;

public final class WorldGenHandler {
    private static volatile long overworldSeed = Long.MIN_VALUE;

    private WorldGenHandler() {}

    /**
     * {@return the seed of the currently loaded overworld, or
     * {@code Long.MIN_VALUE} if no overworld is loaded yet}
     */
    public static long worldSeed() {
        return overworldSeed;
    }

    public static void register() {
        ServerWorldEvents.LOAD.register((server, world) -> {
            if (world.getRegistryKey() == World.OVERWORLD) {
                long seed = world.getSeed();
                overworldSeed = seed;
                LivingTerrainDensityFunction.setWorldSeed(seed);
                LivingWorld.LOGGER.info("[TheLivingWorld] Overworld terrain seeded with {}.", seed);
            }
        });
    }
}
