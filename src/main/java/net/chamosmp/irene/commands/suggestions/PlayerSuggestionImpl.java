package net.chamosmp.irene.commands.suggestions;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import org.bukkit.Bukkit;

import java.util.concurrent.CompletableFuture;

public class PlayerSuggestionImpl {
    @PlayerSuggestion
    public static <S> CompletableFuture<Suggestions> provide(CommandContext<S> ctx, SuggestionsBuilder builder) {
        Bukkit.getServer().getOnlinePlayers()
                .stream()
                .filter(str -> str.getName().toLowerCase().startsWith(builder.getRemainingLowerCase()))
                .forEach(i -> builder.suggest(i.getName()));
        return builder.buildFuture();
    }
}
