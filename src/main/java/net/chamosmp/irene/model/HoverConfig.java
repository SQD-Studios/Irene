package net.chamosmp.irene.model;

import net.chamosmp.sqdlib.paper.util.ColorUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public record HoverConfig(
        String command,
        String hoverMessage
) {
    public String getCompletedHover() {
        return String.format("<hover:show_text:%s>", hoverMessage);
    }

    public Component getCompletedHoverComponent() {
        return ColorUtil.parse(getCompletedHover());
    }

    public void runCommand(Player player) {
    }
}
