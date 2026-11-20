package net.chamosmp.irene.model;

import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@NullMarked
public record FormatConfig(
        @Nullable Component format, // This is the format, which we will then placeholder
        @Nullable HoverConfig hoverConfig
) {
    public Component getFormat() {
        if (hoverConfig != null && format != null) {
            return hoverConfig.getCompletedHover().append(format);
        } else return Objects.requireNonNullElseGet(format, Component::empty);
    }
}
