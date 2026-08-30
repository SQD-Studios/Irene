package net.chamosmp.irene;

import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.chamosmp.irene.commands.IreneCommandBrigadier;
import net.chamosmp.irene.listeners.ChatMessageListener;
import net.chamosmp.irene.listeners.DebugListener;
import net.chamosmp.irene.util.ModerationUtil;
import org.bukkit.plugin.java.JavaPlugin;

public class IrenePlugin extends JavaPlugin {

    private DebugListener debugListener;
    private ChatMessageListener chatMessageListener;
    private ModerationUtil moderationUtil;

    public boolean debug = getConfig().getBoolean("debug", false);

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
        saveDefaultConfig();
        reloadConfig();

        this.moderationUtil = new ModerationUtil(this);
        this.chatMessageListener = new ChatMessageListener(this, moderationUtil);
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS.newHandler(event -> {
            IreneCommandBrigadier.register(event.registrar(), this, moderationUtil);
        }));
        if (debug) {
            getLogger().info("Debug mode is enabled.");
            this.debugListener = new DebugListener(this);
        }
        getLogger().info("Irene is waiting for chat events...");
    }

    @Override
    public void onDisable() {
        getLogger().info("Irene plugin is stopping...");
        AsyncChatEvent.getHandlerList().unregister(this);
    }
}
