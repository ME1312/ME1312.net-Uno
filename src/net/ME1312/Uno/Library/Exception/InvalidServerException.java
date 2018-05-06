package net.ME1312.Uno.Library.Exception;

/**
 * Invalid Server Exception
 */
public class InvalidServerException extends IllegalStateException {
    public InvalidServerException() {}
    public InvalidServerException(String s) {
        super(s);
    }
}
