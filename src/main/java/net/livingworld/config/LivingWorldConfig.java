package net.livingworld.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class LivingWorldConfig {
    public static final LivingWorldConfig INSTANCE = new LivingWorldConfig();

    public double terrainScale = 1.75;
    public double mountainHeight = 1.6;
    public double climateVariation = 1.1;
    public double biomeSize = 1.35;
    public double riverDepth = 1.0;
    public double plateauSharpness = 0.8;
    public boolean enableLargeMountains = true;
    public boolean enableFloodplains = true;
    public boolean enableClimateBiomes = true;
    public long seedOffset = 0L;

    private Path path;

    private LivingWorldConfig() {
    }

    public void load(Path configDir) {
        this.path = configDir.resolve("the-living-world.properties");
        Properties props = new Properties();
        if (Files.exists(path)) {
            try (Reader r = Files.newBufferedReader(path)) {
                props.load(r);
            } catch (IOException ignored) {
            }
        }
        terrainScale = getDouble(props, "terrainScale", terrainScale);
        mountainHeight = getDouble(props, "mountainHeight", mountainHeight);
        climateVariation = getDouble(props, "climateVariation", climateVariation);
        biomeSize = getDouble(props, "biomeSize", biomeSize);
        riverDepth = getDouble(props, "riverDepth", riverDepth);
        plateauSharpness = getDouble(props, "plateauSharpness", plateauSharpness);
        enableLargeMountains = getBoolean(props, "enableLargeMountains", enableLargeMountains);
        enableFloodplains = getBoolean(props, "enableFloodplains", enableFloodplains);
        enableClimateBiomes = getBoolean(props, "enableClimateBiomes", enableClimateBiomes);
        seedOffset = getLong(props, "seedOffset", seedOffset);
        save();
    }

    public void save() {
        if (path == null) return;
        Properties props = new Properties();
        props.setProperty("terrainScale", Double.toString(terrainScale));
        props.setProperty("mountainHeight", Double.toString(mountainHeight));
        props.setProperty("climateVariation", Double.toString(climateVariation));
        props.setProperty("biomeSize", Double.toString(biomeSize));
        props.setProperty("riverDepth", Double.toString(riverDepth));
        props.setProperty("plateauSharpness", Double.toString(plateauSharpness));
        props.setProperty("enableLargeMountains", Boolean.toString(enableLargeMountains));
        props.setProperty("enableFloodplains", Boolean.toString(enableFloodplains));
        props.setProperty("enableClimateBiomes", Boolean.toString(enableClimateBiomes));
        props.setProperty("seedOffset", Long.toString(seedOffset));
        try {
            Files.createDirectories(path.getParent());
            try (Writer w = Files.newBufferedWriter(path)) {
                props.store(w, "The Living World - terrain generation config");
            }
        } catch (IOException ignored) {
        }
    }

    private static double getDouble(Properties p, String key, double def) {
        String v = p.getProperty(key);
        if (v == null) return def;
        try { return Double.parseDouble(v.trim()); } catch (NumberFormatException e) { return def; }
    }

    private static long getLong(Properties p, String key, long def) {
        String v = p.getProperty(key);
        if (v == null) return def;
        try { return Long.parseLong(v.trim()); } catch (NumberFormatException e) { return def; }
    }

    private static boolean getBoolean(Properties p, String key, boolean def) {
        String v = p.getProperty(key);
        if (v == null) return def;
        return "true".equalsIgnoreCase(v.trim());
    }
}
