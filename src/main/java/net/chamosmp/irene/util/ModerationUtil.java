package net.chamosmp.irene.util;

import net.chamosmp.irene.IrenePlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class ModerationUtil {

    private final boolean isEnabled;
    private final IrenePlugin plugin;

    private String message;
    private ConfigurationSection config;
    private Player player;

    public ModerationUtil(IrenePlugin plugin) {
        this.plugin = plugin;
        this.isEnabled = plugin.getConfig().getBoolean("moderation.enabled", false);
    }

    public boolean moderateMessage(Player player, Component message) {
        return moderateMessage(player, ColorUtil.deParse(message));
    }

    public boolean moderateMessage(Player player, String message) {
        this.message = message;
        this.player = player;
        if (isEnabled) {
            ConfigurationSection config = plugin.getConfig().getConfigurationSection("moderation.checks");
            if (config == null) {
                return true;
            }
            this.config = config;

            return limitedLength();
        }
        return true; // It should always return true if moderation is disabled
    }

    public boolean limitedLength() {
        ConfigurationSection limitedLength = config.getConfigurationSection("limited-length");
        if (limitedLength == null) return true;


        int length = limitedLength.getInt("length");
        if (0 > length) return true;


        if (message.length() > length) {
            ConfigurationSection action = limitedLength.getConfigurationSection("action");
            if (action == null) return false;
            punish(getPunishType(action.getString("action", "NONE")));
            return action.getBoolean("block-message");
        }
        return true;
    }

    private void punish(PunishType punishType) {
        switch (punishType) {
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
    }

    public enum PunishType {
        MUTE,
        BAN,
        KICK,
        NONE
    }

    private PunishType getPunishType(String m) {
        return switch (m.toUpperCase()) {
            case "MUTE" -> PunishType.MUTE;
            case "BAN" -> PunishType.BAN;
            case "KICK" -> PunishType.KICK;
            default -> PunishType.NONE;
        };
    }
}
