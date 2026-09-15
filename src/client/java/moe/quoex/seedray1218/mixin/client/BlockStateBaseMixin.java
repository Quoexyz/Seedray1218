package moe.quoex.seedray1218.mixin.client;

import moe.quoex.seedray1218.client.modules.Xray;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Removes non-whitelisted blocks from the terrain build.
 *
 * <p>Both renderers on the vanilla side read {@code getRenderShape()} while
 * compiling a chunk section, so overriding it here is enough:
 * <ul>
 *     <li>Vanilla {@code SectionCompiler} only enters its model branch when the
 *     shape is {@code MODEL}.</li>
 *     <li>Fabric API's Indigo uses a {@code @Redirect} on that very same call
 *     and only claims {@code MODEL} blocks for its own renderer, passing
 *     everything else back through untouched.</li>
 * </ul>
 *
 * <p>Note that the shape is a property of the block, not of its position, so
 * this hides by block type. That is exactly what an x-ray whitelist needs.
 *
 * <p>Side effects are limited to the few other call sites of this method -
 * falling block entities, blocks carried by minecarts and the screen overlay
 * shown when the camera is inside a block. All of them are cosmetic, and none
 * of them touch item rendering, so blocks in the inventory and in hand are
 * unaffected.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {

    @Shadow
    public abstract Block getBlock();

    @Inject(method = "getRenderShape", at = @At("HEAD"), cancellable = true)
    private void seedray$xrayHide(CallbackInfoReturnable<RenderShape> cir) {
        if (Xray.hides(getBlock())) {
            cir.setReturnValue(RenderShape.INVISIBLE);
        }
    }
}
