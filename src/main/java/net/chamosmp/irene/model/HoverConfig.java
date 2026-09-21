package net.chamosmp.irene.model;

import net.chamosmp.sqdlib.paper.util.ColorUtil;
import net.kyori.adventure.text.Component;

public record HoverConfig(
        String command,
        String hoverMessage
) {
    public Component getCompletedHover() {
        return ColorUtil.parse(String.format("<hover:show_text:%s><click:suggest_command:%s>", '"' + hoverMessage + '"', "/" + command));
    }
}
