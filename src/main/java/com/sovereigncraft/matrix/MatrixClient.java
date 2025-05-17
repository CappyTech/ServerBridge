package com.sovereigncraft.matrix;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

public class MatrixClient {
    private final String homeserver;
    private final String accessToken;
    private final String roomId;
    private final String userId;
    private final Logger logger;
    private final JavaPlugin plugin;
    private final File tokenFile;
    private String syncToken;
    private final Map<String, Integer> powerLevels = new HashMap<>();

    public MatrixClient(ConfigurationSection config, Logger logger, JavaPlugin plugin) {
        this.homeserver = config.getString("homeserver");
        this.accessToken = config.getString("access_token");
        this.userId = config.getString("user_id");
        this.roomId = config.getString("room_id");
        this.logger = logger;
        this.plugin = plugin;
        this.tokenFile = new File(plugin.getDataFolder(), "sync_token.txt");
        this.syncToken = readSyncTokenFromFile();
    }

    public void sendMessage(String message) {
        new Thread(() -> {
            try {
                String txnId = String.valueOf(System.currentTimeMillis());
                String endpoint = homeserver + "/_matrix/client/v3/rooms/" + roomId + "/send/m.room.message/" + txnId;

                String escapedMessage = message.replace("\"", "\\\"");
                String json = "{"
                        + "\"msgtype\":\"m.text\","
                        + "\"body\":\"" + escapedMessage + "\""
                        + "}";

                URL url = new URL(endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes());
                }

                int code = conn.getResponseCode();
                if (code != 200 && code != 202) {
                    logger.warning("Failed to send message to Matrix: " + code);
                }

                conn.disconnect();
            } catch (Exception e) {
                logger.severe("Matrix sendMessage failed: " + e.getMessage());
            }
        }).start();
    }

    public void startPollingChatToMinecraft() {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                pollMatrixMessages();
            }
        }, 2000, 5000);
    }

    private void pollMatrixMessages() {
        try {
            StringBuilder endpoint = new StringBuilder(homeserver + "/_matrix/client/v3/sync?timeout=30000");
            if (syncToken != null) {
                endpoint.append("&since=").append(syncToken);
            }

            HttpURLConnection conn = (HttpURLConnection) new URL(endpoint.toString()).openConnection();
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            String json = response.toString();
            syncToken = extractNextBatch(json);
            updatePowerLevels(json);
            handleRoomMessages(json);

            if (syncToken != null) {
                try {
                    Files.writeString(tokenFile.toPath(), syncToken, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                } catch (IOException e) {
                    logger.warning("Failed to save sync token: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            logger.warning("Matrix poll failed: " + e.getMessage());
        }
    }

    private void updatePowerLevels(String json) {
        String plBlock = extractBetween(json, "\"power_levels\":{", "},\"events\"");
        if (plBlock == null) return;

        String[] entries = plBlock.split(",");
        for (String entry : entries) {
            if (entry.contains("\":")) {
                String[] kv = entry.split("\":");
                if (kv.length == 2) {
                    String user = kv[0].replace("\"", "").trim();
                    try {
                        int level = Integer.parseInt(kv[1].trim());
                        powerLevels.put(user, level);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
    }

    private void handleRoomMessages(String json) {
        try {
            if (!json.contains("\"rooms\"")) return;

            String[] parts = json.split("\"type\":\"m.room.message\"");
            for (String part : parts) {
                if (part.contains("\"body\":\"")) {
                    String sender = extractBetween(part, "\"sender\":\"", "\"");
                    String body = extractBetween(part, "\"body\":\"", "\"");

                    if (body == null || sender == null) continue;
                    if (sender.equals(userId)) continue;

                    int level = powerLevels.getOrDefault(sender, 0);
                    String role = "§7User";
                    if (level >= 100) role = "§eAdmin";
                    else if (level >= 50) role = "§9Mod";

                    String prefix = String.format("§8[§7Matrix §8- %s§8]", role);
                    String msg = String.format("%s §7%s§f: %s", prefix, sender, body);

                    Bukkit.getScheduler().runTask(plugin, () -> Bukkit.broadcastMessage(msg));
                }
            }
        } catch (Exception e) {
            logger.warning("Matrix message parse failed: " + e.getMessage());
        }
    }

    private String extractNextBatch(String json) {
        int idx = json.indexOf("\"next_batch\":\"");
        if (idx == -1) return syncToken;
        int start = idx + 14;
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }

    private String extractBetween(String text, String start, String end) {
        int s = text.indexOf(start);
        if (s == -1) return null;
        s += start.length();
        int e = text.indexOf(end, s);
        if (e == -1) return null;
        return text.substring(s, e);
    }

    private String readSyncTokenFromFile() {
        if (!tokenFile.exists()) return null;
        try {
            return Files.readString(tokenFile.toPath()).trim();
        } catch (IOException e) {
            logger.warning("Could not read sync token from file: " + e.getMessage());
            return null;
        }
    }
}
