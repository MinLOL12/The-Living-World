package net.livingworld.world.gen;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.livingworld.LivingWorld;
import net.minecraft.world.World;

public final class WorldGenHandler {
    private WorldGenHandler() {}

    public static void register() {
        ServerWorldEvents.LOAD.register((server, world) -> {
            if (world.getRegistryKey() == World.OVERWORLD) {
                long seed = world.getSeed();
                LivingTerrainDensityFunction.setWorldSeed(seed);
                LivingWorld.LOGGER.info("[TheLivingWorld] Overworld terrain seeded with {}.", seed);
            }
        });
    }
}
