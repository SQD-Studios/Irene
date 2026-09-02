package net.chamosmp.irene.util;

import net.chamosmp.irene.IrenePlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class ModerationUtil {

    /**
     * Is moderation enabled?
     */
    private boolean isEnabled;

    /**
     * The check section of {@code moderation.yml}
     */
    private ConfigurationSection checkSection;

    /**
     * The prefix for all messages
     */
    private String messagePrefix;


    private final IrenePlugin plugin;

    // Used for "spam protection"
    private final Map<UUID, Instant> oldMessageTime = new HashMap<>();

    private final Map<Player, List<String>> messageMap = new HashMap<>();

    public ModerationUtil(IrenePlugin plugin) {
        this.plugin = plugin;
        YamlConfiguration moderationConfig = ConfigUtil.loadDataFile(plugin, "moderation.yml");

        this.isEnabled = moderationConfig.getBoolean("moderation.enabled", false);
        this.checkSection = moderationConfig.getConfigurationSection("moderation.checks");
        this.messagePrefix = moderationConfig.getString("moderation.messages-prefix", "");
    }

    public boolean moderateMessage(Player player, Component message) {
        return moderateMessage(player, ColorUtil.deParse(message));
    }

    public boolean moderateMessage(Player player, String message) {
        if (isEnabled) {
            Instant now = Instant.now();

            List<String> oldList = messageMap.get(player);
            if (oldList != null) {
                if (oldList.size() >= checkSection.getInt("spam.clear-message-cache-after", 3)) {
                    oldList.clear();
                }

                List<String> list = new ArrayList<>(oldList);

                list.add(message);
                messageMap.put(player, list);
            } else {
                messageMap.put(player, new ArrayList<>(List.of(message)));
            }


            return !limitedLength(player, message) || !spam(player, message, now);
        }
        return true; // It should always return true if moderation is disabled
    }

    public boolean limitedLength(Player player, String message) {
        ConfigurationSection limitedLength = checkSection.getConfigurationSection("limited-length");
        if (limitedLength == null) return false;


        int length = limitedLength.getInt("length");
        if (0 > length) return false;


        if (message.length() > length) {
            ConfigurationSection action = limitedLength.getConfigurationSection("action");
            if (action == null) return false;
            PunishType.of(action.getString("action")).punish(player, plugin);

            String punishString = action.getString("message");
            if (punishString != null) {
                player.sendMessage(ColorUtil.parse(null, messagePrefix + punishString, Map.of("a_size", length, "p_size", message.length())));
            }
            return action.getBoolean("block-message");
        }
        return false;
    }

    public boolean spam(Player player, String message, Instant now) {
        ConfigurationSection spamSection = checkSection.getConfigurationSection("spam");
        if (spamSection == null) return false;
        ConfigurationSection action = spamSection.getConfigurationSection("action");
        if (action == null) return false;

        boolean spam = false;

        Instant oldMessageInstant = oldMessageTime.get(player.getUniqueId());
        if (oldMessageInstant != null) {

            int spamTime = spamSection.getInt("time-between-message");

            if (Duration.between(oldMessageInstant, now).getSeconds() < spamTime) {
                spam = true;
            }
            oldMessageTime.put(player.getUniqueId(), now);
        } else {
            oldMessageTime.put(player.getUniqueId(), now);
        }

        if (spam) {
            PunishType.of(action.getString("action")).punish(player, plugin);

            String punishString = action.getString("too-quick-message");
            messagePlayer(player, punishString);

            spam = action.getBoolean("block-message");
        }


        boolean tooSimilar = false;
        List<String> messageList = messageMap.get(player);
        if (messageList != null && !messageList.isEmpty()) {
            int same = spamSection.getInt("how-many-same-characters-to-flag");
            String messagePunish = action.getString("too-similiar-message");
            for (String s : messageList) {
                if (0 < same && s.equalsIgnoreCase(message)) {
                    tooSimilar = action.getBoolean("block-message");
                    PunishType.of(action.getString("action")).punish(player, plugin);
                    messagePlayer(player, messagePunish);
                    break;
                }
                if (0 < same && s.compareToIgnoreCase(message) >= same) {
                    tooSimilar = action.getBoolean("block-message");
                    PunishType.of(action.getString("action")).punish(player, plugin);
                    messagePlayer(player, messagePunish);
                    break;
                }
            }
        }


        return spam || tooSimilar;
    }

    private void messagePlayer(@NotNull Player player, @Nullable String message) {
        if (message == null || message.isEmpty()) return;
        player.sendMessage(ColorUtil.parse(messagePrefix + message));
    }

    public enum PunishType {
        MUTE,
        BAN,
        KICK,
        NONE;

        public static PunishType of(@Nullable String string) {
            if (string == null) return NONE;
            return switch (string.toUpperCase()) {
                case "MUTE" -> MUTE;
                case "BAN" -> BAN;
                case "KICK" -> KICK;
                default -> NONE;
            };
        }

        public void punish(@NotNull Player player, Plugin plugin) {
            Bukkit.getServer().getGlobalRegionScheduler().run(plugin, _ -> {
                switch (this) {
                    case NONE -> {
                        // Do nothing
                    }
                    case BAN -> {
                        return; // TODO Ban the player
                    }
                    case KICK -> {
                        return; // TODO Kick the player
                    }
                    case MUTE -> {
                        return; // TODO Mute the player
                    }
                }
            });
        }
    }

    public void reloadConfig() {
        YamlConfiguration moderationConfig = ConfigUtil.loadDataFile(plugin, "moderation.yml");

        this.isEnabled = moderationConfig.getBoolean("moderation.enabled", false);
        this.checkSection = moderationConfig.getConfigurationSection("moderation.checks");
        this.messagePrefix = moderationConfig.getString("moderation.messages-prefix", "");
    }
}
