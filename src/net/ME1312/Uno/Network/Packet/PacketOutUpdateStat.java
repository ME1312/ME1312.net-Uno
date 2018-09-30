package net.ME1312.Uno.Network.Packet;

import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.Uno.Game.Player;
import net.ME1312.Uno.Network.PacketOut;

public class PacketOutUpdateStat implements PacketOut {
    private Player player;
    private String stat;
    private Object value;

    public PacketOutUpdateStat(Player player, String stat, Object value) {
        this.player = player;
        this.stat = stat;
        this.value = value;
    }

    @Override
    public YAMLSection generate() throws Throwable {
        YAMLSection info = new YAMLSection();
        info.set("player", player.getProfile().getString("name"));
        info.set("stat", stat);
        info.set("value", value);
        return info;
    }

    @Override
    public Version getVersion() {
        return new Version("1.0a");
    }
}
