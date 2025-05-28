package com.sovereigncraft.serverbridge;

import com.sovereigncraft.matrix.MatrixClient;
import com.sovereigncraft.matrix.MatrixUserTracker;

import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ServerBridge extends JavaPlugin implements Listener {

    private MatrixClient matrixClient;
    private MatrixUserTracker userTracker;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);

        String homeserver = getConfig().getString("matrix.homeserver");
        String adminToken = getConfig().getString("matrix.admin_token");

        if (homeserver == null || adminToken == null) {
            getLogger().severe("Matrix homeserver or admin_token missing in config! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        userTracker = new MatrixUserTracker(getDataFolder(), this.getLogger());

        matrixClient = new MatrixClient(getConfig().getConfigurationSection("matrix"), this.getLogger(), this);
        try {
            matrixClient.startPollingChatToMinecraft();
        } catch (Exception e) {
            getLogger().severe("Failed to start Matrix client: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (getCommand("chat") == null) {
            getLogger().severe("Command 'chat' not defined in plugin.yml!");
        } else {
            getCommand("chat").setExecutor(new ChatCommandExecutor(this, homeserver, adminToken));
        }
        getLogger().info("ServerBridge enabled!");
    }

    @Override
    public void onDisable() {
        if (matrixClient != null) {
            //matrixClient.shutdown(); // If you have a shutdown method
        }
        getLogger().info("ServerBridge disabled.");
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String player = event.getPlayer().getName();
        String message = event.getMessage();
        String formatted = String.format("%s: %s", player, message);

        matrixClient.sendMessage(formatted); // This sends chat to the Matrix room
    }

    public MatrixUserTracker getUserTracker() {
        return userTracker;
    }
}
