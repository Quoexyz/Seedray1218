package moe.quoex.seedray1218.client.utils.seeds;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

public class Seed {

    public final long seed;
    public final String version;

    public Seed(long seed, String version) {
        this.seed = seed;
        this.version = version == null ? "unknown" : version;
    }

    public Component toText() {
        MutableComponent text = Component.literal("[" + seed + "] (" + version + ")");
        text.setStyle(text.getStyle()
                .withColor(ChatFormatting.GREEN)
                .withClickEvent(new ClickEvent.CopyToClipboard(Long.toString(seed)))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Copy to clipboard"))));
        return text;
    }
}
