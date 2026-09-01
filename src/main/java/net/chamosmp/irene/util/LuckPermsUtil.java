package net.chamosmp.irene.util;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class LuckPermsUtil {
    private LuckPerms luckPerms;

    private final Plugin plugin;

    public LuckPermsUtil(Plugin plugin) {
        this.plugin = plugin;

        initialize();
    }

    public void initialize() {
        final RegisteredServiceProvider<LuckPerms> luckPermsProvider = plugin.getServer().getServicesManager().getRegistration(LuckPerms.class);
        luckPerms = (luckPermsProvider != null) ? luckPermsProvider.getProvider() : null;
        if (luckPerms == null) {
            LoggerUtil.log(LoggerUtil.LogType.SEVERE, "LuckPerms is not available! Irene will not function properly.");
            throw new IllegalStateException("LuckPerms is required for Irene to function.");
        }
        LoggerUtil.log(LoggerUtil.LogType.INFO, "LuckPerms found, using it for chat formatting.");
    }

    /**
     * Gets the name of the holders primary group.
     *
     * @param player The player to get the group from
     * @return the name of the primary group
     */
    public @Nullable String getPrimaryGroup(@NotNull Player player) {
        return getCachedMetaData(player).getPrimaryGroup();
    }

    /**
     * Gets the prefix.
     * This method uses the rules defined by the {@linkplain CachedMetaData#getPrefixStackDefinition() prefix stack} to produce a {@link String} output.
     * Assuming the default configuration is used, this will usually be the value of the holder's highest priority prefix node.
     * If the resultant prefix stack contained no elements, an empty string is returned
     *
     * @param player The player to get the prefix from
     * @return the prefix
     */
    public @NotNull String getPrefix(@NotNull Player player) {
        String prefix = getCachedMetaData(player).getPrefix();
        return Objects.requireNonNullElse(prefix, "");
    }

    /**
     * Gets the suffix.
     * This method uses the rules defined by the {@linkplain CachedMetaData#getSuffixStackDefinition() suffix stack} to produce a {@link String} output.
     * Assuming the default configuration is used, this will usually be the value of the holder's highest priority suffix node.
     * If the resultant suffix stack contained no elements, an empty string is returned
     *
     * @param player The player to get the prefix from
     * @return the suffix
     */
    public @NotNull String getSuffix(@NotNull Player player) {
        String suffix = getCachedMetaData(player).getSuffix();
        return Objects.requireNonNullElse(suffix, "");
    }

    public @NotNull @Unmodifiable Map<String, List<String>> getMeta(@NotNull Player player) {
        return getCachedMetaData(player).getMeta();
    }

    public @NotNull LuckPerms getLuckPerms() {
        return luckPerms;
    }

    private @NotNull CachedMetaData getCachedMetaData(@NotNull Player player) {
        return luckPerms.getPlayerAdapter(Player.class).getMetaData(player);
    }
}
