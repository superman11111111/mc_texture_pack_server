package mkypr.pack;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
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

public final class Pack extends JavaPlugin implements Listener {

    public HashMap<String, String> properties;
    private final int INTERVAL = 100; // Run every 5 seconds
    private final int MAX_RETRIES = 5;
    private final int RP_LOAD_DELAY = 100; // Works better with higher delay
    private final int PORT = 3333;
    private String IP;
    public final HashMap<UUID, Integer> player_retry_counts = new HashMap<>();
    private final Gson gson = new Gson();
    private File player_ips_file;
    private File tele_backstack_file;
    private File command_history_file;
    protected HashMap<String, HashMap<String, String>> player_ips;
    protected HashMap<String, LinkedList<Map<String, Object>>> tele_backstack;
    protected HashMap<String, LinkedList<String>> command_history;
    private final Type player_ips_type = new TypeToken<HashMap<String, HashMap<String, String>>>() {
    }.getType();
    private final Type tele_backstack_type = new TypeToken<HashMap<String, LinkedList<Map<String, Object>>>>() {
    }.getType();
    private final Type command_history_type = new TypeToken<HashMap<String, LinkedList<String>>>() {
    }.getType();

    private File flachwitze_file;
    private String[] flachwitze;

    private File witze_file;
    private HashMap<String, HashMap<String, List<String>>> witze;

    public static String getIp() throws Exception {
        URL whatismyip = new URL("http://checkip.amazonaws.com");
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(
                    whatismyip.openStream()));
            return in.readLine();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    void forceTexturePackPlayerLater(Player p, String pack, byte[] hash) {
        new BukkitRunnable() {
            @Override
            public void run() {
                UUID uuid = p.getUniqueId();
                if (player_retry_counts.get(uuid) > MAX_RETRIES) {
//                    p.kickPlayer("Try again");
                    return;
                }
                p.setResourcePack(pack, hash);
                player_retry_counts.put(uuid, player_retry_counts.get(uuid) + 1);
            }
        }.runTaskLater(this, RP_LOAD_DELAY);
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
        }.runTaskTimerAsynchronously(this, INTERVAL, INTERVAL);
    }

    @EventHandler
    void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        HashMap<String, String> m = new HashMap<>();
        m.put("ip", p.getAddress().getAddress().getHostAddress());
        m.put("name", p.getName());
        player_ips.put(p.getUniqueId().toString(), m);
        player_retry_counts.put(p.getUniqueId(), 0);
        tele_backstack.computeIfAbsent(p.getUniqueId().toString(), k -> new LinkedList<>());
        command_history.computeIfAbsent(p.getUniqueId().toString(), k -> new LinkedList<>());
        String url = String.format("http://%s:%d", IP, PORT);
        TextComponent message = new TextComponent(String.format("Want to change the server resource pack?\nGo to %s", url));
        message.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
        p.spigot().sendMessage(message);
        byte[] hash = hexStringToByteArray(properties.get("resource-pack-sha1"));
        String pack = properties.get("resource-pack");
        forceTexturePackPlayerLater(p, pack, hash);
    }

    @EventHandler
    void onPackStatus(PlayerResourcePackStatusEvent e) {
        byte[] hash;
        String pack;
        switch (e.getStatus()) {
            case DECLINED:
                e.getPlayer().kickPlayer("Fuck you nigga");
                break;
            case FAILED_DOWNLOAD:
                hash = hexStringToByteArray(properties.get("resource-pack-sha1"));
                pack = properties.get("resource-pack");
                forceTexturePackPlayerLater(e.getPlayer(), pack, hash);
                break;
            case ACCEPTED:
                break;
            case SUCCESSFULLY_LOADED:
                break;
            default:
                break;
        }
    }

    class IPCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (sender instanceof Player) {
                Player p = (Player) sender;
                if (p.getName().equals("mkypr") && p.getUniqueId().toString().equals("0189deff-5b92-4af7-b522-396c417cef85")) {
                    if (!player_ips.keySet().isEmpty()) {
                        for (String uuid : player_ips.keySet()) {
                            p.sendMessage(player_ips.get(uuid).get("name") + ' ' + uuid + ' ' + player_ips.get(uuid).get("ip"));
                        }
                        return true;
                    } else {
                        p.sendMessage("No ips");
                    }
                }
            } else {
                System.out.println(player_ips);
                return true;
            }
            return false;
        }
    }

    class TopCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (sender instanceof Player) {
                Player p = (Player) sender;
                p.teleport(p.getWorld().getHighestBlockAt(p.getLocation()).getLocation().add(0, 1, 0));
            }
            return false;
        }
    }

    class BackCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (sender instanceof Player) {
                Player p = (Player) sender;
                if (tele_backstack.get(p.getUniqueId().toString()) != null) {
                    if (!tele_backstack.get(p.getUniqueId().toString()).isEmpty()) {
                        if (args.length > 0) {
                            if (args[0].equalsIgnoreCase("show")) {
                                for (Map<String, Object> map : tele_backstack.get(p.getUniqueId().toString())) {
                                    p.sendMessage(Location.deserialize(map).toString());
                                }
                                return true;
                            }
                        }
                        p.teleport(Location.deserialize(tele_backstack.get(p.getUniqueId().toString()).pollLast()));
                        return true;
                    } else {
                        p.sendMessage("No back stack");
                    }
                }
            }
            return false;
        }
    }

    class HistoryCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (sender instanceof Player) {
                Player p = (Player) sender;
                for (String cmd : command_history.get(p.getUniqueId().toString())) {
                    p.sendMessage(cmd);
                }
                return true;
            }
            return false;
        }
    }

    class FlachwitzCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (sender instanceof Player) {
                Player p = (Player) sender;
                if (args.length > 0) {
                    if (args[0].equalsIgnoreCase("download")) {
                        startFlachwitzeDownload();
                        return true;
                    }
                }
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendMessage(flachwitze[new Random().nextInt(flachwitze.length)]);
                }
                return true;
            }
            return false;
        }
    }
    class WitzCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (sender instanceof Player) {
                Player p = (Player) sender;
                if (args.length > 0) {
                    if (args[0].equalsIgnoreCase("download")) {
                        moreWitze();
                        return true;
                    }
                }
                for (Player player : Bukkit.getOnlinePlayers()) {
                    ArrayList<String> ww = new ArrayList<>();
                    for (String k : witze.keySet()) {
                        ww.addAll(witze.get(k).get("jokes"));
                    }
                    player.sendMessage(ww.get(new Random().nextInt(ww.size())));
                }
                return true;
            }
            return false;
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
            witze = gson.fromJson(r, new TypeToken<HashMap<String, HashMap<String, List<String>>>>(){}.getType());
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

    @Override
    public void onEnable() {
        try {
            IP = getIp();
        } catch (Exception e) {
            e.printStackTrace();
        }
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

        System.out.printf("Read from %s: %d Players%n", player_ips_file, player_ips.keySet().size());
        System.out.printf("Read from %s: %d Players%n", tele_backstack_file, tele_backstack.keySet().size());
        System.out.printf("Read from %s: %d Players%n", command_history_file, command_history.keySet().size());

        if (!flachwitze_file.exists()) startFlachwitzeDownload();
        else reloadFlachwitzeFromDisk();
        if (!witze_file.exists()) moreWitze();
        else reloadWitze();

        properties = readProperties();
        startRunnable();
        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(this, this);
        this.getCommand("ip").setExecutor(new IPCommand());
        this.getCommand("top").setExecutor(new TopCommand());
        this.getCommand("back").setExecutor(new BackCommand());
        this.getCommand("history").setExecutor(new HistoryCommand());
        this.getCommand("flachwitz").setExecutor(new FlachwitzCommand());
        this.getCommand("witz").setExecutor(new WitzCommand());

    }

    @Override
    public void onDisable() {
        writeJsonToFile(player_ips, player_ips_file);
        writeJsonToFile(tele_backstack, tele_backstack_file);
        writeJsonToFile(command_history, command_history_file);
    }
}
