package net.chamosmp.irene.commands;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.chamosmp.irene.IrenePlugin;
import net.chamosmp.irene.commands.suggestions.LuckpermsGroups;
import net.chamosmp.irene.listeners.ChatMessageListener;
import net.chamosmp.irene.messaging.MessageMessaging;
import net.chamosmp.irene.renderer.IreneChatRenderer;
import net.chamosmp.irene.util.ChatUtil;
import net.chamosmp.irene.util.DeleteMessageUtil;
import net.chamosmp.irene.util.LuckPermsUtil;
import net.chamosmp.irene.util.ModerationUtil;
import net.chamosmp.sqdlib.paper.util.ColorUtil;
import net.kyori.adventure.text.Component;
import net.strokkur.commands.Command;
import net.strokkur.commands.Executes;
import net.strokkur.commands.permission.Permission;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

@Command("irene")
@Permission("irene.admin")
public class IreneCommand {

    private final IrenePlugin plugin;
    private final ModerationUtil moderationUtil;
    private final @Nullable MessageMessaging messageMessaging;
    private final LuckPermsUtil luckPermsUtil;
    private final CommandManager messageStorage;
    private final ChatUtil chatUtil;

    public IreneCommand(IrenePlugin plugin, ModerationUtil moderationUtil, @Nullable MessageMessaging messageMessaging, LuckPermsUtil luckPermsUtil, CommandManager messageStorage, ChatUtil chatUtil) {
        this.plugin = plugin;
        this.moderationUtil = moderationUtil;
        this.messageMessaging = messageMessaging;
        this.luckPermsUtil = luckPermsUtil;
        this.messageStorage = messageStorage;
        this.chatUtil = chatUtil;
    }

    @Executes("reload")
    @Permission("irene.admin.reload")
    public void onReload(CommandSourceStack commandStack) {
        CommandSender commandSender = commandStack.getSender();

        plugin.reloadConfig();
        AsyncChatEvent.getHandlerList().unregister(plugin);
        new ChatMessageListener(plugin, moderationUtil, messageMessaging, luckPermsUtil, chatUtil, messageStorage);
        moderationUtil.reloadConfig();
        commandSender.sendMessage(ColorUtil.parse(plugin.getConfig().getString("messages.reloaded", "<green>Irene Reloaded.")));
    }

    @Executes("spy")
    @Permission("irene.admin.spy")
    public void onSpy(CommandSourceStack commandStack) {
        CommandSender commandSender = commandStack.getSender();

        if (messageStorage.spies.contains(commandSender)) {
            messageStorage.spies.remove(commandSender);
            commandSender.sendMessage(ColorUtil.parse("<red>You are no longer spying"));
        } else {
            messageStorage.spies.add(commandSender);
            commandSender.sendMessage(ColorUtil.parse("<green>You are now spying"));
        }
    }

    @Executes("clear")
    @Permission("irene.admin.clear")
    public void onClear(CommandSourceStack commandStack) {
        CommandSender commandSender = commandStack.getSender();

        DeleteMessageUtil.deleteAllChat();
        commandSender.sendMessage(ColorUtil.parse("<green>The chat has been cleared"));
    }


    @Executes("preview")
    @Permission("irene.admin.preview")
    public void preview(CommandSourceStack commandStack, @LuckpermsGroups String group, String message) {
        CommandSender commandSender = commandStack.getSender();

        Player p;
        if (commandSender instanceof Player p1) {
            p = p1;
        } else {
            p = null;
        }

        commandSender.sendMessage(
                ColorUtil.parse("<red>Irene Preview| ").append(
                        new IreneChatRenderer(plugin, luckPermsUtil, chatUtil, null, moderationUtil, messageStorage, false).preview(p, Component.text(message), group)
                )
        );
    }
}