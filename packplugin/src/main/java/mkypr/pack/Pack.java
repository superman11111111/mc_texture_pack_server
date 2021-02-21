package mkypr.pack;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Paths;
import java.util.*;

import static mkypr.pack.Utils.getIp;
import static mkypr.pack.Utils.hexStringToByteArray;

public final class Pack extends JavaPlugin implements Listener {

    public static Pack instance;

    public HashMap<String, String> properties;
    private final int INTERVAL = 100; // Run every 5 seconds
    private final int MAX_RETRIES = 5;
    private final int RP_LOAD_DELAY = 100; // Works better with higher delay
    private final int PORT = 3333;
    private String IP;
    private final HashMap<UUID, Integer> player_retry_counts = new HashMap<>();
    private final HashMap<UUID, DOWNLOAD_STATUS> player_download_status = new HashMap<>();
    private final Gson gson = new Gson();
    private File player_ips_file;
    private File tele_backstack_file;
    private File command_history_file;
    public HashMap<String, HashMap<String, String>> player_ips;
    public HashMap<String, LinkedList<Map<String, Object>>> tele_backstack;
    public HashMap<String, LinkedList<String>> command_history;
    private final Type player_ips_type = new TypeToken<HashMap<String, HashMap<String, String>>>() {
    }.getType();
    private final Type tele_backstack_type = new TypeToken<HashMap<String, LinkedList<Map<String, Object>>>>() {
    }.getType();
    private final Type command_history_type = new TypeToken<HashMap<String, LinkedList<String>>>() {
    }.getType();

    private File flachwitze_file;
    public String[] flachwitze;

    private File witze_file;
    public HashMap<String, HashMap<String, List<String>>> witze;

    private enum DOWNLOAD_STATUS {
        SUCCESS(1),
        FAILURE(2),
        IS_DOWNLOADING(3),
        NULL(0);

        protected int status;

        DOWNLOAD_STATUS(int status) {
            this.status = status;
        }

        public int getStatus() {
            return status;
        }
    }


    void kickPlayerFromAsync(Player p, String msg) {
        Bukkit.getScheduler().runTask(this, () -> p.kickPlayer(msg));
    }

    void forceTexturePackPlayerLater(Player p, String pack, byte[] hash) {
        System.out.printf("%s: Reloading texture pack %s%n", p.getName(), pack);
        UUID uuid = p.getUniqueId();
        player_retry_counts.put(uuid, 0);
        player_download_status.put(uuid, DOWNLOAD_STATUS.NULL);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player_retry_counts.get(uuid) >= MAX_RETRIES) {
                    if (properties.get("resource-pack").isEmpty()) {
                        kickPlayerFromAsync(p, "Reset texture pack");
                    } else {
                        p.sendMessage("Please rejoin to reload the texture pack!");
                    }
                    this.cancel();
                    return;
                }
                switch (player_download_status.get(uuid)) {
                    case SUCCESS:
                        p.sendMessage("Texture pack successfully loaded!");
                        this.cancel();
                        return;
                    case IS_DOWNLOADING:
                        break;
                    case NULL:
                    case FAILURE:
                    default:
//                        System.out.printf("%s: Try #%d%n", p.getName(), player_retry_counts.get(uuid));
                        player_download_status.put(uuid, DOWNLOAD_STATUS.IS_DOWNLOADING);
                        if (hash.length == 0) {
                            p.setResourcePack(pack);
                        } else {
                            p.setResourcePack(pack, hash);
                        }
                        break;
                }
            }
        }.runTaskTimerAsynchronously(this, 0, 0);
    }

    void forceTexturePackAllLater() {
        byte[] hash = hexStringToByteArray(properties.get("resource-pack-sha1"));
        String pack = properties.get("resource-pack");

        for (Player p : Bukkit.getOnlinePlayers()) {
            forceTexturePackPlayerLater(p, pack, hash);
        }
    }

    HashMap<String, String> readProperties() {
        File f = new File("server.properties");
        HashMap<String, String> tmpProps = new HashMap<>();
        try {
            Scanner reader = new Scanner(f);
            while (reader.hasNextLine()) {
                String data = reader.nextLine();
                String[] sbuf = data.split("=", -1);
                if (sbuf.length > 1) {
                    tmpProps.put(sbuf[0], sbuf[1]);
                }
            }
            return tmpProps;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    boolean sameKeySets(HashMap m1, HashMap m2) {
//        System.out.println(m1);
        for (Object key : m2.keySet()) {
            if (!m1.containsKey(key)) {
                System.out.println(key);
                return false;
            }
        }
//        System.out.println(m2);
        for (Object key : m1.keySet()) {
            if (!m2.containsKey(key)) {
                System.out.println(key);
                return false;
            }
        }
        return true;
    }

    void startRunnable() {
        new BukkitRunnable() {
            @Override
            public void run() {
                HashMap<String, String> tmpProps = readProperties();
//                System.out.printf("%s %s%n", tmpProps.get("resource-pack"), properties.get("resource-pack"));
//                System.out.println(sameKeySets(properties, tmpProps));
//                System.out.println(Objects.equals(tmpProps.get("resource-pack"), properties.get("resource-pack")));

                if (tmpProps != null) {
                    if (sameKeySets(properties, tmpProps)) {
                        for (String key : tmpProps.keySet()) {
                            if (!Objects.equals(tmpProps.get(key), properties.get(key))) {
//                                System.out.println(key);
                                if (key.equals("resource-pack-sha1")) {
                                    properties = tmpProps;
//                                    if (properties.get(key).isEmpty()) {
//                                        Bukkit.broadcastMessage("Reset texture pack!");
//                                    } else {
//                                        Bukkit.broadcastMessage("New texture pack!");
//                                    }
                                    forceTexturePackAllLater();
                                    break;
                                }
                            }
                        }
                    } else {
                        //TODO: Implement when new key appears in server.properties
                    }
                }
            }
        }.runTaskTimerAsynchronously(this, 0, INTERVAL);
    }

    @EventHandler
    void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();

        HashMap<String, String> m = new HashMap<>();
        m.put("ip", p.getAddress().getAddress().getHostAddress());
        m.put("name", p.getName());
        player_ips.put(p.getUniqueId().toString(), m);
        tele_backstack.computeIfAbsent(p.getUniqueId().toString(), k -> new LinkedList<>());
        command_history.computeIfAbsent(p.getUniqueId().toString(), k -> new LinkedList<>());


        String url = String.format("http://%s:%d", IP, PORT);
        TextComponent message = new TextComponent(String.format("Want to change the server resource pack?\nGo to %s", url));
        message.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
        p.spigot().sendMessage(message);

        e.setJoinMessage("Use /help Pack for more commands");

        if (!properties.get("resource-pack").isEmpty()) {
            byte[] hash = hexStringToByteArray(properties.get("resource-pack-sha1"));
            String pack = properties.get("resource-pack");
            forceTexturePackPlayerLater(p, pack, hash);
        }

    }

    @EventHandler
    void onPackStatus(PlayerResourcePackStatusEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        switch (e.getStatus()) {
            case DECLINED:
                e.getPlayer().kickPlayer("Fuck you nigga");
                break;
            case FAILED_DOWNLOAD:
                player_retry_counts.put(uuid, player_retry_counts.get(uuid) + 1);
                player_download_status.put(uuid, DOWNLOAD_STATUS.FAILURE);
                break;
            case ACCEPTED:
                break;
            case SUCCESSFULLY_LOADED:
                player_download_status.put(uuid, DOWNLOAD_STATUS.SUCCESS);
                break;
            default:
                break;
        }
    }

    @EventHandler
    void onCommandPreProcess(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();
        if (!e.isCancelled()) {
            command_history.get(p.getUniqueId().toString()).add(e.getMessage().split(" ")[0]);
        }
    }

    @EventHandler
    void onTeleport(PlayerTeleportEvent e) {
        Player p = e.getPlayer();
        if (!command_history.get(p.getUniqueId().toString()).getLast().equals("/back"))
            tele_backstack.get(e.getPlayer().getUniqueId().toString()).add(p.getLocation().serialize());
    }

    Object readFileToJson(File file, Type type) {
        Object obj = null;
        try {
            FileReader fr = new FileReader(file);
            JsonReader r = new JsonReader(fr);
            obj = gson.fromJson(r, type);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return obj;
    }

    void writeJsonToFile(Object obj, File file) {
        try {
            FileWriter fw = new FileWriter(file);
            fw.write(gson.toJson(obj));
            fw.flush();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void startFlachwitzeDownload() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("https://raw.githubusercontent.com/derphilipp/Flachwitze/master/README.md");
                    ReadableByteChannel rbc = Channels.newChannel(url.openStream());
                    FileOutputStream fos = new FileOutputStream(flachwitze_file);
                    fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                    reloadFlachwitzeFromDisk();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(this);

    }

    void moreWitze() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(String.format("http://127.0.0.1:1234/", IP));
                    ReadableByteChannel rbc = Channels.newChannel(url.openStream());
                    FileOutputStream fos = new FileOutputStream(witze_file);
                    fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                    reloadWitze();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(this);

    }

    void reloadWitze() {
        try {
            JsonReader r = new JsonReader(new FileReader(witze_file));
            witze = gson.fromJson(r, new TypeToken<HashMap<String, HashMap<String, List<String>>>>() {
            }.getType());
            System.out.printf("More Witze loaded!%n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    void reloadFlachwitzeFromDisk() {
        try {
            FileReader fr = new FileReader(flachwitze_file);
            char[] buf = new char[4096];
            fr.read(buf);
            String[] tmp;
            tmp = new String(buf).split("\n");
            tmp = Arrays.copyOfRange(tmp, 7, tmp.length);
            flachwitze = new String[tmp.length];
            for (int i = 0; i < tmp.length; i++) {
                String[] sbuf = tmp[i].split("- ");
                flachwitze[i] = sbuf[1];
            }
            System.out.printf("%d Flachwitze loaded!%n", flachwitze.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getIP() {
        new BukkitRunnable() {
            int c = 0;
            @Override
            public void run() {
                while (IP == null) {
                    if (c >= 5) {
                        return;
                    }
                    IP = getIp();
                    c++;
                }
            }
        }.runTaskAsynchronously(this);
    }

    @Override
    public void onEnable() {
        instance = this;

        getIP();

        getDataFolder().mkdir();
        player_ips_file = Paths.get(getDataFolder().getAbsolutePath(), "ips.json").toFile();
        tele_backstack_file = Paths.get(getDataFolder().getAbsolutePath(), "tele_backstack.json").toFile();
        command_history_file = Paths.get(getDataFolder().getAbsolutePath(), "command_history.json").toFile();
        flachwitze_file = Paths.get("flachwitze.txt").toFile();
        witze_file = Paths.get("witze.txt").toFile();

        player_ips = (HashMap<String, HashMap<String, String>>) readFileToJson(player_ips_file, player_ips_type);
        tele_backstack = (HashMap<String, LinkedList<Map<String, Object>>>) readFileToJson(tele_backstack_file, tele_backstack_type);
        command_history = (HashMap<String, LinkedList<String>>) readFileToJson(command_history_file, command_history_type);

        if (player_ips == null) player_ips = new HashMap<>();
        if (tele_backstack == null) tele_backstack = new HashMap<>();
        if (command_history == null) command_history = new HashMap<>();

        System.out.printf("Read from %s: %d Players%n", player_ips_file.getName(), player_ips.keySet().size());
        System.out.printf("Read from %s: %d Players%n", tele_backstack_file.getName(), tele_backstack.keySet().size());
        System.out.printf("Read from %s: %d Players%n", command_history_file.getName(), command_history.keySet().size());

        if (!flachwitze_file.exists()) startFlachwitzeDownload();
        else reloadFlachwitzeFromDisk();
        if (!witze_file.exists()) moreWitze();
        else reloadWitze();

        properties = readProperties();
        startRunnable();

        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(this, this);

        this.getCommand("ip").setExecutor(new Commands.IPCommand());
        this.getCommand("top").setExecutor(new Commands.TopCommand());
        this.getCommand("back").setExecutor(new Commands.BackCommand());
        this.getCommand("history").setExecutor(new Commands.HistoryCommand());
        this.getCommand("flachwitz").setExecutor(new Commands.FlachwitzCommand());
        this.getCommand("witz").setExecutor(new Commands.WitzCommand());
        this.getCommand("debug").setExecutor(new Commands.DebugCommand());

    }

    @Override
    public void onDisable() {
        writeJsonToFile(player_ips, player_ips_file);
        writeJsonToFile(tele_backstack, tele_backstack_file);
        writeJsonToFile(command_history, command_history_file);
    }
}
