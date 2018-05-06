package net.ME1312.Uno.Network.Packet;

import net.ME1312.Uno.Game.Game;
import net.ME1312.Uno.Game.Player;
import net.ME1312.Uno.Library.Config.YAMLSection;
import net.ME1312.Uno.Library.Version.Version;
import net.ME1312.Uno.Network.PacketOut;

public class PacketOutUpdateHand implements PacketOut {
    private Player player;
    private Game game;

    public PacketOutUpdateHand(Game game, Player player) {
        this.game = game;
        this.player = player;
    }

    @Override
    public YAMLSection generate() throws Throwable {
        YAMLSection info = new YAMLSection();
        YAMLSection playerlist = new YAMLSection();
        for (Player player : game.getPlayers()) {
            playerlist.set(player.getProfile().getString("name"), player.getCards().size());
        }
        info.set("others", playerlist);
        if (player.isPlaying()) {
            YAMLSection cardlist = new YAMLSection();
            for (String id : player.getCards().keySet()) {
                YAMLSection cardinfo = new YAMLSection();
                cardinfo.set("color", player.getCard(id).getColor().toString());
                cardinfo.set("number", player.getCard(id).getNumber());
                cardlist.set(id, cardinfo);
            }
            info.set("self", cardlist);
        }
        return info;
    }

    @Override
    public Version getVersion() {
        return new Version("1.0a");
    }
}
