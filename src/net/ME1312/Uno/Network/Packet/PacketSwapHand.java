package net.ME1312.Uno.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.Uno.Game.Player;
import net.ME1312.Uno.Network.Client;
import net.ME1312.Uno.Network.PacketIn;
import net.ME1312.Uno.Network.PacketOut;
import net.ME1312.Uno.UnoServer;

public class PacketSwapHand implements PacketIn, PacketOut {
    private UnoServer server;

    public PacketSwapHand(UnoServer server) {
        this.server = server;
    }

    @Override
    public ObjectMap<String> generate() throws Throwable {
        return null;
    }

    @Override
    public void execute(Client client, ObjectMap<String> data) throws Throwable {
        if (server.game != null && client.getHandler() instanceof Player && ((Player) client.getHandler()).isPlaying() && server.game.getCurrentPlayer() == client.getHandler()) {
            server.game.swapHands((Player) client.getHandler(), server.players.get(data.getString("player")));
        }
    }

    @Override
    public Version getVersion() {
        return new Version("1.0a");
    }
}
