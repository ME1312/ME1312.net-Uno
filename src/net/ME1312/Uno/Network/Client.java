package net.ME1312.Uno.Network;

import net.ME1312.Galaxi.Library.Config.YAMLSection;
import net.ME1312.Galaxi.Library.Util;
import net.ME1312.Uno.Library.Exception.IllegalPacketException;
import net.ME1312.Uno.Network.Packet.PacketAuthorization;
import net.ME1312.Uno.Network.Packet.PacketMessage;

import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.json.JSONException;
import org.json.JSONObject;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Network Client Class
 */
public class Client {
    private SocketChannel channel;
    private InetSocketAddress address;
    private ClientHandler handler;
    private JSONObject profile = null;
    private Timer authorized;
    private SubDataServer subdata;
    boolean closed;

    /**
     * Network Client
     *
     * @param subdata SubData Direct Server
     * @param channel Channel to bind
     */
    public Client(SubDataServer subdata, SocketChannel channel) {
        if (Util.isNull(subdata, channel)) throw new NullPointerException();
        this.subdata = subdata;
        closed = false;
        this.channel = channel;
        address = channel.remoteAddress();
        authorized = new Timer();
        authorized.schedule(new TimerTask() {
            @Override
            public void run() {
                if (channel.isActive()) try {
                    subdata.removeClient(Client.this);
                } catch (IOException e) {
                    subdata.log.error.println(e);
                }
            }
        }, 15000);
    }

    void recievePacket(String input) {
        try {
            YAMLSection data = new YAMLSection(new JSONObject(input));
            for (PacketIn packet : SubDataServer.decodePacket(this, data)) {
                boolean auth = authorized == null;
                if (auth || packet instanceof PacketAuthorization) {
                    try {
                        if (data.contains("f")) {
                            if (data.getString("f").length() <= 0) {
                                List<Client> clients = new ArrayList<Client>();
                                clients.addAll(subdata.getClients());
                                for (Client client : clients) {
                                    client.channel.write(new TextWebSocketFrame(input));
                                }
                            } else {
                                Client client = subdata.getClient(data.getString("f"));
                                if (client != null) {
                                    client.channel.write(new TextWebSocketFrame(input));
                                } else {
                                    throw new IllegalPacketException(getAddress().toString() + ": Unknown Forward Address: " + data.getString("f"));
                                }
                            }
                        } else {
                            packet.execute(Client.this, (data.contains("c")) ? data.getMap("c") : null);
                        }
                    } catch (Throwable e) {
                        new InvocationTargetException(e, getAddress().toString() + ": Exception while executing PacketIn").printStackTrace();
                    }
                } else {
                    sendPacket(new PacketAuthorization(-1, "Unauthorized"));
                    throw new IllegalPacketException(getAddress().toString() + ": Unauthorized call to packet type: " + data.getMap("h"));
                }
            }
        } catch (JSONException | YAMLException e) {
            new IllegalPacketException(getAddress().toString() + ": Unknown Packet Format: " + input).printStackTrace();
        } catch (IllegalPacketException e) {
            subdata.log.error.println(e);
        } catch (Exception e) {
            new InvocationTargetException(e, getAddress().toString() + ": Exception while decoding packet").printStackTrace();
        }
    }

    /**
     * Authorize Connection
     */
    public void authorize(JSONObject profile) {
        if (authorized != null) {
            authorized.cancel();
            authorized = null;
            this.profile = profile;
            subdata.log.info.println(channel.remoteAddress().toString() + " logged in as: " + profile.getString("displayName") + " (" + (((!profile.getString("name").equals("+" + profile.getLong("id")))?'@':"")) + profile.getString("name") + ")");
            for (Client other : subdata.getClients()) {
                if (other.isAuthorized()) other.sendPacket(new PacketMessage(null, profile.getString("displayName") + " has joined the chat room"));
            }
        }
    }

    /**
     * Send Packet to Client
     *
     * @param packet Packet to send
     */
    public void sendPacket(PacketOut packet) {
        if (Util.isNull(packet)) throw new NullPointerException();
        try {
            channel.write(new TextWebSocketFrame(new YAMLSection(SubDataServer.encodePacket(this, packet)).toJSON().toString()));
            channel.flush();
        } catch (Throwable e) {
            subdata.log.error.println(e);
        }
    }

    /**
     * Get Raw Connection
     *
     * @return Socket
     */
    public SocketChannel getConnection() {
        return channel;
    }

    /**
     * Get if the connection has been closed
     *
     * @return Closed Status
     */
    public boolean isClosed() {
        return closed && !channel.isActive();
    }

    /**
     * Get Remote Address
     *
     * @return Address
     */
    public InetSocketAddress getAddress() {
        return address;
    }

    /**
     * If the connection is authorized
     *
     * @return Authorization Status
     */
    public boolean isAuthorized() {
        return authorized == null;
    }

    /**
     * Get Client Profile (null if unauthorized)
     *
     * @return Profile
     */
    public JSONObject getProfile() {
        return profile;
    }

    /**
     * Gets the Linked Handler
     *
     * @return Handler
     */
    public ClientHandler getHandler() {
        return handler;
    }

    /**
     * Sets the Handler
     *
     * @param obj Handler
     */
    public void setHandler(ClientHandler obj) {
        if (handler != null && handler.getSubData() != null && equals(handler.getSubData())) handler.setSubData(null);
        handler = obj;
        if (handler != null && (handler.getSubData() == null || !equals(handler.getSubData()))) handler.setSubData(this);
    }

    /**
     * Disconnects the Client
     *
     * @throws IOException
     */
    public void disconnect() throws IOException {
        if (channel.isActive()) getConnection().close();
        if (handler != null) {
            setHandler(null);
            handler = null;
        }
        closed = true;
        if (subdata.getClients().contains(this)) subdata.removeClient(this);
    }
}
