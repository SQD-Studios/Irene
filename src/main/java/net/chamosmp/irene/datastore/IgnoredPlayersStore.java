package net.chamosmp.irene.datastore;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import net.chamosmp.sqdlib.paper.util.LoggerUtil;
import net.chamosmp.sqdlib.util.LogType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class IgnoredPlayersStore {

    private final Type type = new TypeToken<Map<String, List<String>>>() {
    }.getType();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File file;

    public IgnoredPlayersStore(Plugin plugin) throws IOException {
        File folder = new File(plugin.getDataFolder(), "data");
        if (!folder.exists() && !folder.mkdirs()) {
            throw new IOException("Could not create " + folder);
        }

        file = new File(folder, "ignoredplayers.json");
        if (!file.exists()) {
            save(new HashMap<>());
        }

        LoggerUtil.log(LogType.INFO, "Successfully loaded ignoredplayers data!");
    }

    public synchronized void add(Player player, Player toIgnore) throws IOException {
        Map<String, List<String>> data = load();
        List<String> list = data.computeIfAbsent(player.getUniqueId().toString(), k -> new ArrayList<>());
        String target = toIgnore.getUniqueId().toString();
        if (!list.contains(target)) {
            list.add(target);
            save(data);
        }
    }

    public synchronized void remove(Player player, Player toRemove) throws IOException {
        Map<String, List<String>> data = load();
        String key = player.getUniqueId().toString();
        List<String> list = data.get(key);
        if (list != null && list.remove(toRemove.getUniqueId().toString())) {
            if (list.isEmpty()) {
                data.remove(key);
            }
            save(data);
        }
    }

    public synchronized Map<OfflinePlayer, List<OfflinePlayer>> get() throws IOException {
        Map<OfflinePlayer, List<OfflinePlayer>> result = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : load().entrySet()) {
            OfflinePlayer owner = Bukkit.getOfflinePlayer(UUID.fromString(entry.getKey()));
            List<OfflinePlayer> ignored = entry.getValue().stream()
                    .map(id -> Bukkit.getOfflinePlayer(UUID.fromString(id)))
                    .toList();
            result.put(owner, ignored);
        }
        return result;
    }

    private Map<String, List<String>> load() throws IOException {
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            Map<String, List<String>> data = gson.fromJson(reader, type);
            return data != null ? data : new HashMap<>();   // empty file -> null
        } catch (JsonParseException e) {
            LoggerUtil.log(LogType.WARNING, "ignoredplayers.json was invalid, starting fresh: " + e.getMessage());
            return new HashMap<>();
        }
    }

    private void save(Map<String, List<String>> data) throws IOException {
        try (Writer writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            gson.toJson(data, type, writer);
        }
    }
}