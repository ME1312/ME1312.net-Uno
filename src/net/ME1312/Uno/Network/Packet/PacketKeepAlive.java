package net.ME1312.Uno.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.Uno.Network.Client;
import net.ME1312.Uno.Network.PacketIn;
import net.ME1312.Uno.Network.PacketOut;

public class PacketKeepAlive implements PacketIn, PacketOut {

    @Override
    public void execute(Client client, ObjectMap<String> data) throws Throwable {
        client.sendPacket(this);
    }

    @Override
    public ObjectMap<String> generate() throws Throwable {
        return null;
    }

    @Override
    public Version getVersion() {
        return new Version("1.0a");
    }
}
