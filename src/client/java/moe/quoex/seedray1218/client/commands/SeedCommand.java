package moe.quoex.seedray1218.client.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import moe.quoex.seedray1218.client.Seedray1218Client;
import moe.quoex.seedray1218.client.utils.ChatUtils;
import moe.quoex.seedray1218.client.utils.seeds.Seed;
import moe.quoex.seedray1218.client.utils.seeds.Seeds;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

/**
 * Client side {@code /seedray} command. Replaces Meteor's {@code .seed} command.
 */
public final class SeedCommand {

    private SeedCommand() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("seedray")
                .executes(ctx -> {
                    Seed seed = Seeds.getSeed();
                    if (seed == null) {
                        ChatUtils.error("No seed stored for this world. Use /seedray set <seed>.");
                    } else {
                        info(ctx.getSource(), seed);
                    }
                    return 1;
                })
                .then(ClientCommandManager.literal("toggle")
                        .executes(ctx -> {
                            Seedray1218Client.getOreSim().toggle();
                            return 1;
                        }))
                .then(ClientCommandManager.literal("xray")
                        .executes(ctx -> {
                            Seedray1218Client.getXray().toggle();
                            return 1;
                        }))
                .then(ClientCommandManager.literal("set")
                        .then(ClientCommandManager.argument("seed", StringArgumentType.string())
                                .executes(ctx -> {
                                    Seeds.setSeed(StringArgumentType.getString(ctx, "seed"));
                                    return 1;
                                })))
                .then(ClientCommandManager.literal("delete")
                        .executes(ctx -> {
                            Seeds.deleteSeed();
                            return 1;
                        }))
                .then(ClientCommandManager.literal("list")
                        .executes(ctx -> {
                            if (Seeds.all().isEmpty()) {
                                ChatUtils.error("No seeds stored.");
                            } else {
                                Seeds.all().forEach((world, seed) -> info(ctx.getSource(), world, seed));
                            }
                            return 1;
                        })));
    }

    private static void info(FabricClientCommandSource source, Seed seed) {
        source.sendFeedback(Component.literal("[Seedray] ").append(seed.toText()));
    }

    private static void info(FabricClientCommandSource source, String world, Seed seed) {
        source.sendFeedback(Component.literal("[Seedray] " + world + " ").append(seed.toText()));
    }
}
