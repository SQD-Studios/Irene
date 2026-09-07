package net.chamosmp.irene.messaging;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;
import net.chamosmp.irene.IrenePlugin;
import net.chamosmp.sqdlib.paper.util.ColorUtil;
import net.chamosmp.sqdlib.paper.util.LoggerUtil;
import net.chamosmp.sqdlib.util.LogType;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

public class RedisMessage implements MessageMessaging {

    private static final String CHANNEL_NAME = "irene-redis-message:";

    private final IrenePlugin plugin;

    private final UUID temporaryServerUuid = UUID.randomUUID();

    private RedisClient redisClient;
    private StatefulRedisPubSubConnection<String, String> connection;
    RedisPubSubAsyncCommands<String, String> async;

    public RedisMessage(IrenePlugin plugin) {
        this.plugin = plugin;

        connect();
    }

    @Override
    public void connect() {
        try {
            FileConfiguration config = plugin.getConfig();
            String host = config.getString("messaging.host");
            int port = config.getInt("messaging.port");
            String password = config.getString("messaging.password");

            if (password != null) {
                this.redisClient = RedisClient.create("redis://" + password + "@" + host + ":" + port + "/0");
            } else {
                this.redisClient = RedisClient.create("redis://" + host + ":" + port + "/0");
            }
            this.connection = redisClient.connectPubSub();

            LoggerUtil.log(LogType.INFO, "Connected to Redis");

            onMessage();

            this.async = connection.async();

            RedisFuture<Void> subscribeAsync =
                    async.subscribe(CHANNEL_NAME);
            subscribeAsync.thenAccept(_ -> {
                LoggerUtil.log(LogType.INFO, "Successfully subscribed to the Redis Channels");
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public @Nullable RedisFuture<Long> sendMessage(@NotNull Component message) {
        return async.publish(CHANNEL_NAME, ColorUtil.deParse(message) + temporaryServerUuid);
    }

    @Override
    public void onMessage() {
        connection.addListener(new RedisPubSubAdapter<>() {
            @Override
            public void message(String channel, String message) {
                if (channel.equals(CHANNEL_NAME) && !message.endsWith(temporaryServerUuid.toString())) {
                    Bukkit.getServer().sendMessage(ColorUtil.parse(removeUuidFromMessage(message)));
                }
            }
        });
    }

    @Override
    public @NonNull String removeUuidFromMessage(@NotNull String message) {
        boolean isStillGoing = true;
        for (int i = 0; isStillGoing; i++) {
            try {
                String temporaryMessage = message.substring(i);
                UUID uuid = UUID.fromString(temporaryMessage);

                message = message.replace(uuid.toString(), "");

                isStillGoing = false;
            } catch (IllegalArgumentException _) {
            }
        }
        return message;
    }

    @Override
    public void closeConnection() {
        async.unsubscribe(CHANNEL_NAME);
        connection.close();
        redisClient.close();
        LoggerUtil.log(LogType.INFO, "Closed connection to Redis");
    }
}
