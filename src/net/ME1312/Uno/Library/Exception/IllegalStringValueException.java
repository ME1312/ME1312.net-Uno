package net.ME1312.Uno.Library.Exception;

/**
 * Illegal String Value Exception
 */
public class IllegalStringValueException extends IllegalStateException {
    public IllegalStringValueException() {}
    public IllegalStringValueException(String s) {
        super(s);
    }
}
