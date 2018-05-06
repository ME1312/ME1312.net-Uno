package net.ME1312.Uno.Library;

import org.fusesource.jansi.Ansi;

import java.util.Arrays;

/**
 * Color Code Converter Enum
 */
public enum TextColor {
    AQUA(Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.CYAN).bold()),
    BLACK(Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.BLACK).boldOff()),
    BLUE(Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.BLUE).bold()),
    BOLD(Ansi.ansi().a(Ansi.Attribute.UNDERLINE_DOUBLE)),
    DARK_AQUA(Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.CYAN).boldOff()),
    DARK_BLUE(Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.BLUE).boldOff()),
    DARK_GRAY(Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.BLACK).bold()),
    DARK_GREEN(Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.GREEN).boldOff()),
    DARK_PURPLE(Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.MAGENTA).boldOff()),
    DARK_RED(Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.RED).boldOff()),
    GOLD(Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.YELLOW).boldOff()),
    GRAY(Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.WHITE).boldOff()),
    GREEN(Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.GREEN).bold()),
    ITALIC(Ansi.ansi().a(Ansi.Attribute.ITALIC)),
    LIGHT_PURPLE(Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.MAGENTA).bold()),
    MAGIC(Ansi.ansi().a(Ansi.Attribute.BLINK_SLOW)),
    RED(Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.RED).bold()),
    RESET(Ansi.ansi().a(Ansi.Attribute.RESET)),
    STRIKETHROUGH(Ansi.ansi().a(Ansi.Attribute.STRIKETHROUGH_ON)),
    UNDERLINE(Ansi.ansi().a(Ansi.Attribute.UNDERLINE)),
    WHITE(Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.WHITE).bold()),
    YELLOW(Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.YELLOW).bold());

    private final Ansi console;

    TextColor(Ansi console) {
        this.console = console;
    }

    /**
     * Get this color as an Ansi Color Code
     *
     * @return Ansi Color Code
     */
    public String asAnsiCode() {
        return console.toString();
    }

    @Override
    public String toString() {
        return asAnsiCode();
    }

    /**
     * Removes all Ansi color codes from a string
     *
     * @param str String to parse
     * @return String without color
     */
    public static String stripColor(String str) {
        for (TextColor color : Arrays.asList(TextColor.values())) {
            str = str.replace(color.asAnsiCode(), "");
        }
        return str;
    }
}
