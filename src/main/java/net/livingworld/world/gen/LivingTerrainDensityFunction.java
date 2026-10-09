package net.livingworld.world.gen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.livingworld.LivingWorld;
import net.livingworld.config.LivingWorldConfig;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.RandomSplitter;
import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandom;
import net.minecraft.world.gen.densityfunction.DensityFunction;

public final class LivingTerrainDensityFunction implements DensityFunction.Base {
    public static final Codec<LivingTerrainDensityFunction> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.fieldOf("seed_hint").forGetter(d -> d.seedHint),
            DensityFunction.FUNCTION_CODEC.fieldOf("original").forGetter(d -> d.original)
    ).apply(i, LivingTerrainDensityFunction::new));

    public static final DensityFunction.DensityFunctionType TYPE = () -> CODEC;

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

    public static void register() {
        Registry.register(Registry.DENSITY_FUNCTION_TYPE, new Identifier(LivingWorld.MOD_ID, "terrain"), TYPE);
    }

    public static void setWorldSeed(long seed) {
        if (CURRENT_SAMPLER != null && CURRENT_WORLD_SEED == seed) return;
        LivingWorldConfig cfg = LivingWorldConfig.INSTANCE;
        Xoroshiro128PlusPlusRandom r = new Xoroshiro128PlusPlusRandom(seed + cfg.seedOffset);
        RandomSplitter sp = r.nextSplitter();
        CURRENT_WORLD_SEED = seed;
        CURRENT_SAMPLER = new TerrainSampler(sp.split("lw_terrain"), cfg);
    }

    public static TerrainSampler sampler() {
        if (CURRENT_SAMPLER == null) {
            setWorldSeed(0L);
        }
        return CURRENT_SAMPLER;
    }

    @Override
    public double sample(DensityFunction.DensityFunctionContext ctx) {
        int bx = ctx.blockX();
        int by = ctx.blockY();
        int bz = ctx.blockZ();
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

        double originalDensity = original.sample(ctx);

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
    public void fill(double[] ds, DensityFunction.EachApplier applier) {
        applier.fill(ds, this);
    }

    @Override
    public double minValue() { return -3.0; }
    @Override
    public double maxValue() { return 3.0; }

    @Override
    public DensityFunction.DensityFunctionType type() {
        return TYPE;
    }
}
