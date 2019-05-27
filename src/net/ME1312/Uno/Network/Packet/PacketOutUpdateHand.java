package net.ME1312.Uno.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.Uno.Game.Game;
import net.ME1312.Uno.Game.Player;
import net.ME1312.Uno.Network.PacketOut;

public class PacketOutUpdateHand implements PacketOut {
    private Player player;
    private Game game;

    public PacketOutUpdateHand(Game game, Player player) {
        this.game = game;
        this.player = player;
    }

    @Override
    public ObjectMap<String> generate() throws Throwable {
        ObjectMap<String> info = new ObjectMap<String>();
        ObjectMap<String> playerlist = new ObjectMap<String>();
        for (Player player : game.getPlayers()) {
            playerlist.set(player.getProfile().getString("name"), player.getCards().size());
        }
        info.set("others", playerlist);
        if (player.isPlaying()) {
            ObjectMap<String> cardlist = new ObjectMap<String>();
            for (String id : player.getCards().keySet()) {
                ObjectMap<String> cardinfo = new ObjectMap<String>();
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
