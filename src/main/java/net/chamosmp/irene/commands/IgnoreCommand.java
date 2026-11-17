package net.chamosmp.irene.commands;

import net.chamosmp.sqdlib.paper.util.ColorUtil;
import net.strokkur.commands.Command;
import net.strokkur.commands.Executes;
import net.strokkur.commands.paper.Executor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Map;

@Command("ignore")
public class IgnoreCommand {

    private final CommandManager commandManager;
    private final Plugin plugin;

    public IgnoreCommand(CommandManager commandManager, Plugin plugin) {
        this.commandManager = commandManager;
        this.plugin = plugin;
    }

    @Executes
    public void execute(@Executor Player sender, Player toIgnore) {
        Map<OfflinePlayer, List<OfflinePlayer>> map = commandManager.getIgnoredPlayers();

        List<OfflinePlayer> players = map.get(sender);
        if (players == null || !players.contains(toIgnore)) {
            commandManager.addIgnoredPlayer(sender, toIgnore);
            sender.sendMessage(ColorUtil.parse(sender, plugin.getConfig().getString("ignore.on-ignore", ""), Map.of("toIgnore", toIgnore.getName())));
        } else {
            commandManager.removeIgnoredPlayer(sender, toIgnore);
            sender.sendMessage(ColorUtil.parse(sender, plugin.getConfig().getString("ignore.on-unignore", ""), Map.of("toIgnore", toIgnore.getName())));
        }
    }
}