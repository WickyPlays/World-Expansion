package me.thienbao860.expansion.world;

import org.bukkit.entity.Player;

public class WorldData {

    private Player recentJoin;
    private Player recentQuit;

    public Player getRecentJoin() {
        return recentJoin;
    }

    public void setRecentJoin(Player recentJoin) {
        this.recentJoin = recentJoin;
    }

    public Player getRecentQuit() {
        return recentQuit;
    }

    public void setRecentQuit(Player recentQuit) {
        this.recentQuit = recentQuit;
    }
}
