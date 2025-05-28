package com.sovereigncraft.commands;

import com.sovereigncraft.matrix.MatrixUserTracker;
import com.sovereigncraft.matrix.MatrixAdminUserCreator;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileWriter;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.logging.Logger;

public class Register implements CommandExecutor {

    private final String homeserver;
    private final MatrixUserTracker userTracker;
    private final File authFile;
    private final Gson gson = new Gson();
    private final String adminToken;
    private final Logger logger;

    public Register(String homeserver, String adminToken, MatrixUserTracker userTracker, File dataFolder, Logger logger) {
        this.homeserver = homeserver;
        this.adminToken = adminToken;
        this.userTracker = userTracker;
        this.authFile = new File(dataFolder, "matrix_auth.json");
        this.logger = logger;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        // No password argument — we generate it automatically
        String username = player.getName().toLowerCase();
        String password = generateRandomPassword(12);

        new Thread(() -> {
            try {
                MatrixAdminUserCreator adminCreator = new MatrixAdminUserCreator(homeserver, adminToken, logger);
                boolean created = adminCreator.createUser(username, password, player.getName());

                Bukkit.getScheduler().runTask(
                    Bukkit.getPluginManager().getPlugin("ServerBridge"),
                    () -> {
                        if (created) {
                            saveLoginInfo(username, password);
                            player.sendMessage("§aYou have been signed up to §b" + homeserver);
                            player.sendMessage("§7Here is your login information:");
                            player.sendMessage("§eUsername: §f" + username);
                            player.sendMessage("§ePassword: §f" + password);
                            player.sendMessage("§6Login here: §b" + homeserver);

                            userTracker.record(player);
                        } else {
                            player.sendMessage("§c❌ Failed to create Matrix user.");
                        }
                    }
                );
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(
                    Bukkit.getPluginManager().getPlugin("ServerBridge"),
                    () -> player.sendMessage("§c❌ Error registering Matrix account: " + e.getMessage())
                );
            }
        }).start();

        return true;
    }

    private String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private void saveLoginInfo(String username, String password) {
        try {
            Map<String, Map<String, String>> map = new HashMap<>();
            if (authFile.exists()) {
                try (var reader = new java.io.FileReader(authFile)) {
                    Map<String, Map<String, String>> existing = gson.fromJson(reader, new TypeToken<Map<String, Map<String, String>>>() {}.getType());
                    if (existing != null) map.putAll(existing);
                }
            }
            Map<String, String> loginInfo = new HashMap<>();
            loginInfo.put("password", password);
            map.put(username, loginInfo);

            try (var writer = new FileWriter(authFile)) {
                gson.toJson(map, writer);
            }
        } catch (Exception e) {
            logger.warning("[Register] Failed to save login info: " + e.getMessage());
        }
    }
}