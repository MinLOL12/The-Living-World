package net.livingworld.world.climate;

import net.livingworld.config.LivingWorldConfig;
import net.minecraft.util.math.noise.DoublePerlinNoiseSampler;
import net.minecraft.util.math.random.RandomSplitter;

public final class ClimateSampler {
    private final DoublePerlinNoiseSampler tempNoise;
    private final DoublePerlinNoiseSampler tempNoiseLarge;
    private final DoublePerlinNoiseSampler humidityNoise;
    private final DoublePerlinNoiseSampler humidityNoiseLarge;
    private final DoublePerlinNoiseSampler rainfallNoise;
    private final double scale;
    private final double variation;

    public ClimateSampler(RandomSplitter splitter, LivingWorldConfig cfg) {
        this.scale = 0.004 / cfg.biomeSize;
        this.variation = cfg.climateVariation;
        this.tempNoise = DoublePerlinNoiseSampler.create(splitter.split("lw_temp"), -8, 1.0);
        this.tempNoiseLarge = DoublePerlinNoiseSampler.create(splitter.split("lw_temp_large"), -10, 1.0);
        this.humidityNoise = DoublePerlinNoiseSampler.create(splitter.split("lw_humidity"), -8, 1.0);
        this.humidityNoiseLarge = DoublePerlinNoiseSampler.create(splitter.split("lw_humidity_large"), -10, 1.0);
        this.rainfallNoise = DoublePerlinNoiseSampler.create(splitter.split("lw_rain"), -7, 1.0);
    }

    public double sampleTemperature(int blockX, int blockY, int blockZ) {
        double sx = blockX * scale;
        double sz = blockZ * scale;
        // Smooth equator/pole cycle (~22k blocks per half wave) instead of a
        // sawtooth, which produced visible striped temperature bands.
        double lat = Math.cos(blockZ * 0.00028);
        double baseTemp = 0.42 + 0.38 * lat;
        double regional = tempNoiseLarge.sample(sx * 0.5, 0.0, sz * 0.5) * 0.35;
        double local = tempNoise.sample(sx, 0.0, sz) * 0.15;
        double elev = (blockY - 64) * -0.0065;
        double t = baseTemp + regional + local + elev;
        return clamp(t * variation, -1.0, 1.0);
    }

    public double sampleHumidity(int blockX, int blockZ) {
        double sx = blockX * scale;
        double sz = blockZ * scale;
        double regional = humidityNoiseLarge.sample(sx * 0.5, 0.0, sz * 0.5) * 0.4;
        double local = humidityNoise.sample(sx, 0.0, sz) * 0.2;
        double h = 0.5 + regional + local;
        return clamp(h * variation, -1.0, 1.0);
    }

    public double sampleRainfall(int blockX, int blockZ) {
        double sx = blockX * scale * 1.2;
        double sz = blockZ * scale * 1.2;
        double r = 0.5 + rainfallNoise.sample(sx, 0.0, sz) * 0.5;
        return clamp(r, 0.0, 1.0);
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
