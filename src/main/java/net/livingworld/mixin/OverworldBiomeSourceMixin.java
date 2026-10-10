package net.livingworld.mixin;

import net.livingworld.config.LivingWorldConfig;
import net.livingworld.world.climate.ClimateSampler;
import net.livingworld.world.gen.LivingTerrainDensityFunction;
import net.livingworld.world.gen.TerrainSampler;
import net.livingworld.world.gen.WorldGenHandler;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.random.RandomSplitter;
import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandom;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.MultiNoiseBiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

/**
 * Replaces Overworld biome placement with climate + altitude driven placement
 * that is <em>consistent with the custom terrain</em>: the picked biome depends
 * on the real sampled surface height of the column (so beaches sit at sea
 * level, peaks sit on mountain tops and deserts never float on plateaus), on
 * the regional temperature/humidity, and on the elevation lapse rate.
 *
 * <p>Positions below the surface (caves) receive the surface biome of their
 * column, matching vanilla behaviour. The Nether also uses a
 * {@link MultiNoiseBiomeSource}, so it is detected and left untouched.
 */
@Mixin(MultiNoiseBiomeSource.class)
public abstract class OverworldBiomeSourceMixin {
    @Unique
    private volatile ClimateSampler livingworld$climateSampler;
    @Unique
    private volatile long livingworld$climateSeed = Long.MIN_VALUE;
    @Unique
    private volatile Map<RegistryKey<Biome>, RegistryEntry<Biome>> livingworld$entries;
    @Unique
    private volatile boolean livingworld$notOverworld;

    @Inject(method = "getBiome", at = @At("HEAD"), cancellable = true)
    private void livingworld$overrideBiome(int biomeX, int biomeY, int biomeZ, MultiNoiseUtil.MultiNoiseSampler noise, CallbackInfoReturnable<RegistryEntry<Biome>> cir) {
        LivingWorldConfig cfg = LivingWorldConfig.INSTANCE;
        if (!cfg.enableClimateBiomes) return;

        if (livingworld$entries == null) {
            synchronized (this) {
                if (livingworld$entries == null) {
                    livingworld$buildEntryMap();
                }
            }
        }
        if (livingworld$notOverworld) return;

        long seed = WorldGenHandler.worldSeed();
        if (seed == Long.MIN_VALUE) return; // world seed not known yet, keep vanilla placement
        if (livingworld$climateSampler == null || livingworld$climateSeed != seed) {
            Xoroshiro128PlusPlusRandom r = new Xoroshiro128PlusPlusRandom(seed ^ 0x9e3779b97f4a7c15L);
            RandomSplitter sp = r.nextSplitter();
            livingworld$climateSampler = new ClimateSampler(sp, cfg);
            livingworld$climateSeed = seed;
        }

        int blockX = biomeX << 2;
        int blockZ = biomeZ << 2;

        // Use the height of the terrain that will actually generate here so
        // biomes line up with oceans, coastlines, hills and mountain tops.
        TerrainSampler terrain = LivingTerrainDensityFunction.sampler();
        double height = terrain.sampleHeight(blockX, blockZ);

        double temp = livingworld$climateSampler.sampleTemperature(blockX, (int) height, blockZ);
        double hum = livingworld$climateSampler.sampleHumidity(blockX, blockZ);

        RegistryKey<Biome> picked = pickBiome(temp, hum, height);
        RegistryEntry<Biome> entry = livingworld$entries.get(picked);
        if (entry == null) {
            entry = livingworld$entries.get(BiomeKeys.PLAINS);
        }
        if (entry == null) return;
        cir.setReturnValue(entry);
    }

    @Unique
    private void livingworld$buildEntryMap() {
        Map<RegistryKey<Biome>, RegistryEntry<Biome>> map = new HashMap<>();
        for (RegistryEntry<Biome> e : ((BiomeSource) (Object) this).getBiomes()) {
            e.getKey().ifPresent(key -> map.put(key, e));
        }
        livingworld$entries = map;
        // The Nether uses a MultiNoiseBiomeSource as well; never touch it.
        livingworld$notOverworld = map.containsKey(BiomeKeys.NETHER_WASTES);
    }

    @Unique
    private static RegistryKey<Biome> pickBiome(double temp, double hum, double height) {
        final double sea = 63.0;
        double elev = height - sea;

        // Oceans: only where the ground really is below sea level.
        if (elev < -30) {
            if (temp > 0.3) return BiomeKeys.DEEP_LUKEWARM_OCEAN;
            if (temp > -0.1) return BiomeKeys.DEEP_OCEAN;
            if (temp > -0.35) return BiomeKeys.DEEP_COLD_OCEAN;
            return BiomeKeys.DEEP_FROZEN_OCEAN;
        }
        if (elev < -5) {
            if (temp > 0.4) return BiomeKeys.WARM_OCEAN;
            if (temp > 0.05) return BiomeKeys.LUKEWARM_OCEAN;
            if (temp > -0.3) return BiomeKeys.OCEAN;
            if (temp > -0.5) return BiomeKeys.COLD_OCEAN;
            return BiomeKeys.FROZEN_OCEAN;
        }
        // Coastlines.
        if (elev < 3) {
            return temp < -0.25 ? BiomeKeys.SNOWY_BEACH : BiomeKeys.BEACH;
        }

        // High mountains: altitude bands, cold via lapse rate.
        if (height > 215) {
            if (temp < -0.1) return BiomeKeys.FROZEN_PEAKS;
            if (hum < 0.0) return BiomeKeys.STONY_PEAKS;
            return BiomeKeys.JAGGED_PEAKS;
        }
        if (height > 178) {
            if (temp < 0.0) return BiomeKeys.SNOWY_SLOPES;
            if (hum > 0.25) return BiomeKeys.GROVE;
            return BiomeKeys.MEADOW;
        }
        if (height > 140) {
            if (temp < -0.2) return BiomeKeys.SNOWY_TAIGA;
            if (hum > 0.15) return BiomeKeys.TAIGA;
            return BiomeKeys.WINDSWEPT_HILLS;
        }

        // Lowland climate zones.
        if (temp > 0.62 && hum < -0.05) return BiomeKeys.DESERT;
        if (temp > 0.45 && hum < 0.18) {
            return elev > 35 ? BiomeKeys.SAVANNA_PLATEAU : BiomeKeys.SAVANNA;
        }
        if (temp > 0.5 && hum > 0.45) return BiomeKeys.JUNGLE;
        if (temp > 0.25 && hum > 0.25 && elev < 6) return BiomeKeys.SWAMP;
        if (temp < -0.32) {
            return hum > 0.05 ? BiomeKeys.SNOWY_TAIGA : BiomeKeys.SNOWY_PLAINS;
        }
        if (temp < -0.05) {
            return hum > 0.15 ? BiomeKeys.TAIGA : BiomeKeys.SNOWY_PLAINS;
        }
        if (hum > 0.45) {
            if (temp < 0.15) return BiomeKeys.DARK_FOREST;
            if (height > 95) return BiomeKeys.WINDSWEPT_FOREST;
            return BiomeKeys.FOREST;
        }
        if (hum > 0.15) {
            if (temp > 0.0 && temp < 0.4 && height < 100) return BiomeKeys.FLOWER_FOREST;
            if (temp < 0.0) return BiomeKeys.TAIGA;
            if (height > 100) return BiomeKeys.WINDSWEPT_FOREST;
            return BiomeKeys.BIRCH_FOREST;
        }
        if (hum > -0.4) {
            if (height > 90) return BiomeKeys.WINDSWEPT_HILLS;
            return temp > 0.3 ? BiomeKeys.SAVANNA : BiomeKeys.PLAINS;
        }
        return BiomeKeys.DESERT;
    }
}
