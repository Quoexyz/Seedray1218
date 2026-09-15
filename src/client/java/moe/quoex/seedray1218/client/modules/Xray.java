package moe.quoex.seedray1218.client.modules;

import moe.quoex.seedray1218.client.config.BoolSetting;
import moe.quoex.seedray1218.client.module.SeedrayModule;
import moe.quoex.seedray1218.client.utils.ChatUtils;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The actual x-ray module: every block that is not in {@link #WHITELIST} stops
 * being rendered, so the terrain disappears and the ores are left floating in
 * the air.
 *
 * <p>This class only holds the state and the whitelist. The filtering itself
 * lives in three mixins, which all read the static flags exposed here:
 * <ul>
 *     <li>{@code BlockStateBaseMixin} - vanilla and Fabric API's Indigo
 *     terrain renderer, both of which dispatch on {@code getRenderShape()}.</li>
 *     <li>{@code SodiumBlockRendererMixin} - Sodium, which builds chunk meshes
 *     itself and ignores {@code getRenderShape()}.</li>
 *     <li>{@code VisGraphMixin} - stops the block occlusion graph from hiding
 *     chunks, otherwise only the sections right next to the player would
 *     render.</li>
 * </ul>
 * A fourth mixin, {@code LightTextureMixin}, keeps the lightmap at full
 * brightness so ores stay readable underground.
 */
public class Xray extends SeedrayModule {

    private static final List<Block> ORES = List.of(
            Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE,
            Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
            Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE,
            Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.NETHER_GOLD_ORE,
            Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE,
            Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
            Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
            Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
            Blocks.NETHER_QUARTZ_ORE,
            Blocks.ANCIENT_DEBRIS
    );

    private static final Set<Block> WHITELIST = new HashSet<>(ORES);

    /** Read on the render thread for every block of every rebuilt section, so kept as a plain flag. */
    private static volatile boolean hiding = false;
    private static volatile boolean bright = true;

    private final BoolSetting fullbright = add(new BoolSetting(
            "xray.fullbright",
            "Force full brightness while x-ray is on, so ores stay visible underground.",
            true
    ));

    public Xray() {
        super("xray", "Hides every block that is not an ore. Real x-ray, not a seed predictor.");
    }

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    @Override
    public boolean onActivate() {
        bright = fullbright.get();
        hiding = true;
        rebuildChunks();
        ChatUtils.info("Xray enabled (fullbright: " + (bright ? "on" : "off") + ").");
        return true;
    }

    @Override
    public void onDeactivate() {
        hiding = false;
        rebuildChunks();
        ChatUtils.info("Xray disabled.");
    }

    /** Chunk meshes are baked, so they have to be thrown away for the change to show up. */
    private void rebuildChunks() {
        if (mc.level != null) {
            mc.levelRenderer.allChanged();
        }
    }

    // ------------------------------------------------------------------
    // State read by the mixins
    // ------------------------------------------------------------------

    public static boolean hiding() {
        return hiding;
    }

    public static boolean fullbright() {
        return hiding && bright;
    }

    /** @return true if this block must not be rendered. */
    public static boolean hides(Block block) {
        return hiding && !WHITELIST.contains(block);
    }

    /** @return true if this block must not be rendered. */
    public static boolean hides(BlockState state) {
        return hides(state.getBlock());
    }
}
