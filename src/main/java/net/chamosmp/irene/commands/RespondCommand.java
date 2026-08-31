package net.chamosmp.irene.commands;

import net.chamosmp.irene.IrenePlugin;
import net.chamosmp.irene.util.ColorUtil;
import net.kyori.adventure.text.Component;
import net.strokkur.commands.Command;
import net.strokkur.commands.Executes;
import net.strokkur.commands.arguments.StringArg;
import net.strokkur.commands.arguments.StringArgType;
import net.strokkur.commands.paper.Executor;
import org.bukkit.entity.Player;

@Command("r")
public class RespondCommand extends MessageCommand { // TODO The whole system needs fixing
    private final IrenePlugin plugin;

    public RespondCommand(IrenePlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @Executes
    public void execute(@Executor Player player, @StringArg(StringArgType.GREEDY) String message) {
        Component notInAConversation = ColorUtil.parse(
                plugin.getConfig().getString("private-message.respond.not-in-conversation", "<red>You are not in a conversation!")
        );

        if (messageMap.containsKey(player)) {
            Player receiver = messageMap.get(player);
            sendMessage(receiver, player, message);
        } else if (messageMap.containsValue(player)) {
            Player receiver = null;
            for (Player p : messageMap.keySet()) {
                if (messageMap.get(p) == player) {
                    receiver = p;
                }
            }
            if (receiver != null) {
                sendMessage(receiver, player, message);
            } else {
                player.sendMessage(notInAConversation);
            }
        } else {
            player.sendMessage(notInAConversation);
        }
    }
 }
