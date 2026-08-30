package net.chamosmp.irene.adventure;

import io.papermc.paper.chat.ChatRenderer;
import me.clip.placeholderapi.PlaceholderAPI;
import net.chamosmp.irene.IrenePlugin;
import net.chamosmp.irene.util.ColorUtil;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Viewer UnAware Chat Renderer for SimpleChat.
 */
public class IreneChatRenderer implements ChatRenderer {

    private final IrenePlugin plugin;
    private final LuckPerms luckPerms;
    private final Map<String, Component> formats = new HashMap<>();
    private final boolean usePlaceholderAPI;

    public IreneChatRenderer(final IrenePlugin plugin) {
        this.plugin = plugin;

        // Setup Luck Perms
        final RegisteredServiceProvider<LuckPerms> lpsp = plugin.getServer().getServicesManager().getRegistration(LuckPerms.class);
        luckPerms = (lpsp != null) ? lpsp.getProvider() : null;
        if (luckPerms == null) {
            plugin.getLogger().severe("LuckPerms is not available! Irene will not function properly.");
            throw new IllegalStateException("LuckPerms is required for Irene to function.");
        }
        plugin.getLogger().info("LuckPerms found, using it for chat formatting.");

        // Setup PlaceholderAPI
        usePlaceholderAPI = plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI");
        if (usePlaceholderAPI) {
            plugin.getLogger().info("PlaceholderAPI found, placeholders will be processed in message format.");
        } else {
            plugin.getLogger().warning("PlaceholderAPI not found, placeholders will not be processed in message format.");
        }

        // Make sure the config has at least one format defined
        if (!plugin.getConfig().isConfigurationSection("chat-formats.formats")) {
            plugin.getLogger().warning("No chat formats found in config! Please define at least a 'default' format.");
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

        plugin.getLogger().info("Loaded " + formats.size() + " chat formats from config.");
    }

    @Override
    public @NotNull Component render(@NotNull Player source, @NotNull Component sourceDisplayName,
                                     @NotNull Component message, @NotNull Audience viewer) {
        if (!(viewer instanceof Player)) {
            // If the viewer is not a player (e.g., console), we can just use the source as the viewer for PlaceholderAPI
            viewer = source;
        }
        final CachedMetaData user = luckPerms.getPlayerAdapter(Player.class).getMetaData(source);

        final String formatKey = user.getPrimaryGroup();
        if (formatKey == null || formatKey.isEmpty()) {
            plugin.getLogger().warning("Player " + source.getName() + " has no primary group set.");
            return Component.empty();
        }

        Component format = formats.getOrDefault(formatKey, formats.get("default"));
        if (format == null) {
            plugin.getLogger().warning("Config does not contain a format for group " + formatKey + " and/or no \"default\" format is set.");
            return Component.empty();
        }
        String stringFormat = ColorUtil.deParse(format);

        if (usePlaceholderAPI) {
            String replacement = ColorUtil.deParse(ColorUtil.parse(source, stringFormat));
            replacement = PlaceholderAPI.setRelationalPlaceholders(source, (Player) viewer, replacement);
            stringFormat = replacement;
        }

        if (plugin.getConfig().getBoolean("emojis.enabled", true)) {
            message = ColorUtil.parse(ColorUtil.emojiPlaceholder(
                    ColorUtil.deParse(message),
                    plugin.getConfig().getString("emojis.character", ":"),
                    plugin.getConfig().getBoolean("emojis.items"),
                    plugin.getConfig().getBoolean("emojis.player-heads")
            ));
        }

        if (plugin.getConfig().getBoolean("chat-heads")) {
            stringFormat = "<head:" + source.getUniqueId() + ">" + stringFormat;
        }

        final Map<String, String> placeholders = new HashMap<>();
        String prefix = user.getPrefix() != null ? user.getPrefix() : "";
        String suffix = user.getSuffix() != null ? user.getSuffix() : "";


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

        placeholders.put("prefix", prefix);
        placeholders.put("suffix", suffix);
        placeholders.put("message", ColorUtil.deParse(message));
        placeholders.put("name", source.getName());
        placeholders.put("group", formatKey);

        // Find meta tag references %meta-key% and replace them with their values
        for (Map.Entry<String, List<String>> meta : user.getMeta().entrySet()) {
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
}
