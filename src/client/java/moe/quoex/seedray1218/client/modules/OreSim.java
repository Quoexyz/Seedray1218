package moe.quoex.seedray1218.client.modules;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import moe.quoex.seedray1218.client.config.EnumSetting;
import moe.quoex.seedray1218.client.config.IntSetting;
import moe.quoex.seedray1218.client.module.SeedrayModule;
import moe.quoex.seedray1218.client.utils.ChatUtils;
import moe.quoex.seedray1218.client.utils.Ore;
import moe.quoex.seedray1218.client.utils.RenderUtils;
import moe.quoex.seedray1218.client.utils.WorldUtils;
import moe.quoex.seedray1218.client.utils.seeds.Seed;
import moe.quoex.seedray1218.client.utils.seeds.Seeds;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Seed based ore predictor, ported from Meteor Rejects' OreSim (which in turn
 * came from Atomic). It re-implements the vanilla ore placement algorithm and
 * therefore does not depend on Meteor or any external seed library.
 */
public class OreSim extends SeedrayModule {

    private final Map<Long, Map<Ore, Set<Vec3>>> chunkRenderers = new ConcurrentHashMap<>();
    private Seed worldSeed = null;
    private Map<ResourceKey<Biome>, List<Ore>> oreConfig;

    /**
     * The dimension {@link #oreConfig} was built for, or null when there is no world. Kept as a
     * field so a dimension change can be detected without relying on event ordering.
     */
    private ResourceKey<Level> syncedDimension = null;

    public enum AirCheck {
        ON_LOAD,
        RECHECK,
        OFF
    }

    private final IntSetting chunkRange = add(new IntSetting("oresim.chunk-range", "Taxi cap distance of chunks being shown.", 5, 1, 10));
    private final EnumSetting<AirCheck> airCheck = add(new EnumSetting<>("oresim.air-check", "Checks if there is air at a calculated ore pos.", AirCheck.RECHECK));

    public OreSim() {
        super("ore-sim", "Xray on crack. Predicts ore positions from the world seed.");
    }

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    @Override
    public boolean onActivate() {
        if (Seeds.getSeed() == null) {
            ChatUtils.error("No seed found. Use /seedray set <seed> first.");
            return false;
        }
        reload();
        // reload() reports its own failures; do not claim to be enabled if it produced nothing.
        return oreConfig != null;
    }

    @Override
    public void onDeactivate() {
        chunkRenderers.clear();
        oreConfig = null;
    }

    /**
     * Keeps the predictor aligned with the dimension the player is actually in.
     *
     * <p>A dimension change makes the server send a fresh login packet, which re-fires
     * {@code ClientPlayConnectionEvents.JOIN}. Fabric injects that event at the return of
     * {@code handleLogin}, though, and nothing guarantees the new level is in place by then. If it
     * is not, {@link #reload()} would build the ore table for the dimension we just left, and
     * nothing would ever correct it - every prediction in the new dimension would be wrong.
     *
     * <p>Comparing against {@link #syncedDimension} instead makes the order irrelevant: whichever
     * of tick / join / chunk load notices the change first performs the reload, and the rest turn
     * into no-ops.
     */
    public void syncDimension() {
        // Read the dimension off mc.level rather than taking it as an argument. reload() resolves
        // it the same way, so this is the only formulation that cannot mark one dimension as
        // synced while building the ore table for another.
        ClientLevel level = mc.level;
        ResourceKey<Level> dimension = level == null ? null : level.dimension();
        if (Objects.equals(dimension, syncedDimension)) {
            return;
        }
        syncedDimension = dimension;
        onWorldChange();
    }

    private void onWorldChange() {
        // Drop everything derived from the old dimension before rebuilding. Chunk keys carry no
        // dimension component, so a surviving entry would be drawn at the same X/Z in the new
        // dimension, with the old ore table, which is exactly the mix-up we are avoiding here.
        chunkRenderers.clear();
        oreConfig = null;
        worldSeed = null;
        if (isActive()) {
            reload();
        }
    }

    private void reload() {
        Seed seed = Seeds.getSeed();
        // The registry is built from the player's world, so there is nothing to do without one.
        // Leaving a world (DISCONNECT) also lands here with a null level.
        if (seed == null || mc.level == null) {
            return;
        }

        worldSeed = seed;
        try {
            oreConfig = Ore.getRegistry(WorldUtils.getDimension());
        } catch (Exception e) {
            // Failing here used to escape into a chunk load callback and surface as a bare
            // exception in chat with no context. Render nothing instead, and say why.
            oreConfig = null;
            ChatUtils.error("Could not build the ore registry: " + e);
            return;
        }

        chunkRenderers.clear();
        loadVisibleChunks();
    }

    private void loadVisibleChunks() {
        if (mc.player == null || mc.level == null) {
            return;
        }
        ChunkPos center = mc.player.chunkPosition();
        int radius = chunkRange.get() + 1;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                ChunkAccess chunk = mc.level.getChunk(center.x + x, center.z + z, ChunkStatus.FULL, false);
                if (chunk != null) {
                    doMathOnChunk(chunk);
                }
            }
        }
    }

    /** Called whenever a chunk finishes loading on the client. */
    public void onChunkLoad(ClientLevel level, ChunkAccess chunk) {
        // Chunk events can still arrive for a level we have already left.
        if (level != mc.level || !isActive()) {
            return;
        }
        // Reconfigure before computing. The ore table has to belong to this chunk's dimension, and
        // deferring that to the next tick would silently lose every chunk that loads in between,
        // because a chunk only gets one CHUNK_LOAD.
        syncDimension();
        doMathOnChunk(chunk);
    }

    /** Called after the local player breaks a block, used by the RECHECK air check. */
    public void onBlockBroken(BlockPos pos) {
        if (!isActive() || airCheck.get() != AirCheck.RECHECK) {
            return;
        }
        Map<Ore, Set<Vec3>> chunk = chunkRenderers.get(ChunkPos.asLong(pos));
        if (chunk == null) {
            return;
        }
        Vec3 vec = Vec3.atLowerCornerOf(pos);
        for (Set<Vec3> ores : chunk.values()) {
            ores.remove(vec);
        }
    }

    // ------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------

    public void onRender(WorldRenderContext context) {
        if (!isActive() || oreConfig == null || mc.player == null) {
            return;
        }
        if (Seeds.getSeed() == null || chunkRenderers.isEmpty()) {
            return;
        }

        PoseStack matrices = context.matrixStack();
        if (matrices == null || context.consumers() == null) {
            return;
        }

        VertexConsumer buffer = context.consumers().getBuffer(RenderType.lines());
        Vec3 camera = context.camera().getPosition();

        int chunkX = mc.player.chunkPosition().x;
        int chunkZ = mc.player.chunkPosition().z;
        int rangeVal = chunkRange.get();

        for (int range = 0; range <= rangeVal; range++) {
            for (int x = -range + chunkX; x <= range + chunkX; x++) {
                renderChunk(x, chunkZ + range - rangeVal, matrices, buffer, camera);
            }
            for (int x = (-range) + 1 + chunkX; x < range + chunkX; x++) {
                renderChunk(x, chunkZ - range + rangeVal + 1, matrices, buffer, camera);
            }
        }
    }

    private void renderChunk(int x, int z, PoseStack matrices, VertexConsumer buffer, Vec3 camera) {
        Map<Ore, Set<Vec3>> chunk = chunkRenderers.get(ChunkPos.asLong(x, z));
        if (chunk == null) {
            return;
        }

        for (Map.Entry<Ore, Set<Vec3>> entry : chunk.entrySet()) {
            Ore ore = entry.getKey();
            if (!ore.active.get()) {
                continue;
            }
            float[] c = ore.color;
            for (Vec3 pos : entry.getValue()) {
                RenderUtils.drawBlockOutline(matrices, buffer, camera, pos.x, pos.y, pos.z, c[0], c[1], c[2], c[3]);
            }
        }
    }

    // ------------------------------------------------------------------
    // Prediction (ported vanilla worldgen logic)
    // ------------------------------------------------------------------

    private void doMathOnChunk(ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        long chunkKey = chunkPos.toLong();

        ClientLevel world = mc.level;
        if (world == null || oreConfig == null || chunkRenderers.containsKey(chunkKey)) {
            return;
        }

        Set<ResourceKey<Biome>> biomes = new HashSet<>();
        ChunkPos.rangeClosed(chunkPos, 1).forEach(pos -> {
            ChunkAccess neighbour = world.getChunk(pos.x, pos.z, ChunkStatus.BIOMES, false);
            if (neighbour == null) {
                return;
            }
            for (LevelChunkSection section : neighbour.getSections()) {
                section.getBiomes().getAll(entry -> biomes.add(entry.unwrapKey().get()));
            }
        });

        Set<Ore> oreSet = biomes.stream().flatMap(b -> getDefaultOres(b).stream()).collect(Collectors.toSet());

        int chunkX = chunkPos.x << 4;
        int chunkZ = chunkPos.z << 4;
        WorldgenRandom random = new WorldgenRandom(WorldgenRandom.Algorithm.XOROSHIRO.newInstance(0));

        long populationSeed = random.setDecorationSeed(worldSeed.seed, chunkX, chunkZ);
        Map<Ore, Set<Vec3>> result = new HashMap<>();

        for (Ore ore : oreSet) {
            HashSet<Vec3> ores = new HashSet<>();

            random.setFeatureSeed(populationSeed, ore.index, ore.step);

            int repeat = ore.count.sample(random);

            for (int i = 0; i < repeat; i++) {
                if (ore.rarity != 1F && random.nextFloat() >= 1 / ore.rarity) {
                    continue;
                }

                int x = random.nextInt(16) + chunkX;
                int z = random.nextInt(16) + chunkZ;
                int y = ore.heightProvider.sample(random, ore.heightContext);
                BlockPos origin = new BlockPos(x, y, z);

                ResourceKey<Biome> biome = chunk.getNoiseBiome(x, y, z).unwrapKey().get();

                if (!getDefaultOres(biome).contains(ore)) {
                    continue;
                }

                if (ore.scattered) {
                    ores.addAll(generateHidden(world, random, origin, ore.size));
                } else {
                    ores.addAll(generateNormal(world, random, origin, ore.size, ore.discardOnAirChance));
                }
            }

            if (!ores.isEmpty()) {
                result.put(ore, ores);
            }
        }

        chunkRenderers.put(chunkKey, result);
    }

    private List<Ore> getDefaultOres(ResourceKey<Biome> biomeRegistryKey) {
        if (oreConfig.containsKey(biomeRegistryKey)) {
            return oreConfig.get(biomeRegistryKey);
        }
        return oreConfig.values().stream().findAny().orElse(List.of());
    }

    private ArrayList<Vec3> generateNormal(ClientLevel world, WorldgenRandom random, BlockPos blockPos, int veinSize, float discardOnAir) {
        float f = random.nextFloat() * 3.1415927F;
        float g = (float) veinSize / 8.0F;
        int i = Mth.ceil(((float) veinSize / 16.0F * 2.0F + 1.0F) / 2.0F);
        double d = (double) blockPos.getX() + Math.sin(f) * (double) g;
        double e = (double) blockPos.getX() - Math.sin(f) * (double) g;
        double h = (double) blockPos.getZ() + Math.cos(f) * (double) g;
        double j = (double) blockPos.getZ() - Math.cos(f) * (double) g;
        double l = (blockPos.getY() + random.nextInt(3) - 2);
        double m = (blockPos.getY() + random.nextInt(3) - 2);
        int n = blockPos.getX() - Mth.ceil(g) - i;
        int o = blockPos.getY() - 2 - i;
        int p = blockPos.getZ() - Mth.ceil(g) - i;
        int q = 2 * (Mth.ceil(g) + i);
        int r = 2 * (2 + i);

        for (int s = n; s <= n + q; ++s) {
            for (int t = p; t <= p + q; ++t) {
                if (o <= world.getHeight(Heightmap.Types.MOTION_BLOCKING, s, t)) {
                    return generateVeinPart(world, random, veinSize, d, e, h, j, l, m, n, o, p, q, r, discardOnAir);
                }
            }
        }

        return new ArrayList<>();
    }

    private ArrayList<Vec3> generateVeinPart(ClientLevel world, WorldgenRandom random, int veinSize, double startX, double endX, double startZ, double endZ, double startY, double endY, int x, int y, int z, int size, int i, float discardOnAir) {
        BitSet bitSet = new BitSet(size * i * size);
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        double[] ds = new double[veinSize * 4];

        ArrayList<Vec3> poses = new ArrayList<>();

        int n;
        double p;
        double q;
        double r;
        double s;
        for (n = 0; n < veinSize; ++n) {
            float f = (float) n / (float) veinSize;
            p = Mth.lerp(f, startX, endX);
            q = Mth.lerp(f, startY, endY);
            r = Mth.lerp(f, startZ, endZ);
            s = random.nextDouble() * (double) veinSize / 16.0D;
            double m = ((double) (Mth.sin(3.1415927F * f) + 1.0F) * s + 1.0D) / 2.0D;
            ds[n * 4] = p;
            ds[n * 4 + 1] = q;
            ds[n * 4 + 2] = r;
            ds[n * 4 + 3] = m;
        }

        for (n = 0; n < veinSize - 1; ++n) {
            if (!(ds[n * 4 + 3] <= 0.0D)) {
                for (int o = n + 1; o < veinSize; ++o) {
                    if (!(ds[o * 4 + 3] <= 0.0D)) {
                        p = ds[n * 4] - ds[o * 4];
                        q = ds[n * 4 + 1] - ds[o * 4 + 1];
                        r = ds[n * 4 + 2] - ds[o * 4 + 2];
                        s = ds[n * 4 + 3] - ds[o * 4 + 3];
                        if (s * s > p * p + q * q + r * r) {
                            if (s > 0.0D) {
                                ds[o * 4 + 3] = -1.0D;
                            } else {
                                ds[n * 4 + 3] = -1.0D;
                            }
                        }
                    }
                }
            }
        }

        for (n = 0; n < veinSize; ++n) {
            double u = ds[n * 4 + 3];
            if (!(u < 0.0D)) {
                double v = ds[n * 4];
                double w = ds[n * 4 + 1];
                double aa = ds[n * 4 + 2];
                int ab = Math.max(Mth.floor(v - u), x);
                int ac = Math.max(Mth.floor(w - u), y);
                int ad = Math.max(Mth.floor(aa - u), z);
                int ae = Math.max(Mth.floor(v + u), ab);
                int af = Math.max(Mth.floor(w + u), ac);
                int ag = Math.max(Mth.floor(aa + u), ad);

                for (int ah = ab; ah <= ae; ++ah) {
                    double ai = ((double) ah + 0.5D - v) / u;
                    if (ai * ai < 1.0D) {
                        for (int aj = ac; aj <= af; ++aj) {
                            double ak = ((double) aj + 0.5D - w) / u;
                            if (ai * ai + ak * ak < 1.0D) {
                                for (int al = ad; al <= ag; ++al) {
                                    double am = ((double) al + 0.5D - aa) / u;
                                    if (ai * ai + ak * ak + am * am < 1.0D) {
                                        int an = ah - x + (aj - y) * size + (al - z) * size * i;
                                        if (!bitSet.get(an)) {
                                            bitSet.set(an);
                                            mutable.set(ah, aj, al);
                                            if (aj >= -64 && aj < 320 && (airCheck.get() == AirCheck.OFF || world.getBlockState(mutable).canOcclude())) {
                                                if (shouldPlace(world, mutable, discardOnAir, random)) {
                                                    poses.add(new Vec3(ah, aj, al));
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return poses;
    }

    private boolean shouldPlace(ClientLevel world, BlockPos orePos, float discardOnAir, WorldgenRandom random) {
        if (discardOnAir == 0F || (discardOnAir != 1F && random.nextFloat() >= discardOnAir)) {
            return true;
        }

        for (Direction direction : Direction.values()) {
            if (!world.getBlockState(orePos.offset(direction.getUnitVec3i())).canOcclude() && discardOnAir != 1F) {
                return false;
            }
        }
        return true;
    }

    private ArrayList<Vec3> generateHidden(ClientLevel world, WorldgenRandom random, BlockPos blockPos, int size) {
        ArrayList<Vec3> poses = new ArrayList<>();

        int i = random.nextInt(size + 1);

        for (int j = 0; j < i; ++j) {
            size = Math.min(j, 7);
            int x = randomCoord(random, size) + blockPos.getX();
            int y = randomCoord(random, size) + blockPos.getY();
            int z = randomCoord(random, size) + blockPos.getZ();
            BlockPos pos = new BlockPos(x, y, z);
            if (airCheck.get() == AirCheck.OFF || world.getBlockState(pos).canOcclude()) {
                if (shouldPlace(world, pos, 1F, random)) {
                    poses.add(new Vec3(x, y, z));
                }
            }
        }

        return poses;
    }

    private int randomCoord(WorldgenRandom random, int size) {
        return Math.round((random.nextFloat() - random.nextFloat()) * (float) size);
    }
}
