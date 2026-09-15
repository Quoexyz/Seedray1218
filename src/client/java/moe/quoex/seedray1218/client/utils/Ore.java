package moe.quoex.seedray1218.client.utils;

import moe.quoex.seedray1218.client.config.BoolSetting;
import moe.quoex.seedray1218.client.config.SeedrayConfig;
import moe.quoex.seedray1218.mixin.client.CountPlacementModifierAccessor;
import moe.quoex.seedray1218.mixin.client.HeightRangePlacementModifierAccessor;
import moe.quoex.seedray1218.mixin.client.RarityFilterPlacementModifierAccessor;
import moe.quoex.seedray1218.client.utils.WorldUtils.Dimension;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.data.worldgen.placement.OrePlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.FeatureSorter;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.feature.ScatteredOreFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RarityFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ported from Meteor Rejects' Ore registry (originally from Atomic).
 * Builds a biome -&gt; ore list map straight from the vanilla registry, so the
 * predictor uses exactly the same placement data as the real world generator.
 */
public class Ore {

    private static final BoolSetting coal = SeedrayConfig.register(new BoolSetting("ores.coal", "Show coal.", true));
    private static final BoolSetting iron = SeedrayConfig.register(new BoolSetting("ores.iron", "Show iron.", true));
    private static final BoolSetting gold = SeedrayConfig.register(new BoolSetting("ores.gold", "Show gold.", true));
    private static final BoolSetting redstone = SeedrayConfig.register(new BoolSetting("ores.redstone", "Show redstone.", true));
    private static final BoolSetting diamond = SeedrayConfig.register(new BoolSetting("ores.diamond", "Show diamond.", true));
    private static final BoolSetting lapis = SeedrayConfig.register(new BoolSetting("ores.lapis", "Show lapis.", true));
    private static final BoolSetting copper = SeedrayConfig.register(new BoolSetting("ores.copper", "Show copper.", true));
    private static final BoolSetting emerald = SeedrayConfig.register(new BoolSetting("ores.emerald", "Show emerald.", true));
    private static final BoolSetting quartz = SeedrayConfig.register(new BoolSetting("ores.quartz", "Show quartz.", true));
    private static final BoolSetting debris = SeedrayConfig.register(new BoolSetting("ores.debris", "Show ancient debris.", true));

    public static final List<BoolSetting> oreSettings = new ArrayList<>(Arrays.asList(
            coal, iron, gold, redstone, diamond, lapis, copper, emerald, quartz, debris
    ));

    public static Map<ResourceKey<Biome>, List<Ore>> getRegistry(Dimension dimension) {
        HolderLookup.Provider registry = VanillaRegistries.createLookup();
        HolderLookup.RegistryLookup<PlacedFeature> features = registry.lookupOrThrow(Registries.PLACED_FEATURE);

        // Biomes are enumerated through the per-dimension biome tags instead of through
        // WorldPresets.NORMAL. A WorldPreset hands back the very LevelStem instances it was
        // bootstrapped with, and the biome source inside those carries Holder.References that
        // belong to a different registry instance. Reading them off a freshly built lookup fails
        // with "Trying to access unbound value '... minecraft:end_highlands'". The tag route
        // always yields holders owned by the lookup we are about to read features from.
        TagKey<Biome> dimensionTag = switch (dimension) {
            case Overworld -> BiomeTags.IS_OVERWORLD;
            case Nether -> BiomeTags.IS_NETHER;
            case End -> BiomeTags.IS_END;
        };

        List<Holder<Biome>> biomes1 = registry.lookupOrThrow(Registries.BIOME)
                .getOrThrow(dimensionTag)
                .stream()
                .toList();

        List<FeatureSorter.StepFeatureData> indexer = FeatureSorter.buildFeaturesPerStep(
                biomes1, biomeEntry -> biomeEntry.value().getGenerationSettings().features(), true
        );

        Map<PlacedFeature, Ore> featureToOre = new HashMap<>();
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_COAL_LOWER, 6, coal, 47, 44, 54);
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_COAL_UPPER, 6, coal, 47, 44, 54);
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_IRON_MIDDLE, 6, iron, 236, 173, 119);
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_IRON_SMALL, 6, iron, 236, 173, 119);
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_IRON_UPPER, 6, iron, 236, 173, 119);
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_GOLD, 6, gold, 247, 229, 30);
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_GOLD_LOWER, 6, gold, 247, 229, 30);
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_GOLD_EXTRA, 6, gold, 247, 229, 30);
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_GOLD_NETHER, 7, gold, 247, 229, 30);
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_GOLD_DELTAS, 7, gold, 247, 229, 30);
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_REDSTONE, 6, redstone, 245, 7, 23);
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_REDSTONE_LOWER, 6, redstone, 245, 7, 23);
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_DIAMOND, 6, diamond, 33, 244, 255);
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_DIAMOND_BURIED, 6, diamond, 33, 244, 255);
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_DIAMOND_LARGE, 6, diamond, 33, 244, 255);
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_DIAMOND_MEDIUM, 6, diamond, 33, 244, 255);
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_LAPIS, 6, lapis, 8, 26, 189);
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_LAPIS_BURIED, 6, lapis, 8, 26, 189);
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_COPPER, 6, copper, 239, 151, 0);
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_COPPER_LARGE, 6, copper, 239, 151, 0);
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_EMERALD, 6, emerald, 27, 209, 45);
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_QUARTZ_NETHER, 7, quartz, 205, 205, 205);
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_QUARTZ_DELTAS, 7, quartz, 205, 205, 205);
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_ANCIENT_DEBRIS_SMALL, 7, debris, 209, 27, 245);
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_ANCIENT_DEBRIS_LARGE, 7, debris, 209, 27, 245);

        Map<ResourceKey<Biome>, List<Ore>> biomeOreMap = new HashMap<>();

        biomes1.forEach(biome -> {
            biomeOreMap.put(biome.unwrapKey().get(), new ArrayList<>());
            biome.value().getGenerationSettings().features().stream()
                    .flatMap(HolderSet::stream)
                    .map(Holder::value)
                    .filter(featureToOre::containsKey)
                    .forEach(feature -> biomeOreMap.get(biome.unwrapKey().get()).add(featureToOre.get(feature)));
        });
        return biomeOreMap;
    }

    private static void registerOre(
            Map<PlacedFeature, Ore> map,
            List<FeatureSorter.StepFeatureData> indexer,
            HolderLookup.RegistryLookup<PlacedFeature> oreRegistry,
            ResourceKey<PlacedFeature> oreKey,
            int genStep,
            BoolSetting active,
            int red, int green, int blue
    ) {
        var orePlacement = oreRegistry.getOrThrow(oreKey).value();

        int index = indexer.get(genStep).indexMapping().applyAsInt(orePlacement);

        Ore ore = new Ore(orePlacement, genStep, index, active, new float[]{red / 255F, green / 255F, blue / 255F, 1F});

        map.put(orePlacement, ore);
    }

    public int step;
    public int index;
    public BoolSetting active;
    public IntProvider count = ConstantInt.of(1);
    public HeightProvider heightProvider;
    public WorldGenerationContext heightContext;
    public float rarity = 1;
    public float discardOnAirChance;
    public int size;
    public float[] color;
    public boolean scattered;

    private Ore(PlacedFeature feature, int step, int index, BoolSetting active, float[] color) {
        this.step = step;
        this.index = index;
        this.active = active;
        this.color = color;

        int bottom = Minecraft.getInstance().level.getMinY();
        int height = Minecraft.getInstance().level.dimensionType().logicalHeight();
        this.heightContext = new WorldGenerationContext(null, LevelHeightAccessor.create(bottom, height));

        for (PlacementModifier modifier : feature.placement()) {
            if (modifier instanceof CountPlacement) {
                this.count = ((CountPlacementModifierAccessor) modifier).getCount();
            } else if (modifier instanceof HeightRangePlacement) {
                this.heightProvider = ((HeightRangePlacementModifierAccessor) modifier).getHeight();
            } else if (modifier instanceof RarityFilter) {
                this.rarity = ((RarityFilterPlacementModifierAccessor) modifier).getChance();
            }
        }

        FeatureConfiguration featureConfig = feature.feature().value().config();

        if (featureConfig instanceof OreConfiguration oreFeatureConfig) {
            this.discardOnAirChance = oreFeatureConfig.discardChanceOnAirExposure;
            this.size = oreFeatureConfig.size;
        } else {
            throw new IllegalStateException("config for " + feature + " is not an OreConfiguration");
        }

        if (feature.feature().value().feature() instanceof ScatteredOreFeature) {
            this.scattered = true;
        }
    }
}
