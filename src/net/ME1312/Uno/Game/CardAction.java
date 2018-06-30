package net.ME1312.Uno.Game;

public enum CardAction {
    DRAW,
    DRAW_NEXT,
    DRAW_ALL,
    DISCARD,
    DISCARD_NEXT,
    DISCARD_ALL,
    SKIP_NEXT,
    REVERSE,
    SWAP_HANDS(false),
    SWAP_HANDS_ALL,
    CHANGE_COLOR(false),

    ;
    private boolean repeatable = true;
    CardAction() {}
    CardAction(boolean repeatable) {
        this.repeatable = repeatable;
    }

    public boolean isRepeatable() {
        return repeatable;
    }
}
