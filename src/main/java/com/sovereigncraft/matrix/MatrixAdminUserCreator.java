package com.sovereigncraft.matrix;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import java.util.logging.Logger;
import com.google.gson.JsonObject;

public class MatrixAdminUserCreator {

    private final String homeserver;
    private final String adminToken;
    private final Logger logger;

    public MatrixAdminUserCreator(String homeserver, String adminToken, Logger logger) {
        this.homeserver = homeserver;
        this.adminToken = adminToken;
        this.logger = logger;
    }

    /**
     * Creates a new Matrix user with the given username, password, and display name.
     * Returns true if creation was successful (HTTP 200 or 201), false otherwise.
     */
    public boolean createUser(String username, String password, String displayName) {
        try {
            // Normalize homeserver for userId (strip protocol and trailing slash)
            String domain = homeserver.replaceFirst("^https?://", "").replaceAll("/$", "");
            String userId = "@" + username + ":" + domain;
            String encodedUserId = URLEncoder.encode(userId, StandardCharsets.UTF_8.toString());

            String urlStr = homeserver.replaceAll("/$", "") + "/_synapse/admin/v2/users/" + encodedUserId;
            logger.info("[DEBUG] Creating Matrix user with URL: " + urlStr);

            // Use Gson for safe JSON construction
            JsonObject payload = new JsonObject();
            payload.addProperty("password", password);
            payload.addProperty("displayname", displayName);

            logger.info("[DEBUG] Sending Matrix user creation request (payload hidden for security)");

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Authorization", "Bearer " + adminToken);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            logger.info("[DEBUG] Response Code: " + responseCode);

            try (InputStream stream = (responseCode >= 200 && responseCode < 300) ?
                    conn.getInputStream() : conn.getErrorStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {

                String responseBody = reader.lines().collect(Collectors.joining("\n"));
                logger.info("[DEBUG] Response Body: " + responseBody);
            }

            conn.disconnect();

            return (responseCode == 200 || responseCode == 201);
        } catch (Exception e) {
            logger.log(java.util.logging.Level.WARNING, "[ERROR] Exception during Matrix user creation", e);
            return false;
        }
    }
}