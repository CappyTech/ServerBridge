package com.sovereigncraft.serverbridge;

import com.sovereigncraft.matrix.MatrixClient;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ServerBridge extends JavaPlugin implements Listener {

    private MatrixClient matrixClient;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);

        matrixClient = new MatrixClient(getConfig().getConfigurationSection("matrix"), this.getLogger(), this);
        matrixClient.startPollingChatToMinecraft();
        getLogger().info("ServerBridge enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("ServerBridge disabled.");
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String player = event.getPlayer().getName();
        String message = event.getMessage();
        String formatted = String.format("%s: %s", player, message);

        matrixClient.sendMessage(formatted); // This sends chat to the Matrix room
    }
}
