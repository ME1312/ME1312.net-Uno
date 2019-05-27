package net.ME1312.Uno.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.Uno.Game.Card;
import net.ME1312.Uno.Game.Player;
import net.ME1312.Uno.Network.Client;
import net.ME1312.Uno.Network.PacketIn;
import net.ME1312.Uno.Network.PacketOut;
import net.ME1312.Uno.UnoServer;

public class PacketPlayCard implements PacketIn, PacketOut {
    private UnoServer server;
    private Player player;
    private String id;
    private Card card;

    public PacketPlayCard(UnoServer server) {
        this.server = server;
    }

    public PacketPlayCard(Player player, String id, Card card) {
        this.player = player;
        this.id = id;
        this.card = card;
    }

    @Override
    public ObjectMap<String> generate() throws Throwable {
        ObjectMap<String> info = new ObjectMap<String>();
        info.set("player", player.getProfile().getString("name"));
        ObjectMap<String> cardinfo = new ObjectMap<String>();
        cardinfo.set("color", card.getColor().toString());
        cardinfo.set("number", card.getNumber());
        cardinfo.set("id", id);
        info.set("card", cardinfo);
        return info;
    }

    @Override
    public void execute(Client client, ObjectMap<String> data) throws Throwable {
        if (server.game != null && client.getHandler() instanceof Player && ((Player) client.getHandler()).isPlaying() && server.game.getCurrentPlayer() == client.getHandler()) {
            server.game.play(data.getString("card"));
        }
    }

    @Override
    public Version getVersion() {
        return new Version("1.0a");
    }
}
