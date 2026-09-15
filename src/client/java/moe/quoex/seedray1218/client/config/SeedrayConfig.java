package moe.quoex.seedray1218.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import moe.quoex.seedray1218.client.utils.seeds.Seeds;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lightweight JSON config that replaces Meteor's config system.
 * Every {@link Setting} registers itself here so it can be saved and loaded.
 */
public final class SeedrayConfig {

    private static final Logger LOG = LoggerFactory.getLogger("Seedray1218");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, Setting<?>> SETTINGS = new LinkedHashMap<>();

    private SeedrayConfig() {
    }

    public static <S extends Setting<?>> S register(S setting) {
        SETTINGS.put(setting.getName(), setting);
        return setting;
    }

    public static Map<String, Setting<?>> getSettings() {
        return SETTINGS;
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("seedray1218.json");
    }

    public static void load() {
        Path path = path();
        if (!Files.exists(path)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

            if (root.has("settings") && root.get("settings").isJsonObject()) {
                JsonObject settings = root.getAsJsonObject("settings");
                for (Map.Entry<String, Setting<?>> entry : SETTINGS.entrySet()) {
                    if (settings.has(entry.getKey())) {
                        entry.getValue().fromJson(settings.get(entry.getKey()));
                    }
                }
            }

            if (root.has("seeds") && root.get("seeds").isJsonObject()) {
                Seeds.fromJson(root.getAsJsonObject("seeds"));
            }
        } catch (Exception e) {
            LOG.error("Failed to load Seedray1218 config", e);
        }
    }

    public static void save() {
        JsonObject root = new JsonObject();

        JsonObject settings = new JsonObject();
        for (Map.Entry<String, Setting<?>> entry : SETTINGS.entrySet()) {
            settings.add(entry.getKey(), entry.getValue().toJson());
        }
        root.add("settings", settings);
        root.add("seeds", Seeds.toJson());

        try (Writer writer = Files.newBufferedWriter(path(), StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        } catch (IOException e) {
            LOG.error("Failed to save Seedray1218 config", e);
        }
    }
}
