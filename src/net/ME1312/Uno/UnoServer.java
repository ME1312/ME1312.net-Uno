package net.ME1312.Uno;

import jline.console.ConsoleReader;
import net.ME1312.Uno.Game.Game;
import net.ME1312.Uno.Game.GameRule;
import net.ME1312.Uno.Game.Player;
import net.ME1312.Uno.Library.*;
import net.ME1312.Uno.Library.Config.YAMLConfig;
import net.ME1312.Uno.Library.Config.YAMLSection;
import net.ME1312.Uno.Library.Log.FileLogger;
import net.ME1312.Uno.Library.Log.Logger;
import net.ME1312.Uno.Library.Version.Version;
import net.ME1312.Uno.Library.Version.VersionType;
import net.ME1312.Uno.Network.SubDataServer;
import org.fusesource.jansi.AnsiConsole;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Uno Main Class
 */
public final class UnoServer {
    public List<GameRule> rules = new ArrayList<GameRule>();
    public LinkedHashMap<String, Player> players = new LinkedHashMap<String, Player>();
    public final TreeMap<String, Command> commands = new TreeMap<String, Command>();
    private final List<String> knownClasses = new ArrayList<String>();

    public Logger log;
    public final UniversalFile dir = new UniversalFile(new File(System.getProperty("user.dir")));
    public YAMLConfig config;
    public SubDataServer subdata = null;
    public Game game = null;
    public Game lastGame = null;
    public final Version version = new Version("1.2a");
    //public final Version version = new Version(new Version("1.1a"), VersionType.BETA, 1); // TODO Beta Version Setting

    private static UnoServer instance;
    private ConsoleReader jline;
    private boolean ready = false;

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
        try {
            JarFile jarFile = new JarFile(new File(UnoServer.class.getProtectionDomain().getCodeSource().getLocation().toURI()));
            Enumeration<JarEntry> entries = jarFile.entries();

            boolean isplugin = false;
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                    knownClasses.add(entry.getName().substring(0, entry.getName().length() - 6).replace('/', '.'));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        log = new Logger("Server");
        try {
            jline = new ConsoleReader(System.in, AnsiConsole.out());
            Logger.setup(AnsiConsole.out(), AnsiConsole.err(), jline, dir);
            log.info.println("Loading Uno v" + version.toString() + " Libraries");
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
            log.info.println("SubData WebDirect Listening on ws://" + config.get().getSection("Settings").getSection("SubData").getRawString("Address", "127.0.0.1:24392") + "/game");
            loadDefaults();

            loop();
        } catch (Exception e) {
            log.error.println(e);
            stop(1);
        }
    }

    public void reload() throws IOException {
        if (subdata != null)
            subdata.destroy();

        config.reload();
        try {
            subdata = new SubDataServer(this, Integer.parseInt(config.get().getSection("Settings").getSection("SubData").getRawString("Address", "127.0.0.1:24392").split(":")[1]),
                    (config.get().getSection("Settings").getSection("SubData").getRawString("Address", "127.0.0.1:24392").split(":")[0].equals("0.0.0.0"))?null:InetAddress.getByName(config.get().getSection("Settings").getSection("SubData").getRawString("Address", "127.0.0.1:24392").split(":")[0]));
        } catch (Exception e) {
            log.error.println(e);
        }
    }

    private void loop() throws Exception {
        String umsg;
        ready = true;
        while (ready && (umsg = jline.readLine(">")) != null) {
            if (!ready || umsg.equals("")) continue;
            final String cmd = (umsg.startsWith("/"))?((umsg.contains(" ")?umsg.split(" "):new String[]{umsg})[0].substring(1)):((umsg.contains(" ")?umsg.split(" "):new String[]{umsg})[0]);
            if (commands.keySet().contains(cmd.toLowerCase())) {
                ArrayList<String> args = new ArrayList<String>();
                args.addAll(Arrays.asList(umsg.contains(" ")?umsg.split(" "):new String[]{umsg}));
                args.remove(0);
                try {
                    commands.get(cmd.toLowerCase()).command(cmd, args.toArray(new String[args.size()]));
                } catch (Exception e) {
                    log.error.println(new InvocationTargetException(e, "Uncaught exception while running command"));
                }
            } else {
                log.message.println("Unknown Command - " + umsg);
            }
            jline.getOutput().write("\b \b");
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

    public Player getPlayer(String name) {
        HashMap<String, String> insensitive = new HashMap<String, String>();
        for (String value : players.keySet()) insensitive.put(value.toLowerCase(), value);
        return players.get(insensitive.get(name.toLowerCase()));
    }

    public boolean hasPlayer(String name) {
        HashMap<String, String> insensitive = new HashMap<String, String>();
        for (String value : players.keySet()) insensitive.put(value.toLowerCase(), value);
        return insensitive.keySet().contains(name.toLowerCase());
    }

    /**
     * Stop Uno
     *
     * @param exit Exit Code
     */
    public void stop(int exit) {
        if (ready) {
            log.info.println("Shutting down...");
            ready = false;

            if (subdata != null) Util.isException(() -> subdata.destroy());

            Util.isException(FileLogger::end);
            System.exit(exit);
        }
    }
}