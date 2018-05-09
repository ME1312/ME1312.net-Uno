package net.ME1312.Uno;

import net.ME1312.Uno.Game.Card;
import net.ME1312.Uno.Game.Game;
import net.ME1312.Uno.Game.GameRule;
import net.ME1312.Uno.Game.Player;
import net.ME1312.Uno.Library.Command;
import net.ME1312.Uno.Library.Util;
import net.ME1312.Uno.Network.Packet.PacketOutAlert;
import net.ME1312.Uno.Network.Packet.PacketOutUpdateHand;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Command Class
 */
public class UnoCommand {
    private UnoCommand() {}
    protected static void load(UnoServer server) {
        new Command() {
            @Override
            public void command(String handle, String[] args) {
                server.log.message.println(
                        System.getProperty("os.name") + ' ' + System.getProperty("os.version") + ',',
                        "Java " + System.getProperty("java.version") + ',',
                        "Uno v" + server.version.toExtendedString());
            }
        }.usage("[plugin]").description("Gets the version of the System and Uno or the specified Plugin").help(
                "This command will print what OS you're running, your OS version,",
                "your Java version, and the Uno version.",
                "",
                "Example:",
                "  /version"
        ).register("ver", "version");
        new Command() {
            @Override
            public void command(String handle, String[] args) {
                if (server.players.size() >= 2) {
                    boolean restart = false;
                    if (server.game != null) {
                        server.game.stop();
                        restart = true;
                    }
                    server.log.info.println(((restart)?"Res":"S") + "tarting Uno...");
                    LinkedList<Player> players = new LinkedList<Player>();
                    players.addAll(server.players.values());
                    LinkedList<GameRule> rules = new LinkedList<GameRule>();
                    rules.addAll(server.rules);
                    server.lastGame = null;
                    server.game = new Game(server, players, rules, server.config.get().getSection("Settings").getInt("Starting-Cards", 7), server.config.get().getSection("Settings").getInt("Turn-Timeout", 60));
                    if (restart) {
                        for (Player other : players)
                            other.getSubData().sendPacket(new PacketOutAlert("The game has been reset"));
                    }
                } else {
                    server.log.message.println("Uno must be played with two or more players");
                }
            }
        }.description("Starts the game").help(
                "This command will start a game of Uno.",
                "",
                "Example:",
                "  /start"
        ).register("start");
        new Command() {
            @Override
            public void command(String handle, String[] args) {
                if (server.lastGame != null) {
                    server.game = server.lastGame;
                    server.lastGame = null;
                    if (server.game.start()) {
                        server.log.info.println("Continuing the previous Uno game...");
                        for (Player player : server.players.values()) {
                            if (!server.game.getPlayers().contains(player)) {
                                server.game.addSpectator(player);
                                player.getSubData().sendPacket(new PacketOutAlert("The game continues on"));
                            }
                        }
                    } else {
                        server.log.info.println("The previous uno game can not be continued");
                    }
                } else {
                    server.log.message.println("An uno game has not yet been finished");
                }
            }
        }.description("Continues a finished game").help(
                "This command will continue the previous game of Uno.",
                "",
                "Example:",
                "  /continue"
        ).register("continue");
        LinkedList<String> help = new LinkedList<String>();
        help.add("This command toggles whether or not a rule is activated,");
        help.add("for the next game of Uno.");
        help.add("");
        help.add("If no arguments are supplied, it will instead print");
        help.add("a list of what rules are enabled.");
        help.add("");
        help.add("Currently, the following rules are available:");
        for (GameRule rule : GameRule.values()) {
            help.add("  - " + rule.toString());
        }
        help.add("");
        help.add("Examples:");
        help.add("  /gamerule");
        help.add("  /gamerule Stacking");
        new Command() {
            @Override
            public void command(String handle, String[] args) {
                if (args.length > 0) {
                    String str = args[0];
                    for (int i = 1; i < args.length; i++) {
                        str += "_" + args[i];
                    }
                    str = str.replaceAll("[^A-Za-z0-9_]", "").toUpperCase();

                    try {
                        GameRule rule = GameRule.valueOf(str);
                        if (server.rules.contains(rule)) {
                            server.rules.remove(rule);
                            server.log.message.println(rule.toString() + " has been disabled");
                        } else {
                            server.rules.add(rule);
                            server.log.message.println(rule.toString() + " has been enabled");
                        }
                    } catch (Exception e) {
                        server.log.error.println(new InvocationTargetException(e, "Could not toggle rule: " + str));
                    }
                } else if (!server.rules.isEmpty()) {
                    server.log.message.println("The following rules are enabled:");
                    for (GameRule rule : server.rules) {
                        server.log.message.println("  - " + rule.toString());
                    }
                } else {
                    server.log.message.println("There are no extra rules enabled");
                }
            }
        }.description("Toggles game rules").usage("[rule]").help(help.toArray(new String[help.size()])).register("gamerule", "rule");
        help = new LinkedList<String>();
        help.add("This command will set a player's deck using the");
        help.add("card types supplied as arguments.");
        help.add("");
        help.add("Currently, the following card types are available:");
        for (Card card : Card.values()) {
            help.add("  - " + card.toString());
        }
        help.add("");
        help.add("Examples:");
        help.add("  /rig ME1312 WD8");
        help.add("  /rig ME1312 WD8 WD4");
        new Command() {
            @Override
            public void command(String handle, String[] args) {
                if (args.length > 1) {
                    if (server.game != null) {
                        Player player;
                        if (!server.hasPlayer(args[0])) {
                            server.log.message.println("There is no player with that tag");
                        } else if (!server.game.getPlayers().contains(player = server.getPlayer(args[0]))) {
                            server.log.message.println("That player is not playing Uno");
                        } else {
                            try {
                                player.setPlaying(false);
                                ArrayList<String> cids = new ArrayList<String>();
                                cids.addAll(player.getCards().keySet());
                                for (String id : cids) {
                                    player.removeCard(id);
                                }
                                LinkedList<String> cards = new LinkedList<String>();
                                cards.addAll(Arrays.asList(args));
                                cards.remove(0);
                                for (String card : cards) {
                                    card = card.replace(' ', '_').replaceAll("[^A-Za-z0-9_]", "").toUpperCase();
                                    try {
                                        player.addCard(Card.valueOf(card));
                                    } catch (Exception e) {
                                        server.log.error.println(new InvocationTargetException(e, "Could not find card: " + card));
                                    }
                                }
                                if (player.getCards().size() <= 0) player.addCard(Card.YM);
                                player.setPlaying(true);
                                for (Player other : server.game.getAllPlayers()) {
                                    other.getSubData().sendPacket(new PacketOutUpdateHand(server.game, other));
                                }
                                server.game.beginTurn();
                            } catch (Exception e) {
                                server.log.error.println(e);
                            }
                        }
                    } else {
                        server.log.message.println("There is no Uno game running at the moment");
                    }
                }else {
                    server.log.message.println("Usage: /" + handle + " <player> <cards...>");
                }
            }
        }.description("Sets a players deck").usage("<player>", "<cards...>").help(help.toArray(new String[help.size()])).register("stack", "rig");
        new Command() {
            @Override
            public void command(String handle, String[] args) {
                if (server.game != null) {
                    server.game.stop();
                } else {
                    server.log.message.println("There is no Uno game running at the moment");
                }
            }
        }.description("Stops the game").help(
                "This command will stop a currently running Uno game.",
                "",
                "Example:",
                "  /stop"
        ).register("stop");
        new Command() {
            public void command(String handle, String[] args) {
                HashMap<String, String> commands = new LinkedHashMap<String, String>();
                HashMap<Command, String> handles = new LinkedHashMap<Command, String>();

                int length = 0;
                for(String command : server.commands.keySet()) {
                    String formatted = "/ ";
                    Command cmd = server.commands.get(command);
                    String alias = (handles.keySet().contains(cmd))?handles.get(cmd):null;

                    if (alias != null) formatted = commands.get(alias);
                    if (cmd.usage().length == 0 || alias != null) {
                        formatted = formatted.replaceFirst("\\s", ((alias != null)?"|":"") + command + ' ');
                    } else {
                        String usage = "";
                        for (String str : cmd.usage()) usage += ((usage.length() == 0)?"":" ") + str;
                        formatted = formatted.replaceFirst("\\s", command + ' ' + usage + ' ');
                    }
                    if(formatted.length() > length) {
                        length = formatted.length();
                    }

                    if (alias == null) {
                        commands.put(command, formatted);
                        handles.put(cmd, command);
                    } else {
                        commands.put(alias, formatted);
                    }
                }

                if (args.length == 0) {
                    server.log.message.println("Uno Command List:");
                    for (String command : commands.keySet()) {
                        String formatted = commands.get(command);
                        Command cmd = server.commands.get(command);

                        while (formatted.length() < length) {
                            formatted += ' ';
                        }
                        formatted += ((cmd.description() == null || cmd.description().length() == 0)?"  ":"- "+cmd.description());

                        server.log.message.println(formatted);
                    }
                } else if (server.commands.keySet().contains((args[0].startsWith("/"))?args[0].toLowerCase().substring(1):args[0].toLowerCase())) {
                    Command cmd = server.commands.get((args[0].startsWith("/"))?args[0].toLowerCase().substring(1):args[0].toLowerCase());
                    String formatted = commands.get(Util.getBackwards(server.commands, cmd).get(0));
                    server.log.message.println(formatted.substring(0, formatted.length() - 1));
                    for (String line : cmd.help()) {
                        server.log.message.println("  " + line);
                    }
                } else {
                    server.log.message.println("There is no command with that name");
                }
            }
        }.usage("[command]").description("Prints a list of the commands and/or their descriptions").help(
                "This command will print a list of all currently registered commands and aliases,",
                "along with their usage and a short description.",
                "",
                "If the [command] option is provided, it will print that command, it's aliases,",
                "it's usage, and an extended description like the one you see here instead.",
                "",
                "Examples:",
                "  /help",
                "  /help end"
        ).register("help", "?");
        new Command() {
            @Override
            public void command(String handle, String[] args) {
                server.stop(0);
            }
        }.description("Stops this Uno instance").help(
                "This command will shutdown this instance of UnoServer.",
                "",
                "Example:",
                "  /exit"
        ).register("exit", "end");
    }
}
