package moe.quoex.seedray1218.client.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;

public final class WorldUtils {

    public enum Dimension {
        Overworld,
        Nether,
        End
    }

    private WorldUtils() {
    }

    public static Dimension getDimension() {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return Dimension.Overworld;
        }
        if (level.dimension() == Level.NETHER) {
            return Dimension.Nether;
        }
        if (level.dimension() == Level.END) {
            return Dimension.End;
        }
        return Dimension.Overworld;
    }

    /** Stable key used to store one seed per world/server. */
    public static String getWorldKey() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null) {
            return "sp:" + mc.getSingleplayerServer().getWorldData().getLevelName();
        }
        var server = mc.getCurrentServer();
        if (server != null) {
            return "mp:" + server.ip;
        }
        return "unknown";
    }
}
