package net.ME1312.Uno.Network.Packet;

import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.Uno.Game.Player;
import net.ME1312.Uno.Network.Client;
import net.ME1312.Uno.Network.PacketIn;
import net.ME1312.Uno.UnoServer;

public class PacketInCallOut implements PacketIn {
    private UnoServer server;
    private Player from;
    private Player to;

    public PacketInCallOut(UnoServer server) {
        this.server = server;
    }

    @Override
    public void execute(Client client, YAMLSection data) throws Throwable {
        if (server.game != null && client.getHandler() instanceof Player && ((Player) client.getHandler()).isPlaying()) {
            server.game.callout((Player) client.getHandler(), server.players.get(data.getRawString("player")));
        }
    }

    @Override
    public Version getVersion() {
        return new Version("1.0a");
    }
}
