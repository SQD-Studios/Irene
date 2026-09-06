package net.chamosmp.irene.messaging;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;
import net.chamosmp.irene.IrenePlugin;
import net.chamosmp.sqdlib.util.ColorUtil;
import net.chamosmp.sqdlib.util.LoggerUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class NatsMessage implements MessageMessaging {

    private static final String CHANNEL_NAME = "irene-nats-message:";

    private final IrenePlugin plugin;

    private final UUID temporaryServerUuid = UUID.randomUUID();

    private Connection connection;

    public NatsMessage(IrenePlugin plugin) {
        this.plugin = plugin;

        connect();
    }

    @Override
    public void connect() {
        try {
            FileConfiguration config = plugin.getConfig();
            String host = config.getString("messaging.host", "localhost");
            int port = config.getInt("messaging.port", 4222);

            connection = Nats.connect("nats://" + host + ":" + port);

            LoggerUtil.log(LoggerUtil.LogType.INFO, "Connected to NATS");

            onMessage();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Future<?> sendMessage(@NotNull Component message) {
        return CompletableFuture.runAsync(() -> {
            String messageAndUuid = ColorUtil.deParse(message) + temporaryServerUuid;
            byte[] messageInBytes = messageAndUuid.getBytes(StandardCharsets.UTF_8);
            connection.publish(CHANNEL_NAME, messageInBytes);
        });
    }

    @Override
    public void onMessage() {
        Dispatcher d = connection.createDispatcher(msg -> {
            String stringMessage = new String(msg.getData(), StandardCharsets.UTF_8);
            if (!stringMessage.endsWith(temporaryServerUuid.toString())) {
                Bukkit.getServer().sendMessage(ColorUtil.parse(removeUuidFromMessage(stringMessage)));
            }
        });
        d.subscribe(CHANNEL_NAME);
        LoggerUtil.log(LoggerUtil.LogType.INFO, "Successfully subscribed to the NATS Channels");
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
        try {
            connection.close();
            LoggerUtil.log(LoggerUtil.LogType.INFO, "Closed connection to NATS");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
