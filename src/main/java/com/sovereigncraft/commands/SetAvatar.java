package com.sovereigncraft.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class SetAvatar implements CommandExecutor {

    private final String homeserver;
    private final String accessToken;

    public SetAvatar(String homeserver, String accessToken) {
        this.homeserver = homeserver;
        this.accessToken = accessToken;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        String username = player.getName();

        new Thread(() -> {
            try {
                // Get Minecraft skin render (head only)
                String imageUrl = "https://mc-heads.net/avatar/" + username + "/128";
                InputStream imageStream = new URL(imageUrl).openStream();
                byte[] imageBytes = imageStream.readAllBytes();
                imageStream.close();

                // Upload to Matrix media repo
                URL uploadUrl = new URL(homeserver + "/_matrix/media/v3/upload?filename=" + username + ".png");
                HttpURLConnection uploadConn = (HttpURLConnection) uploadUrl.openConnection();
                uploadConn.setRequestMethod("POST");
                uploadConn.setRequestProperty("Authorization", "Bearer " + accessToken);
                uploadConn.setRequestProperty("Content-Type", "image/png");
                uploadConn.setDoOutput(true);

                try (OutputStream os = uploadConn.getOutputStream()) {
                    os.write(imageBytes);
                }

                InputStream responseStream = uploadConn.getInputStream();
                String response = new String(responseStream.readAllBytes());
                responseStream.close();

                // Use Gson for safe JSON parsing
                JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
                String contentUri = jsonResponse.get("content_uri").getAsString();

                // Set avatar using returned mxc:// URI
                String matrixUserId = "@" + username + ":" + new URL(homeserver).getHost();
                URL avatarUrl = new URL(homeserver + "/_matrix/client/v3/profile/" + matrixUserId + "/avatar_url");
                HttpURLConnection avatarConn = (HttpURLConnection) avatarUrl.openConnection();
                avatarConn.setRequestMethod("PUT");
                avatarConn.setRequestProperty("Authorization", "Bearer " + accessToken);
                avatarConn.setRequestProperty("Content-Type", "application/json");
                avatarConn.setDoOutput(true);

                JsonObject avatarPayload = new JsonObject();
                avatarPayload.addProperty("avatar_url", contentUri);

                try (OutputStream os = avatarConn.getOutputStream()) {
                    os.write(avatarPayload.toString().getBytes());
                }

                int avatarResponseCode = avatarConn.getResponseCode();

                Bukkit.getScheduler().runTask(
                    Bukkit.getPluginManager().getPlugin("ServerBridge"),
                    () -> {
                        if (avatarResponseCode == 200) {
                            player.sendMessage("§a✅ Matrix avatar updated to your Minecraft skin.");
                        } else {
                            player.sendMessage("§c❌ Failed to update Matrix avatar. HTTP " + avatarResponseCode);
                        }
                    }
                );

            } catch (Exception e) {
                Bukkit.getScheduler().runTask(
                    Bukkit.getPluginManager().getPlugin("ServerBridge"),
                    () -> player.sendMessage("§c❌ Error setting avatar: " + e.getMessage())
                );
            }
        }).start();

        return true;
    }
}