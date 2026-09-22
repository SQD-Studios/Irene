package net.chamosmp.irene.util;

import net.chamosmp.irene.IrenePlugin;
import net.chamosmp.sqdlib.paper.util.DialogUtil;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ChatUtil extends DeleteMessageUtil {

    private final IrenePlugin plugin;

    public ChatUtil(IrenePlugin plugin, DialogUtil dialogUtil) {
        super(plugin, dialogUtil);
        this.plugin = plugin;
    }

    public @NotNull String pingCheck(@NotNull String message, @NotNull String pingChar) {
        @SuppressWarnings("all") final FileConfiguration config = plugin.getConfig();

        @SuppressWarnings("all") final String stringSound = config.getString("pings.sound.name", "block.note_block.banjo").toLowerCase();
        @SuppressWarnings("all")
        Sound sound = Registry.SOUND_EVENT.get(Key.key(stringSound));
        if (sound == null) {
            sound = Sound.BLOCK_NOTE_BLOCK_BIT;
        }
        float volume = config.getInt("pings.sound.volume", 1);
        float pitch = config.getInt("pings.sound.pitch", 1);

        if (message.contains(pingChar)) {
            List<String> list = new ArrayList<>();
            for (int i = message.indexOf(pingChar); message.indexOf(pingChar, i) != -1; i++) {
                int second = message.indexOf(" ", i + 1);

                // We are not certain that this may be a ping, but if you just ping a player (without a space), it will ping.
                // To be sure it's a player below snippets check if a player exists with that name and is online
                if (second == -1) {
                    second = message.length();
                    String key = message.substring(i + 1, second);
                    message = message.replace(pingChar + key, pingWithPlayer(key, pingChar, sound, volume, pitch));
                    break;
                }

                if (i == -1) return message;

                String key = message.substring(i + 1, second);
                if (list.contains(key)) {
                    message = message.replace(pingChar + key, plugin.getConfig().getString("pings.color", "<gold>") + pingChar + key + "<reset>");
                    continue;
                }
                message = message.replace(pingChar + key, pingWithPlayer(key, pingChar, sound, volume, pitch));
                list.add(key);
            }
            return message;
        }
        return message;
    }

    private @NotNull String pingWithPlayer(@NotNull String key, @NotNull String pingChar, Sound sound, float volume, float pitch) {
        String result = pingChar + key;
        Player player = Bukkit.getPlayerExact(key);
        if (player != null && player.isConnected()) {
            result = plugin.getConfig().getString("pings.color", "<gold>") + result + "<reset>";
            player.getScheduler().run(plugin, _ -> {
                player.stopSound(sound);
                player.playSound(player, sound, volume, pitch);
            }, null);
        }
        return result;
    }

    public static @NotNull String emojiPlaceholder(@NotNull String message, @NotNull String emojiCharacter, boolean playerHeads, boolean items) {
        if (message.contains(emojiCharacter)) {
            for (int i = message.indexOf(emojiCharacter); message.indexOf(emojiCharacter, i) != -1; i++) {
                int second = message.indexOf(emojiCharacter, i + 1);
                if (i == -1 || second == -1) return message;

                String key = message.substring(i + 1, second);
                message = message.replace(emojiCharacter + key + emojiCharacter, keyEmojiPlaceholder(key, emojiCharacter, playerHeads, items));
            }
            return message;
        }

        return message;
    }

    private static @NotNull String keyEmojiPlaceholder(@NotNull String key, @NotNull String emojiChar, boolean playerHeads, boolean items) {
        String result = emojiChar + key + emojiChar;
        Material material = Material.getMaterial(key.toUpperCase());
        if (material != null && items) {
            if (material.isBlock()) {
                result = "<white><sprite:blocks:block/" + key + "></white>";
            } else if (material.isItem()) {
                result = "<white><sprite:items:item/" + key + "></white>";
            }
        } else {
            Player player = Bukkit.getPlayerExact(key);
            if (player != null && playerHeads) {
                result = "<white><head:" + player.getName() + "></white>";
            }
        }
        return result;
    }

    public DeleteMessageUtil getDeleteMessageUtil() {
        return super.get();
    }
}
