package net.livingworld.mixin;

import net.livingworld.LivingWorld;
import net.livingworld.world.gen.LivingTerrainDensityFunction;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
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
    private int bedrockFloorY;
    @Shadow @Final
    private int seaLevel;
    @Shadow @Final
    private int minY;
    @Shadow @Final
    private int height;

    @Unique
    private static final AtomicLong livingworld$counter = new AtomicLong(0);

    @Inject(method = "<init>", at = @At("RETURN"))
    private void livingworld$patchNoiseRouter(CallbackInfo ci) {
        if (noiseRouter == null) return;
        if (minY != -64 || height != 384) return;
        if (bedrockFloorY != -64) return;
        if (seaLevel != 63) return;

        DensityFunction originalFinal = noiseRouter.finalDensity();
        long hint = livingworld$counter.getAndIncrement() ^ 0x5deece66dL;
        DensityFunction wrapped = new LivingTerrainDensityFunction(hint, originalFinal);
        this.noiseRouter = new NoiseRouter(
                noiseRouter.barrenNoise(),
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
