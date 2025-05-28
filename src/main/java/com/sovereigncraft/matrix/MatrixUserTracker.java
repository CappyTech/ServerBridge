package com.sovereigncraft.matrix;

import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class MatrixUserTracker {
    private final File dataFile;
    private final Gson gson = new Gson();
    private Map<String, String> userMap = new HashMap<>();
    private final Logger logger;

    public MatrixUserTracker(File pluginFolder, Logger logger) {
        this.dataFile = new File(pluginFolder, "matrix_users.json");
        this.logger = logger;
        load();
    }

    private synchronized void load() {
        if (!dataFile.exists()) return;
        try (FileReader reader = new FileReader(dataFile)) {
            Map<String, String> loaded = gson.fromJson(reader, new TypeToken<Map<String, String>>(){}.getType());
            if (loaded != null) {
                userMap = loaded;
            }
        } catch (Exception e) {
            logger.warning("[MatrixUserTracker] Failed to load user map: " + e.getMessage());
        }
    }

    private synchronized void save() {
        try (FileWriter writer = new FileWriter(dataFile)) {
            gson.toJson(userMap, writer);
        } catch (Exception e) {
            logger.warning("[MatrixUserTracker] Failed to save user map: " + e.getMessage());
        }
    }

    public synchronized void record(Player player) {
        String uuidStr = player.getUniqueId().toString();
        String name = player.getName();
        if (!name.equals(userMap.get(uuidStr))) {
            userMap.put(uuidStr, name);
            save();
        }
    }

    public synchronized String getMatrixUsername(UUID uuid) {
        return userMap.get(uuid.toString());
    }

    public synchronized boolean hasRecord(UUID uuid) {
        return userMap.containsKey(uuid.toString());
    }

    public synchronized void remove(UUID uuid) {
        if (userMap.remove(uuid.toString()) != null) {
            save();
        }
    }
}