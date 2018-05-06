package net.ME1312.Uno.Network.Packet;

import net.ME1312.Uno.Game.Player;
import net.ME1312.Uno.Library.Config.YAMLSection;
import net.ME1312.Uno.Library.Version.Version;
import net.ME1312.Uno.Network.PacketOut;

public class PacketOutEndGame implements PacketOut {
    private Player player;
    private boolean draw;
    private String[] cards;

    public PacketOutEndGame(Player player) {
        this.player = player;
    }

    @Override
    public YAMLSection generate() throws Throwable {
        YAMLSection info = new YAMLSection();
        if (player != null) info.set("winner", player.getProfile().getString("name"));
        return info;
    }

    @Override
    public Version getVersion() {
        return new Version("1.0a");
    }
}
