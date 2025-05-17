package com.sovereigncraft.commands;

import com.sovereigncraft.matrix.MatrixUserTracker;
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
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class Register implements CommandExecutor {

    private final String homeserver;
    private final MatrixUserTracker userTracker;
    private final File authFile;
    private final Gson gson = new Gson();

    public Register(String homeserver, MatrixUserTracker userTracker, File dataFolder) {
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
            player.sendMessage("§cUsage: /chat register <password>");
            return true;
        }

        String username = player.getName();
        String password = args[0];

        new Thread(() -> {
            try {
                URL url = new URL(homeserver + "/_matrix/client/v3/register");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                String json = "{" +
                        "\"auth\": {\"type\": \"m.login.dummy\"}," +
                        "\"username\": \"" + username + "\"," +
                        "\"password\": \"" + password + "\"" +
                        "}";

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes());
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 200 || responseCode == 201) {
                    String accessToken = new String(conn.getInputStream().readAllBytes());
                    String tokenValue = extractBetween(accessToken, "\"access_token\":\"", "\"");

                    if (tokenValue != null) {
                        saveAccessToken(username, tokenValue);
                    }

                    player.sendMessage("§a✅ Matrix account registered successfully.");
                    userTracker.record(player);
                } else {
                    player.sendMessage("§c❌ Registration failed. Code: " + responseCode);
                }

                conn.disconnect();
            } catch (Exception e) {
                player.sendMessage("§c❌ Error registering Matrix account: " + e.getMessage());
            }
        }).start();

        return true;
    }

    private void saveAccessToken(String username, String token) {
        try {
            Map<String, String> map = new HashMap<>();
            if (authFile.exists()) {
                try (FileReader reader = new FileReader(authFile)) {
                    map = gson.fromJson(reader, new TypeToken<Map<String, String>>(){}.getType());
                }
            }
            map.put(username, token);
            try (FileWriter writer = new FileWriter(authFile)) {
                gson.toJson(map, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String extractBetween(String text, String start, String end) {
        int s = text.indexOf(start);
        if (s == -1) return null;
        s += start.length();
        int e = text.indexOf(end, s);
        if (e == -1) return null;
        return text.substring(s, e);
    }
}
