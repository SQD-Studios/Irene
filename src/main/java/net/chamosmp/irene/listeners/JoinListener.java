package net.chamosmp.irene.listeners;

import net.chamosmp.irene.IrenePlugin;
import net.chamosmp.irene.util.LuckPermsUtil;
import net.chamosmp.sqdlib.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class JoinListener implements Listener {

    private final IrenePlugin plugin;
    private final LuckPermsUtil util;

    public JoinListener(IrenePlugin plugin, LuckPermsUtil util) {
        this.plugin = plugin;
        this.util = util;

        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        FileConfiguration config = plugin.getConfig();

        Map<String, String> placeholders = getPlaceholders(player);

        if (config.getBoolean("join-and-leave-messages.enabled")) {
            String join = config.getString("join-and-leave-messages.join");

            if (join != null) {
                event.joinMessage(ColorUtil.parse(player, join, placeholders));
            }
        }

        if (!player.hasPlayedBefore() && config.getBoolean("first-join-message.enabled")) { // On first join
            List<String> messages = config.getStringList("first-join-message.messages");
            sendMultipleMessages(messages, player, placeholders);
        }

        if (config.getBoolean("join-message.enabled")) {
            if (player.hasPlayedBefore() || config.getBoolean("join-message.send-on-first-join")) {
                List<String> messages = config.getStringList("join-message.messages");
                sendMultipleMessages(messages, player, placeholders);
            }
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        FileConfiguration config = plugin.getConfig();
        Map<String, String> placeholders = getPlaceholders(player);

        if (config.getBoolean("join-and-leave-messages.enabled")) {
            String leave = config.getString("join-and-leave-messages.leave");

            if (leave != null) {
                event.quitMessage(ColorUtil.parse(player, leave, placeholders));
            }
        }
    }

    private void sendMultipleMessages(@NotNull List<String> messages, @NotNull Player player, @Nullable Map<?, ?> placeholders) {
        for (String message : messages) {
            if (placeholders == null) placeholders = Map.of();
            player.sendMessage(ColorUtil.parse(player, message, placeholders));
        }
    }

    private Map<String, String> getPlaceholders(@NotNull Player player) {
        return Map.of(
                "name", player.getName(),
                "prefix", util.getPrefix(player),
                "suffix", util.getSuffix(player)
        );
    }
}
