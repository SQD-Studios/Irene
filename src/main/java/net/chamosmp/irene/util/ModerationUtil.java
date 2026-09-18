package net.chamosmp.irene.util;

import net.chamosmp.irene.IrenePlugin;
import net.chamosmp.sqdlib.paper.util.ColorUtil;
import net.chamosmp.sqdlib.paper.util.ConfigUtil;
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
import java.util.concurrent.atomic.AtomicBoolean;

public class ModerationUtil {

    /**
     * Is moderation enabled?
     */
    private boolean isEnabled;

    /**
     * The check section of {@code moderation.yml}
     */
    private ConfigurationSection checkSection;

    private YamlConfiguration config;

    /**
     * The prefix for all messages
     */
    private String messagePrefix;


    private final IrenePlugin plugin;

    private final static boolean IS_LITEBANS = Bukkit.getPluginManager().isPluginEnabled("LiteBans");

    // Used for "spam protection"
    private final Map<UUID, Instant> oldMessageTime = new HashMap<>();

    private final Map<Player, List<String>> messageMap = new HashMap<>();

    public ModerationUtil(IrenePlugin plugin) {
        this.plugin = plugin;
        config = ConfigUtil.loadDataFile(plugin, "moderation.yml");

        this.isEnabled = config.getBoolean("moderation.enabled", false);
        this.checkSection = config.getConfigurationSection("moderation.checks");
        this.messagePrefix = config.getString("moderation.messages-prefix", "");
    }

    public boolean moderateMessage(Player player, Component message) {
        return moderateMessage(player, ColorUtil.deParse(message));
    }

    public boolean moderateMessage(Player player, String message) {
        if (isEnabled) {
            return !limitedLength(player, message) && !spam(player, message);
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
            PunishType.of(action.getString("action")).punish(player, plugin, action.getString("reason"), action.getInt("duration"));

            String punishString = action.getString("message");
            if (punishString != null) {
                player.sendMessage(ColorUtil.parse(null, messagePrefix + punishString, Map.of("a_size", length, "p_size", message.length())));
            }
            return action.getBoolean("block-message");
        }
        return false;
    }

    public boolean spam(Player player, String message) {
        ConfigurationSection spamSection = checkSection.getConfigurationSection("spam");
        if (spamSection == null) return false;
        ConfigurationSection action = spamSection.getConfigurationSection("action");
        if (action == null) return false;

        boolean spam = false;

        Instant oldMessageInstant = oldMessageTime.get(player.getUniqueId());
        Instant now = Instant.now();
        if (oldMessageInstant != null) {
            int spamTime = spamSection.getInt("time-between-message");

            if (Duration.between(oldMessageInstant, now).getSeconds() < spamTime) {
                PunishType.of(action.getString("action")).punish(player, plugin, action.getString("reason"), action.getInt("duration"));

                String punishString = action.getString("too-quick-message");
                messagePlayer(player, punishString);

                spam = action.getBoolean("block-message");
            }
            oldMessageTime.put(player.getUniqueId(), now);
        } else {
            oldMessageTime.put(player.getUniqueId(), now);
        }

        boolean tooSimilar = false;
        List<String> messageList = messageMap.get(player);
        if (messageList != null && !messageList.isEmpty()) {
            int sameThreshold = spamSection.getInt("how-many-same-characters-to-flag");
            String messagePunish = action.getString("too-similiar-message");
            for (String s : messageList) {
                if (sameThreshold > 0) {
                    if (s.equalsIgnoreCase(message)) {
                        tooSimilar = action.getBoolean("block-message");
                        PunishType.of(action.getString("action")).punish(player, plugin, action.getString("reason"), action.getInt("duration"));
                        messagePlayer(player, messagePunish);
                        break;
                    }

                    int commonChars = countCommonCharacters(s, message);
                    if (commonChars >= sameThreshold) {
                        tooSimilar = action.getBoolean("block-message");
                        PunishType.of(action.getString("action")).punish(player, plugin, action.getString("reason"), action.getInt("duration"));
                        messagePlayer(player, messagePunish);
                        break;
                    }
                }
            }
        }

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

        return spam || tooSimilar;
    }

    public boolean worldFilter(Player player, String message) {
        ConfigurationSection worldFilterSection = config.getConfigurationSection("world-filter");
        if (worldFilterSection == null) return true;

        AtomicBoolean passes = new AtomicBoolean(true);
        worldFilterSection.getKeys(false).forEach(key -> {

        });

        if (!passes.get()) {
            ConfigurationSection action = worldFilterSection.getConfigurationSection("action");
            if (action == null) return false;
            PunishType.of(action.getString("action", "none")).punish(player, plugin, action.getString("reason"), action.getInt("duration"));
        }
        return !passes.get();
    }

    private int countCommonCharacters(String s1, String s2) {
        int[] count1 = new int[256];
        int[] count2 = new int[256];
        for (char c : s1.toLowerCase().toCharArray()) if (c < 256) count1[c]++;
        for (char c : s2.toLowerCase().toCharArray()) if (c < 256) count2[c]++;
        int common = 0;
        for (int i = 0; i < 256; i++) common += Math.min(count1[i], count2[i]);
        return common;
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

        public void punish(@NotNull Player player, Plugin plugin, String reason, int time) {
            Bukkit.getServer().getGlobalRegionScheduler().run(plugin, _ -> {
                String liteBansTime = "";
                Duration duration = null;
                boolean isPermanent = time < 0;
                if (!isPermanent) {
                    duration = Duration.ofHours(time);
                    liteBansTime = time + "h";
                }

                switch (this) {
                    case NONE -> { // Do nothing
                    }
                    case BAN -> {
                        if (IS_LITEBANS) {
                            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "ipban " + player.getName() + liteBansTime + "--sender=Irene " + reason);
                        } else {
                            player.ban(reason, duration, "Irene");
                        }
                    }
                    case KICK -> {
                        if (IS_LITEBANS) {
                            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "kick " + player.getName() + liteBansTime + "--sender=Irene " + reason);
                        } else {
                            player.kick(ColorUtil.parse(player, reason));
                        }
                    }
                    case MUTE -> {
                        if (IS_LITEBANS) {
                            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "ipmute " + player.getName() + liteBansTime + "--sender=Irene " + reason);
                        }
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
