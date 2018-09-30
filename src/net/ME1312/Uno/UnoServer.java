package net.ME1312.Uno;

import net.ME1312.Galaxi.Engine.GalaxiEngine;
import net.ME1312.Galaxi.Library.Config.YAMLConfig;
import net.ME1312.Galaxi.Library.Log.Logger;
import net.ME1312.Galaxi.Library.UniversalFile;
import net.ME1312.Galaxi.Library.Version.Version;
import net.ME1312.Galaxi.Plugin.Plugin;
import net.ME1312.Galaxi.Plugin.PluginInfo;
import net.ME1312.Uno.Game.Game;
import net.ME1312.Uno.Game.GameRule;
import net.ME1312.Uno.Game.Player;
import net.ME1312.Uno.Library.*;
import net.ME1312.Uno.Network.SubDataServer;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.util.*;

/**
 * Uno Main Class
 */
@Plugin(name = "UnoServer", version = "1.2a", authors = "ME1312", description = "Play Uno through ME1312.net", website = "https://www.me1312.net/uno")
public final class UnoServer {
    public List<GameRule> rules = new ArrayList<GameRule>();
    public LinkedHashMap<String, Player> players = new LinkedHashMap<String, Player>();

    public Logger log;
    public YAMLConfig config;
    public SubDataServer subdata = null;
    public Game game = null;
    public Game lastGame = null;
    public PluginInfo app = null;

    private static UnoServer instance;

    /**
     * Uno Launch
     *
     * @param args Args
     */
    public static void main(String[] args) {
        instance = new UnoServer();
        instance.init(args);
    }

    private void init(String[] args) {
        log = new Logger("Server");
        try {
            app = PluginInfo.getPluginInfo(this);
            app.setLogger(log);
            GalaxiEngine engine = GalaxiEngine.init(app);

            log.info.println("Loading Uno v" + app.getVersion().toString() + " Libraries");
            File dir = engine.getRuntimeDirectory();
            dir.mkdirs();
            if (!(new UniversalFile(dir, "config.yml").exists())) {
                Util.copyFromJar(UnoServer.class.getClassLoader(), "net/ME1312/Uno/Library/Files/config.yml", new UniversalFile(dir, "config.yml").getPath());
                log.info.println("Created ~/config.yml");
            } else if ((new Version((new YAMLConfig(new UniversalFile(dir, "config.yml"))).get().getSection("Settings").getString("Version", "0")).compareTo(new Version("1.0a+"))) != 0) {
                Files.move(new UniversalFile(dir, "config.yml").toPath(), new UniversalFile(dir, "config.old" + Math.round(Math.random() * 100000) + ".yml").toPath());

                Util.copyFromJar(UnoServer.class.getClassLoader(), "net/ME1312/Uno/Library/Files/config.yml", new UniversalFile(dir, "config.yml").getPath());
                log.info.println("Updated ~/config.yml");
            }
            config = new YAMLConfig(new UniversalFile(dir, "config.yml"));

            for (String rule : config.get().getSection("Settings").getRawStringList("Game-Rules")) {
                rule = rule.replace(' ', '_').replaceAll("[^A-Za-z0-9_]", "").toUpperCase();
                try {
                    rules.add(GameRule.valueOf(rule));
                    log.info.println("Enabled Rule: " + rule);
                } catch (Exception e) {
                    log.error.println(new InvocationTargetException(e, "Could not enable rule: " + rule));
                }
            }

            subdata = new SubDataServer(this, Integer.parseInt(config.get().getSection("Settings").getSection("SubData").getRawString("Address", "127.0.0.1:24392").split(":")[1]),
                    (config.get().getSection("Settings").getSection("SubData").getRawString("Address", "127.0.0.1:24392").split(":")[0].equals("0.0.0.0"))?null:InetAddress.getByName(config.get().getSection("Settings").getSection("SubData").getRawString("Address", "127.0.0.1:24392").split(":")[0]));
            log.info.println("Server Listening on ws://" + config.get().getSection("Settings").getSection("SubData").getRawString("Address", "127.0.0.1:24392") + "/game");
            loadDefaults();

            engine.start(this::stop);
        } catch (Exception e) {
            log.error.println(e);
            if (GalaxiEngine.getInstance() != null) {
                GalaxiEngine.getInstance().stop();
            } else {
                System.exit(1);
            }
        }
    }

    private void loadDefaults() {
        UnoCommand.load(this);
    }

    /**
     * Get Uno Instance
     *
     * @return Uno Instance
     */
    public static UnoServer getInstance() {
        return instance;
    }

    public Player getPlayer(String tag) {
        long id = 0;
        if (!tag.startsWith("+")) {
            if (tag.startsWith("@")) tag = tag.substring(1);
            if (tag.contains("#")) {
                id = Long.parseLong(tag.substring(tag.split("#")[0].length() + 1).toLowerCase(), 36);
                tag = tag.substring(0, tag.split("#")[0].length());
            }
        } else id = Integer.parseInt(tag.substring(1));
        tag = tag.toLowerCase();

        if (id > 0) {
            HashMap<String, String> idbased = new HashMap<String, String>();
            for (String value : players.keySet()) idbased.put(Long.toString(players.get(value).getProfile().getLong("id")), value);
            return players.get(idbased.get(Long.toString(id)));
        } else {
            HashMap<String, String> insensitive = new HashMap<String, String>();
            for (String value : players.keySet()) insensitive.put(value.toLowerCase(), value);
            return players.get(insensitive.get(tag));
        }
    }

    /**
     * Stop Uno
     */
    private void stop() {
        log.info.println("Shutting down...");

        if (subdata != null) Util.isException(() -> subdata.destroy());
    }
}