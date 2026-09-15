package moe.quoex.seedray1218.client.config;

import com.google.gson.JsonElement;

/**
 * Base class for a single configurable value.
 * Replaces Meteor Client's Setting without pulling in any Meteor dependency.
 */
public abstract class Setting<T> {

    private final String name;
    private final String description;
    private final T defaultValue;

    protected T value;

    protected Setting(String name, String description, T defaultValue) {
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public T get() {
        return value;
    }

    public void set(T value) {
        this.value = value;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public void reset() {
        this.value = defaultValue;
    }

    public abstract JsonElement toJson();

    public abstract void fromJson(JsonElement json);
}
