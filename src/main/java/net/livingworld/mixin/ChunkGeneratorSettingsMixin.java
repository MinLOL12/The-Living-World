package net.livingworld.mixin;

import net.livingworld.LivingWorld;
import net.livingworld.world.gen.LivingTerrainDensityFunction;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.GenerationShapeConfig;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.noise.NoiseRouter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicLong;

@Mixin(ChunkGeneratorSettings.class)
public class ChunkGeneratorSettingsMixin {
    @Shadow @Final @Mutable
    private NoiseRouter noiseRouter;

    @Shadow @Final
    private GenerationShapeConfig generationShapeConfig;

    @Shadow @Final
    private int seaLevel;

    @Unique
    private static final AtomicLong livingworld$counter = new AtomicLong(0);

    @Inject(method = "<init>", at = @At("RETURN"))
    private void livingworld$patchNoiseRouter(CallbackInfo ci) {
        if (noiseRouter == null || generationShapeConfig == null) return;
        // Only patch overworld-shaped noise settings: full 384-block column
        // starting at y=-64 with sea level 63.
        if (generationShapeConfig.minimumY() != -64 || generationShapeConfig.height() != 384) return;
        if (seaLevel != 63) return;

        DensityFunction originalFinal = noiseRouter.finalDensity();
        long hint = livingworld$counter.getAndIncrement() ^ 0x5deece66dL;
        DensityFunction wrapped = new LivingTerrainDensityFunction(hint, originalFinal);
        this.noiseRouter = new NoiseRouter(
                noiseRouter.barrierNoise(),
                noiseRouter.fluidLevelFloodednessNoise(),
                noiseRouter.fluidLevelSpreadNoise(),
                noiseRouter.lavaNoise(),
                noiseRouter.temperature(),
                noiseRouter.vegetation(),
                noiseRouter.continents(),
                noiseRouter.erosion(),
                noiseRouter.depth(),
                noiseRouter.ridges(),
                noiseRouter.initialDensityWithoutJaggedness(),
                wrapped,
                noiseRouter.veinToggle(),
                noiseRouter.veinRidged(),
                noiseRouter.veinGap()
        );
        LivingWorld.LOGGER.info("[TheLivingWorld] Overworld NoiseRouter patched.");
    }
}
