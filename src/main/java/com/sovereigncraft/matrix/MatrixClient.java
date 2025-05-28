package com.sovereigncraft.matrix;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import com.google.gson.*;

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
    private Timer timer;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final Gson gson = new Gson();

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
        executor.submit(() -> {
            try {
                String txnId = String.valueOf(System.currentTimeMillis());
                String endpoint = homeserver + "/_matrix/client/v3/rooms/" + roomId + "/send/m.room.message/" + txnId;

                JsonObject payload = new JsonObject();
                payload.addProperty("msgtype", "m.text");
                payload.addProperty("body", message);

                URL url = new URL(endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(gson.toJson(payload).getBytes());
                }

                int code = conn.getResponseCode();
                if (code != 200 && code != 202) {
                    logger.warning("Failed to send message to Matrix: " + code);
                }

                conn.disconnect();
            } catch (Exception e) {
                logger.severe("Matrix sendMessage failed: " + e.getMessage());
            }
        });
    }

    public void startPollingChatToMinecraft() {
        timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                pollMatrixMessages();
            }
        }, 2000, 5000);
    }

    public void shutdown() {
        if (timer != null) {
            timer.cancel();
        }
        executor.shutdownNow();
    }

    private void pollMatrixMessages() {
        try {
            StringBuilder endpoint = new StringBuilder(homeserver + "/_matrix/client/v3/sync?timeout=30000");
            if (syncToken != null) {
                endpoint.append("&since=").append(syncToken);
            }

            HttpURLConnection conn = (HttpURLConnection) new URL(endpoint.toString()).openConnection();
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                logger.warning("Matrix sync poll failed with HTTP code: " + responseCode);
                conn.disconnect();
                return;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            conn.disconnect();

            String json = response.toString();
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            syncToken = root.has("next_batch") ? root.get("next_batch").getAsString() : syncToken;
            updatePowerLevels(root);
            handleRoomMessages(root);

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

    private void updatePowerLevels(JsonObject root) {
        try {
            if (!root.has("rooms")) return;
            JsonObject rooms = root.getAsJsonObject("rooms");
            if (!rooms.has("join")) return;
            JsonObject join = rooms.getAsJsonObject("join");
            if (!join.has(roomId)) return;
            JsonObject room = join.getAsJsonObject(roomId);
            if (!room.has("state")) return;
            JsonObject state = room.getAsJsonObject("state");
            if (!state.has("events")) return;
            for (JsonElement eventElem : state.getAsJsonArray("events")) {
                JsonObject event = eventElem.getAsJsonObject();
                if (event.has("type") && event.get("type").getAsString().equals("m.room.power_levels")) {
                    if (event.has("content")) {
                        JsonObject content = event.getAsJsonObject("content");
                        if (content.has("users")) {
                            JsonObject users = content.getAsJsonObject("users");
                            for (Map.Entry<String, JsonElement> entry : users.entrySet()) {
                                powerLevels.put(entry.getKey(), entry.getValue().getAsInt());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to parse power levels: " + e.getMessage());
        }
    }

    private void handleRoomMessages(JsonObject root) {
        try {
            if (!root.has("rooms")) return;
            JsonObject rooms = root.getAsJsonObject("rooms");
            if (!rooms.has("join")) return;
            JsonObject join = rooms.getAsJsonObject("join");
            if (!join.has(roomId)) return;
            JsonObject room = join.getAsJsonObject(roomId);
            if (!room.has("timeline")) return;
            JsonObject timeline = room.getAsJsonObject("timeline");
            if (!timeline.has("events")) return;
            for (JsonElement eventElem : timeline.getAsJsonArray("events")) {
                JsonObject event = eventElem.getAsJsonObject();
                if (!event.has("type") || !event.get("type").getAsString().equals("m.room.message")) continue;
                if (!event.has("content")) continue;
                JsonObject content = event.getAsJsonObject("content");
                if (!content.has("body")) continue;
                String sender = event.has("sender") ? event.get("sender").getAsString() : null;
                String body = content.get("body").getAsString();
                if (sender == null || sender.equals(userId)) continue;

                int level = powerLevels.getOrDefault(sender, 0);
                String role = "§7User";
                if (level >= 100) role = "§eAdmin";
                else if (level >= 50) role = "§9Mod";

                String prefix = String.format("§8[§7Matrix §8- %s§8]", role);
                String msg = String.format("%s §7%s§f: %s", prefix, sender, body);

                Bukkit.getScheduler().runTask(plugin, () -> Bukkit.broadcastMessage(msg));
            }
        } catch (Exception e) {
            logger.warning("Matrix message parse failed: " + e.getMessage());
        }
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