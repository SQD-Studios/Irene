package net.chamosmp.irene.commands.suggestions;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.chamosmp.irene.util.LuckPermsUtil;

import java.util.concurrent.CompletableFuture;

public class LuckpermsGroupsImpl {

    private static LuckPermsUtil util;

    public LuckpermsGroupsImpl(LuckPermsUtil util) {
        LuckpermsGroupsImpl.util = util;
    }

    @LuckpermsGroups
    public static CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> commandContext, SuggestionsBuilder suggestionsBuilder) {
        util.getLuckPerms().getGroupManager().getLoadedGroups()
                .forEach(
                        group -> suggestionsBuilder.suggest(group.getName())
                );
        return suggestionsBuilder.buildFuture();
    }
}