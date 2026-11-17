package net.chamosmp.irene.commands;

import net.chamosmp.irene.datastore.IgnoredPlayersStore;
import net.chamosmp.irene.util.ModerationUtil;
import net.chamosmp.sqdlib.lang.value.DoubleValueList;
import net.chamosmp.sqdlib.paper.util.ColorUtil;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandManager {

    private final Plugin plugin;
    private final IgnoredPlayersStore ignoredPlayersStore;

    // TODO Implement moderation on DMs
    private final ModerationUtil moderationUtil;

    public DoubleValueList<Player, Player> messageMap = DoubleValueList.of();

    public final List<Audience> spies = new ArrayList<>();

    private final Map<OfflinePlayer, List<OfflinePlayer>> ignored = new HashMap<>();

    public CommandManager(Plugin plugin, IgnoredPlayersStore ignoredPlayersStore, ModerationUtil moderationUtil) {
        this.plugin = plugin;
        this.ignoredPlayersStore = ignoredPlayersStore;
        this.moderationUtil = moderationUtil;


        ignored.putAll(reloadIgnoredPlayers());
    }

    public void addIgnoredPlayer(Player player, Player toIgnore) {
        try {
            ignoredPlayersStore.add(player, toIgnore);

            List<OfflinePlayer> current = ignored.get(player);
            List<OfflinePlayer> newPls = new ArrayList<>();
            if (current != null) {
                newPls.addAll(current);
            }
            newPls.add(toIgnore);
            ignored.put(player, newPls);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void removeIgnoredPlayer(Player player, Player toRemove) {
        try {
            ignoredPlayersStore.remove(player, toRemove);
            ignored.remove(toRemove);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public @Unmodifiable Map<OfflinePlayer, List<OfflinePlayer>> getIgnoredPlayers() {
        return ignored;
    }

    public Map<OfflinePlayer, List<OfflinePlayer>> reloadIgnoredPlayers() {
        try {
            return ignoredPlayersStore.get();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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

        List<OfflinePlayer> ignoredPlayer = ignored.get(sender);
        if (ignoredPlayer != null && ignoredPlayer.contains(sender)) {
            receiver.sendMessage(ColorUtil.parse(
                    receiver,
                    format,
                    placeholders
            ));
        }

        String ignored = ignoredPlayer != null && ignoredPlayer.contains(sender) ?
                "(Ignored by" + receiver.getName() + ") " :
                "";

        sendMessageToSpies(spies, ColorUtil.parse(
                receiver,
                "<red><b>ISPY<reset>| " + ignored + format,
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