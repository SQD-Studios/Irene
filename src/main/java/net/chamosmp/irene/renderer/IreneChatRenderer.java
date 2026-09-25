package net.chamosmp.irene.renderer;

import io.papermc.paper.chat.ChatRenderer;
import me.clip.placeholderapi.PlaceholderAPI;
import net.chamosmp.irene.IrenePlugin;
import net.chamosmp.irene.model.FormatConfig;
import net.chamosmp.irene.model.HoverConfig;
import net.chamosmp.irene.util.ChatUtil;
import net.chamosmp.irene.util.LuckPermsUtil;
import net.chamosmp.sqdlib.paper.util.ColorUtil;
import net.chamosmp.sqdlib.paper.util.LoggerUtil;
import net.chamosmp.sqdlib.util.LogType;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.chat.SignedMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IreneChatRenderer implements ChatRenderer {

    private final IrenePlugin plugin;
    private final LuckPermsUtil luckPermsUtil;
    private final ChatUtil chatUtil;

    private final SignedMessage.Signature signature;

    private final Map<String, FormatConfig> formats = new HashMap<>();
    private static final boolean IS_PAPI_ENABLED = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");

    public IreneChatRenderer(final IrenePlugin plugin, LuckPermsUtil luckPermsUtil, ChatUtil chatUtil, SignedMessage.Signature signature) {
        this.plugin = plugin;
        this.luckPermsUtil = luckPermsUtil;
        this.chatUtil = chatUtil;
        this.signature = signature;

        // Make sure the config has at least one format defined
        if (!plugin.getConfig().isConfigurationSection("chat-formats.formats")) {
            LoggerUtil.log(LogType.WARNING, "No chat formats found in config! Please define at least a 'default' format.");
            throw new IllegalStateException("Chat formats are not defined in the config.");
        }

        plugin.getConfig().getConfigurationSection("chat-formats.formats").getKeys(false).forEach(key -> {
            String formatString = plugin.getConfig().getString("chat-formats.formats." + key);
            if (formatString != null) {
                formats.put(key, new FormatConfig(ColorUtil.parse(formatString), new HoverConfig(
                        plugin.getConfig().getString("chat-formats.hover." + key + ".click-command", plugin.getConfig().getString("chat-formats.hover.default.click-command", null)),
                        plugin.getConfig().getString("chat-formats.hover." + key + ".message", plugin.getConfig().getString("chat-formats.hover.default.message", null))
                )));
            }
        });
    }

    @Override
    public @NotNull Component render(@NotNull Player source, @NotNull Component sourceDisplayName,
                                     @NotNull Component message, @NotNull Audience viewer) {
        if (!(viewer instanceof Player)) {
            // If the viewer is not a player (e.g., console), we can just use the source as the viewer for PlaceholderAPI
            viewer = source;
        }

        final String primaryGroup = luckPermsUtil.getPrimaryGroup(source);
        if (primaryGroup == null || primaryGroup.isEmpty()) {
            LoggerUtil.log(LogType.WARNING, "Player " + source.getName() + " has no primary group set.");
            return Component.empty();
        }

        Component format = formats.getOrDefault(primaryGroup, formats.get("default")).getFormat();
        String stringFormat = ColorUtil.deParse(format);

        if (IS_PAPI_ENABLED) {
            String replacement = ColorUtil.deParse(ColorUtil.parse(source, stringFormat));
            replacement = PlaceholderAPI.setRelationalPlaceholders(source, (Player) viewer, replacement);
            stringFormat = replacement;
        }

        if (plugin.getConfig().getBoolean("chat-heads.enabled")) {
            String type = plugin.getConfig().getString("chat-heads.type", "").toLowerCase();
            if ("message".equals(type)) {
                stringFormat = "<head:" + source.getName() + ">" + stringFormat;
            } else if ("name".equals(type)) {
                stringFormat = stringFormat.replace(source.getName(), "<head:" + source.getName() + ">" + source.getName());
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
            message = ColorUtil.parse(ChatUtil.emojiPlaceholder(
                    ColorUtil.deParse(message),
                    plugin.getConfig().getString("emojis.character", ":"),
                    plugin.getConfig().getBoolean("emojis.items"),
                    plugin.getConfig().getBoolean("emojis.player-heads")
            ));
        }

        if (plugin.getConfig().getBoolean("pings.enabled", true)) {
            message = ColorUtil.parse(chatUtil.pingCheck(ColorUtil.deParse(message), plugin.getConfig().getString("pings.ping-character", "@")));
        }

        placeholders.put("prefix", prefix);
        placeholders.put("suffix", suffix);
        placeholders.put("message", ColorUtil.deParse(message));
        placeholders.put("name", source.getName());
        placeholders.put("group", primaryGroup);

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
            return ColorUtil.parse(source, stringFormat, placeholders).append(chatUtil.getDeleteMessageUtil().createDeleteButton(signature, source));
        } else if (plugin.getConfig().getBoolean("chat-heads")) {
            return ColorUtil.parse("<head:" + source.getName() + ">").append(message).append(chatUtil.getDeleteMessageUtil().createDeleteButton(signature, source));
        } else {
            return message.append(chatUtil.getDeleteMessageUtil().createDeleteButton(signature, source));
        }
    }
}
