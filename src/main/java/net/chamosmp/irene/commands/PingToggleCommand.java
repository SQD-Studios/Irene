package net.chamosmp.irene.commands;

import net.chamosmp.sqdlib.paper.util.ColorUtil;
import net.strokkur.commands.Command;
import net.strokkur.commands.Executes;
import net.strokkur.commands.paper.Executor;
import net.strokkur.commands.permission.Permission;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

@Command("toggleping")
@Permission("irene.pingtoggle")
public class PingToggleCommand {

    private final Plugin plugin;

    public static NamespacedKey PING_PDC;

    public PingToggleCommand(Plugin plugin) {
        this.plugin = plugin;

        PING_PDC = new NamespacedKey(plugin, "pings_enabled");
    }

    @Executes
    public void execute(@Executor Player sender) {
        if (!sender.getPersistentDataContainer().has(PING_PDC, PersistentDataType.BOOLEAN)) {
            sender.getPersistentDataContainer().set(PING_PDC, PersistentDataType.BOOLEAN, false);
        }

        boolean currentStatus = Boolean.TRUE.equals(sender.getPersistentDataContainer().get(PING_PDC, PersistentDataType.BOOLEAN));
        sender.getPersistentDataContainer().set(PING_PDC, PersistentDataType.BOOLEAN, !currentStatus);

        sender.sendMessage(ColorUtil.parse(
                plugin.getConfig().getString("pings.toggle-ping.toggle-message", "")
        ));
    }
}