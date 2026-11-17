package net.chamosmp.irene.util;

import com.google.common.base.Strings;
import net.chamosmp.irene.IrenePlugin;
import net.chamosmp.sqdlib.paper.util.ColorUtil;
import net.chamosmp.sqdlib.paper.util.DialogUtil;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.chat.SignedMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

public class DeleteMessageUtil {

    private final IrenePlugin plugin;
    private final DialogUtil dialogUtil;

    public DeleteMessageUtil(IrenePlugin plugin, DialogUtil dialogUtil) {
        this.plugin = plugin;
        this.dialogUtil = dialogUtil;
    }

    public @NonNull Component createDeleteButton(SignedMessage messageSignature, Player player, Audience viewer) {
        if (!plugin.getConfig().getBoolean("deletions.enabled", false)) return Component.empty();
        if (!player.equals(viewer)) return Component.empty();

        Component deleteButton = ColorUtil.parse(plugin.getConfig().getString("deletions.delete-button", "<b><dark_gray>[</dark_gray><red>X</red><dark_gray>]</dark_gray></b>"));
        deleteButton = deleteButton.clickEvent(ClickEvent.callback(i -> {
            if (i.equals(player)) {
                if (!plugin.getConfig().getBoolean("deletions.use-dialog", true)) {
                    Bukkit.getServer().deleteMessage(messageSignature);
                } else {
                    dialogUtil.getYesNo(ColorUtil.parse("Are you sure you want to delete the message?"), player, yes -> {
                        if (yes) {
                            Bukkit.getServer().deleteMessage(messageSignature);
                        }
                    });
                }
            }
        }));

        return Component.space().append(deleteButton);
    }

    public static void deleteAllChat() {
        Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(ColorUtil.parse(Strings.repeat(" <br>", 100))));
    }

    protected DeleteMessageUtil get() {
        return this;
    }
}
