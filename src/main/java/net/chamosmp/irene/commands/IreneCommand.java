package net.chamosmp.irene.commands;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.chamosmp.irene.IrenePlugin;
import net.chamosmp.irene.listeners.ChatMessageListener;
import net.chamosmp.irene.messaging.MessageMessaging;
import net.chamosmp.irene.util.ColorUtil;
import net.chamosmp.irene.util.LuckPermsUtil;
import net.chamosmp.irene.util.ModerationUtil;
import net.strokkur.commands.Executes;
import net.strokkur.commands.permission.Permission;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@net.strokkur.commands.Command("irene")
public class IreneCommand {

    private final IrenePlugin plugin;
    private final ModerationUtil moderationUtil;
    private final @Nullable MessageMessaging messageMessaging;
    private final LuckPermsUtil luckPermsUtil;

    public IreneCommand(final IrenePlugin plugin, ModerationUtil moderationUtil, @Nullable MessageMessaging messageMessaging, LuckPermsUtil luckPermsUtil) {
        this.plugin = plugin;
        this.moderationUtil = moderationUtil;
        this.messageMessaging = messageMessaging;
        this.luckPermsUtil = luckPermsUtil;
    }

    @Executes("reload")
    @Permission("irene.reload")
    public void onReload(@NotNull CommandSender commandSender) {
        plugin.reloadConfig();
        AsyncChatEvent.getHandlerList().unregister(plugin);
        new ChatMessageListener(plugin, moderationUtil, messageMessaging, luckPermsUtil);
        moderationUtil.reloadConfig();
        commandSender.sendMessage(ColorUtil.parse(plugin.getConfig().getString("messages.reloaded", "<green>Irene Reloaded.")));
    }
}
