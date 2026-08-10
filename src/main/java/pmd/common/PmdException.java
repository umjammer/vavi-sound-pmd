
package pmd.common;

import java.io.Serializable;
import java.util.ResourceBundle;


public class PmdException extends RuntimeException implements Serializable {

    private static final ResourceBundle rb = ResourceBundle.getBundle("pmd/message");

    public PmdException() {
    }

    public PmdException(String message) {
        super(message);
    }

    public PmdException(String message, Exception innerException) {
        super(message, innerException);
    }

    public PmdException(String message, int row, int col) {
        super(String.format(rb.getString("E0300"), row, col, message));
    }
}
