package net.ME1312.Uno.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.Galaxi.Log.Logger;
import net.ME1312.Uno.Game.Player;
import net.ME1312.Uno.Network.Client;
import net.ME1312.Uno.Network.PacketIn;
import net.ME1312.Uno.Network.PacketOut;
import net.ME1312.Uno.UnoServer;

public class PacketMessage implements PacketIn, PacketOut {
    private static final Logger log = new Logger("Chat");
    private UnoServer server;
    private Player player;
    private String message;

    public PacketMessage(UnoServer server) {
        this.server = server;
    }

    public PacketMessage(Player player, String message) {
        this.player = player;
        this.message = message;
    }

    @Override
    public void execute(Client client, ObjectMap<String> data) throws Throwable {
        if (client.getHandler() instanceof Player) {
            log.info.println(((Player) client.getHandler()).getProfile().getString("displayName") + " > " + data.getString("message"));
            for (Client other : server.subdata.getClients()) {
                if (other.isAuthorized()) other.sendPacket(new PacketMessage((Player) client.getHandler(), data.getString("message")));
            }
        }
    }

    @Override
    public ObjectMap<String> generate() throws Throwable {
        ObjectMap<String> info = new ObjectMap<String>();
        if (player != null) info.set("sender", player.getProfile().getString("name"));
        info.set("message", message);
        return info;
    }

    @Override
    public Version getVersion() {
        return new Version("1.0a");
    }
}
