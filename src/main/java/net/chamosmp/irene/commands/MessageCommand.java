package net.chamosmp.irene.commands;

import net.chamosmp.irene.IrenePlugin;
import net.chamosmp.irene.messaging.MessageMessaging;
import net.chamosmp.irene.util.LuckPermsUtil;
import net.chamosmp.irene.util.ModerationUtil;
import net.chamosmp.sqdlib.util.ColorUtil;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.strokkur.commands.Command;
import net.strokkur.commands.Executes;
import net.strokkur.commands.arguments.StringArg;
import net.strokkur.commands.arguments.StringArgType;
import net.strokkur.commands.paper.Executor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Command("msg")
public class MessageCommand {

    private final IrenePlugin plugin;
    private final ModerationUtil moderationUtil;

    protected Map<Player, Player> messageMap = new HashMap<>();

    private final IreneCommand subcommand;

    public MessageCommand(IrenePlugin plugin, ModerationUtil moderationUtil, @Nullable MessageMessaging messaging, LuckPermsUtil luckPermsUtil) {
        this.plugin = plugin;
        this.moderationUtil = moderationUtil;

        this.subcommand = new IreneCommand(plugin, moderationUtil, messaging, luckPermsUtil);

    }

    @Executes
    public void execute(@Executor Player sender, Player player, @StringArg(StringArgType.GREEDY) String message) {
        //if (moderationUtil.moderateMessage(sender, message)) {
        //    return;
        //}
        if (!player.isOnline()) {
            sender.sendMessage(ColorUtil.parse(
                    sender,
                    plugin.getConfig().getString("private-message.player-not-online", "<gray>%player% is not online"),
                    Map.of("player", player.getName())
            ));
        }
        checkMapAndFix(player, sender);
        sendMessage(player, sender, message);
        messageMap.put(player, sender);
    }

    public void sendMessage(Player receiver, Player sender, String message) {
        if (checkSamePlayer(receiver, sender)) return;
        String format = plugin.getConfig().getString("private-message.format", "<#FF00FF>MSG %sending_player% to %receiving_player%</#FF00FF>| <#FF00FF>%message%");
        String youString = plugin.getConfig().getString("private-message.you-string");
        Map<String, String> placeholders = new HashMap<>(
                Map.of("sending_player", sender.getName(), "message", message, "receiving_player", receiver.getName())
        );

        placeholders.put("sending_player_you", youString);
        placeholders.put("receiving_player_you", receiver.getName());
        sender.sendMessage(ColorUtil.parse(
                sender,
                format,
                placeholders
        ));

        placeholders.remove("sending_player_you");
        placeholders.remove("receiving_player_you");


        placeholders.put("sending_player_you", sender.getName());
        placeholders.put("receiving_player_you", youString);
        receiver.sendMessage(ColorUtil.parse(
                receiver,
                format,
                placeholders
        ));
        sendMessageToSpies(subcommand.getSpies(), ColorUtil.parse(
                receiver,
                "<red>ISPY| <reset>" + format,
                placeholders
        ));
    }

    private void sendMessageToSpies(List<Audience> spies, Component message) {
        for (Audience spy : spies) {
            spy.sendMessage(message);
        }
    }

    private boolean checkSamePlayer(Player sender, Player receiver) {
        if (plugin.getConfig().getBoolean("private-message.allow-players-to-self-message")) return false;
        else {
            if (sender.equals(receiver)) {
                sender.sendMessage(ColorUtil.parse(
                        plugin.getConfig().getString("you-cannot-message-yourself", "<red>You cannot message yourself!")
                ));
                return true;
            } else {
                return false;
            }
        }
    }

    private void checkMapAndFix(Player receiver, Player sender) {
        Player key = messageMap.containsKey(receiver) ? receiver : messageMap.containsKey(sender) ? sender : null;
        if (key == null) return;

        Player value = messageMap.get(key);
        if (value != sender || value != receiver) {
            messageMap.remove(key);
        }
    }

    public Map<Player, Player> getMessageMap() {
        return messageMap;
    }
}