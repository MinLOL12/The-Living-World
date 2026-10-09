package net.livingworld.mixin;

import net.livingworld.config.LivingWorldConfig;
import net.livingworld.world.climate.ClimateSampler;
import net.livingworld.world.gen.WorldGenHandler;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.random.RandomSplitter;
import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandom;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.source.MultiNoiseBiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.Set;

@Mixin(MultiNoiseBiomeSource.class)
public abstract class OverworldBiomeSourceMixin {
    @Shadow
    public abstract Set<RegistryEntry<Biome>> getBiomes();

    @Unique
    private ClimateSampler livingworld$climateSampler;
    @Unique
    private long livingworld$climateSeed = Long.MIN_VALUE;

    @Inject(method = "getBiome", at = @At("HEAD"), cancellable = true)
    private void livingworld$overrideBiome(int biomeX, int biomeY, int biomeZ, MultiNoiseUtil.MultiNoiseSampler noise, CallbackInfoReturnable<RegistryEntry<Biome>> cir) {
        LivingWorldConfig cfg = LivingWorldConfig.INSTANCE;
        if (!cfg.enableClimateBiomes) return;

        long seed = WorldGenHandler.worldSeed();
        if (seed == Long.MIN_VALUE) return; // world seed not known yet, keep vanilla placement
        if (livingworld$climateSampler == null || livingworld$climateSeed != seed) {
            Xoroshiro128PlusPlusRandom r = new Xoroshiro128PlusPlusRandom(seed ^ 0x9e3779b97f4a7c15L);
            RandomSplitter sp = r.nextSplitter();
            livingworld$climateSampler = new ClimateSampler(sp.split("lw_climate"), cfg);
            livingworld$climateSeed = seed;
        }

        int blockX = biomeX << 2;
        int blockY = biomeY << 2;
        int blockZ = biomeZ << 2;

        double temp = livingworld$climateSampler.sampleTemperature(blockX, blockY, blockZ);
        double hum = livingworld$climateSampler.sampleHumidity(blockX, blockZ);

        RegistryKey<Biome> picked = pickBiome(temp, hum, blockY);
        for (RegistryEntry<Biome> e : this.getBiomes()) {
            Optional<RegistryKey<Biome>> key = e.getKey();
            if (key.isPresent() && key.get() == picked) {
                cir.setReturnValue(e);
                return;
            }
        }
        for (RegistryEntry<Biome> e : this.getBiomes()) {
            Optional<RegistryKey<Biome>> key = e.getKey();
            if (key.isPresent() && key.get() == BiomeKeys.PLAINS) {
                cir.setReturnValue(e);
                return;
            }
        }
    }

    @Unique
    private RegistryKey<Biome> pickBiome(double temp, double hum, int blockY) {
        if (blockY < 40) {
            if (temp > 0.5) return BiomeKeys.WARM_OCEAN;
            if (temp > 0.1) return BiomeKeys.LUKEWARM_OCEAN;
            if (temp > -0.3) return BiomeKeys.COLD_OCEAN;
            return BiomeKeys.FROZEN_OCEAN;
        }
        if (blockY > 190) {
            if (temp < -0.1) return BiomeKeys.FROZEN_PEAKS;
            if (hum < 0.0) return BiomeKeys.STONY_PEAKS;
            return BiomeKeys.JAGGED_PEAKS;
        }
        if (blockY > 150) {
            if (temp < 0.0) return BiomeKeys.SNOWY_SLOPES;
            return BiomeKeys.MEADOW;
        }
        if (blockY > 120) {
            if (temp < -0.1) return BiomeKeys.SNOWY_TAIGA;
            if (hum > 0.2) return BiomeKeys.TAIGA;
            return BiomeKeys.WINDSWEPT_HILLS;
        }
        if (temp > 0.7 && hum < 0.0) return BiomeKeys.DESERT;
        if (temp > 0.5 && hum < 0.2) return BiomeKeys.SAVANNA;
        if (temp > 0.6 && hum > 0.5) return BiomeKeys.JUNGLE;
        if (temp > 0.2 && hum > 0.3 && blockY < 68) return BiomeKeys.SWAMP;
        if (temp < -0.3) {
            if (hum > 0.1) return BiomeKeys.SNOWY_TAIGA;
            return BiomeKeys.SNOWY_PLAINS;
        }
        if (temp < 0.0) {
            if (hum > 0.2) return BiomeKeys.TAIGA;
            return BiomeKeys.SNOWY_PLAINS;
        }
        if (hum > 0.5) {
            if (temp < 0.2) return BiomeKeys.DARK_FOREST;
            if (blockY > 80) return BiomeKeys.WINDSWEPT_FOREST;
            return BiomeKeys.FOREST;
        }
        if (hum > 0.2) {
            if (temp > 0.2 && blockY > 80) return BiomeKeys.BIRCH_FOREST;
            return BiomeKeys.PLAINS;
        }
        if (blockY > 75) return BiomeKeys.WINDSWEPT_HILLS;
        if (hum < -0.3) return BiomeKeys.DESERT;
        return BiomeKeys.PLAINS;
    }
}
