package moe.quoex.seedray1218.client.module;

import moe.quoex.seedray1218.client.config.SeedrayConfig;
import moe.quoex.seedray1218.client.config.Setting;
import moe.quoex.seedray1218.client.utils.ChatUtils;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal stand-in for Meteor's Module. Handles the enabled flag, the settings
 * and the activate/deactivate lifecycle.
 */
public abstract class SeedrayModule {

    protected static final Minecraft mc = Minecraft.getInstance();

    private final String name;
    private final String description;
    private final List<Setting<?>> settings = new ArrayList<>();
    private boolean active;

    protected SeedrayModule(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /** Registers a setting with the config system and remembers it for listing. */
    protected <S extends Setting<?>> S add(S setting) {
        SeedrayConfig.register(setting);
        settings.add(setting);
        return setting;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<Setting<?>> getSettings() {
        return settings;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /** @return true if the module is enabled after the call. */
    public boolean toggle() {
        if (active) {
            active = false;
            onDeactivate();
            ChatUtils.info(name + " disabled.");
            SeedrayConfig.save();
            return false;
        }

        if (!onActivate()) {
            return false;
        }
        active = true;
        ChatUtils.info(name + " enabled.");
        SeedrayConfig.save();
        return true;
    }

    /** Called when the module is enabled. Return false to abort enabling. */
    public boolean onActivate() {
        return true;
    }

    public void onDeactivate() {
    }
}
