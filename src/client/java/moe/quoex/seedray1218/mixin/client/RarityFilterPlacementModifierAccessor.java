package moe.quoex.seedray1218.mixin.client;

import net.minecraft.world.level.levelgen.placement.RarityFilter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RarityFilter.class)
public interface RarityFilterPlacementModifierAccessor {
    @Accessor
    int getChance();
}
