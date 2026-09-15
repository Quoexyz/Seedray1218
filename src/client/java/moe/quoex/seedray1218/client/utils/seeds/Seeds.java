package moe.quoex.seedray1218.client.utils.seeds;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import moe.quoex.seedray1218.client.config.SeedrayConfig;
import moe.quoex.seedray1218.client.utils.ChatUtils;
import moe.quoex.seedray1218.client.utils.WorldUtils;
import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores one seed per world/server. In singleplayer the seed is read straight
 * from the integrated server, in multiplayer it has to be set manually.
 */
public final class Seeds {

    private static final Map<String, Seed> SEEDS = new HashMap<>();

    private Seeds() {
    }

    public static Seed getSeed() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null) {
            return new Seed(mc.getSingleplayerServer().overworld().getSeed(), "singleplayer");
        }
        return SEEDS.get(WorldUtils.getWorldKey());
    }

    public static void setSeed(String seedText) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.hasSingleplayerServer()) {
            ChatUtils.error("In singleplayer the seed is read automatically.");
            return;
        }
        long value = parseSeed(seedText);
        SEEDS.put(WorldUtils.getWorldKey(), new Seed(value, "manual"));
        ChatUtils.info("Seed set to " + value + " for this world.");
        SeedrayConfig.save();
    }

    public static void deleteSeed() {
        Seed removed = SEEDS.remove(WorldUtils.getWorldKey());
        if (removed != null) {
            ChatUtils.info("Deleted seed " + removed.seed + ".");
        } else {
            ChatUtils.error("No seed stored for this world.");
        }
        SeedrayConfig.save();
    }

    public static Map<String, Seed> all() {
        return SEEDS;
    }

    private static long parseSeed(String text) {
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            return text.strip().hashCode();
        }
    }

    public static JsonObject toJson() {
        JsonObject json = new JsonObject();
        SEEDS.forEach((key, seed) -> {
            JsonObject entry = new JsonObject();
            entry.add("seed", new JsonPrimitive(seed.seed));
            entry.add("version", new JsonPrimitive(seed.version));
            json.add(key, entry);
        });
        return json;
    }

    public static void fromJson(JsonObject json) {
        SEEDS.clear();
        for (String key : json.keySet()) {
            if (!json.get(key).isJsonObject()) {
                continue;
            }
            JsonObject entry = json.getAsJsonObject(key);
            long seed = entry.has("seed") ? entry.get("seed").getAsLong() : 0L;
            String version = entry.has("version") ? entry.get("version").getAsString() : "unknown";
            SEEDS.put(key, new Seed(seed, version));
        }
    }
}
