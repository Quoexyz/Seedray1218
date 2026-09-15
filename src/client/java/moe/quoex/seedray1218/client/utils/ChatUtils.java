package moe.quoex.seedray1218.client.utils;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class ChatUtils {

    private static final String PREFIX = "[Seedray] ";

    private ChatUtils() {
    }

    public static void info(Component message) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        player.displayClientMessage(Component.literal(PREFIX).append(message), false);
    }

    public static void info(String message) {
        info(Component.literal(message));
    }

    public static void error(String message) {
        info(Component.literal(message).withStyle(ChatFormatting.RED));
    }
}
