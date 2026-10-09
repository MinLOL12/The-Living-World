package net.livingworld;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.livingworld.config.LivingWorldConfig;
import net.livingworld.world.biome.ClimateBiomeSource;
import net.livingworld.world.gen.WorldGenHandler;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LivingWorld implements ModInitializer {
    public static final String MOD_ID = "livingworld";
    public static final Logger LOGGER = LoggerFactory.getLogger("TheLivingWorld");

    @Override
    public void onInitialize() {
        LivingWorldConfig.INSTANCE.load(java.nio.file.Paths.get("config"));
        ClimateBiomeSource.register();
        WorldGenHandler.register();

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            LivingWorldConfig.INSTANCE.load(server.getRunDirectory().toPath().resolve("config"));
            LOGGER.info("[TheLivingWorld] Config reloaded.");
        });

        LOGGER.info("[TheLivingWorld] Terrain overhaul initialized.");
    }

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }
}
