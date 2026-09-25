package net.chamosmp.irene.messaging;

import com.google.errorprone.annotations.DoNotCall;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.UUID;
import java.util.concurrent.Future;

public interface MessageMessaging {
    void connect();

    Future<?> sendMessage(@NotNull Component message);

    @DoNotCall
    void onMessage();

    void closeConnection();

    static @NonNull String removeUuidFromMessage(@NotNull String message) {
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
}