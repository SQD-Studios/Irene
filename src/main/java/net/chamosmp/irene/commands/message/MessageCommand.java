package net.chamosmp.irene.commands.message;

import net.chamosmp.irene.IrenePlugin;
import net.chamosmp.irene.util.ModerationUtil;
import net.chamosmp.sqdlib.lang.value.DoubleValue;
import net.chamosmp.sqdlib.paper.util.ColorUtil;
import net.strokkur.commands.Command;
import net.strokkur.commands.Executes;
import net.strokkur.commands.arguments.StringArg;
import net.strokkur.commands.arguments.StringArgType;
import net.strokkur.commands.paper.Executor;
import org.bukkit.entity.Player;

import java.util.Map;

@Command("msg")
public class MessageCommand {

    private final IrenePlugin plugin;
    private final ModerationUtil moderationUtil;


    private final MessageCommandManager storage;

    public MessageCommand(IrenePlugin plugin, ModerationUtil moderationUtil, MessageCommandManager storage) {
        this.plugin = plugin;
        this.moderationUtil = moderationUtil;

        this.storage = storage;
    }

    @Executes
    public void execute(@Executor Player sender, Player player, @StringArg(StringArgType.GREEDY) String message) {
        if (!player.isOnline()) {
            sender.sendMessage(ColorUtil.parse(
                    sender,
                    plugin.getConfig().getString("private-message.player-not-online", "<gray>%player% is not online"),
                    Map.of("player", player.getName())
            ));
        }
        storage.messageMap.add(new DoubleValue<>(player, sender));
        storage.sendMessage(player, sender, message);
    }
}