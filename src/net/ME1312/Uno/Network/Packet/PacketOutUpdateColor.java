package net.ME1312.Uno.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.Uno.Game.CardColor;
import net.ME1312.Uno.Network.PacketOut;

public class PacketOutUpdateColor implements PacketOut {
    private CardColor color;

    public PacketOutUpdateColor(CardColor color) {
        this.color = color;
    }

    @Override
    public ObjectMap<String> generate() throws Throwable {
        ObjectMap<String> info = new ObjectMap<String>();
        info.set("color", color.toString());
        return info;
    }

    @Override
    public Version getVersion() {
        return new Version("1.0a");
    }
}
