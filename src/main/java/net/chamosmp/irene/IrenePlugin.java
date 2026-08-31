package net.chamosmp.irene;

import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.chamosmp.irene.commands.IreneCommandBrigadier;
import net.chamosmp.irene.commands.MessageCommandBrigadier;
import net.chamosmp.irene.commands.RespondCommandBrigadier;
import net.chamosmp.irene.listeners.ChatMessageListener;
import net.chamosmp.irene.listeners.DebugListener;
import net.chamosmp.irene.util.ConfigUtil;
import net.chamosmp.irene.util.LoggerUtil;
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
        ConfigUtil.loadOrAdapt(this, "config.yml");

        registerCommands();

        this.moderationUtil = new ModerationUtil(this);
        this.chatMessageListener = new ChatMessageListener(this, moderationUtil);

        if (debug) {
            LoggerUtil.log(LoggerUtil.LogType.INFO, "Debug mode is enabled.");
            this.debugListener = new DebugListener(this);
        }
        LoggerUtil.log(LoggerUtil.LogType.INFO, "Irene is waiting for chat events...");
    }

    @Override
    public void onDisable() {
        LoggerUtil.log(LoggerUtil.LogType.INFO, "Irene plugin is stopping...");
        AsyncChatEvent.getHandlerList().unregister(this);
    }

    public void registerCommands() {
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS.newHandler(event -> {
            IreneCommandBrigadier.register(event.registrar(), this, moderationUtil);

            if (getConfig().getBoolean("private-message.enabled", true)) {
                event.registrar().register(
                        MessageCommandBrigadier.create(this),
                        "",
                        getConfig().getStringList("private-message.aliases")
                );
                if (getConfig().getBoolean("private-message.respond.enabled", true)) {
                    event.registrar().register(
                            RespondCommandBrigadier.create(this),
                            "",
                            getConfig().getStringList("private-message.respond.aliases")
                    );
                }
            }
        }));
    }
}
