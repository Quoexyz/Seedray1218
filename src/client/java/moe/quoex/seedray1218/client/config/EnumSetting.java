package moe.quoex.seedray1218.client.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class EnumSetting<T extends Enum<T>> extends Setting<T> {

    private final T[] values;

    public EnumSetting(String name, String description, T defaultValue) {
        super(name, description, defaultValue);
        this.values = defaultValue.getDeclaringClass().getEnumConstants();
    }

    public T[] getValues() {
        return values;
    }

    /** Cycles to the next enum constant. */
    public void next() {
        value = values[(value.ordinal() + 1) % values.length];
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(value.name());
    }

    @Override
    public void fromJson(JsonElement json) {
        if (json == null || !json.isJsonPrimitive()) {
            return;
        }
        String name = json.getAsString();
        for (T candidate : values) {
            if (candidate.name().equals(name)) {
                value = candidate;
                return;
            }
        }
    }
}
