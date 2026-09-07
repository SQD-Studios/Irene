package net.chamosmp.irene.discord;

import net.chamosmp.irene.IrenePlugin;
import net.chamosmp.sqdlib.paper.util.ColorUtil;
import net.essentialsx.api.v2.events.discord.DiscordRelayEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;

public class EssentialsDiscordIntegration implements Listener {

    private final IrenePlugin plugin;

    public EssentialsDiscordIntegration(IrenePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler()
    public void onEvent(DiscordRelayEvent event) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("channel", event.getChannel().getName());
        placeholders.put("sender", event.getMember().getName());
        placeholders.put("message", event.getRawMessage());

        String format = plugin.getConfig().getString("discord-integration.mc-format", "<blue>Discord <reset>| %sender% > %message%");

        event.setCancelled(true);
        Bukkit.getServer().sendMessage(ColorUtil.parse(null, format, placeholders));
    }
}
