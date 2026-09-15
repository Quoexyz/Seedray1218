package moe.quoex.seedray1218.mixin.client;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import moe.quoex.seedray1218.client.modules.Xray;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps the world at full brightness while x-ray is on.
 *
 * <p>With every non-ore block hidden, the only thing left on screen is a handful
 * of ores, and the block light around a deep mining tunnel is close to zero. The
 * ores would be rendered as near-black specks. Overwriting the lightmap with
 * solid white is the cheapest way to avoid that, and it is renderer agnostic -
 * Sodium samples the same lightmap.
 *
 * <p>Injected at {@code TAIL} rather than cancelling the method so the vanilla
 * body (and the lightmap UBO it uploads) still runs; we only repaint the texture
 * afterwards.
 */
@Mixin(LightTexture.class)
public abstract class LightTextureMixin {

    @Shadow
    @Final
    private GpuTexture texture;

    @Inject(method = "updateLightTexture", at = @At("TAIL"))
    private void seedray$fullbright(float partialTicks, CallbackInfo ci) {
        if (!Xray.fullbright()) {
            return;
        }

        GpuDevice device = RenderSystem.tryGetDevice();
        if (device == null) {
            return;
        }

        device.createCommandEncoder().clearColorTexture(texture, 0xFFFFFFFF);
    }
}
