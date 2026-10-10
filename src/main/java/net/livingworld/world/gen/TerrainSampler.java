package net.livingworld.world.gen;

import net.livingworld.config.LivingWorldConfig;
import net.minecraft.util.math.noise.DoublePerlinNoiseSampler;
import net.minecraft.util.math.random.RandomSplitter;

public final class TerrainSampler {
    private final DoublePerlinNoiseSampler continentNoise;
    private final DoublePerlinNoiseSampler mountainRangeNoise;
    private final DoublePerlinNoiseSampler ridgeNoise;
    private final DoublePerlinNoiseSampler plateauNoise;
    private final DoublePerlinNoiseSampler hillsNoise;
    private final DoublePerlinNoiseSampler detailNoise;
    private final DoublePerlinNoiseSampler erosionNoise;
    private final DoublePerlinNoiseSampler riverNoise;
    private final DoublePerlinNoiseSampler riverNoise2;
    private final DoublePerlinNoiseSampler valleyNoise;
    private final DoublePerlinNoiseSampler caveCheeseNoise;
    private final DoublePerlinNoiseSampler caveSpaghettiNoise;
    private final DoublePerlinNoiseSampler caveWormNoise;
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

        this.continentNoise = DoublePerlinNoiseSampler.create(splitter.split("lw_continent"), -10, 1.0);
        this.mountainRangeNoise = DoublePerlinNoiseSampler.create(splitter.split("lw_mountain_range"), -9, 1.0);
        this.ridgeNoise = DoublePerlinNoiseSampler.create(splitter.split("lw_ridge"), -8, 1.0);
        this.plateauNoise = DoublePerlinNoiseSampler.create(splitter.split("lw_plateau"), -9, 1.0);
        this.hillsNoise = DoublePerlinNoiseSampler.create(splitter.split("lw_hills"), -7, 1.0);
        this.detailNoise = DoublePerlinNoiseSampler.create(splitter.split("lw_detail"), -6, 1.0);
        this.erosionNoise = DoublePerlinNoiseSampler.create(splitter.split("lw_erosion"), -8, 1.0);
        this.riverNoise = DoublePerlinNoiseSampler.create(splitter.split("lw_river"), -9, 1.0);
        this.riverNoise2 = DoublePerlinNoiseSampler.create(splitter.split("lw_river2"), -8, 1.0);
        this.valleyNoise = DoublePerlinNoiseSampler.create(splitter.split("lw_valley"), -8, 1.0);
        this.caveCheeseNoise = DoublePerlinNoiseSampler.create(splitter.split("lw_cave_cheese"), -9, 1.0);
        this.caveSpaghettiNoise = DoublePerlinNoiseSampler.create(splitter.split("lw_cave_spaghetti"), -8, 1.0);
        this.caveWormNoise = DoublePerlinNoiseSampler.create(splitter.split("lw_cave_worm"), -7, 1.0);
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

    /**
     * Cave carving strength at a block position, in {@code [0, 1]}.
     *
     * <p>Three layered 3D noise fields reproduce the vanilla cave vocabulary
     * without depending on vanilla's surface-relative cave density (which is
     * anchored to the vanilla surface and therefore useless once the surface
     * height has been replaced):
     * <ul>
     *   <li><b>cheese</b> — blobby caverns and rooms,</li>
     *   <li><b>spaghetti</b> — long winding tunnels from the zero-crossing of a noise,</li>
     *   <li><b>worm</b> — thinner, tighter tunnels.</li>
     * </ul>
     * Caves fade out within ~10 blocks below the surface so they never break
     * the terrain skin, and stop above the deepslate floor.
     */
    public double sampleCaveFactor(int blockX, int blockY, int blockZ, double surfaceHeight) {
        if (blockY > surfaceHeight - 9.0 || blockY < -52) return 0.0;

        double fade = (surfaceHeight - 9.0 - blockY) / 10.0;
        if (fade > 1.0) fade = 1.0;

        double c = 0.0;

        double cheese = caveCheeseNoise.sample(blockX * 0.016, blockY * 0.024, blockZ * 0.016);
        if (cheese > 0.40) {
            double t = (cheese - 0.40) / 0.22;
            if (t > 1.0) t = 1.0;
            c = t;
        }

        double spag = Math.abs(caveSpaghettiNoise.sample(blockX * 0.02, blockY * 0.03, blockZ * 0.02));
        if (spag < 0.08) {
            double t = 1.0 - spag / 0.08;
            if (t > c) c = t;
        }

        double worm = Math.abs(caveWormNoise.sample(blockX * 0.03, blockY * 0.045, blockZ * 0.03));
        if (worm < 0.055) {
            double t = (1.0 - worm / 0.055) * 0.85;
            if (t > c) c = t;
        }

        return c * fade;
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
