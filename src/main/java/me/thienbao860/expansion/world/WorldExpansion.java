package me.thienbao860.expansion.world;

import me.clip.placeholderapi.expansion.Cacheable;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class WorldExpansion extends PlaceholderExpansion implements Listener, Cacheable {

    public Map<String, WorldData> worldData;

    private Economy econ = null;
    private Permission perms = null;

    public WorldExpansion() {
        worldData = new HashMap<>();
        setupEconomy();
        setupPermissions();
    }

    @Override
    public String getIdentifier() {
        return "world";
    }

    @Override
    public String getAuthor() {
        return "thienbao860";
    }

    @Override
    public String getVersion() {
        return "1.0.2";
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {

        final Player p = (Player) player;
        final World world;

        final String[] args = params.split("_");
        if (args[0] == null) return null;
        
        // return all worlds
        if (args[0].equals("worlds")) {
            return Bukkit.getServer().getWorlds().stream().map(World::getName).collect(Collectors.joining(", "));
        }
        
        if (args[0].equalsIgnoreCase("#")) {
            switch (args[1]) {
                case "total":
                    return String.valueOf(Bukkit.getWorlds().size());
                case "biome":
                    return p.getWorld().getBiome(p.getLocation().getBlockX(), p.getLocation().getBlockZ()).name().toLowerCase();
                case "nearbyEntites":
                    if (args.length != 3) break;
                    try {
                        int dis = Integer.parseInt(args[2]);
                        return String.valueOf(p.getNearbyEntities(dis, dis, dis).size());
                    } catch (NumberFormatException e) {
                        return null;
                    }

            }
            return null;
        }

        // Server world related
        if (args[0].equalsIgnoreCase("$")) {
            world = p.getWorld();
        } else {
            world = Bukkit.getWorld(args[0]);
        }

        if (world != null) {
            switch (args[1]) {
                case "name":
                    return world.getName();
                case "seed":
                    return String.valueOf(world.getSeed());
                case "time":
                    return timeFormat(world.getTime());
                case "canPvP":
                    return String.valueOf(world.getPVP());
                case "thunder":
                    return String.valueOf(world.isThundering());
                case "animalAllowed":
                    return String.valueOf(world.getAllowAnimals());
                case "monsterAllowed":
                    return String.valueOf(world.getAllowMonsters());
                case "difficulty":
                    return world.getDifficulty().name().toLowerCase();
                case "players":
                    if (args.length == 3) {
                        if (perms != null) {
                            return String.valueOf(playersInGroup(world, args[2]));
                        } else return "0";
                    } else return String.valueOf(world.getPlayers().size());
                case "playerExist":
                    if (args.length > 3) {
                        return String.valueOf(playerExist(world, args[2]));
                    }
                    break;
                case "gamerule":
                    if (args[2] != null) {
                        return String.valueOf(world.isGameRule(args[1].toUpperCase()));
                    }
                    break;

                case "recentjoin":
                    if (!worldData.containsKey(world.getName())) return "";
                    return worldData.get(world.getName()).getRecentJoin().getName();
                case "recentquit":
                    if (!worldData.containsKey(world.getName())) return "";
                    return worldData.get(world.getName()).getRecentQuit().getName();
                case "totalBalance":
                    if (econ != null) {
                        return String.valueOf(totalMoney(world));
                    }
                    break;
            }

        }
        return null;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String world = player.getWorld().getName();
        if (!worldData.containsKey(world)) {
            worldData.put(world, new WorldData());
        }
        worldData.get(world).setRecentJoin(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String world = player.getWorld().getName();
        if (!worldData.containsKey(world)) {
            worldData.put(world, new WorldData());
        }
        worldData.get(world).setRecentJoin(player);
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (event.getFrom().getWorld().getName().equals(event.getTo().getWorld().getName())) return;
        String world = event.getTo().getWorld().getName();
        if (!worldData.containsKey(world)) {
            worldData.put(world, new WorldData());
        }
        worldData.get(world).setRecentJoin(player);

    }

    private void setupEconomy() {
        Server server = Bukkit.getServer();
        if (!isVaultExist()) return;
        RegisteredServiceProvider<Economy> rsp = server.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return;
        econ = rsp.getProvider();
    }

    private void setupPermissions() {
        Server server = Bukkit.getServer();
        RegisteredServiceProvider<Permission> rsp = server.getServicesManager().getRegistration(Permission.class);
        perms = rsp.getProvider();
    }

    private double totalMoney(World world) {
        double total = 0;
        for (Player player : world.getPlayers()) {
            total += econ.getBalance(player);
        }
        return total;
    }

    private boolean isVaultExist() {
        return Bukkit.getServer().getPluginManager().isPluginEnabled("Vault");
    }

    private boolean playerExist(World world, String name) {
        for (Player player : world.getPlayers()) {
            if (player.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private int playersInGroup(World world, String group) {
        int i = 0;
        for (Player player : world.getPlayers()) {
            if (perms.playerInGroup(player, group)) {
                i++;
            }
        }
        return i;
    }

    private String timeFormat(long tick) {

        int hour = (int) tick / 1000;
        String minutesAsString = String.valueOf(tick);
        int length = minutesAsString.length();
        /*
            1 - 0
            10 - 1
            100 - 2
            1000 - 2
            10000 - 2
         */
        String newStr = length < 3 ? minutesAsString.substring(length - 1) : minutesAsString.substring(length - 3);
        int minutes = (Integer.parseInt(newStr) * 60 / 999);
        String add0 = minutes < 10 ? "0" : "";
        return hour + ":" + add0 + minutes;
    }

    @Override
    public void clear() {
        worldData.clear();
        econ = null;
        perms = null;
    }
}
