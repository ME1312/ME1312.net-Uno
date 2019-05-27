package net.ME1312.Uno.Network.Packet;

import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.Uno.Game.Player;
import net.ME1312.Uno.Network.PacketOut;
import org.json.JSONObject;

public class PacketOutPlayerJoin implements PacketOut {
    private Player player;

    public PacketOutPlayerJoin(Player player) {
        this.player = player;
    }

    @Override
    public ObjectMap<String> generate() throws Throwable {
        ObjectMap<String> info = new ObjectMap<String>();
        info.set("player", new YAMLSection(new JSONObject(player.toString())));
        return info;
    }

    @Override
    public Version getVersion() {
        return new Version("1.0a");
    }
}