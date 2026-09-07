package net.chamosmp.irene.commands;

import net.chamosmp.irene.IrenePlugin;
import net.chamosmp.irene.messaging.MessageMessaging;
import net.chamosmp.irene.util.LuckPermsUtil;
import net.chamosmp.irene.util.ModerationUtil;
import net.chamosmp.sqdlib.paper.util.ColorUtil;
import net.kyori.adventure.text.Component;
import net.strokkur.commands.Command;
import net.strokkur.commands.Executes;
import net.strokkur.commands.arguments.StringArg;
import net.strokkur.commands.arguments.StringArgType;
import net.strokkur.commands.paper.Executor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

@Command("r")
public class RespondCommand { // TODO The whole system needs fixing
    private final IrenePlugin plugin;
    private final ModerationUtil moderationUtil;

    private final MessageCommand subcommand;

    public RespondCommand(IrenePlugin plugin, ModerationUtil moderationUtil, @Nullable MessageMessaging messageMessaging, LuckPermsUtil luckPermsUtil) {
        this.plugin = plugin;
        this.moderationUtil = moderationUtil;

        this.subcommand = new MessageCommand(plugin, moderationUtil, messageMessaging, luckPermsUtil);
    }

    @Executes
    public void execute(@Executor Player player, @StringArg(StringArgType.GREEDY) String message) {
        //if (moderationUtil.moderateMessage(player, message)) {
        //    return;
        //}
        Component notInAConversation = ColorUtil.parse(
                plugin.getConfig().getString("private-message.respond.not-in-conversation", "<red>You are not in a conversation!")
        );

        if (subcommand.messageMap.containsKey(player)) {
            Player receiver = subcommand.messageMap.get(player);
            subcommand.sendMessage(receiver, player, message);
        } else if (subcommand.messageMap.containsValue(player)) {
            Player receiver = null;
            for (Player p : subcommand.messageMap.keySet()) {
                if (subcommand.messageMap.get(p) == player) {
                    receiver = p;
                }
            }
            if (receiver != null) {
                subcommand.sendMessage(receiver, player, message);
            } else {
                player.sendMessage(notInAConversation);
            }
        } else {
            player.sendMessage(notInAConversation);
        }
    }

}
