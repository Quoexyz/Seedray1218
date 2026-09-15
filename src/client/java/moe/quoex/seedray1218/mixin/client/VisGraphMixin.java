package moe.quoex.seedray1218.mixin.client;

import moe.quoex.seedray1218.client.modules.Xray;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Turns off the per-section occlusion graph while x-ray is on.
 *
 * <p>{@code SectionCompiler} feeds every opaque block into a {@link VisGraph},
 * which is what lets the game skip rendering whole chunk sections that are
 * hidden behind solid terrain. Because hiding a block here only removes it from
 * the mesh and leaves the block data alone, that graph would still mark sections
 * as occluded and the player would only ever see the ores immediately around
 * them.
 *
 * <p>Swallowing {@code setOpaque} leaves every section fully visible, which is
 * the same trade Meteor makes. It costs draw calls, but it is what makes an
 * x-ray actually usable at range.
 */
@Mixin(VisGraph.class)
public abstract class VisGraphMixin {

    @Inject(method = "setOpaque", at = @At("HEAD"), cancellable = true)
    private void seedray$skipOcclusion(BlockPos pos, CallbackInfo ci) {
        if (Xray.hiding()) {
            ci.cancel();
        }
    }
}
