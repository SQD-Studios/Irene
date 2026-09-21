package net.chamosmp.irene.model;


import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record FormatConfig(
        Component format, // This is the format, which we will then placeholder
        HoverConfig hoverConfig
) {
    public Component getFormat() {
        return hoverConfig.getCompletedHover().append(format);
    }
}
