package net.chamosmp.irene;

import github.scarsz.discordsrv.DiscordSRV;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.chamosmp.irene.commands.IreneCommandBrigadier;
import net.chamosmp.irene.commands.MessageCommandBrigadier;
import net.chamosmp.irene.commands.RespondCommandBrigadier;
import net.chamosmp.irene.discord.DiscordSRVIntegration;
import net.chamosmp.irene.discord.EssentialsDiscordIntegration;
import net.chamosmp.irene.listeners.ChatMessageListener;
import net.chamosmp.irene.listeners.DebugListener;
import net.chamosmp.irene.listeners.JoinListener;
import net.chamosmp.irene.messaging.MessageMessaging;
import net.chamosmp.irene.messaging.NatsMessage;
import net.chamosmp.irene.messaging.RabbitMessage;
import net.chamosmp.irene.messaging.RedisMessage;
import net.chamosmp.irene.util.ConfigUtil;
import net.chamosmp.irene.util.LoggerUtil;
import net.chamosmp.irene.util.LuckPermsUtil;
import net.chamosmp.irene.util.ModerationUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

public class IrenePlugin extends JavaPlugin {

    private DebugListener debugListener;
    private ChatMessageListener chatMessageListener;
    private ModerationUtil moderationUtil;
    private @Nullable MessageMessaging messageMessaging;
    private LuckPermsUtil luckPermsUtil;

    public boolean debug = getConfig().getBoolean("debug", false);

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
        ConfigUtil.loadOrAdapt(this, "config.yml");

        setupPluginMessaging();

        registerCommands();

        this.luckPermsUtil = new LuckPermsUtil(this);
        this.moderationUtil = new ModerationUtil(this);
        this.chatMessageListener = new ChatMessageListener(this, moderationUtil, messageMessaging, luckPermsUtil);
        new JoinListener(this, luckPermsUtil);

        if (debug) {
            LoggerUtil.log(LoggerUtil.LogType.INFO, "Debug mode is enabled.");
            this.debugListener = new DebugListener(this);
        }

        setupDiscord();

        LoggerUtil.log(LoggerUtil.LogType.INFO, "Irene is waiting for chat events...");
    }

    @Override
    public void onDisable() {
        LoggerUtil.log(LoggerUtil.LogType.INFO, "Irene plugin is stopping...");
        AsyncChatEvent.getHandlerList().unregister(this);

        LoggerUtil.log(LoggerUtil.LogType.INFO, "Unregistered listeners...");

        if (messageMessaging != null) {
            messageMessaging.closeConnection();
        }
        LoggerUtil.log(LoggerUtil.LogType.INFO, "Irene has been disabled successfully.");
    }

    @SuppressWarnings("all")
    public void registerCommands() {
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS.newHandler(event -> {
            IreneCommandBrigadier.register(event.registrar(), this, moderationUtil, messageMessaging, luckPermsUtil); // We know the parameter may be null

            if (getConfig().getBoolean("private-message.enabled", true)) {
                event.registrar().register(
                        MessageCommandBrigadier.create(getConfig().getString("private-message.name", "msg"),
                                this, moderationUtil, messageMessaging, luckPermsUtil),
                        "",
                        getConfig().getStringList("private-message.aliases")
                );
                LoggerUtil.log(LoggerUtil.LogType.INFO, "Registered message command");

                // Respond Command
                if (getConfig().getBoolean("private-message.respond.enabled", true)) {
                    event.registrar().register(
                            RespondCommandBrigadier.create(getConfig().getString("private-message.respond.name", "r"),
                                    this, moderationUtil, messageMessaging, luckPermsUtil),
                            "",
                            getConfig().getStringList("private-message.respond.aliases")
                    );
                    LoggerUtil.log(LoggerUtil.LogType.INFO, "Registered respond command");
                }
            }
        }));
    }

    public void setupPluginMessaging() {
        if (getConfig().getBoolean("messaging.enabled", false)) {
            switch (getConfig().getString("messaging.database", "REDIS").toUpperCase()) {
                case "REDIS":
                    messageMessaging = new RedisMessage(this);
                case "NATS":
                    messageMessaging = new NatsMessage(this);
                case "RABBITMQ":
                    messageMessaging = new RabbitMessage(this);
            }
        }
    }

    public void setupDiscord() {
        if (getConfig().getBoolean("discord-integration.enabled", false)) {
            if (getServer().getPluginManager().isPluginEnabled("DiscordSRV")) {
                DiscordSRV.api.subscribe(new DiscordSRVIntegration(this));
                LoggerUtil.log(LoggerUtil.LogType.INFO, "DiscordSRV integration enabled.");
            }
            if (getServer().getPluginManager().isPluginEnabled("EssentialsDiscord")) {
                Bukkit.getPluginManager().registerEvents(new EssentialsDiscordIntegration(this), this);
            }
        }
    }
}
