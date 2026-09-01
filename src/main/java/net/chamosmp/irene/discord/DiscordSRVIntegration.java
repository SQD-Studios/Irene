package net.chamosmp.irene.discord;

import github.scarsz.discordsrv.api.ListenerPriority;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePostProcessEvent;
import github.scarsz.discordsrv.dependencies.kyori.adventure.text.Component;
import github.scarsz.discordsrv.dependencies.kyori.adventure.text.minimessage.MiniMessage;
import net.chamosmp.irene.IrenePlugin;
import net.chamosmp.irene.util.ColorUtil;

import java.util.HashMap;
import java.util.Map;

public class DiscordSRVIntegration {

    private final IrenePlugin plugin;

    public DiscordSRVIntegration(IrenePlugin plugin) {
        this.plugin = plugin;
    }

    @Subscribe(priority = ListenerPriority.HIGHEST)
    public void onMessageReceived(DiscordGuildMessagePostProcessEvent event) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("channel", event.getChannel().getName());
        placeholders.put("sender", event.getAuthor().getName());
        placeholders.put("message", event.getMessage().getContentRaw());

        String format = plugin.getConfig().getString("discord-integration.mc-format", "<blue>Discord <reset>| %sender% > %message%");

        Component component = MiniMessage.miniMessage().deserialize(ColorUtil.placeholder(format, placeholders));

        event.setMinecraftMessage(component);
    }
}
