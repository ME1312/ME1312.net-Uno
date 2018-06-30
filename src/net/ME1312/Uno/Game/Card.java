package net.ME1312.Uno.Game;

import net.ME1312.Uno.Network.Packet.PacketOutAlert;
import net.ME1312.Uno.UnoServer;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import static net.ME1312.Uno.Game.CardColor.*;
import static net.ME1312.Uno.Game.CardAction.*;

public enum Card {
    // Red 0-9
    R0(0, RED, 1, SWAP_HANDS_ALL),
    R1(1, RED, 2),
    R2(2, RED, 2),
    R3(3, RED, 2),
    R4(4, RED, 2),
    R5(5, RED, 2),
    R6(6, RED, 2),
    R7(7, RED, 2, SWAP_HANDS),
    R8(8, RED, 2),
    R9(9, RED, 2),

    // Red Special
    RR("Reverse", 10, RED, 2, REVERSE),
    RS("Skip", 11, RED, 2, SKIP_NEXT),
    RD2("Draw 2", 12, RED, 2, DRAW_NEXT, DRAW_NEXT),
    RM("Mystery Card", 13, RED, 0),


    // Green 0-9
    G0(0, GREEN, 1, SWAP_HANDS_ALL),
    G1(1, GREEN, 2),
    G2(2, GREEN, 2),
    G3(3, GREEN, 2),
    G4(4, GREEN, 2),
    G5(5, GREEN, 2),
    G6(6, GREEN, 2),
    G7(7, GREEN, 2, SWAP_HANDS),
    G8(8, GREEN, 2),
    G9(9, GREEN, 2),

    // Green Special
    GR("Reverse", 10, GREEN, 2, REVERSE),
    GS("Skip", 11, GREEN, 2, SKIP_NEXT),
    GD2("Draw 2", 12, GREEN, 2, DRAW_NEXT, DRAW_NEXT),
    GM("Mystery Card", 13, GREEN, 0),


    // Blue 0-9
    B0(0, BLUE, 1, SWAP_HANDS_ALL),
    B1(1, BLUE, 2),
    B2(2, BLUE, 2),
    B3(3, BLUE, 2),
    B4(4, BLUE, 2),
    B5(5, BLUE, 2),
    B6(6, BLUE, 2),
    B7(7, BLUE, 2, SWAP_HANDS),
    B8(8, BLUE, 2),
    B9(9, BLUE, 2),

    // Blue Special
    BR("Reverse", 10, BLUE, 2, REVERSE),
    BS("Skip", 11, BLUE, 2, SKIP_NEXT),
    BD2("Draw 2", 12, BLUE, 2, DRAW_NEXT, DRAW_NEXT),
    BM("Mystery Card", 13, BLUE, 0),


    // Yellow 0-9
    Y0(0, YELLOW, 1, SWAP_HANDS_ALL),
    Y1(1, YELLOW, 2),
    Y2(2, YELLOW, 2),
    Y3(3, YELLOW, 2),
    Y4(4, YELLOW, 2),
    Y5(5, YELLOW, 2),
    Y6(6, YELLOW, 2),
    Y7(7, YELLOW, 2, SWAP_HANDS),
    Y8(8, YELLOW, 2),
    Y9(9, YELLOW, 2),

    // Yellow Special
    YR("Reverse", 10, YELLOW, 2, REVERSE),
    YS("Skip", 11, YELLOW, 2, SKIP_NEXT),
    YD2("Draw 2", 12, YELLOW, 2, DRAW_NEXT, DRAW_NEXT),
    YM("Mystery Card", 13, YELLOW, 0),


    // Special
    W("Wild Card", -1, BLACK, 4, CHANGE_COLOR),
    WD4("Wild Draw 4", -2, BLACK, 4, CHANGE_COLOR, DRAW_NEXT, DRAW_NEXT, DRAW_NEXT, DRAW_NEXT),
    WD8("Wild Draw 8", -3, BLACK, 0, CHANGE_COLOR, DRAW_NEXT, DRAW_NEXT, DRAW_NEXT, DRAW_NEXT, DRAW_NEXT, DRAW_NEXT, DRAW_NEXT, DRAW_NEXT)

    ;
    private static final Random random = new Random();
    private final String name;
    private final int number;
    private final CardColor color;
    private final int defamount;
    private int amount;
    private int used = 0;
    private final CardAction[] actions;
    Card(int number, CardColor color, int amount, CardAction... actions) {
        this(Integer.toString(number), number, color, amount, actions);
    }
    Card(String name, int number, CardColor color, int amount, CardAction... actions) {
        this.name = name;
        this.number = number;
        this.color = color;
        this.amount = (this.defamount = amount);
        this.actions = actions;
    }

    public String getName() {
        return name;
    }

    public int getNumber() {
        return number;
    }

    public CardColor getColor() {
        return color;
    }

    public int getDefaultAmount() {
        return defamount;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int value) {
        amount = value;
    }

    public void resetAmount() {
        amount = defamount;
    }

    public int getAmountLeft() {
        return amount - used;
    }

    public List<CardAction> getActions() {
        return Arrays.asList(actions);
    }

    public void use() {
        if (used < amount) used++;
    }

    public void reset() {
        used = 0;
    }

    public static int getDeckSize() {
        int amount = 0;
        for (Card card : Card.values()) amount += card.getAmountLeft();
        return amount;
    }

    public static Card getRandomCard() {
        if (getDeckSize() <= 0) resetDeck();

        LinkedList<Card> values = new LinkedList<Card>();
        for (Card card : Card.values()) {
            int i = card.getAmountLeft();
            while (i > 0) {
                values.add(card);
                i--;
            }
        }

        Card card = values.get(random.nextInt(values.size()));
        card.use();
        return card;
    }

    public static void resetDeck() {
        for (Card card : Card.values()) card.reset();
        if (UnoServer.getInstance().game != null) {
            Game.log.info.println("The deck has been reshuffled");
            for (Player player : UnoServer.getInstance().game.getPlayers()) {
                player.getSubData().sendPacket(new PacketOutAlert("The deck has been reshuffled"));
            }
        }
    }
}
