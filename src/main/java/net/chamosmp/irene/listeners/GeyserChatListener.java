package net.chamosmp.irene.listeners;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatMessage;
import net.chamosmp.irene.renderer.IreneChatRenderer;
import net.chamosmp.irene.util.FakeSignedMessage;
import net.kyori.adventure.chat.ChatType;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.geysermc.floodgate.api.FloodgateApi;

import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class GeyserChatListener implements PacketListener {

    private final Plugin plugin;

    private final IreneChatRenderer ireneChatRenderer;

    public GeyserChatListener(Plugin plugin, IreneChatRenderer ireneChatRenderer) {
        this.plugin = plugin;
        this.ireneChatRenderer = ireneChatRenderer;

        if (Bukkit.getServer().isEnforcingSecureProfiles()) {
            PacketEvents.getAPI().getEventManager().registerListener(this, PacketListenerPriority.LOW);
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.CHAT_MESSAGE) return;

        User user = event.getUser();
        UUID uuid = user.getUUID();
        if (!FloodgateApi.getInstance().isFloodgatePlayer(uuid)) return;

        String message = new WrapperPlayClientChatMessage(event).getMessage();
        event.setCancelled(true);

        Bukkit.getGlobalRegionScheduler().run(plugin, _ -> {
            Player player = event.getPlayer();

            FakeSignedMessage sm;
            try {
                sm = new FakeSignedMessage(
                        Component.text(message),
                        message,
                        player.getUniqueId()
                );
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
            ireneChatRenderer.setSignature(sm);

            Bukkit.getServer().forEachAudience(audience -> {
                Component rendered = ireneChatRenderer.render(
                        player,
                        player.displayName(),
                        Component.text(message),
                        audience
                );
                sm.setMessage(rendered);
                if (!rendered.equals(Component.empty()))
                    audience.sendMessage(sm, ChatType.CHAT.bind(Component.text(message)));
            });
        });
    }
}