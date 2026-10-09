package net.livingworld.world.biome;

import com.mojang.serialization.Codec;
import net.livingworld.LivingWorld;
import net.livingworld.config.LivingWorldConfig;
import net.livingworld.world.climate.ClimateSampler;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.RandomSplitter;
import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandom;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.gen.noise.NoiseConfig;

import java.util.ArrayList;
import java.util.List;

public final class ClimateBiomeSource extends BiomeSource {
    public static final Codec<ClimateBiomeSource> CODEC = Codec.LONG.fieldOf("seed").xmap(ClimateBiomeSource::new, s -> s.seed).codec();

    public static final Factory TYPE = new Factory() {
        @Override public Codec<? extends BiomeSource> codec() { return CODEC; }
        @Override public BiomeSource withSeed(long seed) { return new ClimateBiomeSource(seed); }
    };

    private final long seed;
    private transient ClimateSampler climate;

    private ClimateBiomeSource(long seed) {
        super(new ArrayList<>());
        this.seed = seed;
    }

    @Override
    protected Codec<? extends BiomeSource> getCodec() { return CODEC; }

    @Override
    public BiomeSource withSeed(long seed) { return new ClimateBiomeSource(seed); }

    @Override
    public void setNoiseConfig(NoiseConfig noiseConfig) {
        super.setNoiseConfig(noiseConfig);
        LivingWorldConfig cfg = LivingWorldConfig.INSTANCE;
        Xoroshiro128PlusPlusRandom r = new Xoroshiro128PlusPlusRandom(noiseConfig.getLegacyWorldSeed() ^ 0x9e3779b97f4a7c15L);
        RandomSplitter sp = r.nextSplitter();
        this.climate = new ClimateSampler(sp.split("lw_climate"), cfg);
    }

    @Override
    public RegistryEntry<Biome> getBiome(int biomeX, int biomeY, int biomeZ, NoiseConfig noiseConfig) {
        return RegistryEntry.of(BiomeKeys.PLAINS);
    }

    @Override
    public void addDebugInfo(List<String> info, BlockPos pos, MultiNoiseUtil.MultiNoiseSampler noiseSampler) {
        info.add("The Living World climate biome source (registered)");
    }

    public static void register() {
        Registry.register(Registry.BIOME_SOURCE, new Identifier(LivingWorld.MOD_ID, "climate"), TYPE);
    }
}
