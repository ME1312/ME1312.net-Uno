package net.ME1312.Uno.Game;

import net.ME1312.Galaxi.Library.Log.Logger;
import net.ME1312.Galaxi.Library.NamedContainer;
import net.ME1312.Uno.Network.Packet.*;
import net.ME1312.Uno.UnoServer;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class Game {
    static Logger log = new Logger("Game");
    private UnoServer server;
    private LinkedList<Player> players;
    private LinkedList<Player> spectators = new LinkedList<Player>();
    private List<GameRule> rules;
    private int initialCardCount;
    private int timeout;
    private LinkedList<CardAction> pendingActions = new LinkedList<CardAction>();

    // Global Current Turn Attributes
    private short direction = 1;
    private int currentPlayer = 0;
    private int lastCardNumber;
    private CardColor lastCardColor;
    private int cardsdrawn = 0;
    private boolean canplay = false;
    private boolean candraw = false;
    private boolean canchangecolor = false;
    private boolean canswaphands = false;
    private boolean stackmode = false;
    private Timer timer = null;

    public Game(UnoServer server, LinkedList<Player> players, List<GameRule> rules, int cards, int timeout) {
        this.server = server;
        this.players = players;
        this.rules = rules;
        this.initialCardCount = cards;
        this.timeout = timeout;

        log.info.println("Starting Uno...");

        Card.WD4.resetAmount();
        if (rules.contains(GameRule.DRAW_8_CARD)) {
            Card.WD4.setAmount(Card.WD4.getAmount() - 1);
            Card.WD8.setAmount(1);
        } else Card.WD8.resetAmount();
        if (rules.contains(GameRule.MYSTERY_CARD)) {
            Card.RM.setAmount(1);
            Card.BM.setAmount(1);
            Card.GM.setAmount(1);
            Card.YM.setAmount(1);
        } else {
            Card.RM.resetAmount();
            Card.BM.resetAmount();
            Card.GM.resetAmount();
            Card.YM.resetAmount();
        }
        Card.resetDeck();

        for (Player player : players) {
            ArrayList<String> cids = new ArrayList<String>();
            cids.addAll(player.getCards().keySet());
            for (String id : cids) {
                player.removeCard(id);
            }
        }
        int i = initialCardCount;
        while (i > 0) {
            for (Player player : players)
                player.addCard(Card.getRandomCard());
            i--;
        }

        Card safecard = null;
        while (safecard == null) {
            Card tmp = Card.getRandomCard();
            if (tmp.getNumber() >= 0 && tmp.getNumber() <= 9)
                safecard = tmp;
        }
        lastCardNumber = safecard.getNumber();
        lastCardColor = safecard.getColor();

        for (Player player : players)
            player.setPlaying(true);
        for (Player player : getAllPlayers())
            player.getSubData().sendPacket(new PacketOutStartGame(this, player));

        currentPlayer = 1;
        beginTurn();
    }

    public boolean start() {
        LinkedList<Player> tmpplayers = new LinkedList<Player>();
        tmpplayers.addAll(players);
        for (Player player : tmpplayers) {
            if (player.getSubData() == null) {
                this.players.remove(player);
            }
        }

        if (players.size() > 1) {
            log.info.println("Restarting Uno...");

            for (Player player : players)
                player.setPlaying(true);
            for (Player player : getAllPlayers()) {
                player.getSubData().sendPacket(new PacketOutStartGame(this, player));
                player.getSubData().sendPacket(new PacketOutUpdateHand(this, player));
                player.getSubData().sendPacket(new PacketOutAlert("The game continues on"));
            }
            if (lastCardColor == CardColor.BLACK) {
                CardColor color = CardColor.values()[new Random().nextInt(4)];
                lastCardColor = color;
                for (Player player : getAllPlayers()) {
                    player.getSubData().sendPacket(new PacketOutUpdateColor(color));
                }
            }
            if (currentPlayer < 0 || currentPlayer >= players.size()) {
                if (direction > 0) {
                    currentPlayer = 0;
                } else {
                    currentPlayer = players.size() - 1;
                }
            }
            beginTurn();
            return true;
        } else {
            server.game = null;
            server.lastGame = this;
            return false;
        }
    }

    public void beginTurn() {
        Player player = players.get(currentPlayer);
        canplay = false;
        candraw = true;
        canchangecolor = false;
        canswaphands = false;
        stackmode = false;
        boolean skipped = false;
        int discard_next = 0;

        // 1st pass of card actions
        for (CardAction action : pendingActions) {
            boolean stop = false;
            switch (action) {
                case DISCARD_NEXT:
                    if (player.getCards().size() > 0) player.removeCard(player.getCards().keySet().toArray(new String[0])[new Random().nextInt(player.getCards().size())]);
                    if (player.getCards().size() <= 0) skipped = true;
                    discard_next++;
                    break;
                case DRAW_NEXT:
                    if (rules.contains(GameRule.STACKING)) {
                        candraw = false;
                        stackmode = true;
                        stop = true;
                    } else skipped = true;
                    break;
                case SKIP_NEXT:
                    skipped = true;
                    break;
            }
            if (stop) break;
        }
        if (discard_next > 0) {
            log.info.println(player.getProfile().getString("displayName") + " was forced to discard " + discard_next + " card" + ((discard_next == 1) ? "" : "s"));
            for (Player other : getAllPlayers()) {
                other.getSubData().sendPacket(new PacketOutAlert(player.getProfile().getString("displayName") + " was forced to discard " + discard_next + " card" + ((discard_next == 1) ? "" : "s")));
                other.getSubData().sendPacket(new PacketOutUpdateHand(this, other));
            }
        }

        // Generate playable card list
        LinkedList<String> cards = new LinkedList<String>();
        for (String id : player.getCards().keySet()) {
            if (stackmode) {
                if (player.getCard(id).getNumber() == 12 || player.getCard(id) == Card.WD4 || player.getCard(id) == Card.WD8)
                    cards.add(id);
            } else {
                if (player.getCard(id).getNumber() == lastCardNumber || player.getCard(id).getColor() == CardColor.BLACK || player.getCard(id).getColor() == lastCardColor)
                    cards.add(id);
            }
        }
        LinkedList<CardAction> tmpactions = new LinkedList<CardAction>();
        tmpactions.addAll(pendingActions);
        if (!skipped && (candraw || cards.size() > 0)) {
            pendingActions.clear();
            if (stackmode) for (CardAction action : tmpactions) {
                if (action == CardAction.DRAW_NEXT) pendingActions.add(CardAction.DRAW_NEXT);
            }

            // Initialize turn
            if (cards.size() > 0) canplay = true;
            player.getSubData().sendPacket(new PacketOutBeginTurn(player, candraw, cards.toArray(new String[cards.size()])));
            for (Player other : getAllPlayers())
                if (player != other) other.getSubData().sendPacket(new PacketOutBeginTurn(player));

            Timer lastimer;
            (timer = (lastimer = new Timer())).schedule(new TimerTask() {
                @Override
                public void run() {
                    if (server.game == Game.this && timer == lastimer && player.getSubData() != null) {
                        player.getSubData().sendPacket(new PacketOutEndTurn());
                        log.info.println(player.getProfile().getString("displayName") + " timed out");
                        for (Player other : getAllPlayers()) {
                            other.getSubData().sendPacket(new PacketOutAlert(player.getProfile().getString("displayName") + " timed out"));
                        }
                        boolean ended = false;
                        if (lastCardColor == CardColor.BLACK) {
                            CardColor color = CardColor.values()[new Random().nextInt(4)];
                            changeColor(color);
                            lastCardColor = color;
                            ended = true;
                        }
                        if (canswaphands) {
                            Random rand = new Random();
                            Player other = null;
                            while (other == null) {
                                Player tmp = players.get(rand.nextInt(players.size()));
                                if (player != tmp)
                                    other = tmp;
                            }
                            swapHands(player, other);
                            ended = true;
                        }
                        if (!ended) {
                            endTurn();
                        }
                    }
                }
            }, TimeUnit.SECONDS.toMillis(timeout));
        } else {
            // 2nd pass on card actions
            if (pendingActions.contains(CardAction.DRAW_NEXT)) {
                int draw_next = 0;
                for (CardAction action : pendingActions)
                    if (action == CardAction.DRAW_NEXT) {
                        player.addCard(Card.getRandomCard());
                        draw_next++;
                        cardsdrawn++;
                    }
                log.info.println(player.getProfile().getString("displayName") + " was forced to draw " + draw_next + " card" + ((draw_next == 1)?"":"s"));
                for (Player other : getAllPlayers()) {
                    other.getSubData().sendPacket(new PacketOutAlert(player.getProfile().getString("displayName") + " was forced to draw " + draw_next + " card" + ((draw_next == 1)?"":"s")));
                }
            } else if (!pendingActions.contains(CardAction.REVERSE)) {
                log.info.println(player.getProfile().getString("displayName") + " got skipped");
                for (Player other : getAllPlayers()) {
                    other.getSubData().sendPacket(new PacketOutAlert(player.getProfile().getString("displayName") + " got skipped"));
                }
            }
            tmpactions.removeFirstOccurrence(CardAction.SKIP_NEXT);
            pendingActions.clear();
            for (CardAction action : tmpactions) {
                if (action == CardAction.SKIP_NEXT) pendingActions.add(CardAction.SKIP_NEXT);
            }
            endTurn();
        }
    }

    public void play(String id) {
        if (canplay) {
            Player player = players.get(currentPlayer);
            boolean canplay = false;
            if (player.getCards().keySet().contains(id)) {
                if (stackmode) {
                    if (player.getCard(id).getNumber() == 12 || player.getCard(id) == Card.WD4 || player.getCard(id) == Card.WD8)
                        canplay = true;
                } else {
                    if (player.getCard(id).getNumber() == lastCardNumber || player.getCard(id).getColor() == CardColor.BLACK || player.getCard(id).getColor() == lastCardColor)
                        canplay = true;
                }
            }
            if (canplay) {
                Card card = player.getCard(id);
                lastCardNumber = card.getNumber();
                lastCardColor = card.getColor();
                log.info.println(player.getProfile().getString("displayName") + " played a " + ((card.getColor() == CardColor.BLACK)?"":card.getColor().toString().substring(0, 1).toUpperCase() + card.getColor().toString().substring(1).toLowerCase() + " ") + card.getName());
                pendingActions.addAll(card.getActions());
                if (card.getNumber() == 13) {
                    Random random = new Random();
                    int i = random.nextInt(10);
                    while (i > 0) {
                        boolean enabled = true;
                        CardAction action = CardAction.values()[random.nextInt(CardAction.values().length)];
                        if ((action == CardAction.SWAP_HANDS || action == CardAction.SWAP_HANDS_ALL) && !rules.contains(GameRule.SWAPPING)) enabled = false;
                        if (pendingActions.contains(action) && !action.isRepeatable()) enabled = false;

                        if (enabled) {
                            pendingActions.add(action);
                            i--;
                        }
                    }

                    if (pendingActions.size() <= 0) for (Player other : getAllPlayers()) {
                        other.getSubData().sendPacket(new PacketOutAlert("The mystery card has spoken"));
                    }
                }
                player.removeCard(id);

                JSONObject stats = player.getStats();
                stats.put("cardsPlayed", stats.getInt("cardsPlayed") + 1);
                player.setStats(stats);

                for (Player other : getAllPlayers()) {
                    other.getSubData().sendPacket(new PacketPlayCard(player, id, card));
                }

                if (player.getCards().size() > 0) {
                    if (card.getColor() == CardColor.BLACK || pendingActions.contains(CardAction.CHANGE_COLOR)) {
                        if (rules.contains(GameRule.SPIN_THAT_WHEEL)) {
                            CardColor color = CardColor.values()[new Random().nextInt(4)];
                            lastCardColor = color;
                            for (Player other : getAllPlayers()) {
                                other.getSubData().sendPacket(new PacketOutUpdateColor(color));
                            }
                        } else {
                            canchangecolor = true;
                            pendingActions.remove(CardAction.CHANGE_COLOR);
                            player.getSubData().sendPacket(new PacketChangeColor(server));
                        }
                    }
                    if (rules.contains(GameRule.SWAPPING)) {
                        if (pendingActions.contains(CardAction.SWAP_HANDS)) {
                            pendingActions.remove(CardAction.SWAP_HANDS);
                            canswaphands = true;
                            player.getSubData().sendPacket(new PacketSwapHand(server));
                        }
                    }
                }
                if (!canchangecolor && !canswaphands) {
                    endTurn();
                }
            }
        }
    }

    public void draw() {
        if (candraw) {
            candraw = rules.contains(GameRule.PICK_TILL_YOURE_SICK);
            Player player = players.get(currentPlayer);
            player.addCard(Card.getRandomCard());
            cardsdrawn++;
            LinkedList<String> cards = new LinkedList<String>();
            for (String id : player.getCards().keySet()) {
                if (player.getCard(id).getNumber() == lastCardNumber || player.getCard(id).getColor() == CardColor.BLACK || player.getCard(id).getColor() == lastCardColor)
                    cards.add(id);
            }
            if (!candraw && cards.size() <= 0) {
                log.info.println(player.getProfile().getString("displayName") + " could not play a card");
                for (Player other : getAllPlayers()) {
                    other.getSubData().sendPacket(new PacketOutAlert(player.getProfile().getString("displayName") + " could not play a card"));
                }
                endTurn();
            } else {
                canplay = true;
                player.getSubData().sendPacket(new PacketOutBeginTurn(player, candraw && cards.size() <= 0, cards.toArray(new String[cards.size()])));
            }
        }
    }

    public void changeColor(CardColor color) {
        if (canchangecolor && color != CardColor.BLACK) {
            lastCardColor = color;
            canchangecolor = false;
            log.info.println(players.get(currentPlayer).getProfile().getString("displayName") + " changed the color to " + color.toString().substring(0, 1).toUpperCase() + color.toString().substring(1).toLowerCase());
            for (Player player : getAllPlayers()) {
                player.getSubData().sendPacket(new PacketOutUpdateColor(color));
            }
            if (!canswaphands) endTurn();
        }
    }

    public void swapHands(Player from, Player to) {
        if (to.isPlaying() && canswaphands && from != to) {
            LinkedHashMap<String, Card> f = from.cards;
            LinkedHashMap<String, Card> t = to.cards;
            boolean funo = from.uno;
            boolean tuno = to.uno;
            from.uno = funo;
            from.cards = t;
            to.uno = tuno;
            to.cards = f;
            canswaphands = false;
            log.info.println(from.getProfile().getString("displayName") + " swapped hands with " + to.getProfile().getString("displayName"));
            for (Player other : getAllPlayers()) {
                other.getSubData().sendPacket(new PacketOutAlert(from.getProfile().getString("displayName") + " swapped hands with " + to.getProfile().getString("displayName")));
                other.getSubData().sendPacket(new PacketOutUpdateHand(this, other));
            }
            if (!canchangecolor) endTurn();
        }
    }

    public void callout(Player from, Player to) {
        if (from.isPlaying() && to.isPlaying()) {
            if (from == to) {
                if (!to.hasUno()) {
                    if (to.getCards().size() == 1) {
                        to.uno();
                    } else if (to.getCards().size() == 2 && players.get(currentPlayer) == to) {
                        LinkedList<String> cards = new LinkedList<String>();
                        for (String id : to.getCards().keySet()) {
                            if (stackmode) {
                                if (to.getCard(id).getNumber() == 12 || to.getCard(id) == Card.WD4 || to.getCard(id) == Card.WD8)
                                    cards.add(id);
                            } else {
                                if (to.getCard(id).getNumber() == lastCardNumber || to.getCard(id).getColor() == CardColor.BLACK || to.getCard(id).getColor() == lastCardColor)
                                    cards.add(id);
                            }
                        }
                        if (cards.size() > 0) to.uno();
                    }
                }
            } else if (!rules.contains(GameRule.NO_CALLOUT)) {
                if (to.getCards().size() == 1 && !to.hasUno()) {
                    log.info.println(from.getProfile().getString("displayName") + " called out " + to.getProfile().getString("displayName"));
                    for (Player other : getAllPlayers()) {
                        other.getSubData().sendPacket(new PacketOutAlert(from.getProfile().getString("displayName") + " called out " + to.getProfile().getString("displayName")));
                    }
                    to.addCard(Card.getRandomCard());
                    to.addCard(Card.getRandomCard());
                }
            }
        }
    }

    public void endTurn() {
        Player player = players.get(currentPlayer);
        if (timer != null) {
            timer = null;
        }

        boolean updatehand = false;
        Random random = new Random();
        LinkedList<CardAction> tmpactions = new LinkedList<CardAction>();
        tmpactions.addAll(pendingActions);
        int draw = 0;
        int draw_all = 0;
        int discard = 0;
        int discard_all = 0;
        for (CardAction action : tmpactions) {
            if (action == CardAction.REVERSE) {
                direction *= -1;
                if (players.size() <= 2) {
                    pendingActions.add(CardAction.SKIP_NEXT);
                } else {
                    log.info.println(player.getProfile().getString("displayName") + " reversed it");
                    for (Player other : getAllPlayers()) {
                        other.getSubData().sendPacket(new PacketOutAlert(player.getProfile().getString("displayName") + " reversed it"));
                    }
                }
                pendingActions.removeFirstOccurrence(CardAction.REVERSE);
            }
            if (action == CardAction.DRAW) {
                if (player.getCards().size() > 0) player.addCard(Card.getRandomCard());
                cardsdrawn++;
                draw++;
            }
            if (action == CardAction.DRAW_ALL) {
                for (Player other : players) {
                    if (other.getCards().size() > 0) other.addCard(Card.getRandomCard());
                }
                cardsdrawn++;
                draw_all++;
            }
            if (action == CardAction.DISCARD) {
                if (player.getCards().size() > 0) player.removeCard(player.getCards().keySet().toArray(new String[0])[random.nextInt(player.getCards().size())]);
                updatehand = true;
                discard++;
            }
            if (action == CardAction.DISCARD_ALL) {
                for (Player other : players) {
                    if (other.getCards().size() > 0) other.removeCard(other.getCards().keySet().toArray(new String[0])[random.nextInt(other.getCards().size())]);
                }
                discard_all++;
                updatehand = true;
            }

            if (player.getCards().size() > 0) {
                if (rules.contains(GameRule.SWAPPING)) {
                    if (pendingActions.contains(CardAction.SWAP_HANDS_ALL)) {
                        pendingActions.removeFirstOccurrence(CardAction.SWAP_HANDS_ALL);
                        LinkedList<Player> players = new LinkedList<Player>();
                        players.addAll(this.players);
                        if (direction > 0) Collections.reverse(players);

                        LinkedHashMap<String, Card> limbo = new LinkedHashMap<String, Card>();
                        boolean limbuno = false;
                        Player last = null;
                        for (int i = 0; i < players.size(); i++) {
                            if (i == 0) {
                                limbuno = players.get(i).uno;
                                limbo = players.get(i).cards;
                            } else {
                                last.uno = players.get(i).uno;
                                last.cards = players.get(i).cards;
                            }
                            last = players.get(i);
                            if (i >= players.size() - 1) {
                                players.get(i).uno = limbuno;
                                players.get(i).cards = limbo;
                            }
                        }

                        log.info.println("Everyone swapped hands");
                        for (Player other : getAllPlayers()) {
                            other.getSubData().sendPacket(new PacketOutAlert("Everyone swapped hands"));
                        }
                        updatehand = true;
                    }
                }
            }
        }
        if (updatehand) for (Player other : getAllPlayers()) {
            other.getSubData().sendPacket(new PacketOutUpdateHand(this, other));
        }
        if (draw_all > 0) {
            log.info.println("Everyone was forced to draw " + draw_all + " card" + ((draw_all == 1) ? "" : "s"));
            for (Player other : getAllPlayers()) {
                other.getSubData().sendPacket(new PacketOutAlert("Everyone was forced to draw " + draw_all + " card" + ((draw_all == 1) ? "" : "s")));
            }
        }
        if (discard_all > 0) {
            log.info.println("Everyone was forced to discard " + discard_all + " card" + ((discard_all == 1) ? "" : "s"));
            for (Player other : getAllPlayers()) {
                other.getSubData().sendPacket(new PacketOutAlert("Everyone was forced to discard " + discard_all + " card" + ((discard_all == 1) ? "" : "s")));
            }
        }
        if (draw > 0) {
            log.info.println(player.getProfile().getString("displayName") + " was forced to draw " + draw + " card" + ((draw == 1) ? "" : "s"));
            for (Player other : getAllPlayers()) {
                other.getSubData().sendPacket(new PacketOutAlert(player.getProfile().getString("displayName") + " was forced to draw " + draw + " card" + ((draw == 1) ? "" : "s")));
            }
        }
        if (discard > 0) {
            log.info.println(player.getProfile().getString("displayName") + " was forced to discard " + discard + " card" + ((discard == 1) ? "" : "s"));
            for (Player other : getAllPlayers()) {
                other.getSubData().sendPacket(new PacketOutAlert(player.getProfile().getString("displayName") + " was forced to discard " + discard + " card" + ((discard == 1) ? "" : "s")));
            }
        }

        JSONObject stats = player.getStats();
        if (stats.getInt("consecutiveCardsDrawn") < cardsdrawn) {
            stats.put("consecutiveCardsDrawn", cardsdrawn);
            player.setStats(stats);
            for (Player other : getAllPlayers()) {
                other.getSubData().sendPacket(new PacketOutUpdateStat(player, "consecutiveCardsDrawn", cardsdrawn));
            }
        }
        cardsdrawn = 0;

        ArrayList<Player> winners = new ArrayList<Player>();

        for (Player other : players) {
            if (other.getCards().size() <= 0) {
                winners.add(other);
            }
        }

        if (winners.size() > 0) {
            stop(winners.toArray(new Player[winners.size()]));
        } else {
            currentPlayer += direction;
            if (currentPlayer < 0 || currentPlayer >= players.size()) {
                if (direction > 0) {
                    currentPlayer = 0;
                } else {
                    currentPlayer = players.size() - 1;
                }
            }
            beginTurn();
        }
    }

    public void stop(Player... winners) {
        server.game = null;
        for (Player other : getAllPlayers()) {
            other.getSubData().sendPacket(new PacketOutEndGame(winners));
            other.setPlaying(false);
        }
        int i = 0;
        String s = "";
        for (Player winner : winners) {
            i++;
            if (i > 1 ) {
                if (winners.length > 2) s += ", ";
                else if (winners.length == 2) s += ' ';
                if (i == winners.length) s += "and ";
            }
            s += winner.getProfile().getString("displayName");

            players.remove(winner);
            JSONObject stats = winner.getStats();
            stats.put("gamesWon", stats.getInt("gamesWon") + 1);
            winner.setStats(stats);
        }
        log.info.println((winners.length <= 0)?"Uno has been stopped":s+" won Uno");
        server.lastGame = this;
    }

    public void quit(Player player) {
        if (players.contains(player)) {
            int index = players.indexOf(player);
            players.remove(player);
            player.setPlaying(false);
            log.info.println(player.getProfile().getString("displayName") + " has left the game");
            for (Player other : getAllPlayers())
                other.getSubData().sendPacket(new PacketOutAlert(player.getProfile().getString("displayName") + " has left the game"));
            if (players.size() <= 1) {
                stop();
            } else if (currentPlayer == index) {
                if (timer != null) {
                    timer.cancel();
                    timer = null;
                }
                if (lastCardColor == CardColor.BLACK) {
                    CardColor color = CardColor.values()[new Random().nextInt(4)];
                    changeColor(color);
                    lastCardColor = color;
                }
                if (currentPlayer < 0 || currentPlayer >= players.size()) {
                    if (direction > 0) {
                        currentPlayer = 0;
                    } else {
                        currentPlayer = players.size() - 1;
                    }
                }
                beginTurn();
            } else if (currentPlayer > index) {
                currentPlayer--;
            }
        }
        spectators.remove(player);
    }

    public NamedContainer<CardColor, Integer> getCurrentCard() {
        return new NamedContainer<>(lastCardColor, lastCardNumber);
    }

    public Player getCurrentPlayer() {
        return players.get(currentPlayer);
    }

    public LinkedList<Player> getAllPlayers() {
        LinkedList<Player> list = new LinkedList<Player>();
        list.addAll(spectators);
        list.addAll(players);
        return list;
    }

    public LinkedList<Player> getPlayers() {
        return new LinkedList<>(players);
    }

    public LinkedList<Player> getSpectators() {
        return new LinkedList<>(spectators);
    }

    public void addSpectator(Player player) {
        spectators.add(player);
        player.setPlaying(false);
        player.getSubData().sendPacket(new PacketOutStartGame(this, player));
        player.getSubData().sendPacket(new PacketOutUpdateHand(this, player));
    }
}
