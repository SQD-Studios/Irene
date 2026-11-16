package net.chamosmp.irene.modl;

import gg.modl.minecraft.api.http.PunishmentApi;
import gg.modl.minecraft.api.http.request.CreatePunishmentRequest;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Objects;


public class ModlModeration {

    private final PunishmentApi punishmentApi;

    private static ModlModeration instance;

    private ModlModeration() {
        RegisteredServiceProvider<PunishmentApi> pro = Bukkit.getServicesManager().getRegistration(PunishmentApi.class);
        if (pro != null) {
            punishmentApi = pro.getProvider();
        } else {
            punishmentApi = null;
        }
    }

    // mute, kick, ban

    public static void ban(Player player, String reason, int time) {
        get().punishmentApi.createPunishment(new CreatePunishmentRequest(
                player.getUniqueId().toString(),
                "irene-auto-mod",
                null,
                reason,
                null,
                null,
                null,
                2, // Ban ordinal
                time
        ));
    }

    public static void kick(Player player, String reason, int time) {
        get().punishmentApi.createPunishment(new CreatePunishmentRequest(
                player.getUniqueId().toString(),
                "irene-auto-mod",
                null,
                reason,
                null,
                null,
                null,
                0, // Kick ordinal
                time
        ));
    }

    public static void mute(Player player, String reason, int time) {
        get().punishmentApi.createPunishment(new CreatePunishmentRequest(
                player.getUniqueId().toString(),
                "irene-auto-mod",
                null,
                reason,
                null,
                null,
                null,
                1, // Mute ordinal
                time
        ));
    }

    private static ModlModeration get() {
        synchronized (ModlModeration.class) {
            return Objects.requireNonNullElseGet(instance, () -> instance = new ModlModeration());
        }
    }
}
