package net.chamosmp.irene.adventure;

import io.papermc.paper.chat.ChatRenderer;
import me.clip.placeholderapi.PlaceholderAPI;
import net.chamosmp.irene.IrenePlugin;
import net.chamosmp.irene.util.LuckPermsUtil;
import net.chamosmp.sqdlib.util.ColorUtil;
import net.chamosmp.sqdlib.util.LoggerUtil;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Viewer UnAware Chat Renderer for SimpleChat.
 */
public class IreneChatRenderer implements ChatRenderer {

    private final IrenePlugin plugin;
    private final LuckPermsUtil luckPermsUtil;

    private final Map<String, Component> formats = new HashMap<>();
    private static final boolean IS_PAPI_ENABLED = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");

    public IreneChatRenderer(final IrenePlugin plugin, LuckPermsUtil luckPermsUtil) {
        this.plugin = plugin;

        // Setup LuckPerms
        this.luckPermsUtil = luckPermsUtil;

        // Setup PlaceholderAPI
        if (IS_PAPI_ENABLED) {
            LoggerUtil.log(LoggerUtil.LogType.INFO, "PlaceholderAPI found, placeholders will be processed in message format.");
        } else {
            LoggerUtil.log(LoggerUtil.LogType.WARNING, "PlaceholderAPI not found, placeholders will not be processed in message format.");
        }

        // Make sure the config has at least one format defined
        if (!plugin.getConfig().isConfigurationSection("chat-formats.formats")) {
            LoggerUtil.log(LoggerUtil.LogType.WARNING, "No chat formats found in config! Please define at least a 'default' format.");
            throw new IllegalStateException("Chat formats are not defined in the config.");
        }

        Map<String, String> components = new HashMap<>();
        if (plugin.getConfig().isConfigurationSection("components")) {
            plugin.getConfig().getConfigurationSection("components").getKeys(false).forEach(key -> {
                String componentString = plugin.getConfig().getString("components." + key).trim();
                components.put(key, componentString);
            });
        }

        plugin.getConfig().getConfigurationSection("chat-formats.formats").getKeys(false).forEach(key -> {
            String formatString = plugin.getConfig().getString("chat-formats.formats." + key);
            for (Map.Entry<String, String> component : components.entrySet()) {
                formatString = formatString.replace("<" + component.getKey() + ">", component.getValue());
            }
            if (formatString != null) {
                formats.put(key, ColorUtil.parse(formatString));
            }
        });

        LoggerUtil.log(LoggerUtil.LogType.INFO, "Loaded " + formats.size() + " chat formats from config.");
    }

    @Override
    public @NotNull Component render(@NotNull Player source, @NotNull Component sourceDisplayName,
                                     @NotNull Component message, @NotNull Audience viewer) {
        if (!(viewer instanceof Player)) {
            // If the viewer is not a player (e.g., console), we can just use the source as the viewer for PlaceholderAPI
            viewer = source;
        }

        final String formatKey = luckPermsUtil.getPrimaryGroup(source);
        if (formatKey == null || formatKey.isEmpty()) {
            LoggerUtil.log(LoggerUtil.LogType.WARNING, "Player " + source.getName() + " has no primary group set.");
            return Component.empty();
        }

        Component format = formats.getOrDefault(formatKey, formats.get("default"));
        if (format == null) {
            LoggerUtil.log(LoggerUtil.LogType.WARNING, "Config does not contain a format for group " + formatKey + " and/or no \"default\" format is set.");
            return Component.empty();
        }
        String stringFormat = ColorUtil.deParse(format);

        if (IS_PAPI_ENABLED) {
            String replacement = ColorUtil.deParse(ColorUtil.parse(source, stringFormat));
            replacement = PlaceholderAPI.setRelationalPlaceholders(source, (Player) viewer, replacement);
            stringFormat = replacement;
        }

        if (plugin.getConfig().getBoolean("chat-heads.enabled")) {
            String type = plugin.getConfig().getString("chat-heads.type", "").toLowerCase();
            if ("message".equals(type)) {
                stringFormat = "<head:" + source.getUniqueId() + ">" + stringFormat;
            } else if ("name".equals(type)) {
                stringFormat = stringFormat.replace(source.getName(), "<head:" + source.getUniqueId() + ">" + source.getName());
            }
        }

        final Map<String, String> placeholders = new HashMap<>();
        String prefix = luckPermsUtil.getPrefix(source);
        String suffix = luckPermsUtil.getSuffix(source);


        String stringMessage = PlainTextComponentSerializer.plainText().serialize(message);
        switch (plugin.getConfig().getString("minimessage-formatting", "NONE").toUpperCase()) {
            case "NON_INTERACTABLE":
                message = MiniMessage.miniMessage(MiniMessage.Preset.NON_INTERACTABLE).deserialize(stringMessage);
                break;
            case "FORMATTED_TEXT":
                message = MiniMessage.miniMessage(MiniMessage.Preset.FORMATTED_TEXT).deserialize(stringMessage);
                break;
            case "DEFAULT":
                message = MiniMessage.miniMessage(MiniMessage.Preset.DEFAULT).deserialize(stringMessage);
                break;
            default:
                break;
        }

        if (plugin.getConfig().getBoolean("emojis.enabled", true)) {
            message = ColorUtil.parse(emojiPlaceholder(
                    ColorUtil.deParse(message),
                    plugin.getConfig().getString("emojis.character", ":"),
                    plugin.getConfig().getBoolean("emojis.items"),
                    plugin.getConfig().getBoolean("emojis.player-heads")
            ));
        }

        if (plugin.getConfig().getBoolean("pings.enabled", true)) {
            message = ColorUtil.parse(pingCheck(ColorUtil.deParse(message), plugin.getConfig().getString("pings.ping-character", "@")));
        }

        placeholders.put("prefix", prefix);
        placeholders.put("suffix", suffix);
        placeholders.put("message", ColorUtil.deParse(message));
        placeholders.put("name", source.getName());
        placeholders.put("group", formatKey);

        // Find meta tag references %meta-key% and replace them with their values
        for (Map.Entry<String, List<String>> meta : luckPermsUtil.getMeta(source).entrySet()) {
            String key = meta.getKey();
            List<String> values = meta.getValue();
            if (values.isEmpty()) {
                continue;
            }
            // We only map the first value for each meta key
            String value = values.getFirst();
            String placeholder = "%" + key + "%";
            placeholders.put(placeholder, value);
        }

        if (plugin.getConfig().getBoolean("chat-formats.enabled")) {
            return ColorUtil.parse(source, stringFormat, placeholders);
        } else if (plugin.getConfig().getBoolean("chat-heads")) {
            return ColorUtil.parse("<head:" + source.getUniqueId() + ">").append(message);
        } else {
            return message;
        }
    }

    public @NotNull String pingCheck(@NotNull String message, @NotNull String pingChar) {
        FileConfiguration config = plugin.getConfig();

        String stringSound = config.getString("pings.sound.name", "NOTE_BLOCK_BANJO").toLowerCase();
        Sound sound = Registry.SOUND_EVENT.get(new NamespacedKey("minecraft", stringSound));
        if (sound == null) { // TODO Doesn't really work so it always falls back here
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
                result = "<white><head:" + player.getUniqueId() + "></white>";
            }
        }
        return result;
    }
}
