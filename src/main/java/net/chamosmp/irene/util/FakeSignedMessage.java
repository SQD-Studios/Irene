package net.chamosmp.irene.util;

import net.chamosmp.sqdlib.paper.util.ColorUtil;
import net.chamosmp.sqdlib.paper.util.LoggerUtil;
import net.chamosmp.sqdlib.util.LogType;
import net.kyori.adventure.chat.SignedMessage;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

public class FakeSignedMessage implements SignedMessage {

    private Component message;
    private String stringMessage;

    private final Instant timestamp = Instant.now();

    private final long salt = new Random().nextLong();

    private final byte[] signature;

    private final UUID senderUuid;

    public FakeSignedMessage(Component message, String stringMessage, @Nullable UUID senderUuid) throws NoSuchAlgorithmException {
        this.message = message;
        this.stringMessage = stringMessage;

        this.senderUuid = Objects.requireNonNullElseGet(senderUuid, UUID::randomUUID);

        byte[] bytes = new byte[256];
        new Random().nextBytes(bytes);
        this.signature = bytes;
    }

    public void setMessage(Component message) {
        this.message = message;
        this.stringMessage = ColorUtil.deParse(message);
    }

    @Override
    public @NonNull Instant timestamp() {
        return timestamp;
    }

    @Override
    public long salt() {
        return salt;
    }

    @Override
    public @Nullable Signature signature() {
        return SignedMessage.signature(signature);
    }

    @Override
    public @Nullable Component unsignedContent() {
        return message;
    }

    @Override
    public @NonNull String message() {
        return stringMessage;
    }

    @Override
    public @NonNull Identity identity() {
        return Identity.identity(senderUuid);
    }

    @Override
    public boolean isSystem() {
        return false;
    }

    @Override
    public boolean canDelete() {
        return true;
    }
}