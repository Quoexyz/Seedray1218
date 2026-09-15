package moe.quoex.seedray1218.mixin.client;

import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * OreSim builds a {@link WorldGenerationContext} with a {@code null}
 * ChunkGenerator (it only needs the height accessor). Vanilla dereferences the
 * generator in the constructor, so we neutralise those two calls.
 */
@Mixin(WorldGenerationContext.class)
public abstract class HeightContextMixin {

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/ChunkGenerator;getMinY()I"))
    private int seedray$onMinY(ChunkGenerator instance) {
        return instance == null ? -9999999 : instance.getMinY();
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/ChunkGenerator;getGenDepth()I"))
    private int seedray$onHeight(ChunkGenerator instance) {
        return instance == null ? 100000000 : instance.getGenDepth();
    }
}
