package net.livingworld.world.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.livingworld.LivingWorld;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;

import java.util.List;

/**
 * A minimal registered biome source kept for datapack use. The primary
 * climate-driven biome placement is done by the overworld
 * {@code MultiNoiseBiomeSource} mixin for maximum compatibility.
 */
public final class ClimateBiomeSource extends BiomeSource {
    public static final Codec<ClimateBiomeSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RegistryOps.getEntryLookupCodec(RegistryKeys.BIOME),
            Codec.LONG.fieldOf("seed").forGetter(source -> source.seed)
    ).apply(instance, (biomeLookup, seed) ->
            new ClimateBiomeSource(biomeLookup.getOrThrow(BiomeKeys.PLAINS), seed)));

    private final RegistryEntry<Biome> plainsEntry;
    private final long seed;

    private ClimateBiomeSource(RegistryEntry<Biome> plainsEntry, long seed) {
        super();
        this.plainsEntry = plainsEntry;
        this.seed = seed;
    }

    @Override
    protected Codec<? extends BiomeSource> getCodec() {
        return CODEC;
    }

    @Override
    public RegistryEntry<Biome> getBiome(int biomeX, int biomeY, int biomeZ, MultiNoiseUtil.MultiNoiseSampler noise) {
        return plainsEntry;
    }

    @Override
    public void addDebugInfo(List<String> info, BlockPos pos, MultiNoiseUtil.MultiNoiseSampler noiseSampler) {
        info.add("The Living World climate biome source (registered)");
    }

    public static void register() {
        Registry.register(Registries.BIOME_SOURCE, new Identifier(LivingWorld.MOD_ID, "climate"), CODEC);
    }
}
