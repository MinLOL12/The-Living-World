package net.livingworld.world.climate;

import net.livingworld.config.LivingWorldConfig;
import net.minecraft.util.math.noise.NormalNoise;
import net.minecraft.util.math.random.RandomSplitter;

public final class ClimateSampler {
    private final NormalNoise tempNoise;
    private final NormalNoise tempNoiseLarge;
    private final NormalNoise humidityNoise;
    private final NormalNoise humidityNoiseLarge;
    private final NormalNoise rainfallNoise;
    private final double scale;
    private final double variation;

    public ClimateSampler(RandomSplitter splitter, LivingWorldConfig cfg) {
        this.scale = 0.004 / cfg.biomeSize;
        this.variation = cfg.climateVariation;
        this.tempNoise = NormalNoise.create(splitter.split("lw_temp"), -8, 1.0);
        this.tempNoiseLarge = NormalNoise.create(splitter.split("lw_temp_large"), -10, 1.0);
        this.humidityNoise = NormalNoise.create(splitter.split("lw_humidity"), -8, 1.0);
        this.humidityNoiseLarge = NormalNoise.create(splitter.split("lw_humidity_large"), -10, 1.0);
        this.rainfallNoise = NormalNoise.create(splitter.split("lw_rain"), -7, 1.0);
    }

    public double sampleTemperature(int blockX, int blockY, int blockZ) {
        double sx = blockX * scale;
        double sz = blockZ * scale;
        double lat = 1.0 - Math.abs(Math.IEEEremainder(blockZ * 0.0008, 2.0)) / 1.0;
        lat = Math.max(0.0, Math.min(1.0, lat));
        double baseTemp = lat * 0.6 + 0.2;
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
