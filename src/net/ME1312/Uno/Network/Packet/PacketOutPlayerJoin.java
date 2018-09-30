package net.ME1312.Uno.Network.Packet;

import net.ME1312.Galaxi.Library.Config.YAMLSection;
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
    public YAMLSection generate() throws Throwable {
        YAMLSection info = new YAMLSection();
        info.set("player", new YAMLSection(new JSONObject(player.toString())));
        return info;
    }

    @Override
    public Version getVersion() {
        return new Version("1.0a");
    }
}