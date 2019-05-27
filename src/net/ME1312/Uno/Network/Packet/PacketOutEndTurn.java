package net.ME1312.Uno.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.Uno.Network.PacketOut;

public class PacketOutEndTurn implements PacketOut {
    @Override
    public ObjectMap<String> generate() throws Throwable {
        return null;
    }

    @Override
    public Version getVersion() {
        return new Version("1.0a");
    }
}
