package mkypr.pack;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.util.*;

public final class Pack extends JavaPlugin implements Listener {

    public HashMap<String, String> properties;
    private final int INTERVAL = 100; // Run every 5 seconds

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    void forceTexturePackPlayer(Player p, byte[] hash, String pack) {
        p.setResourcePack(pack, hash);
    }

    void forceTexturePackAll() {
        byte[] hash = hexStringToByteArray(properties.get("resource-pack-sha1"));
        String pack = properties.get("resource-pack");
        for (Player p : Bukkit.getOnlinePlayers()) {
            forceTexturePackPlayer(p, hash, pack);
        }
    }

    HashMap<String, String> readProperties() {
        File f = new File("server.properties");
        HashMap<String, String> tmpProps = new HashMap<>();
        try {
            Scanner reader = new Scanner(f);
            while (reader.hasNextLine()) {
                String data = reader.nextLine();
                String[] sbuf = data.split("=");
                if (sbuf.length == 2) {
                    tmpProps.put(sbuf[0], sbuf[1]);
                }
            }
            return tmpProps;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    void startRunnable() {
        new BukkitRunnable() {
            @Override
            public void run() {
                HashMap<String, String> tmpProps = readProperties();
                if (tmpProps != null) {
                    Set<String> keys = tmpProps.keySet();
                    Set<String> old_keys = properties.keySet();
                    Set<String> new_keys = new HashSet<>(keys);
                    new_keys.removeAll(old_keys);
                    if (new_keys.size() == 0) {
                        for (String key : keys) {
                            if (!tmpProps.get(key).equals(properties.get(key))) {
                                System.out.println(key);
                                if (key.equals("resource-pack-sha1")) {
                                    properties = tmpProps;
                                    forceTexturePackAll();
                                    break;
                                }
                            }
                        }
                    } else {
                        //TODO: Implement when new key appears in server.properties
                    }

                }
            }
        }.runTaskTimerAsynchronously(this, INTERVAL, INTERVAL);
    }

    @EventHandler
    void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        byte[] hash = hexStringToByteArray(properties.get("resource-pack-sha1"));
        String pack = properties.get("resource-pack");
        forceTexturePackPlayer(p, hash, pack);

    }

    @EventHandler
    void onPackStatus(PlayerResourcePackStatusEvent e) {
        byte[] hash;
        String pack;
        switch (e.getStatus()) {
            case DECLINED:
                e.getPlayer().kickPlayer("Fuck you nigga");
                hash = hexStringToByteArray(properties.get("resource-pack-sha1"));
                pack = properties.get("resource-pack");
                forceTexturePackPlayer(e.getPlayer(), hash, pack);
                break;
            case FAILED_DOWNLOAD:
                hash = hexStringToByteArray(properties.get("resource-pack-sha1"));
                pack = properties.get("resource-pack");
                forceTexturePackPlayer(e.getPlayer(), hash, pack);
                break;
            case ACCEPTED:
                break;
            case SUCCESSFULLY_LOADED:
                break;
            default:
                break;
        }
    }

    @Override
    public void onEnable() {
        properties = readProperties();
        startRunnable();
        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(this, this);

    }

    @Override
    public void onDisable() {

    }
}
