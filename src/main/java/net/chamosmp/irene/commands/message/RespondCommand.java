package net.chamosmp.irene.commands.message;

import net.chamosmp.irene.IrenePlugin;
import net.chamosmp.sqdlib.lang.value.DoubleValueList;
import net.chamosmp.sqdlib.paper.util.ColorUtil;
import net.kyori.adventure.text.Component;
import net.strokkur.commands.Command;
import net.strokkur.commands.Executes;
import net.strokkur.commands.arguments.StringArg;
import net.strokkur.commands.arguments.StringArgType;
import net.strokkur.commands.paper.Executor;
import org.bukkit.entity.Player;

import java.util.Map;

@Command("r")
public class RespondCommand {
    private final IrenePlugin plugin;

    private final MessageCommandManager storage;

    public RespondCommand(IrenePlugin plugin, MessageCommandManager storage) {
        this.plugin = plugin;

        this.storage = storage;
    }

    @Executes
    public void execute(@Executor Player player, @StringArg(StringArgType.GREEDY) String message) {
        Component notInAConversation = ColorUtil.parse(
                plugin.getConfig().getString("private-message.respond.not-in-conversation", "<red>You are not in a conversation!")
        );

        DoubleValueList<Player, Player> doubleValueList = storage.messageMap;

        if (doubleValueList.contains(player)) {
            Object receiver = doubleValueList.get(player);
            if (receiver instanceof Player p) {
                if (!p.isOnline() || !player.canSee(p)) {
                    player.sendMessage(ColorUtil.parse(
                            player,
                            plugin.getConfig().getString("private-message.player-not-online", "<gray>%player% is not online"),
                            Map.of("player", player.getName())
                    ));
                }

                storage.sendMessage(p, player, message);
            } else {
                player.sendMessage(notInAConversation);
            }
        } else {
            player.sendMessage(notInAConversation);
        }
    }
}