package net.ME1312.Uno.Game;

public enum CardAction {
    DRAW(true),
    DRAW_NEXT(true),
    DRAW_ALL(true),
    DISCARD(true),
    DISCARD_NEXT(true),
    DISCARD_ALL(true),
    SKIP_NEXT(true),
    REVERSE(true),
    SWAP_HANDS,
    SWAP_HANDS_ALL(true),
    CHANGE_COLOR,

    ;
    private boolean repeatable = false;
    CardAction() {}
    CardAction(boolean repeatable) {
        this.repeatable = repeatable;
    }

    public boolean isRepeatable() {
        return repeatable;
    }
}
