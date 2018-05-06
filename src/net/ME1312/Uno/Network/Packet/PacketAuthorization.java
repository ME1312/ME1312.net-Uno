package net.ME1312.Uno.Network.Packet;

import net.ME1312.Uno.Game.Player;
import net.ME1312.Uno.Library.Config.YAMLSection;
import net.ME1312.Uno.Library.Log.Logger;
import net.ME1312.Uno.Library.Util;
import net.ME1312.Uno.Library.Version.Version;
import net.ME1312.Uno.Network.Client;
import net.ME1312.Uno.Network.PacketIn;
import net.ME1312.Uno.Network.PacketOut;
import net.ME1312.Uno.Network.SubDataServer;
import net.ME1312.Uno.UnoServer;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLConnection;

/**
 * Authorization Packet
 */
public final class PacketAuthorization implements PacketIn, PacketOut {
    private UnoServer server;
    private int response;
    private String message;
    private Logger log;

    /**
     * New PacketAuthorization (In)
     *
     * @param server UnoServer
     */
    public PacketAuthorization(UnoServer server) {
        if (Util.isNull(server)) throw new NullPointerException();
        this.server = server;
        try {
            Field f = SubDataServer.class.getDeclaredField("log");
            f.setAccessible(true);
            this.log = (Logger) f.get(null);
            f.setAccessible(false);
        } catch (IllegalAccessException | NoSuchFieldException e) {}
    }

    /**
     * New PacketAuthorization (Out)
     *
     * @param response Response ID
     * @param message Message
     */
    public PacketAuthorization(int response, String message) {
        if (Util.isNull(response, message)) throw new NullPointerException();
        this.response = response;
        this.message = message;
    }

    @Override
    public YAMLSection generate() {
        YAMLSection data = new YAMLSection();
        data.set("r", response);
        data.set("m", message);
        return data;
    }

    @Override
    public void execute(Client client, YAMLSection data) {
        try {
            if (data.getRawString("password").equals(server.config.get().getSection("Settings").getSection("SubData").getRawString("Password"))) {
                URLConnection request = new URL("https://www.me1312.net/account/auth.php?token=" + data.getRawString("profile")).openConnection();
                request.addRequestProperty("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
                request.addRequestProperty("accept-encoding", "gzip, deflate, b");
                request.addRequestProperty("accept-language", "en-US,en;q=0.9");
                request.addRequestProperty("cache-control", "max-age=0");
                request.addRequestProperty("cookie", "");
                request.addRequestProperty("upgrade-insecure-requests", "1");
                request.addRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.117 Safari/537.36");
                request.connect();
                JSONObject auth = new JSONObject(Util.ungzip(request.getInputStream()));
                if (auth.getString("error").length() == 0) {
                    if (!server.players.keySet().contains(auth.getJSONObject("profile").getString("name")) || server.players.get(auth.getJSONObject("profile").getString("name")).getSubData() == null) {
                        client.authorize(auth.getJSONObject("profile"));
                        Player player;
                        if (server.players.keySet().contains(auth.getJSONObject("profile").getString("name"))) {
                            player = server.players.get(auth.getJSONObject("profile").getString("name"));
                        } else {
                            player = new Player(server, auth.getJSONObject("profile"));
                            for (Client other : server.subdata.getClients())
                                if (client != other && other.isAuthorized()) other.sendPacket(new PacketOutPlayerJoin(player));
                        }
                        client.setHandler(player);
                        server.players.put(auth.getJSONObject("profile").getString("name"), player);
                        client.sendPacket(new PacketAuthorization(0, auth.getJSONObject("profile").getString("name")));
                    } else {
                        client.sendPacket(new PacketAuthorization(4, "Already Logged In"));
                    }
                } else {
                    client.sendPacket(new PacketAuthorization(3, "Invalid Profile"));
                }
            } else {
                client.sendPacket(new PacketAuthorization(2, "Invalid Password"));
            }
        } catch (Exception e) {
            client.sendPacket(new PacketAuthorization(1, e.getClass().getCanonicalName() + ": " + e.getMessage()));
            log.error.println(e);
        }
    }

    @Override
    public Version getVersion() {
        return new Version("1.0a");
    }
}
