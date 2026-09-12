package net.chamosmp.irene.commands.message;

import net.chamosmp.irene.IrenePlugin;
import net.chamosmp.irene.util.ModerationUtil;
import net.chamosmp.sqdlib.lang.value.DoubleValueList;
import net.chamosmp.sqdlib.paper.util.ColorUtil;
import net.chamosmp.sqdlib.paper.util.LoggerUtil;
import net.chamosmp.sqdlib.util.LogType;
import net.kyori.adventure.text.Component;
import net.strokkur.commands.Command;
import net.strokkur.commands.Executes;
import net.strokkur.commands.arguments.StringArg;
import net.strokkur.commands.arguments.StringArgType;
import net.strokkur.commands.paper.Executor;
import org.bukkit.entity.Player;

@Command("r")
public class RespondCommand {
    private final IrenePlugin plugin;
    private final ModerationUtil moderationUtil;

    private final MessageCommandManager storage;

    public RespondCommand(IrenePlugin plugin, ModerationUtil moderationUtil, MessageCommandManager storage) {
        this.plugin = plugin;
        this.moderationUtil = moderationUtil;

        this.storage = storage;
    }

    @Executes
    public void execute(@Executor Player player, @StringArg(StringArgType.GREEDY) String message) {
        Component notInAConversation = ColorUtil.parse(
                plugin.getConfig().getString("private-message.respond.not-in-conversation", "<red>You are not in a conversation!")
        );

        DoubleValueList<Player, Player> doubleValueList = storage.messageMap;

        if (doubleValueList.contains(player)) {
            Player receiver = (Player) doubleValueList.get(player);
            if (receiver != null) {
                storage.sendMessage(receiver, player, message);
            } else {
                LoggerUtil.log(LogType.INFO, "The receiver is null");
                player.sendMessage(notInAConversation);
            }
        } else {
            LoggerUtil.log(LogType.INFO, "The double value list does not contain a player");
            player.sendMessage(notInAConversation);
        }
    }
}