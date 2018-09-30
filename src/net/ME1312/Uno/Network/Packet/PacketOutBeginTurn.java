package net.ME1312.Uno.Network.Packet;

import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.Uno.Game.Player;
import net.ME1312.Uno.Network.PacketOut;

import java.util.Arrays;

public class PacketOutBeginTurn implements PacketOut {
    private Player player;
    private boolean draw;
    private String[] cards;

    public PacketOutBeginTurn(Player player) {
        this.player = player;
    }

    public PacketOutBeginTurn(Player player, boolean canDraw, String... cards) {
        this.player = player;
        this.draw = canDraw;
        this.cards = cards;
    }

    @Override
    public YAMLSection generate() throws Throwable {
        YAMLSection info = new YAMLSection();
        info.set("player", player.getProfile().getString("name"));
        if (cards != null) {
            info.set("cards", Arrays.asList(cards));
            info.set("canDraw", draw);
        }
        return info;
    }

    @Override
    public Version getVersion() {
        return new Version("1.0a");
    }
}
