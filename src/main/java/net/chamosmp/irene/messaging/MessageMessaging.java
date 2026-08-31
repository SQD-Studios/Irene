package net.chamosmp.irene.messaging;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Future;

public interface MessageMessaging {
    void connect();

    Future<?> sendMessage(@NotNull Component message);

    void onMessage();

    void closeConnection();
}