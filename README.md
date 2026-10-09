# The Living World — Terrain Generation Overhaul (Fabric / Minecraft 1.20.1)

A Fabric mod for Minecraft 1.20.1 that overhauls Overworld terrain and biome placement to produce larger, geographically consistent landforms with climate-driven biomes. Nether and The End are left untouched.

## Features

### Terrain
- Multi-octave procedural terrain producing oceans, coastal plains, rolling hills, river valleys, floodplains, plateaus, and large mountain ranges.
- Gradual, natural transitions between landforms (continents → hills → plateaus → mountains).
- Large-scale continental noise so landforms are geographically consistent across thousands of blocks.
- Continuous terrain across chunk borders (deterministic per-block sampling).
- Vanilla caves, ravines, ore veins, and underground structures preserved by blending the custom surface density with the vanilla final density (caves carve through the new terrain unchanged).

### Climate & biomes
- Regional temperature, humidity, and rainfall noise sampled at biome scale.
- Temperature decreases with elevation (lapse rate) so high mountains have colder biomes.
- Large, smooth climate regions with gradual edges.
- Biome placement is overridden based on climate + altitude: oceans, beaches, plains, forests, birch forests, dark forests, taiga, snowy biomes, meadows, groves, snowy slopes, peaks (frozen / jagged / stony), windswept hills, savanna / savanna plateau, desert, jungle, swamp, windswept forest, and flower forests.
- Nether and End are unchanged.

### World generation
- Uses Minecraft 1.20.1's `NoiseRouter` density-function pipeline via a mixin that wraps the Overworld `finalDensity`.
- Integrates with the real Fabric / Minecraft world-generation pipeline (new worlds use this terrain).
- Fully deterministic from the world seed.

### Configuration
On first run the mod writes a config file at `config/the-living-world.properties`:

| Key                 | Default | Meaning                                                |
|---------------------|---------|--------------------------------------------------------|
| `terrainScale`      | 1.75    | Horizontal scale of landforms (larger = bigger continents) |
| `mountainHeight`    | 1.6     | Mountain amplitude multiplier                          |
| `climateVariation`  | 1.1     | How strongly climate varies across the map             |
| `biomeSize`         | 1.35    | Horizontal scale of climate/biome regions              |
| `riverDepth`        | 1.0     | How deeply rivers cut into the terrain                 |
| `plateauSharpness`  | 0.8     | Sharpness of plateau transitions                       |
| `enableLargeMountains` | true | Generate large, ridge-based mountain ranges            |
| `enableFloodplains` | true    | Generate low floodplains along rivers                  |
| `enableClimateBiomes` | true  | Override biome placement with the climate system       |
| `seedOffset`        | 0       | Offset added to the world seed for noise sampling      |

## Building

Requirements: Java 17.

```bash
./gradlew build
```

The mod JAR will be produced under `build/libs/`.

## Running a dev client

```bash
./gradlew runClient
```

Create a new world — newly generated chunks will use the overhauled terrain. Existing chunks will not be modified, so travel to new terrain to see the overhaul.

## Project layout

- `src/main/java/net/livingworld/LivingWorld.java` — mod entrypoint.
- `src/main/java/net/livingworld/config/LivingWorldConfig.java` — simple properties-file configuration.
- `src/main/java/net/livingworld/world/gen/TerrainSampler.java` — multi-octave terrain height sampler (continents, mountains, plateaus, hills, ridges, valleys, rivers, floodplains, erosion, detail).
- `src/main/java/net/livingworld/world/climate/ClimateSampler.java` — temperature / humidity / rainfall sampling including elevation temperature fall-off and latitude temperature bias.
- `src/main/java/net/livingworld/world/gen/LivingTerrainDensityFunction.java` — custom `DensityFunction` that blends sampled terrain into the density pipeline while preserving caves/ravines/ores.
- `src/main/java/net/livingworld/world/gen/WorldGenHandler.java` — seeds the terrain sampler with the current world's seed when an Overworld loads.
- `src/main/java/net/livingworld/mixin/ChunkGeneratorSettingsMixin.java` — patches the Overworld `NoiseRouter` so our custom density drives the surface.
- `src/main/java/net/livingworld/mixin/OverworldBiomeSourceMixin.java` — overrides `MultiNoiseBiomeSource.getBiome` for the Overworld to pick biomes from the climate sampler.
- `src/main/java/net/livingworld/world/biome/ClimateBiomeSource.java` — optional registered `BiomeSource` (codec registered for future use; primary biome override is via the mixin for maximum compatibility).

## Notes

- Only new chunks are affected; old chunks from worlds created without the mod retain their original terrain.
- Nether and End generation is intentionally not modified.
- Determinism is guaranteed because all noise is seeded from the world seed plus an optional configured offset.
