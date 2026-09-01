package net.chamosmp.irene.messaging;

import com.google.errorprone.annotations.DoNotCall;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Future;

public interface MessageMessaging {
    void connect();

    Future<?> sendMessage(@NotNull Component message);

    @DoNotCall
    void onMessage();

    void closeConnection();
}