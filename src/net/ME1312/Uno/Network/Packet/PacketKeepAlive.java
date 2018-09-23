package net.ME1312.Uno.Network.Packet;

import net.ME1312.Uno.Library.Config.YAMLSection;
import net.ME1312.Uno.Library.Version.Version;
import net.ME1312.Uno.Network.Client;
import net.ME1312.Uno.Network.PacketIn;
import net.ME1312.Uno.Network.PacketOut;

public class PacketKeepAlive implements PacketIn, PacketOut {

    @Override
    public void execute(Client client, YAMLSection data) throws Throwable {
        client.sendPacket(this);
    }

    @Override
    public YAMLSection generate() throws Throwable {
        return null;
    }

    @Override
    public Version getVersion() {
        return new Version("1.0a");
    }
}
