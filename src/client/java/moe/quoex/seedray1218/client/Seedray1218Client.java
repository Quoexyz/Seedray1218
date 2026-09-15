package moe.quoex.seedray1218.client;

import com.mojang.blaze3d.platform.InputConstants;
import moe.quoex.seedray1218.client.commands.SeedCommand;
import moe.quoex.seedray1218.client.config.SeedrayConfig;
import moe.quoex.seedray1218.client.modules.OreSim;
import moe.quoex.seedray1218.client.modules.Xray;
import moe.quoex.seedray1218.client.utils.Ore;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.client.player.ClientPlayerBlockBreakEvents;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class Seedray1218Client implements ClientModInitializer {

    public static final String MOD_ID = "seedray1218";
    public static final String KEY_CATEGORY = "Seedray1218";

    private static OreSim oreSim;
    private static Xray xray;
    private static KeyMapping oreSimKey;
    private static KeyMapping xrayKey;

    @Override
    public void onInitializeClient() {
        // Modules register their settings in their constructor, so they have to be
        // built before the config is read - otherwise the values in the file would
        // be applied to settings that do not exist yet and silently dropped.
        touchSettingRegistries();
        oreSim = new OreSim();
        xray = new Xray();

        SeedrayConfig.load();

        oreSimKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.seedray1218.toggle",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                KEY_CATEGORY
        ));
        xrayKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.seedray1218.xray",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_X,
                KEY_CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (oreSimKey.consumeClick()) {
                oreSim.toggle();
            }
            while (xrayKey.consumeClick()) {
                xray.toggle();
            }
            // Backstop for dimension changes; join and chunk load also call this, whichever fires
            // first wins and the rest are no-ops.
            oreSim.syncDimension();
        });

        ClientChunkEvents.CHUNK_LOAD.register((world, chunk) -> oreSim.onChunkLoad(world, chunk));
        ClientPlayerBlockBreakEvents.AFTER.register((world, player, pos, state) -> oreSim.onBlockBroken(pos));

        WorldRenderEvents.AFTER_ENTITIES.register(oreSim::onRender);

        // Deliberately routed through syncDimension rather than a blind reset: on a dimension
        // change JOIN fires again while the level swap may not have landed yet, and the dimension
        // comparison is what makes that harmless.
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> oreSim.syncDimension());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> oreSim.syncDimension());

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> SeedCommand.register(dispatcher));

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> SeedrayConfig.save());
    }

    /**
     * Forces the classes that keep their settings in static fields to initialise,
     * so those settings are registered with {@link SeedrayConfig} before the
     * config file is read.
     */
    private static void touchSettingRegistries() {
        if (Ore.oreSettings.isEmpty()) {
            throw new IllegalStateException("Ore settings failed to register");
        }
    }

    public static OreSim getOreSim() {
        return oreSim;
    }

    public static Xray getXray() {
        return xray;
    }
}
