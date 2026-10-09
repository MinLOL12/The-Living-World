package net.livingworld.world.gen;

import net.livingworld.config.LivingWorldConfig;
import net.minecraft.util.math.noise.NormalNoise;
import net.minecraft.util.math.random.RandomSplitter;

public final class TerrainSampler {
    private final NormalNoise continentNoise;
    private final NormalNoise mountainRangeNoise;
    private final NormalNoise ridgeNoise;
    private final NormalNoise plateauNoise;
    private final NormalNoise hillsNoise;
    private final NormalNoise detailNoise;
    private final NormalNoise erosionNoise;
    private final NormalNoise riverNoise;
    private final NormalNoise riverNoise2;
    private final NormalNoise valleyNoise;
    private final double continentalScale;
    private final double mountainScale;
    private final double cfgScale;
    private final double mountainHeightMul;
    private final double riverDepthMul;
    private final double plateauSharpness;
    private final boolean largeMountains;
    private final boolean floodplains;

    public TerrainSampler(RandomSplitter splitter, LivingWorldConfig cfg) {
        this.cfgScale = cfg.terrainScale;
        this.continentalScale = 0.0012 / cfgScale;
        this.mountainScale = 0.0025 / cfgScale;
        this.mountainHeightMul = cfg.mountainHeight;
        this.riverDepthMul = cfg.riverDepth;
        this.plateauSharpness = cfg.plateauSharpness;
        this.largeMountains = cfg.enableLargeMountains;
        this.floodplains = cfg.enableFloodplains;

        this.continentNoise = NormalNoise.create(splitter.split("lw_continent"), -10, 1.0);
        this.mountainRangeNoise = NormalNoise.create(splitter.split("lw_mountain_range"), -9, 1.0);
        this.ridgeNoise = NormalNoise.create(splitter.split("lw_ridge"), -8, 1.0);
        this.plateauNoise = NormalNoise.create(splitter.split("lw_plateau"), -9, 1.0);
        this.hillsNoise = NormalNoise.create(splitter.split("lw_hills"), -7, 1.0);
        this.detailNoise = NormalNoise.create(splitter.split("lw_detail"), -6, 1.0);
        this.erosionNoise = NormalNoise.create(splitter.split("lw_erosion"), -8, 1.0);
        this.riverNoise = NormalNoise.create(splitter.split("lw_river"), -9, 1.0);
        this.riverNoise2 = NormalNoise.create(splitter.split("lw_river2"), -8, 1.0);
        this.valleyNoise = NormalNoise.create(splitter.split("lw_valley"), -8, 1.0);
    }

    public double sampleHeight(int blockX, int blockZ) {
        double cx = blockX * continentalScale;
        double cz = blockZ * continentalScale;

        double continent = continentNoise.sample(cx, 0.0, cz) * 0.5 + 0.5;
        continent = smoothStep(continent);

        double baseHeight;
        if (continent < 0.28) {
            baseHeight = -10 + (continent / 0.28) * 20;
        } else if (continent < 0.45) {
            double t = (continent - 0.28) / 0.17;
            baseHeight = 10 + t * 42;
        } else if (continent < 0.72) {
            double t = (continent - 0.45) / 0.27;
            baseHeight = 52 + t * 12;
        } else {
            double t = (continent - 0.72) / 0.28;
            baseHeight = 64 + t * 24;
        }

        double mx = blockX * mountainScale;
        double mz = blockZ * mountainScale;
        double mountainRegion = mountainRangeNoise.sample(mx * 0.4, 0.0, mz * 0.4);
        mountainRegion = Math.max(0.0, mountainRegion - 0.05);
        mountainRegion = mountainRegion * mountainRegion;

        double ridges = 0;
        if (largeMountains) {
            double r1 = ridgeNoise.sample(mx, 0.0, mz);
            double r2 = ridgeNoise.sample(mx * 2.0, 0.0, mz * 2.0) * 0.5;
            ridges = 1.0 - Math.abs(r1);
            ridges = Math.pow(Math.max(0.0, ridges), 2.2);
            ridges *= (0.6 + r2 * 0.4);
        }

        double mountainHeight = mountainRegion * (40 + ridges * 90 * mountainHeightMul);

        double px = blockX * 0.003 / cfgScale;
        double pz = blockZ * 0.003 / cfgScale;
        double plateauRaw = plateauNoise.sample(px, 0.0, pz);
        double plateau = Math.max(0.0, plateauRaw - 0.1) * 30 * plateauSharpness;
        plateau = Math.min(plateau, 40 * plateauSharpness);

        double hx = blockX * 0.008 / cfgScale;
        double hz = blockZ * 0.008 / cfgScale;
        double hills = hillsNoise.sample(hx, 0.0, hz) * 12;
        double rolling = hillsNoise.sample(hx * 0.35, 0.0, hz * 0.35) * 18;

        double dx = blockX * 0.04;
        double dz = blockZ * 0.04;
        double detail = detailNoise.sample(dx, 0.0, dz) * 2.5;

        double ex = blockX * 0.002;
        double ez = blockZ * 0.002;
        double erosion = erosionNoise.sample(ex, 0.0, ez);
        double erosionFactor = smoothStep01(erosion * 0.5 + 0.5);

        double vx = blockX * 0.004 / cfgScale;
        double vz = blockZ * 0.004 / cfgScale;
        double valleyRaw = valleyNoise.sample(vx, 0.0, vz);
        double valley = Math.max(0.0, -valleyRaw) * 20;

        double rx = blockX * 0.003 / cfgScale;
        double rz = blockZ * 0.003 / cfgScale;
        double riverA = riverNoise.sample(rx, 0.0, rz);
        double riverB = riverNoise2.sample(rx * 2.0, 0.0, rz * 2.0);
        double riverCombined = Math.abs(riverA) * (0.55 + Math.abs(riverB) * 0.45);
        double river = 0;
        if (riverCombined < 0.04) {
            double t = 1.0 - riverCombined / 0.04;
            river = t * t * 22 * riverDepthMul;
        }

        double floodplain = 0;
        if (floodplains) {
            double fl = Math.abs(valleyRaw) + riverCombined * 2.0;
            if (fl < 0.12) {
                double t = 1.0 - fl / 0.12;
                floodplain = t * 8;
            }
        }

        double height = baseHeight;
        height += plateau;
        height += mountainHeight;
        height += rolling;
        height += hills;
        height += detail;
        height -= valley * 0.6;
        height -= river;
        height -= floodplain;

        height -= erosionFactor * 6;

        return height;
    }

    public double sampleRiverFactor(int blockX, int blockZ) {
        double rx = blockX * 0.003 / cfgScale;
        double rz = blockZ * 0.003 / cfgScale;
        double a = riverNoise.sample(rx, 0.0, rz);
        double b = riverNoise2.sample(rx * 2.0, 0.0, rz * 2.0);
        double combined = Math.abs(a) * (0.55 + Math.abs(b) * 0.45);
        if (combined < 0.04) {
            return 1.0 - combined / 0.04;
        }
        return 0.0;
    }

    public double sampleMountainness(int blockX, int blockZ) {
        double mx = blockX * mountainScale;
        double mz = blockZ * mountainScale;
        double mr = mountainRangeNoise.sample(mx * 0.4, 0.0, mz * 0.4);
        return Math.max(0.0, Math.min(1.0, mr * 1.8));
    }

    private static double smoothStep(double v) {
        v = Math.max(0.0, Math.min(1.0, v));
        return v * v * (3 - 2 * v);
    }

    private static double smoothStep01(double v) {
        v = Math.max(0.0, Math.min(1.0, v));
        return v * v * (3 - 2 * v);
    }
}
