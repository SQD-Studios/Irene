package net.chamosmp.irene.model;

import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record FormatConfig(
        @Nullable Component format, // This is the format, which we will then placeholder
        @Nullable HoverConfig hoverConfig
) {
    public Component getFormat() {
        if (format == null || hoverConfig == null) return Component.text("");
        return hoverConfig.getCompletedHover().append(format);
    }
}
