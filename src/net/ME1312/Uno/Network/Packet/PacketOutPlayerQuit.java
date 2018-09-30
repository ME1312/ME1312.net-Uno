package net.ME1312.Uno.Network.Packet;

import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.Uno.Network.PacketOut;

public class PacketOutPlayerQuit implements PacketOut {
    private String name;

    public PacketOutPlayerQuit(String name) {
        this.name = name;
    }

    @Override
    public YAMLSection generate() throws Throwable {
        YAMLSection info = new YAMLSection();
        info.set("player", name);
        return info;
    }

    @Override
    public Version getVersion() {
        return new Version("1.0a");
    }
}
