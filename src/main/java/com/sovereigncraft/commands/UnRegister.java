package com.sovereigncraft.commands;

import com.sovereigncraft.matrix.MatrixUserTracker;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;

public class UnRegister implements CommandExecutor {

    private final String homeserver;
    private final MatrixUserTracker userTracker;
    private final File authFile;
    private final Gson gson = new Gson();
    private final Logger logger = Bukkit.getLogger();

    public UnRegister(String homeserver, MatrixUserTracker userTracker, File dataFolder) {
        this.homeserver = homeserver;
        this.userTracker = userTracker;
        this.authFile = new File(dataFolder, "matrix_auth.json");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length != 1) {
            player.sendMessage("§cUsage: /chat unregister");
            return true;
        }

        UUID uuid = player.getUniqueId();
        String currentUsername = player.getName();
        String password = args[0];

        // Determine Matrix username from tracked data
        String matrixUsername = userTracker.getMatrixUsername(uuid);
        if (matrixUsername == null) {
            player.sendMessage("§cNo Matrix registration found for you. Try /chat register.");
            return true;
        }

        boolean nameChanged = !matrixUsername.equals(currentUsername);
        String effectiveUsername = nameChanged ? matrixUsername : currentUsername;

        new Thread(() -> {
            try {
                URL url = new URL(homeserver + "/_matrix/client/v3/account/password");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                // Use Gson for safe JSON construction
                JsonObject auth = new JsonObject();
                auth.addProperty("type", "m.login.password");
                auth.addProperty("user", effectiveUsername);
                auth.addProperty("password", password);

                JsonObject payload = new JsonObject();
                payload.add("auth", auth);
                payload.addProperty("new_password", password);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = conn.getResponseCode();

                // Ensure Bukkit API is called on the main thread
                Bukkit.getScheduler().runTask(
                    Bukkit.getPluginManager().getPlugin("ServerBridge"),
                    () -> {
                        if (responseCode == 200) {
                            if (nameChanged) {
                                player.sendMessage("§a✅ Authentication successful for old Matrix name §e" + matrixUsername + "§a.");
                                userTracker.remove(uuid);
                                removeStoredToken(matrixUsername);
                                player.sendMessage("§7Matrix record cleared. You can now register your new name §b" + currentUsername + "§7.");
                            } else {
                                player.sendMessage("§a✅ Matrix account verified under §b" + currentUsername + "§a.");
                                player.sendMessage("§eThis Matrix account cannot be renamed. You must delete it manually in your Matrix client.");
                            }
                        } else {
                            player.sendMessage("§c❌ Unregistration failed. Code: " + responseCode);
                        }
                    }
                );

                conn.disconnect();
            } catch (Exception e) {
                logger.warning("[UnRegister] Error unregistering Matrix account: " + e.getMessage());
                Bukkit.getScheduler().runTask(
                    Bukkit.getPluginManager().getPlugin("ServerBridge"),
                    () -> player.sendMessage("§c❌ Error unregistering Matrix account: " + e.getMessage())
                );
            }
        }).start();

        return true;
    }

    private void removeStoredToken(String username) {
        try {
            Map<String, String> map = new HashMap<>();
            if (authFile.exists()) {
                try (FileReader reader = new FileReader(authFile)) {
                    Type type = new TypeToken<Map<String, String>>() {}.getType();
                    Map<String, String> parsedMap = gson.fromJson(reader, type);
                    if (parsedMap != null) {
                        map.putAll(parsedMap);
                    }
                }
            }
            map.remove(username);
            try (FileWriter writer = new FileWriter(authFile)) {
                gson.toJson(map, writer);
            }
        } catch (Exception e) {
            logger.warning("[UnRegister] Failed to remove stored token: " + e.getMessage());
        }
    }
}