package me.thienbao860.expansion.world;

import com.google.common.primitives.Ints;
import me.clip.placeholderapi.expansion.Cacheable;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WorldExpansion extends PlaceholderExpansion implements Listener, Cacheable {

    private final Map<String, WorldData> worldData;

    private Economy econ = null;
    private Permission perms = null;

    public WorldExpansion() {
        this.worldData = new HashMap<>();
        if (isVaultExist()) {
            setupEconomy();
            setupPermissions();
        }
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
        return "1.2.2";
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public String onRequest(OfflinePlayer p, String params) {

        final Player player = (Player) p;
        String[] args;
        ArrayList<String> arrayList = new ArrayList<>();

        // Define a regular expression pattern to match the strings
        Pattern pattern = Pattern.compile("(\\b\\w+\\b)|(\\b\\w+\\b)(\\B_+?\\B)(?<=\\*)\\w+(?=\\*(?:_|$))");
// Create a Matcher object
        Matcher matcher = pattern.matcher(params);
// Check if the pattern matches the input string

        while (matcher.find()) {
            if (matcher.group(1) != null)
                arrayList.add(matcher.group(1));
            if (matcher.group(2) != null)
                arrayList.add(matcher.group(2));
        }

        if (arrayList.size() == 0) return null;
        if (arrayList.size() == 1) {
            args = params.split("_");
        } else {
            //This will separate first input wich can be only identifier in bottom switch(){}
            //That parts are then put together with world name
            String[] parts = arrayList.get(0).split("_");
            // add each part to the ArrayList
            ArrayList<String> list = new ArrayList<>(Arrays.asList(parts));
            list.add(arrayList.get(1));
            args = list.toArray(new String[0]);
        }
        arrayList.clear();
        //===== Mutual world =====

        switch (args[0].toLowerCase()) {
            case "total":
                return String.valueOf(Bukkit.getWorlds().size());
            case "biome":
                Location loc = player.getLocation();
                return player.getWorld().getBiome(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()).name().toLowerCase();
            case "nearbyentites":
                if (args.length != 2) break;
                Integer dis = Ints.tryParse(args[1]);
                if (dis == null) return "0";
                return String.valueOf(player.getNearbyEntities(dis, dis, dis).size());
        }

        //===== Specific world =====
        if (args.length < 2) return null;
        World world = getWorld(player, args);

        if (world == null) return "";

        switch (args[0]) {
            case "name":
                return world.getName();
            case "seed":
                return String.valueOf(world.getSeed());
            case "sealevel":
                return String.valueOf(world.getSeaLevel());

            case "time":
                return timeFormat24(world.getTime());
            case "timein12":
                return timeFormat(world.getTime(), true);

            case "canpvp":
                return String.valueOf(world.getPVP());
            case "thunder":
                return String.valueOf(world.isThundering());
            case "animalallowed":
                return String.valueOf(world.getAllowAnimals());
            case "monsterallowed":
                return String.valueOf(world.getAllowMonsters());
            case "difficulty":
                return world.getDifficulty().name().toLowerCase();
            case "players":
                if (args.length >= 3) {
                    if (perms != null) {
                        return String.valueOf(playersInGroup(world, args[1]));
                    } else return "0";
                }
                return String.valueOf(world.getPlayers().size());
            case "haspermission":
                if (args.length >= 3) {
                    return String.valueOf(playersPermission(world, args[1]));
                }
                break;

            case "playerexist":
                if (args.length >= 3) {
                    return String.valueOf(playerExist(world, args[1]));
                }
                break;
            case "isgamerule":
                if (args.length >= 3) {
                    return String.valueOf(world.isGameRule(args[1].toUpperCase()));
                }
                break;
            case "recentjoin":
                if (!worldData.containsKey(world.getName())) return "";
                if (player == null) return "";
                return worldData.get(world.getName()).getRecentJoin().getName();
            case "recentquit":
                if (!worldData.containsKey(world.getName())) return "";
                if (player == null) return "";
                return worldData.get(world.getName()).getRecentQuit().getName();
            case "totalbalance":
                if (econ != null) {
                    return String.valueOf(getTotalMoney(world));
                }
                break;

        }
        return null;
    }

    public World getWorld(Player player, String[] args) {
        final String worldName = args[args.length - 1];
        if (worldName.equals("$")) {
            return player.getWorld();
        }
        return Bukkit.getWorld(worldName);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final String world = player.getWorld().getName();

        worldData.putIfAbsent(world, new WorldData());
        worldData.get(world).setRecentJoin(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String world = player.getWorld().getName();

        worldData.putIfAbsent(world, new WorldData());
        worldData.get(world).setRecentQuit(player);
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {

        final Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();

        if (to == null || from.getWorld() == null || to.getWorld() == null) {
            return;
        }
        if (from.getWorld().getName().equals(to.getWorld().getName())) {
            return;
        }

        String world = to.getWorld().getName();
        worldData.putIfAbsent(world, new WorldData());

        worldData.get(world).setRecentJoin(player);

    }

    private void setupEconomy() {
        Server server = Bukkit.getServer();
        if (!isVaultExist()) return;
        RegisteredServiceProvider<Economy> rsp = server.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return;
        this.econ = rsp.getProvider();
    }

    private void setupPermissions() {
        Server server = Bukkit.getServer();
        RegisteredServiceProvider<Permission> rsp = server.getServicesManager().getRegistration(Permission.class);
        if (rsp == null) return;

        this.perms = rsp.getProvider();
    }

    private double getTotalMoney(final World world) {
        double total = 0;

        if (econ == null) {
            return total;
        }

        for (Player player : world.getPlayers()) {
            total += econ.getBalance(player);
        }
        return total;
    }

    private boolean isVaultExist() {
        return Bukkit.getServer().getPluginManager().isPluginEnabled("Vault");
    }

    private boolean playerExist(final World world, String name) {
        for (Player player : world.getPlayers()) {
            if (player.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private int playersInGroup(final World world, String group) {
        int i = 0;
        if (perms == null) {
            return 0;
        }
        for (Player player : world.getPlayers()) {
            if (perms.playerInGroup(world.toString(), player, group)) {
                i++;
            }
        }
        return i;
    }

    private int playersPermission(final World world, String perm) {
        int i = 0;
        perm.replace("_", "");
        for (Player player : world.getPlayers()) {
            if (player.isOp() || player.hasPermission(perm)) {
                i++;
            }
        }
        return i;
    }

    private String timeFormat24(long tick) {
        return timeFormat(tick, false);
    }

    private String timeFormat(long tick, boolean is12) {
        int hour = ((int) ((tick / 1000) + 6)) % 24;
        boolean am = hour < 12;

        if (is12) {
            if (hour > 12) {
                hour -= 12;
            }
        }
        String minutesAsString = String.valueOf(tick);
        int length = minutesAsString.length();

        String newStr = length < 3 ? minutesAsString.substring(length - 1) : minutesAsString.substring(length - 3);
        int minutes = Integer.parseInt(newStr) * 60 / 999;
        if (is12) {
            return String.format("%d:%02d%s", hour, minutes, am ? "am" : "pm");
        }
        return String.format("%d:%02d", hour, minutes);

    }

    @Override
    public void clear() {
        this.worldData.clear();
        this.econ = null;
        this.perms = null;
    }

}
