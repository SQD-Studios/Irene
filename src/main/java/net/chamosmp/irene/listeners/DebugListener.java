package net.chamosmp.irene.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.chamosmp.irene.IrenePlugin;
import net.chamosmp.irene.util.LoggerUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class DebugListener implements Listener {

    public DebugListener(IrenePlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void checkCancelled(AsyncChatEvent event) {
        if (event.isCancelled()) {
            LoggerUtil.log(LoggerUtil.LogType.INFO, "AsyncChatEvent was cancelled!");
        } else {
            LoggerUtil.log(LoggerUtil.LogType.INFO, "AsyncChatEvent was not cancelled.");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void checkCancelledMonitor(AsyncChatEvent event) {
        if (event.isCancelled()) {
            LoggerUtil.log(LoggerUtil.LogType.INFO, "AsyncChatEvent was cancelled in MONITOR phase!");
        } else {
            LoggerUtil.log(LoggerUtil.LogType.INFO, "AsyncChatEvent was not cancelled in MONITOR phase.");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void detectRenderer(AsyncChatEvent event) {
        LoggerUtil.log(LoggerUtil.LogType.INFO, "LOWEST priority renderer: " + event.renderer().getClass().getName());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void detectRendererAfter(AsyncChatEvent event) {
        LoggerUtil.log(LoggerUtil.LogType.INFO, "AFTER event renderer: " + event.renderer().getClass().getName());
    }
}
