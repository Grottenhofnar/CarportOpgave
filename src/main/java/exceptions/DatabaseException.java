package exceptions;

import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseException extends Exception {
    private static final Logger LOGGER = Logger.getLogger(DatabaseException.class.getName());

    public DatabaseException(String message) {
        super(message);
        LOGGER.log(Level.SEVERE, message);
    }

    public DatabaseException(Throwable cause, String message) {
        super(message, cause);
        LOGGER.log(Level.SEVERE, message, cause);
    }
}
