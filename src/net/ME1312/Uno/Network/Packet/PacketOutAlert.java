package net.ME1312.Uno.Network.Packet;

import net.ME1312.Uno.Library.Config.YAMLSection;
import net.ME1312.Uno.Library.Version.Version;
import net.ME1312.Uno.Network.PacketOut;

public class PacketOutAlert implements PacketOut {
    private String message;

    public PacketOutAlert(String message) {
        this.message = message;
    }

    @Override
    public YAMLSection generate() throws Throwable {
        YAMLSection info = new YAMLSection();
        info.set("message", message);
        return info;
    }

    @Override
    public Version getVersion() {
        return new Version("1.0a");
    }
}
