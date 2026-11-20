package net.chamosmp.irene.renderer;

import me.clip.placeholderapi.PlaceholderAPI;
import net.chamosmp.irene.model.FormatConfig;
import net.chamosmp.irene.model.HoverConfig;
import net.chamosmp.irene.util.ChatUtil;
import net.chamosmp.irene.util.LuckPermsUtil;
import net.chamosmp.irene.util.ModerationUtil;
import net.chamosmp.sqdlib.paper.util.ColorUtil;
import net.chamosmp.sqdlib.paper.util.LoggerUtil;
import net.chamosmp.sqdlib.util.LogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.node.NodeType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class PreviewRenderer {
    private final Plugin plugin;
    private final LuckPermsUtil luckPermsUtil;
    private final ChatUtil chatUtil;
    private final ModerationUtil moderationUtil;

    private @Nullable FormatConfig defaultFormatConfig;

    private static final boolean IS_PAPI_ENABLED = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");

    public PreviewRenderer(Plugin plugin, LuckPermsUtil luckPermsUtil, ChatUtil chatUtil, ModerationUtil moderationUtil) {
        this.plugin = plugin;
        this.luckPermsUtil = luckPermsUtil;
        this.chatUtil = chatUtil;
        this.moderationUtil = moderationUtil;

        if (plugin.getConfig().isConfigurationSection("chat-formats.formats.default")) {
            String formatString = plugin.getConfig().getString("chat-formats.formats.default", null);
            if (formatString != null) {
                defaultFormatConfig = new FormatConfig(ColorUtil.parse(formatString), new HoverConfig(
                        plugin.getConfig().getString("chat-formats.hover.default.click-command", plugin.getConfig().getString("chat-formats.hover.default.click-command", null)),
                        plugin.getConfig().getString("chat-formats.hover.default.message", plugin.getConfig().getString("chat-formats.hover.default.message", null))
                ));
            }
        }
    }

    public @NonNull Component preview(@Nullable Player source, Component message, String group) {
        if (plugin.getConfig().getBoolean("chat-heads") && source != null) {
            String type = plugin.getConfig().getString("chat-heads.type", "").toLowerCase();
            if ("message".equals(type)) {
                message = ColorUtil.parse("<head:" + source.getName() + ">").append(message);
            } else if ("name".equals(type)) {
                message = message.replaceText(b -> {
                            b.matchLiteral(source.getName());
                            b.replacement(ColorUtil.parse("<head:" + source.getName() + ">" + source.getName()));
                        }
                );
            }
        }

        if (plugin.getConfig().getBoolean("emojis.current-item") && source != null) {
            ItemStack hand = source.getInventory().getItemInMainHand();
            message = message.replaceText(i -> {
                i.matchLiteral("[item]");
                i.replacement(hand.displayName().hoverEvent(hand));
            });
        }

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

        if (source != null) {
            if (source.hasPermission("irene.minimessage.bypass.default")) {
                message = MiniMessage.miniMessage(MiniMessage.Preset.DEFAULT).deserialize(stringMessage);
            } else if (source.hasPermission("irene.minimessage.bypass.formattedtext")) {
                message = MiniMessage.miniMessage(MiniMessage.Preset.FORMATTED_TEXT).deserialize(stringMessage);
            } else if (source.hasPermission("irene.minimessage.bypass.noninteractable")) {
                message = MiniMessage.miniMessage(MiniMessage.Preset.NON_INTERACTABLE).deserialize(stringMessage);
            }
        }

        if (plugin.getConfig().getBoolean("pings.enabled", true)) {
            message = ColorUtil.parse(chatUtil.pingCheck(ColorUtil.deParse(message), plugin.getConfig().getString("pings.ping-character", "@")));
        }

        message = ColorUtil.parse(moderationUtil.censorWordFilter(ColorUtil.deParse(message)));


        if (plugin.getConfig().getBoolean("emojis.enabled", true)) {
            message = ChatUtil.emojiPlaceholder(
                    message,
                    plugin.getConfig().getString("emojis.character", ":"),
                    plugin.getConfig().getBoolean("emojis.items"),
                    plugin.getConfig().getBoolean("emojis.player-heads")
            );
        }

        FormatConfig g = null;
        if (plugin.getConfig().isConfigurationSection("chat-formats.formats." + group)) {
            String formatString = plugin.getConfig().getString("chat-formats.formats.default", null);
            if (formatString != null) {
                g = new FormatConfig(ColorUtil.parse(formatString), new HoverConfig(
                        plugin.getConfig().getString("chat-formats.hover." + group + ".click-command", plugin.getConfig().getString("chat-formats.hover." + group + ".click-command", null)),
                        plugin.getConfig().getString("chat-formats.hover." + group + ".message", plugin.getConfig().getString("chat-formats.hover." + group + ".message", null))
                ));
            }
        }

        FormatConfig config;
        if (defaultFormatConfig != null) {
            config = Objects.requireNonNullElse(g, defaultFormatConfig);
        } else {
            config = g;
        }

        if (config == null) {
            LoggerUtil.log(LogType.WARNING, "Could not find format for player, sending the processed message " + source.getName());
            return message;
        }

        String stringFormat = ColorUtil.deParse(config.getFormat());

        if (IS_PAPI_ENABLED) {
            String replacement = PlaceholderAPI.setPlaceholders(source, stringFormat);
            replacement = PlaceholderAPI.setRelationalPlaceholders(source, source, replacement);
            stringFormat = replacement;
        }

        final Map<String, String> placeholders = new HashMap<>();
        Group groupLp = luckPermsUtil.getLuckPerms().getGroupManager().getGroup(group);

        AtomicReference<String> prefix = new AtomicReference<>("");
        AtomicReference<String> suffix = new AtomicReference<>("");

        if (groupLp != null) {
            groupLp.getNodes().forEach(node -> {
                if (node.getType() == NodeType.PREFIX) {
                    prefix.set(node.getKey());
                } else if (node.getType() == NodeType.SUFFIX) {
                    suffix.set(node.getKey());
                }
            });
        } else if (source != null) {
            source.sendMessage(ColorUtil.parse("<red>Could not find Luckperms group with that name. Continuing with empty Prefix and Suffix"));
        }

        placeholders.put("prefix", prefix.get());
        placeholders.put("suffix", suffix.get());
        placeholders.put("message", ColorUtil.deParse(message));
        if (source != null) {
            placeholders.put("name", source.getName());
        }
        placeholders.put("group", group);

        // Find meta tag references %meta-key% and replace them with their values
        if (source != null) {
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
        }

        if (plugin.getConfig().getBoolean("chat-formats.enabled")) {
            return ColorUtil.parse(source, stringFormat, placeholders);
        } else {
            return message;
        }
    }
}
