package moe.quoex.seedray1218.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Gatekeeper for the mixin configs that target optional mods.
 *
 * <p>Right now that is only {@code Seedray1218.sodium.mixins.json}. The Sodium
 * hooks address Sodium's classes by name and the project does not depend on
 * Sodium, so without this plugin the mixin environment would look for a target
 * that is not installed. Returning {@code false} here makes Mixin skip the
 * whole config instead.
 */
public class SeedrayMixinPlugin implements IMixinConfigPlugin {

    private boolean sodiumPresent;

    @Override
    public void onLoad(String mixinPackage) {
        sodiumPresent = FabricLoader.getInstance().isModLoaded("sodium");
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return sodiumPresent;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
