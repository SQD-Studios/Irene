package net.chamosmp.irene.messaging;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import net.chamosmp.irene.IrenePlugin;
import net.chamosmp.irene.util.ColorUtil;
import net.chamosmp.irene.util.LoggerUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class RabbitMessage implements MessageMessaging {

    private static final String CHANNEL_NAME = "irene-rabbitmq-message:";

    private final IrenePlugin plugin;

    private final UUID temporaryServerUuid = UUID.randomUUID();

    private ConnectionFactory factory;
    private Connection connection;
    private Channel channel;

    public RabbitMessage(IrenePlugin plugin) {
        this.plugin = plugin;

        connect();
    }

    @Override
    public void connect() {
        try {
            FileConfiguration config = plugin.getConfig();
            String host = config.getString("messaging.host", "localhost");
            int port = config.getInt("messaging.port", 5672);
            String password = config.getString("messaging.password");
            String user = config.getString("messaging.rabbitmq-user");


            factory = new ConnectionFactory();
            factory.setHost(host);
            factory.setUsername(user);
            factory.setPassword(password);
            factory.setPort(port);


            connection = factory.newConnection();
            channel = connection.createChannel();
            channel.exchangeDeclare(CHANNEL_NAME, "fanout");

            LoggerUtil.log(LoggerUtil.LogType.INFO, "Connected to RabbitMQ");

            onMessage();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Future<?> sendMessage(@NotNull Component message) {
        return CompletableFuture.runAsync(() -> {
            try {
                String stringMessage = ColorUtil.deParse(message) + temporaryServerUuid;
                byte[] messageBytes = stringMessage.getBytes(StandardCharsets.UTF_8);

                channel.basicPublish(CHANNEL_NAME, "", null, messageBytes);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onMessage() {
        try {
            String queueName = channel.queueDeclare().getQueue();
            channel.queueBind(queueName, CHANNEL_NAME, "");

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String stringMessage = new String(delivery.getBody(), StandardCharsets.UTF_8);
                if (!stringMessage.endsWith(temporaryServerUuid.toString())) {
                    Bukkit.getServer().sendMessage(ColorUtil.parse(removeUuidFromMessage(stringMessage)));
                }
            };
            channel.basicConsume(queueName, true, deliverCallback, _ -> {});
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            LoggerUtil.log(LoggerUtil.LogType.INFO, "Closed connection to RabbitMQ");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
