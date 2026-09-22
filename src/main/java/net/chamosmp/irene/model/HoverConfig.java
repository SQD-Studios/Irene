package net.chamosmp.irene.model;

import net.chamosmp.sqdlib.paper.util.ColorUtil;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record HoverConfig(
        @Nullable String command,
        @Nullable String hoverMessage
) {
    public Component getCompletedHover() {
        if (command == null || hoverMessage == null) return ColorUtil.parse("");
        return ColorUtil.parse(String.format("<hover:show_text:%s><click:suggest_command:%s>", '"' + hoverMessage + '"', "/" + command));
    }
}
