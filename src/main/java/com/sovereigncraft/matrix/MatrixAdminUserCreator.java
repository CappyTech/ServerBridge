package com.sovereigncraft.matrix;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MatrixAdminUserCreator {

    private final String homeserver;
    private final String adminToken;

    public MatrixAdminUserCreator(String homeserver, String adminToken) {
        this.homeserver = homeserver;
        this.adminToken = adminToken;
    }

    /**
     * Creates a new Matrix user with the given username, password, and display name.
     * Returns true if creation was successful (HTTP 200 or 201), false otherwise.
     */
    public boolean createUser(String username, String password, String displayName) {
        try {
            // Format the user ID with the homeserver domain
            String userId = "@" + username + ":" + homeserver.replace("https://", "").replace("http://", "");

            String urlStr = homeserver + "/_synapse/admin/v2/users/" + userId;
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + adminToken);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // JSON payload
            String jsonPayload = String.format(
                "{\"password\": \"%s\", \"displayname\": \"%s\"}",
                password, displayName
            );

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            conn.disconnect();

            return (responseCode == 200 || responseCode == 201);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
