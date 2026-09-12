package net.chamosmp.irene.commands;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.chamosmp.irene.IrenePlugin;
import net.chamosmp.irene.commands.message.MessageCommandManager;
import net.chamosmp.irene.listeners.ChatMessageListener;
import net.chamosmp.irene.messaging.MessageMessaging;
import net.chamosmp.irene.util.LuckPermsUtil;
import net.chamosmp.irene.util.ModerationUtil;
import net.chamosmp.sqdlib.paper.util.ColorUtil;
import net.strokkur.commands.Executes;
import net.strokkur.commands.permission.Permission;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@net.strokkur.commands.Command("irene")
@Permission("irene.admin")
public class IreneCommand {

    private final IrenePlugin plugin;
    private final ModerationUtil moderationUtil;
    private final @Nullable MessageMessaging messageMessaging;
    private final LuckPermsUtil luckPermsUtil;
    private final MessageCommandManager messageStorage;

    public IreneCommand(final IrenePlugin plugin, ModerationUtil moderationUtil, @Nullable MessageMessaging messageMessaging, LuckPermsUtil luckPermsUtil, MessageCommandManager messageStorage) {
        this.plugin = plugin;
        this.moderationUtil = moderationUtil;
        this.messageMessaging = messageMessaging;
        this.luckPermsUtil = luckPermsUtil;

        this.messageStorage = messageStorage;
    }

    @Executes("reload")
    @Permission("irene.admin.reload")
    public void onReload(@NotNull CommandSender commandSender) {
        plugin.reloadConfig();
        AsyncChatEvent.getHandlerList().unregister(plugin);
        new ChatMessageListener(plugin, moderationUtil, messageMessaging, luckPermsUtil);
        moderationUtil.reloadConfig();
        commandSender.sendMessage(ColorUtil.parse(plugin.getConfig().getString("messages.reloaded", "<green>Irene Reloaded.")));
    }

    @Executes("spy")
    @Permission("irene.admin.spy")
    public void onSpy(@NotNull CommandSender commandSender) {
        if (messageStorage.spies.contains(commandSender)) {
            messageStorage.spies.remove(commandSender);
            commandSender.sendMessage(ColorUtil.parse("<red>You are no longer spying"));
        } else {
            messageStorage.spies.add(commandSender);
            commandSender.sendMessage(ColorUtil.parse("<green>You are now spying"));
        }
    }
}