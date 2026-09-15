package moe.quoex.seedray1218.mixin.sodium;

import moe.quoex.seedray1218.client.modules.Xray;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sodium hook for the x-ray.
 *
 * <p>Sodium replaces the whole chunk renderer and builds its own meshes, so it
 * never consults {@code getRenderShape()} and is completely unaffected by
 * {@code BlockStateBaseMixin}. It calls this method once per block it is about
 * to bake into a chunk mesh, which makes it the exact right place to drop the
 * hidden blocks.
 *
 * <p>The target class is referenced by its string name on purpose. Sodium is an
 * optional dependency here - nothing in this project compiles against it - and
 * {@link moe.quoex.seedray1218.mixin.SeedrayMixinPlugin} switches this whole
 * config off when Sodium is absent. If Sodium ever changes the signature of
 * {@code renderModel}, the config's {@code defaultRequire} of 0 turns the
 * mismatch into a log warning instead of a crash.
 */
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer", remap = false)
public abstract class SodiumBlockRendererMixin {

    @Inject(method = "renderModel", at = @At("HEAD"), cancellable = true)
    private void seedray$xrayHide(BlockStateModel model, BlockState state, BlockPos pos, BlockPos origin, CallbackInfo ci) {
        if (Xray.hides(state)) {
            ci.cancel();
        }
    }
}
