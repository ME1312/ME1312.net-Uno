package net.ME1312.Uno.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.Uno.Network.PacketOut;

public class PacketOutPlayerQuit implements PacketOut {
    private String name;

    public PacketOutPlayerQuit(String name) {
        this.name = name;
    }

    @Override
    public ObjectMap<String> generate() throws Throwable {
        ObjectMap<String> info = new ObjectMap<String>();
        info.set("player", name);
        return info;
    }

    @Override
    public Version getVersion() {
        return new Version("1.0a");
    }
}
