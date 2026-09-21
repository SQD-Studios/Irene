package net.chamosmp.irene.commands;

import net.chamosmp.sqdlib.paper.util.ColorUtil;
import net.kyori.adventure.text.Component;
import net.strokkur.commands.Command;
import net.strokkur.commands.Executes;
import net.strokkur.commands.arguments.StringArg;
import net.strokkur.commands.arguments.StringArgType;
import net.strokkur.commands.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.HashMap;

@Command("broadcast")
public class BroadcastCommand {

    private final YamlConfiguration configuration;

    public BroadcastCommand(YamlConfiguration plugin) {
        this.configuration = plugin;
    }

    @Executes
    @Permission("irene.broadcast")
    public void onExecute(@StringArg(StringArgType.GREEDY) String message) {
        HashMap<String, String> placeholders = new HashMap<>();
        placeholders.put("message", message);

        Component format = ColorUtil.parse(
                null,
                configuration.getString("broadcast.format", "%message%"),
                placeholders
        );

        Bukkit.getServer().sendMessage(format);
    }
}
