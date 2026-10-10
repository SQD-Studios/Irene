package net.chamosmp.irene.commands.suggestions;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

public class NoVanishPlayersImpl {
    @NoVanishPlayers
    public static CompletableFuture<Suggestions> provide(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        Bukkit.getOnlinePlayers().stream()
                .filter(player -> {
                    if (ctx.getSource().getSender() instanceof Player p) {
                        return p.canSee(player);
                    } else {
                        return true;
                    }
                })
                .map(Player::getName)
                .forEach(builder::suggest);
        return builder.buildFuture();
    }
}