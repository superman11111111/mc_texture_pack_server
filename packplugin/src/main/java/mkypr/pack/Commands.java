package mkypr.pack;

import net.minecraft.server.v1_16_R3.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

public class Commands {

    static class IPCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (sender instanceof Player) {
                Player p = (Player) sender;
                if (p.getName().equals("mkypr") && p.getUniqueId().toString().equals("0189deff-5b92-4af7-b522-396c417cef85")) {
                    if (!Pack.instance.player_ips.keySet().isEmpty()) {
                        for (String uuid : Pack.instance.player_ips.keySet()) {
                            p.sendMessage(Pack.instance.player_ips.get(uuid).get("name") + ' ' + uuid + ' ' + Pack.instance.player_ips.get(uuid).get("ip"));
                        }
                        return true;
                    } else {
                        p.sendMessage("No ips");
                    }
                }
            }
            return false;
        }
    }

    static class TopCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (sender instanceof Player) {
                Player p = (Player) sender;
                p.teleport(p.getWorld().getHighestBlockAt(p.getLocation()).getLocation().add(0, 1, 0));
            }
            return false;
        }
    }

    static class BackCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (sender instanceof Player) {
                Player p = (Player) sender;
                if (Pack.instance.tele_backstack.get(p.getUniqueId().toString()) != null) {
                    if (!Pack.instance.tele_backstack.get(p.getUniqueId().toString()).isEmpty()) {
                        if (args.length > 0) {
                            if (args[0].equalsIgnoreCase("show")) {
                                for (Map<String, Object> map : Pack.instance.tele_backstack.get(p.getUniqueId().toString())) {
                                    Location loc = Location.deserialize(map);
                                    String msg = String.format("%s: %f, %f, %f", loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
                                    p.sendMessage(msg);
                                }
                                return true;
                            }
                        }
                        p.teleport(Location.deserialize(Pack.instance.tele_backstack.get(p.getUniqueId().toString()).pollLast()));
                        return true;
                    } else {
                        p.sendMessage("No back stack");
                    }
                }
            }
            return false;
        }
    }

    static class HistoryCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (sender instanceof Player) {
                Player p = (Player) sender;
                for (String cmd : Pack.instance.command_history.get(p.getUniqueId().toString())) {
                    p.sendMessage(cmd);
                }
                return true;
            }
            return false;
        }
    }


    static class DebugCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (sender instanceof Player) {
                Player p = (Player) sender;
                for (String key : Pack.instance.properties.keySet()) {
                    p.sendMessage(String.format("%s:%s", key, Pack.instance.properties.get(key)));
                }
                p.sendMessage(String.format("Current TPS: %d", MinecraftServer.currentTick));
                return true;
            }
            for (String key : Pack.instance.properties.keySet()) {
                System.out.printf("%s:%s%n", key, Pack.instance.properties.get(key));
            }
            System.out.printf("Current TPS: %d%n", MinecraftServer.currentTick);
            return false;
        }
    }


    static class FlachwitzCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (sender instanceof Player) {
                Player p = (Player) sender;
                if (args.length > 0) {
                    if (args[0].equalsIgnoreCase("download")) {
                        Pack.instance.startFlachwitzeDownload();
                        return true;
                    }
                }
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendMessage(Pack.instance.flachwitze[new Random().nextInt(Pack.instance.flachwitze.length)]);
                }
                return true;
            }
            return false;
        }
    }

    static class WitzCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (sender instanceof Player) {
                Player p = (Player) sender;
                if (args.length > 0) {
                    if (args[0].equalsIgnoreCase("download")) {
                        Pack.instance.moreWitze();
                        return true;
                    }
                }
                for (Player player : Bukkit.getOnlinePlayers()) {
                    ArrayList<String> ww = new ArrayList<>();
                    for (String k : Pack.instance.witze.keySet()) {
                        ww.addAll(Pack.instance.witze.get(k).get("jokes"));
                    }
                    player.sendMessage(ww.get(new Random().nextInt(ww.size())));
                }
                return true;
            }
            return false;
        }
    }

}
