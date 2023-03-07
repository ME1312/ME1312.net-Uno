package net.ME1312.Uno.Network.Packet;

import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.Uno.Game.Player;
import net.ME1312.Uno.Network.Client;
import net.ME1312.Uno.Network.PacketIn;
import net.ME1312.Uno.Network.PacketOut;
import net.ME1312.Uno.UnoServer;

import org.json.JSONObject;

import java.math.BigInteger;

public class PacketPlayerList implements PacketIn, PacketOut {
    private UnoServer server;
    private String id;

    public PacketPlayerList(UnoServer server) {
        this.server = server;
    }

    public PacketPlayerList(UnoServer server, String id) {
        this.server = server;
        this.id = id;
    }

    @Override
    public void execute(Client client, ObjectMap<String> data) throws Throwable {
        client.sendPacket(new PacketPlayerList(server, (data.contains("id"))?data.getString("id"):null));
    }

    @Override
    public ObjectMap<String> generate() throws Throwable {
        ObjectMap<String> info = new ObjectMap<String>();
        ObjectMap<String> players = new ObjectMap<String>();
        BigInteger i = BigInteger.ZERO;
        for (Player player : server.players.values()) {
            players.set(i.toString(), new YAMLSection(new JSONObject(player.toString())));
            i = i.add(BigInteger.ONE);
        }
        info.set("players", players);
        info.set("id", id);
        return info;
    }

    @Override
    public Version getVersion() {
        return new Version("1.0a");
    }
}
