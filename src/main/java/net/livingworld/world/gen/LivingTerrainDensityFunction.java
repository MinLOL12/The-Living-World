package net.livingworld.world.gen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.livingworld.config.LivingWorldConfig;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.util.math.random.RandomSplitter;
import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandom;
import net.minecraft.world.gen.densityfunction.DensityFunction;

/**
 * Drives the Overworld surface from the height field produced by
 * {@link TerrainSampler}.
 *
 * <p>Unlike a naive "blend near the surface" approach (which produces floating
 * shelves and a vanilla stone floor underneath, because the vanilla density
 * re-appears below the blend window), this function owns the whole column:
 * everything below the sampled height is solid, everything above is air, with
 * a linear density gradient across the surface so surface rules, heightmaps
 * and aquifers behave exactly like vanilla.
 *
 * <p>Caves are carved by subtracting a dedicated 3D cave field
 * ({@link TerrainSampler#sampleCaveFactor}) below the surface, so caverns,
 * spaghetti tunnels and worm tunnels appear in the new terrain without
 * depending on vanilla's surface-relative cave density. Ore veins, aquifers
 * and structures keep working because they only consume the final density.
 */
public final class LivingTerrainDensityFunction implements DensityFunction {
    public static final Codec<LivingTerrainDensityFunction> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("seed_hint").forGetter(function -> function.seedHint),
            DensityFunction.FUNCTION_CODEC.fieldOf("original").forGetter(function -> function.original)
    ).apply(instance, LivingTerrainDensityFunction::new));

    public static final CodecHolder<LivingTerrainDensityFunction> CODEC_HOLDER = CodecHolder.of(CODEC);

    /** Density units per block of depth below the sampled surface height. */
    private static final double HEIGHT_GRADIENT = 0.05;
    private static final double BASE_CLAMP = 2.5;
    private static final double CAVE_STRENGTH = 2.2;

    private static volatile long CURRENT_WORLD_SEED = 0L;
    private static volatile TerrainSampler CURRENT_SAMPLER;

    /** Per-thread column cache: {packed x/z key, seed, height bits}. */
    private static final ThreadLocal<long[]> COLUMN_CACHE = ThreadLocal.withInitial(() -> new long[3]);

    private final long seedHint;
    private final DensityFunction original;

    public LivingTerrainDensityFunction(long seedHint, DensityFunction original) {
        this.seedHint = seedHint;
        this.original = original;
    }

    public static void setWorldSeed(long seed) {
        if (CURRENT_SAMPLER != null && CURRENT_WORLD_SEED == seed) return;
        LivingWorldConfig cfg = LivingWorldConfig.INSTANCE;
        Xoroshiro128PlusPlusRandom r = new Xoroshiro128PlusPlusRandom(seed + cfg.seedOffset);
        RandomSplitter sp = r.nextSplitter();
        CURRENT_WORLD_SEED = seed;
        CURRENT_SAMPLER = new TerrainSampler(sp, cfg);
    }

    public static TerrainSampler sampler() {
        if (CURRENT_SAMPLER == null) {
            setWorldSeed(0L);
        }
        return CURRENT_SAMPLER;
    }

    @Override
    public double sample(DensityFunction.NoisePos pos) {
        int bx = pos.blockX();
        int by = pos.blockY();
        int bz = pos.blockZ();

        TerrainSampler s = sampler();
        double h = columnHeight(s, bx, bz);

        double base = (h - by) * HEIGHT_GRADIENT;
        if (base > BASE_CLAMP) {
            base = BASE_CLAMP;
        } else if (base < -BASE_CLAMP) {
            base = -BASE_CLAMP;
        }

        double cave = s.sampleCaveFactor(bx, by, bz, h);
        double density = base - cave * CAVE_STRENGTH;

        if (density > 3.0) return 3.0;
        if (density < -3.0) return -3.0;
        return density;
    }

    /**
     * Samples (and memoises per thread) the terrain height of a column.
     * Chunk generation calls {@link #sample} for every Y of a column before
     * moving to the next column, so a one-entry cache removes ~95% of the
     * height-field evaluations.
     */
    private static double columnHeight(TerrainSampler sampler, int bx, int bz) {
        long[] cache = COLUMN_CACHE.get();
        long key = ((long) bx << 32) ^ (bz & 0xFFFFFFFFL);
        long seed = CURRENT_WORLD_SEED;
        if (cache[0] != key || cache[2] != seed) {
            cache[0] = key;
            cache[2] = seed;
            cache[1] = Double.doubleToRawLongBits(sampler.sampleHeight(bx, bz));
        }
        return Double.longBitsToDouble(cache[1]);
    }

    @Override
    public void fill(double[] densities, DensityFunction.EachApplier applier) {
        applier.fill(densities, this);
    }

    @Override
    public DensityFunction apply(DensityFunction.DensityFunctionVisitor visitor) {
        return visitor.apply(new LivingTerrainDensityFunction(this.seedHint, this.original.apply(visitor)));
    }

    @Override
    public double minValue() {
        return -3.0;
    }

    @Override
    public double maxValue() {
        return 3.0;
    }

    @Override
    public CodecHolder<? extends DensityFunction> getCodecHolder() {
        return CODEC_HOLDER;
    }
}
