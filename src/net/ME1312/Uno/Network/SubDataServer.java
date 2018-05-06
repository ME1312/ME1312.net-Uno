package net.ME1312.Uno.Network;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import net.ME1312.Uno.Library.Container;
import net.ME1312.Uno.Library.Config.YAMLSection;
import net.ME1312.Uno.Library.Exception.IllegalPacketException;
import net.ME1312.Uno.Library.Log.Logger;
import net.ME1312.Uno.Library.Util;
import net.ME1312.Uno.Library.Version.Version;
import net.ME1312.Uno.Network.Packet.*;
import net.ME1312.Uno.UnoServer;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.util.*;

/**
 * SubDataServer Class
 */
public final class SubDataServer {
    private static HashMap<Class<? extends PacketOut>, String> pOut = new HashMap<Class<? extends PacketOut>, String>();
    private static HashMap<String, List<PacketIn>> pIn = new HashMap<String, List<PacketIn>>();
    private static boolean defaults = false;
    private HashMap<String, Client> clients = new HashMap<String, Client>();
    private EventLoopGroup bossGroup;
    private EventLoopGroup workGroup;
    private Channel webserver;
    protected UnoServer server;
    static Logger log = new Logger("SubData");

    /**
     * SubData Server Instance
     *
     * @param server SubPlugin
     * @param port Port
     * @param address Bind
     * @throws IOException
     */
    public SubDataServer(UnoServer server, int port, InetAddress address) throws Exception {
        if (Util.isNull(server, port)) throw new NullPointerException();

        this.server = server;

        final SslContext sslCtx;
        if (System.getProperty("ssl") != null) {
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
        } else {
            sslCtx = null;
        }

        bossGroup = new NioEventLoopGroup(1);
        workGroup = new NioEventLoopGroup();
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    private static final String WEBSOCKET_PATH = "/game";

                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        Container<Boolean> first = new Container<Boolean>(true);
                        InetSocketAddress remoteAddress = ch.remoteAddress();
                        ChannelPipeline pipeline = ch.pipeline();
                        if (sslCtx != null) {
                            pipeline.addLast(sslCtx.newHandler(ch.alloc()));
                        }
                        pipeline.addLast(new HttpServerCodec());
                        pipeline.addLast(new HttpObjectAggregator(65536));
                        pipeline.addLast(new WebSocketServerCompressionHandler());
                        pipeline.addLast(new WebSocketServerProtocolHandler(WEBSOCKET_PATH, null, true));
                        pipeline.addLast(new WebServer());
                        pipeline.addLast(new SimpleChannelInboundHandler<WebSocketFrame>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
                                if (frame instanceof TextWebSocketFrame) {
                                    if (!clients.keySet().contains(remoteAddress.toString())) addClient(ch);
                                    clients.get(remoteAddress.toString()).recievePacket(((TextWebSocketFrame) frame).text());
                                } else {
                                    String message = "unsupported frame type: " + frame.getClass().getName();
                                    throw new UnsupportedOperationException(message);
                                }
                            }
                        });
                        ch.closeFuture().addListener(future -> {
                            if (clients.keySet().contains(remoteAddress.toString())) {
                                JSONObject profile = clients.get(remoteAddress.toString()).getProfile();
                                removeClient(clients.get(remoteAddress.toString()));
                                if (profile != null) for (Client other : getClients()) {
                                    if (other.isAuthorized()) other.sendPacket(new PacketMessage(null, profile.getString("displayName") + " has left the chat room"));
                                }
                            }
                        });
                    }
                });
        webserver = ((address == null)?b.bind(port):b.bind(new InetSocketAddress(address, port))).sync().channel();

        if (!defaults) loadDefaults();
    }

    private void loadDefaults() {
        defaults = true;

        registerPacket(new PacketAuthorization(server), "Authorization");
        registerPacket(new PacketInCallOut(server), "CallOut");
        registerPacket(new PacketChangeColor(server), "ChangeColor");
        registerPacket(new PacketPlayerList(server), "PlayerList");
        registerPacket(new PacketInDrawCard(server), "DrawCard");
        registerPacket(new PacketInSpectateGame(server), "SpectateGame");
        registerPacket(new PacketMessage(server), "Message");
        registerPacket(new PacketPlayCard(server), "PlayCard");
        registerPacket(new PacketSwapHand(server), "SwapHand");

        registerPacket(PacketAuthorization.class, "Authorization");
        registerPacket(PacketChangeColor.class, "ChangeColor");
        registerPacket(PacketMessage.class, "Message");
        registerPacket(PacketOutAddCard.class, "AddCard");
        registerPacket(PacketOutAlert.class, "Alert");
        registerPacket(PacketOutBeginTurn.class, "BeginTurn");
        registerPacket(PacketOutEndGame.class, "EndGame");
        registerPacket(PacketOutEndTurn.class, "EndTurn");
        registerPacket(PacketOutPlayerJoin.class, "PlayerJoin");
        registerPacket(PacketOutPlayerQuit.class, "PlayerQuit");
        registerPacket(PacketOutStartGame.class, "StartGame");
        registerPacket(PacketOutUpdateColor.class, "UpdateColor");
        registerPacket(PacketOutUpdateHand.class, "UpdateHand");
        registerPacket(PacketOutUpdateStat.class, "UpdateStat");
        registerPacket(PacketPlayCard.class, "PlayCard");
        registerPacket(PacketPlayerList.class, "PlayerList");
        registerPacket(PacketSwapHand.class, "SwapHand");
    }

    /**
     * Gets the Server Socket
     *
     * @return Server Socket
     */
    public Channel getServer() {
        return webserver;
    }

    /**
     * Add a Client to the Network
     *
     * @param socket Client to add
     * @throws IOException
     */
    public Client addClient(SocketChannel socket) throws IOException {
        if (Util.isNull(socket)) throw new NullPointerException();
        Client client = new Client(this, socket);
        log.info.println(client.getAddress().toString() + " has connected");
        clients.put(client.getAddress().toString(), client);
        return client;
    }

    /**
     * Grabs a Client from the Network
     *
     * @param socket Socket to search
     * @return Client
     */
    public Client getClient(Socket socket) {
        if (Util.isNull(socket)) throw new NullPointerException();
        return clients.get(new InetSocketAddress(socket.getInetAddress(), socket.getPort()).toString());
    }

    /**
     * Grabs a Client from the Network
     *
     * @param address Address to search
     * @return Client
     */
    public Client getClient(InetSocketAddress address) {
        if (Util.isNull(address)) throw new NullPointerException();
        return clients.get(address.toString());
    }

    /**
     * Grabs a Client from the Network
     *
     * @param address Address to search
     * @return Client
     */
    public Client getClient(String address) {
        if (Util.isNull(address)) throw new NullPointerException();
        return clients.get(address);
    }

    /**
     * Grabs all the Clients on the Network
     *
     * @return Client List
     */
    public Collection<Client> getClients() {
        return clients.values();
    }

    /**
     * Remove a Client from the Network
     *
     * @param client Client to Kick
     * @throws IOException
     */
    public void removeClient(Client client) throws IOException {
        if (Util.isNull(client)) throw new NullPointerException();
        SocketAddress address = client.getAddress();
        if (clients.keySet().contains(address.toString())) {
            clients.remove(address.toString());
            if (!client.closed) client.disconnect();
            log.info.println(client.getAddress().toString() + ((client.getProfile() == null)?"":" (" + client.getProfile().getString("displayName") + ")") + " has disconnected");
        }
    }

    /**
     * Remove a Client from the Network
     *
     * @param address Address to Kick
     * @throws IOException
     */
    public void removeClient(InetSocketAddress address) throws IOException {
        if (Util.isNull(address)) throw new NullPointerException();
        Client client = clients.get(address.toString());
        if (clients.keySet().contains(address.toString())) {
            clients.remove(address.toString());
            client.disconnect();
            log.info.println(client.getAddress().toString() + " has disconnected");
        }
    }

    /**
     * Remove a Client from the Network
     *
     * @param address Address to Kick
     * @throws IOException
     */
    public void removeClient(String address) throws IOException {
        if (Util.isNull(address)) throw new NullPointerException();
        Client client = clients.get(address);
        if (clients.keySet().contains(address)) {
            clients.remove(address);
            client.disconnect();
            log.info.println(client.getAddress().toString() + " has disconnected");
        }
    }

    /**
     * Register PacketIn to the Network
     *
     * @param packet PacketIn to register
     * @param handle Handle to Bind
     */
    public static void registerPacket(PacketIn packet, String handle) {
        if (Util.isNull(packet, handle)) throw new NullPointerException();
        List<PacketIn> list = (pIn.keySet().contains(handle.toLowerCase()))?pIn.get(handle.toLowerCase()):new ArrayList<PacketIn>();
        if (!list.contains(packet)) {
            list.add(packet);
            pIn.put(handle.toLowerCase(), list);
        }
    }

    /**
     * Unregister PacketIn from the Network
     *
     * @param packet PacketIn to unregister
     */
    public static void unregisterPacket(PacketIn packet) {
        if (Util.isNull(packet)) throw new NullPointerException();
        List<String> search = new ArrayList<String>();
        search.addAll(pIn.keySet());
        for (String handle : search) if (pIn.get(handle.toLowerCase()).contains(packet)) {
            List<PacketIn> list = pIn.get(handle.toLowerCase());
            list.remove(packet);
            if (list.isEmpty()) {
                pIn.remove(handle.toLowerCase());
            } else {
                pIn.put(handle.toLowerCase(), list);
            }
        }
    }

    /**
     * Register PacketOut to the Network
     *
     * @param packet PacketOut to register
     * @param handle Handle to bind
     */
    public static void registerPacket(Class<? extends PacketOut> packet, String handle) {
        if (Util.isNull(packet, handle)) throw new NullPointerException();
        pOut.put(packet, handle.toLowerCase());
    }

    /**
     * Unregister PacketOut to the Network
     *
     * @param packet PacketOut to unregister
     */
    public static void unregisterPacket(Class<? extends PacketOut> packet) {
        if (Util.isNull(packet)) throw new NullPointerException();
        pOut.remove(packet);
    }

    /**
     * Grab PacketIn Instances via handle
     *
     * @param handle Handle
     * @return PacketIn
     */
    public static List<? extends PacketIn> getPacket(String handle) {
        if (Util.isNull(handle)) throw new NullPointerException();
        return new ArrayList<PacketIn>(pIn.get(handle.toLowerCase()));
    }

    /**
     * Broadcast a Packet to everything on the Network<br>
     * <b>Warning:</b> There are usually different types of applications on the network at once, they may not recognise the same packet handles
     *
     * @param packet Packet to send
     */
    public void broadcastPacket(PacketOut packet) {
        if (Util.isNull(packet)) throw new NullPointerException();
        List<Client> clients = new ArrayList<Client>();
        clients.addAll(getClients());
        for (Client client : clients) {
            client.sendPacket(packet);
        }
    }

    /**
     * Encode PacketOut
     *
     * @param packet PacketOut
     * @return JSON Formatted Packet
     * @throws IllegalPacketException
     */
    protected static YAMLSection encodePacket(Client client, PacketOut packet) throws IllegalPacketException, InvocationTargetException {
        YAMLSection section = new YAMLSection();

        if (!pOut.keySet().contains(packet.getClass())) throw new IllegalPacketException(packet.getClass().getCanonicalName() + ": Unknown PacketOut Channel: " + packet.getClass().getCanonicalName());
        if (packet.getVersion().toString() == null) throw new NullPointerException(packet.getClass().getCanonicalName() + ": PacketOut getVersion() cannot be null: " + packet.getClass().getCanonicalName());

        try {
            YAMLSection contents = packet.generate();
            section.set("h", pOut.get(packet.getClass()));
            section.set("v", packet.getVersion().toString());
            if (contents != null) section.set("c", contents);
            return section;
        } catch (Throwable e) {
            throw new InvocationTargetException(e, packet.getClass().getCanonicalName() + ": Exception while encoding packet");
        }
    }

    /**
     * Decode PacketIn
     *
     * @param data Data to Decode
     * @return PacketIn
     * @throws IllegalPacketException
     */
    protected static List<PacketIn> decodePacket(Client client, YAMLSection data) throws IllegalPacketException {
        if (!data.contains("h") || !data.contains("v")) throw new IllegalPacketException(client.getAddress().toString() + ": Unknown Packet Format: " + data.toString());
        if (!pIn.keySet().contains(data.getRawString("h"))) throw new IllegalPacketException(client.getAddress().toString() + ": Unknown PacketIn Channel: " + data.getRawString("h"));

        List<PacketIn> list = new ArrayList<PacketIn>();
        for (PacketIn packet : pIn.get(data.getRawString("h"))) {
            if (packet.isCompatible(new Version(data.getRawString("v")))) {
                list.add(packet);
            } else {
                log.error.println(new IllegalPacketException(client.getAddress().toString() + ": Packet Version Mismatch in " + data.getRawString("h") + ": " + data.getRawString("v") + " =/= " + packet.getVersion().toString()));
            }
        }

        return list;
    }

    /**
     * Drops All Connections and Stops the SubData Listener
     *
     * @throws IOException
     */
    public void destroy() throws IOException {
        while(clients.size() > 0) {
            removeClient((Client) clients.values().toArray()[0]);
        }
        bossGroup.shutdownGracefully();
        workGroup.shutdownGracefully();
        webserver.close();
        log.info.println("The SubData Listener has been closed");
    }
}
