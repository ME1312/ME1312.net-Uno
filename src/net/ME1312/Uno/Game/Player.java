package net.ME1312.Uno.Game;

import net.ME1312.Galaxi.Engine.GalaxiEngine;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Uno.Network.Client;
import net.ME1312.Uno.Network.ClientHandler;
import net.ME1312.Uno.Network.Packet.PacketOutAddCard;
import net.ME1312.Uno.Network.Packet.PacketOutAlert;
import net.ME1312.Uno.Network.Packet.PacketOutPlayerQuit;
import net.ME1312.Uno.UnoServer;

import org.json.JSONObject;

import java.io.*;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;

public class Player implements ClientHandler {
    LinkedHashMap<String, Card> cards = new LinkedHashMap<String, Card>();
    boolean uno = false;
    private static BigInteger id = BigInteger.ZERO;
    private boolean playing = false;
    private JSONObject profile;
    private UnoServer server;
    private Client client = null;

    public Player(UnoServer server, JSONObject profile) {
        this.server = server;
        this.profile = profile;
    }

    @Override
    public Client getSubData() {
        return client;
    }

    @Override
    public void setSubData(Client client) {
        if (this.client != null && client == null) {
            if (server.game != null) server.game.quit(this);
            server.players.remove(profile.getString("name"));
            for (Client other : server.subdata.getClients())
                if (this.client != other && !other.isClosed() && other.isAuthorized()) other.sendPacket(new PacketOutPlayerQuit(profile.getString("name")));
        }
        this.client = client;
        if (client != null && (client.getHandler() == null || !equals(client.getHandler()))) client.setHandler(this);
    }

    public JSONObject getProfile() {
        return profile;
    }

    public JSONObject getStats() {
        File file = new File(GalaxiEngine.getInstance().getRuntimeDirectory(), "Stats/" + profile.getLong("id") +  ".json");
        JSONObject stats = new JSONObject();
        if (file.exists()) {
            try {
                InputStream is = new FileInputStream(file);
                stats = new JSONObject(Util.readAll(new InputStreamReader(is)));
                is.close();

            } catch (Exception e) {
                server.log.error.println(e);
            }
        }

        if (!stats.keySet().contains("cardsPlayed")) stats.put("cardsPlayed", 0);
        if (!stats.keySet().contains("consecutiveCardsDrawn")) stats.put("consecutiveCardsDrawn", 0);
        if (!stats.keySet().contains("gamesWon")) stats.put("gamesWon", 0);

        return stats;
    }

    public void setStats(JSONObject stats) {
        File parent = new File(GalaxiEngine.getInstance().getRuntimeDirectory(), "Stats");
        if (parent.exists() || parent.mkdirs()) {
            File file = new File(parent, profile.getLong("id") + ".json");
            try {
                FileWriter writer = new FileWriter(file, false);
                stats.write(writer);
                writer.close();
            } catch (Exception e) {
                server.log.error.println(e);
            }
        }
    }

    public void uno() {
        uno = true;
        Game.log.info.println(profile.getString("displayName") + " called Uno");
        if (server.game != null && isPlaying()) for (Player player : server.game.getPlayers()) {
            player.getSubData().sendPacket(new PacketOutAlert(profile.getString("displayName") + " called Uno"));
        }
    }

    public boolean hasUno() {
        return uno;
    }

    public String addCard(Card card) {
        uno = false;
        String id = (Player.id = Player.id.add(BigInteger.ONE)).toString();
        cards.put(id, card);
        if (isPlaying() && server.game != null) {
            client.sendPacket(new PacketOutAddCard(this, id, card));
            for (Player other : server.game.getAllPlayers())
                if (this != other) other.getSubData().sendPacket(new PacketOutAddCard(this));
        }
        return id;
    }

    public Map<String, Card> getCards() {
        return new LinkedHashMap<String, Card>(cards);
    }

    public Card getCard(String id) {
        return getCards().get(id);
    }

    public void removeCard(String id) {
        cards.remove(id);
    }

    public boolean isPlaying() {
        return playing;
    }

    public void setPlaying(boolean value) {
        this.playing = value;
    }

    @Override
    public String toString() {
        JSONObject json = new JSONObject();
        json.put("profile", getProfile());
        json.put("stats", getStats());
        return json.toString();
    }
}
