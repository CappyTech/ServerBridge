package com.sovereigncraft.matrix;

import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class MatrixUserTracker {
    private final File dataFile;
    private final Gson gson = new Gson();
    private Map<UUID, String> userMap = new HashMap<>();

    public MatrixUserTracker(File pluginFolder) {
        this.dataFile = new File(pluginFolder, "matrix_users.json");
        load();
    }

    private void load() {
        if (!dataFile.exists()) return;
        try (FileReader reader = new FileReader(dataFile)) {
            userMap = gson.fromJson(reader, new TypeToken<Map<UUID, String>>(){}.getType());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void save() {
        try (FileWriter writer = new FileWriter(dataFile)) {
            gson.toJson(userMap, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void record(Player player) {
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        if (!name.equals(userMap.get(uuid))) {
            userMap.put(uuid, name);
            save();
        }
    }

    public String getMatrixUsername(UUID uuid) {
        return userMap.get(uuid);
    }

    public boolean hasRecord(UUID uuid) {
        return userMap.containsKey(uuid);
    }

    public void remove(UUID uuid) {
        if (userMap.remove(uuid) != null) {
            save();
        }
    }
}
