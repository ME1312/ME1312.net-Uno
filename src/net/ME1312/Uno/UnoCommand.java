package net.ME1312.Uno;

import net.ME1312.Galaxi.Command.Command;
import net.ME1312.Galaxi.Command.CommandSender;
import net.ME1312.Uno.Game.Card;
import net.ME1312.Uno.Game.Game;
import net.ME1312.Uno.Game.GameRule;
import net.ME1312.Uno.Game.Player;
import net.ME1312.Uno.Network.Packet.PacketOutAlert;
import net.ME1312.Uno.Network.Packet.PacketOutUpdateHand;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Command Class
 */
public class UnoCommand {
    private UnoCommand() {}
    protected static void load(UnoServer server) {
        new Command(server.app) {
            @Override
            public void command(CommandSender sender, String handle, String[] args) {
                server.log.message.println("There are " + server.players.size() + " player" + ((server.players.size() == 1)?"":"s") + " online" + ((server.players.size() > 0)?":":""));
                for (Player player : server.players.values()) {
                    String s = player.getProfile().getString("displayName") + " (";
                    if (player.getProfile().getString("name").equals("+" + player.getProfile().getLong("id"))) {
                        s += player.getProfile().getString("name");
                    } else {
                        s += '@' + player.getProfile().getString("name") + '#' + Long.toString(player.getProfile().getLong("id"), 36).toUpperCase();
                    }
                    s += ')';
                    server.log.message.println("  - " + s);
                }
            }
        }.description("Get the player list").help(
                "This command will print a list of all the players",
                "logged into this server.",
                "",
                "Example:",
                "  /list"
        ).register("list");
        new Command(server.app) {
            @Override
            public void command(CommandSender sender, String handle, String[] args) {
                if (args.length > 0) {
                    String str = args[0];
                    for (int i = 1; i < args.length; i++) {
                        str += "_" + args[i];
                    }
                    server.config.get().getMap("Settings").getMap("SubData").set("Password", str);
                    server.log.message.println("The server's password has been updated");
                } else {
                    server.config.get().getMap("Settings").getMap("SubData").set("Password", "");
                    server.log.message.println("The server's password has been disabled");
                }
            }
        }.description("Change the server password").usage("[password]").help(
                "This command will change the password that",
                "players use to login to the server with.",
                "",
                "If no arguments are supplied,",
                "the server password will be disabled.",
                "",
                "Example:",
                "  /password VerySecure").register("password");
        new Command(server.app) {
            @Override
            public void command(CommandSender sender, String handle, String[] args) {
                if (args.length > 0) {
                    Player player;
                    if ((player = server.getPlayer(args[0])) == null) {
                        server.log.message.println("There is no player with that tag");
                    } else {
                        if (player.getSubData() == null) {
                            server.players.remove(player.getProfile().getString("name"));
                        } else try {
                            player.getSubData().disconnect();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        server.log.message.println(player.getProfile().getString("displayName") + " has been kicked");
                    }
                } else {
                    server.log.message.println("Usage: /" + handle + " <player>");
                }
            }
        }.autocomplete(((sender, handle, args) -> {
            String last = (args.length > 0)?args[args.length - 1].toLowerCase():"";
            List<String> list = new ArrayList<String>();
            if (args.length == 1) {
                if (last.length() == 0) {
                    for (Player player : server.players.values()) {
                        if (!player.getProfile().getString("name").equals("+" + player.getProfile().getLong("id"))) {
                            list.add(player.getProfile().getString("name") + "#" + Long.toString(player.getProfile().getLong("id"), 36));
                        }
                        list.add("+" + player.getProfile().getLong("id"));
                    }
                } else {
                    for (Player player : server.players.values()) {
                        if (!player.getProfile().getString("name").equals("+" + player.getProfile().getLong("id"))) {
                            String tag = player.getProfile().getString("name") + "#" + Long.toString(player.getProfile().getLong("id"), 36);
                            if (tag.toLowerCase().startsWith(last)) list.add(tag);
                        }
                        if (("+" + player.getProfile().getLong("id")).toLowerCase().startsWith(last)) list.add("+" + player.getProfile().getLong("id"));
                    }
                }
                return list.toArray(new String[0]);
            } else {
                return new String[0];
            }
        })).description("Kick a player").usage("<player>").help(
                "This command will kick the specified player",
                "from this server.",
                "",
                "Example:",
                "  /kick ME1312").register("kick");
        new Command(server.app) {
            @Override
            public void command(CommandSender sender, String handle, String[] args) {
                if (server.players.size() >= 2) {
                    boolean restart = false;
                    if (server.game != null) {
                        server.game.stop();
                        restart = true;
                    }
                    LinkedList<Player> players = new LinkedList<Player>();
                    players.addAll(server.players.values());
                    LinkedList<GameRule> rules = new LinkedList<GameRule>();
                    rules.addAll(server.rules);
                    server.lastGame = null;
                    server.game = new Game(server, players, rules, server.config.get().getMap("Settings").getInt("Starting-Cards", 7), server.config.get().getMap("Settings").getInt("Turn-Timeout", 60));
                    if (restart) {
                        for (Player other : players)
                            other.getSubData().sendPacket(new PacketOutAlert("The game has been reset"));
                    }
                } else {
                    server.log.message.println("Uno must be played with two or more players");
                }
            }
        }.description("Starts the game").help(
                "This command will start the game.",
                "",
                "Example:",
                "  /start"
        ).register("start");
        new Command(server.app) {
            @Override
            public void command(CommandSender sender, String handle, String[] args) {
                if (server.lastGame != null) {
                    server.game = server.lastGame;
                    server.lastGame = null;
                    if (server.game.start()) {
                        for (Player player : server.players.values()) {
                            if (!server.game.getPlayers().contains(player)) {
                                server.game.addSpectator(player);
                                player.getSubData().sendPacket(new PacketOutAlert("The game continues on"));
                            }
                        }
                    } else {
                        server.log.message.println("The previous uno game cannot be continued");
                    }
                } else {
                    server.log.message.println("An uno game has not yet been finished");
                }
            }
        }.description("Continues a finished game").help(
                "This command will continue the previous game.",
                "",
                "Example:",
                "  /continue"
        ).register("continue");
        int hi = 0;
        String hs = "";
        LinkedList<String> help = new LinkedList<String>();
        help.add("This command toggles whether or not a rule is activated,");
        help.add("to be used in the next game.");
        help.add("");
        help.add("If no arguments are supplied, it will instead print");
        help.add("a list of what rules are enabled.");
        help.add("");
        help.add("Currently, the following rules are available:");
        for (GameRule rule : GameRule.values()) {
            help.add("  - " + rule.toString());
            hi++;
        }
        help.add("");
        help.add("Examples:");
        help.add("  /gamerule");
        help.add("  /gamerule Stacking");
        new Command(server.app) {
            @Override
            public void command(CommandSender sender, String handle, String[] args) {
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

        hi = 0;
        hs = "  ";
        help = new LinkedList<String>();
        help.add("This command will set a player's deck using the");
        help.add("card types supplied as arguments.");
        help.add("");
        help.add("Currently, the following card types are available:");
        for (Card card : Card.values()) {
            hs += card;
            hi++;
            if (hi != Card.values().length) {
                hs += ", ";
                if (hi % 14 == 0) hs += "\n  ";
            }
        }
        help.addAll(Arrays.asList(hs.split("\\n")));
        help.add("");
        help.add("Examples:");
        help.add("  /rig ME1312 WD8");
        help.add("  /rig ME1312 WD8 WD4");
        new Command(server.app) {
            @Override
            public void command(CommandSender sender, String handle, String[] args) {
                if (args.length > 1) {
                    if (server.game != null) {
                        Player player;
                        if ((player = server.getPlayer(args[0])) == null) {
                            server.log.message.println("There is no player with that tag");
                        } else if (!server.game.getPlayers().contains(player)) {
                            server.log.message.println("That player is not playing Uno");
                        } else {
                            try {
                                player.setPlaying(false);
                                ArrayList<String> cids = new ArrayList<>(player.getCards().keySet());
                                for (String id : cids) {
                                    player.removeCard(id);
                                }

                                boolean first = true;
                                for (String card : args) {
                                    if (first) {
                                        first = false;
                                        continue;
                                    }

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
                                    other.getSubData().sendPacket(new PacketOutAlert(player.getProfile().getString("displayName") + " actually cheated"));
                                }
                                server.game.beginTurn();
                                server.log.message.println(player.getProfile().getString("displayName") + '\'' + ((player.getProfile().getString("displayName").toLowerCase().endsWith("s"))?"":"s") +  " hand has been updated");
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

        new Command(server.app) {
            @Override
            public void command(CommandSender sender, String handle, String[] args) {
                if (server.game != null) {
                    server.game.stop();
                } else {
                    server.log.message.println("There is no Uno game running at the moment");
                }
            }
        }.description("Stops the game").help(
                "This command will stop the game.",
                "",
                "Example:",
                "  /stop"
        ).register("stop");
    }
}
