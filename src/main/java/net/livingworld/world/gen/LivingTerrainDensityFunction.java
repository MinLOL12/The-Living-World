package net.livingworld.world.gen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.livingworld.config.LivingWorldConfig;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.util.math.random.RandomSplitter;
import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandom;
import net.minecraft.world.gen.densityfunction.DensityFunction;

/**
 * Wraps the vanilla overworld {@code finalDensity} and blends it toward the
 * height field produced by {@link TerrainSampler}. The vanilla function is
 * still sampled, so caves, ravines, aquifers and ore veins keep carving the
 * new terrain exactly like vanilla carves the old one.
 */
public final class LivingTerrainDensityFunction implements DensityFunction {
    public static final Codec<LivingTerrainDensityFunction> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("seed_hint").forGetter(function -> function.seedHint),
            DensityFunction.FUNCTION_CODEC.fieldOf("original").forGetter(function -> function.original)
    ).apply(instance, LivingTerrainDensityFunction::new));

    public static final CodecHolder<LivingTerrainDensityFunction> CODEC_HOLDER = CodecHolder.of(CODEC);

    private static volatile long CURRENT_WORLD_SEED = 0L;
    private static volatile TerrainSampler CURRENT_SAMPLER;

    private final long seedHint;
    private final DensityFunction original;
    private transient int lastX = Integer.MIN_VALUE;
    private transient int lastZ = Integer.MIN_VALUE;
    private transient double lastH;

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
        double h;
        if (bx == lastX && bz == lastZ) {
            h = lastH;
        } else {
            h = s.sampleHeight(bx, bz);
            lastX = bx;
            lastZ = bz;
            lastH = h;
        }

        double originalDensity = original.sample(pos);

        double desired = (h - by) / 80.0;
        if (by < -48) desired = Math.max(desired, 1.0);
        if (by > 300) desired = Math.min(desired, -1.0);

        double dist = Math.abs(by - h);
        double blend;
        if (dist < 8) {
            blend = 0.85;
        } else if (dist < 24) {
            blend = 0.85 * (1.0 - (dist - 8.0) / 16.0);
        } else if (by > h + 50 || by < h - 20) {
            blend = 0.0;
        } else {
            blend = 0.15;
        }

        if (by < 0) blend *= 0.4;

        if (originalDensity < -0.3 && by < h - 5 && by > -10) {
            blend *= 0.15;
        }

        double r = originalDensity * (1.0 - blend) + desired * blend;
        return Math.max(-3.0, Math.min(3.0, r));
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
