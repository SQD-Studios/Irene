package net.chamosmp.irene.commands.message;

import net.chamosmp.sqdlib.lang.value.DoubleValueList;
import net.chamosmp.sqdlib.paper.util.ColorUtil;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageCommandManager {

    private final Plugin plugin;

    public DoubleValueList<Player, Player> messageMap = DoubleValueList.of();

    public final List<Audience> spies = new ArrayList<>();

    public MessageCommandManager(Plugin plugin) {
        this.plugin = plugin;
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
        sendMessageToSpies(spies, ColorUtil.parse(
                receiver,
                "<red><b>ISPY<reset>| " + format,
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
}