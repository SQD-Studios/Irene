package net.chamosmp.irene.renderer;

import io.papermc.paper.chat.ChatRenderer;
import me.clip.placeholderapi.PlaceholderAPI;
import net.chamosmp.irene.IrenePlugin;
import net.chamosmp.irene.commands.CommandManager;
import net.chamosmp.irene.model.FormatConfig;
import net.chamosmp.irene.model.HoverConfig;
import net.chamosmp.irene.util.ChatUtil;
import net.chamosmp.irene.util.LuckPermsUtil;
import net.chamosmp.irene.util.ModerationUtil;
import net.chamosmp.sqdlib.paper.util.ColorUtil;
import net.chamosmp.sqdlib.paper.util.LoggerUtil;
import net.chamosmp.sqdlib.util.LogType;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.chat.SignedMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.node.NodeType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class IreneChatRenderer implements ChatRenderer {

    private final IrenePlugin plugin;
    private final LuckPermsUtil luckPermsUtil;
    private final ChatUtil chatUtil;

    private @Nullable SignedMessage signature;

    private final ModerationUtil moderationUtil;

    private final CommandManager commandManager;

    private final Map<String, FormatConfig> formats = new HashMap<>();
    private static final boolean IS_PAPI_ENABLED = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");

    private final boolean shouldAddDeleteButton;

    public IreneChatRenderer(IrenePlugin plugin, LuckPermsUtil luckPermsUtil, ChatUtil chatUtil, @Nullable SignedMessage signature, ModerationUtil moderationUtil, CommandManager commandManager, boolean shouldAddDeleteButton) {
        this.plugin = plugin;
        this.luckPermsUtil = luckPermsUtil;
        this.chatUtil = chatUtil;
        this.signature = signature;
        this.moderationUtil = moderationUtil;
        this.commandManager = commandManager;
        this.shouldAddDeleteButton = shouldAddDeleteButton;

        // Make sure the config has at least one format defined
        if (!plugin.getConfig().isConfigurationSection("chat-formats.formats")) {
            LoggerUtil.log(LogType.WARNING, "No chat formats found in config! Please define at least a 'default' format.");
            throw new IllegalStateException("Chat formats are not defined in the config.");
        }

        ConfigurationSection formatsSection = plugin.getConfig().getConfigurationSection("chat-formats.formats");
        if (formatsSection != null) {
            formatsSection.getKeys(false).forEach(key -> {
                String formatString = plugin.getConfig().getString("chat-formats.formats." + key);
                if (formatString != null) {
                    formats.put(key, new FormatConfig(ColorUtil.parse(formatString), new HoverConfig(
                            plugin.getConfig().getString("chat-formats.hover." + key + ".click-command", plugin.getConfig().getString("chat-formats.hover.default.click-command", null)),
                            plugin.getConfig().getString("chat-formats.hover." + key + ".message", plugin.getConfig().getString("chat-formats.hover.default.message", null))
                    )));
                }
            });
        }
    }

    @SuppressWarnings("")
    public void setSignature(@Nullable SignedMessage signature) {
        this.signature = signature;
    }

    @Override
    public @NonNull Component render(@NotNull Player source, @NotNull Component sourceDisplayName, @NotNull Component message, @NotNull Audience viewer) {
        if (!(viewer instanceof Player viewerPlayer)) {
            // If the viewer is not a player (e.g., console), we can just use the source as the viewer for PlaceholderAPI
            viewer = source;
        } else {
            Map<OfflinePlayer, List<OfflinePlayer>> map = commandManager.getIgnoredPlayers();
            List<OfflinePlayer> players = map.get(viewerPlayer);
            if (players != null && players.contains(source)) {
                if (signature != null)
                    viewerPlayer.deleteMessage(signature);
                return Component.empty();
            }
        }

        if (plugin.getConfig().getBoolean("chat-heads")) {
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

        if (plugin.getConfig().getBoolean("emojis.current-item")) {
            ItemStack hand = source.getInventory().getItemInMainHand();
            if (hand.getType() != Material.AIR) {
                message = message.replaceText(i -> {
                    i.matchLiteral("[item]");
                    i.replacement(hand.displayName().hoverEvent(hand));
                });
            }
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

        if (source.hasPermission("irene.minimessage.bypass.default")) {
            message = MiniMessage.miniMessage(MiniMessage.Preset.DEFAULT).deserialize(stringMessage);
        } else if (source.hasPermission("irene.minimessage.bypass.formattedtext")) {
            message = MiniMessage.miniMessage(MiniMessage.Preset.FORMATTED_TEXT).deserialize(stringMessage);
        } else if (source.hasPermission("irene.minimessage.bypass.noninteractable")) {
            message = MiniMessage.miniMessage(MiniMessage.Preset.NON_INTERACTABLE).deserialize(stringMessage);
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

        if (signature != null && shouldAddDeleteButton)
            message = message.append(chatUtil.getDeleteMessageUtil().createDeleteButton(signature, source, viewer));

        final String primaryGroup = luckPermsUtil.getPrimaryGroup(source);
        if (primaryGroup == null || primaryGroup.isEmpty()) {
            LoggerUtil.log(LogType.WARNING, "Player " + source.getName() + " has no primary group set.");
            return Component.empty();
        }

        Component format = formats.getOrDefault(primaryGroup, formats.get("default")).getFormat();
        String stringFormat = ColorUtil.deParse(format);

        if (IS_PAPI_ENABLED) {
            String replacement = PlaceholderAPI.setPlaceholders(source, stringFormat);
            replacement = PlaceholderAPI.setRelationalPlaceholders(source, (Player) viewer, replacement);
            stringFormat = replacement;
        }

        final Map<String, String> placeholders = new HashMap<>();
        String prefix = luckPermsUtil.getPrefix(source);
        String suffix = luckPermsUtil.getSuffix(source);

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
            return ColorUtil.parse(source, stringFormat, placeholders);
        } else {
            return message;
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

        Component format = formats.getOrDefault(group, formats.get("default")).getFormat();
        String stringFormat = ColorUtil.deParse(format);

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
