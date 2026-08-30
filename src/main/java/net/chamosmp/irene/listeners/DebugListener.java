package net.chamosmp.irene.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.chamosmp.irene.IrenePlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class DebugListener implements Listener {

    private final IrenePlugin plugin;

    public DebugListener(IrenePlugin plugin) {
        this.plugin = plugin;
        this.plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void checkCancelled(AsyncChatEvent event) {
        if (event.isCancelled()) {
            plugin.getLogger().info("AsyncChatEvent was cancelled!");
        } else {
            plugin.getLogger().info("AsyncChatEvent was not cancelled.");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void checkCancelledMonitor(AsyncChatEvent event) {
        if (event.isCancelled()) {
            plugin.getLogger().info("AsyncChatEvent was cancelled in MONITOR phase!");
        } else {
            plugin.getLogger().info("AsyncChatEvent was not cancelled in MONITOR phase.");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void detectRenderer(AsyncChatEvent event) {
        plugin.getLogger().info("LOWEST priority renderer: " + event.renderer().getClass().getName());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void detectRendererAfter(AsyncChatEvent event) {
        plugin.getLogger().info("AFTER event renderer: " + event.renderer().getClass().getName());
    }
}
