package net.ME1312.Uno.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.Uno.Game.Game;
import net.ME1312.Uno.Game.Player;
import net.ME1312.Uno.Network.PacketOut;

import java.math.BigInteger;

public class PacketOutStartGame implements PacketOut {
    private Player player;
    private Game game;

    public PacketOutStartGame(Game game, Player player) {
        this.game = game;
        this.player = player;
    }

    @Override
    public ObjectMap<String> generate() throws Throwable {
        ObjectMap<String> info = new ObjectMap<String>();
        ObjectMap<String> playerlist = new ObjectMap<String>();
        BigInteger i = BigInteger.ZERO;
        for (Player player : game.getPlayers()) {
            playerlist.set(i.toString(), player.getProfile().getString("name"));
            i = i.add(BigInteger.ONE);
        }
        info.set("order", playerlist);
        if (player.isPlaying()) {
            ObjectMap<String> cardlist = new ObjectMap<String>();
            for (String id : player.getCards().keySet()) {
                ObjectMap<String> cardinfo = new ObjectMap<String>();
                cardinfo.set("color", player.getCard(id).getColor().toString());
                cardinfo.set("number", player.getCard(id).getNumber());
                cardlist.set(id, cardinfo);
            }
            info.set("cards", cardlist);
        }
        ObjectMap<String> cardinfo = new ObjectMap<String>();
        cardinfo.set("color", game.getCurrentCard().name().toString());
        cardinfo.set("number", game.getCurrentCard().get());
        info.set("houseCard", cardinfo);
        return info;
    }

    @Override
    public Version getVersion() {
        return new Version("1.0a");
    }
}
