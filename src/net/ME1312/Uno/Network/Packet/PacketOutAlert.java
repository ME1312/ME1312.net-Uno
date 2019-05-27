package net.ME1312.Uno.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.Uno.Network.PacketOut;

public class PacketOutAlert implements PacketOut {
    private String message;

    public PacketOutAlert(String message) {
        this.message = message;
    }

    @Override
    public ObjectMap<String> generate() throws Throwable {
        ObjectMap<String> info = new ObjectMap<String>();
        info.set("message", message);
        return info;
    }

    @Override
    public Version getVersion() {
        return new Version("1.0a");
    }
}
