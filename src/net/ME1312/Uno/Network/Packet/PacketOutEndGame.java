package net.ME1312.Uno.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.Uno.Game.Player;
import net.ME1312.Uno.Network.PacketOut;

import java.util.ArrayList;

public class PacketOutEndGame implements PacketOut {
    private Player[] players;
    private boolean draw;
    private String[] cards;

    public PacketOutEndGame(Player... players) {
        this.players = players;
    }

    @Override
    public ObjectMap<String> generate() throws Throwable {
        ObjectMap<String> info = new ObjectMap<String>();
        if (players.length > 0) {
            ArrayList<String> names = new ArrayList<String>();
            for (Player player : players) names.add(player.getProfile().getString("name"));
            info.set("winner", names);
        }
        return info;
    }

    @Override
    public Version getVersion() {
        return new Version("1.0a");
    }
}
