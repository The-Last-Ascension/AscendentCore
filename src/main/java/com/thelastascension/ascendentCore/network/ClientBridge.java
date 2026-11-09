package com.thelastascension.ascendentCore.network;

import com.thelastascension.ascendentCore.AscendentCore;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Puente de comunicación por paquetes nativos (Plugin Messaging) entre el servidor Purpur 1.21.1
 * y nuestro mod cliente superligero en Fabric (AscendentClient).
 */
public class ClientBridge implements PluginMessageListener {

    public static final String CHANNEL = "ascendent:main";

    private final AscendentCore core;
    private final Map<String, BiConsumer<Player, byte[]>> packetHandlers = new ConcurrentHashMap<>();

    public ClientBridge(AscendentCore core) {
        this.core = core;
    }

    public void initialize() {
        core.getServer().getMessenger().registerOutgoingPluginChannel(core, CHANNEL);
        core.getServer().getMessenger().registerIncomingPluginChannel(core, CHANNEL, this);
    }

    public void shutdown() {
        core.getServer().getMessenger().unregisterOutgoingPluginChannel(core, CHANNEL);
        core.getServer().getMessenger().unregisterIncomingPluginChannel(core, CHANNEL, this);
    }

    public void registerHandler(String subChannel, BiConsumer<Player, byte[]> handler) {
        packetHandlers.put(subChannel.toLowerCase(), handler);
    }

    public void sendToClient(Player player, String subChannel, String jsonPayload) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeUTF(subChannel);
            out.writeUTF(jsonPayload != null ? jsonPayload : "{}");
            player.sendPluginMessage(core, CHANNEL, bytes.toByteArray());
        } catch (IOException e) {
            core.getLogger().warning("Error enviando paquete a AscendentClient (" + player.getName() + "): " + e.getMessage());
        }
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        if (!CHANNEL.equals(channel) || message.length == 0) {
            return;
        }
        BiConsumer<Player, byte[]> defaultHandler = packetHandlers.get("raw");
        if (defaultHandler != null) {
            defaultHandler.accept(player, message);
        }
    }
}
