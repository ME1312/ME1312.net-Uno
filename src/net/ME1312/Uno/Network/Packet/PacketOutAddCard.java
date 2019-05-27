package net.ME1312.Uno.Network.Packet;

import net.ME1312.Galaxi.Library.Map.ObjectMap;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.Uno.Game.Card;
import net.ME1312.Uno.Game.Player;
import net.ME1312.Uno.Network.PacketOut;

public class PacketOutAddCard implements PacketOut {
    private Player player;
    private String id;
    private Card card;

    public PacketOutAddCard(Player player) {
        this.player = player;
    }

    public PacketOutAddCard(Player player, String id, Card card) {
        this.player = player;
        this.id = id;
        this.card = card;
    }

    @Override
    public ObjectMap<String> generate() throws Throwable {
        ObjectMap<String> info = new ObjectMap<String>();
        info.set("player", player.getProfile().getString("name"));
        if (id != null) {
            ObjectMap<String> cardinfo = new ObjectMap<String>();
            cardinfo.set("color", card.getColor().toString());
            cardinfo.set("number", card.getNumber());
            cardinfo.set("id", id);
            info.set("card", cardinfo);
        }
        return info;
    }

    @Override
    public Version getVersion() {
        return new Version("1.0a");
    }
}
