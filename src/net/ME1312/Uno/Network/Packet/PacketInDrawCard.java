package net.ME1312.Uno.Network.Packet;

import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.Uno.Game.Player;
import net.ME1312.Uno.Network.Client;
import net.ME1312.Uno.Network.PacketIn;
import net.ME1312.Uno.UnoServer;

public class PacketInDrawCard implements PacketIn {
    private UnoServer server;

    public PacketInDrawCard(UnoServer server) {
        this.server = server;
    }

    @Override
    public void execute(Client client, YAMLSection data) throws Throwable {
        if (server.game != null && client.getHandler() instanceof Player && ((Player) client.getHandler()).isPlaying() && server.game.getCurrentPlayer() == client.getHandler()) {
            server.game.draw();
        }
    }

    @Override
    public Version getVersion() {
        return new Version("1.0a");
    }
}
