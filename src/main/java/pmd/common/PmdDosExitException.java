package pmd.common;

import java.io.Serializable;


public class PmdDosExitException extends PmdException implements Serializable {

    public PmdDosExitException() {
    }

    public PmdDosExitException(String message) {
        super(message);
    }

    public PmdDosExitException(String message, Exception innerException) {
        super(message, innerException);
    }

    public PmdDosExitException(String message, int row, int col) {
        super(message, row, col);
    }

    public static class PmdErrorExitException extends PmdException implements Serializable {

        public PmdErrorExitException() {
        }

        public PmdErrorExitException(String message) {
            super(message);
        }

        public PmdErrorExitException(String message, Exception innerException) {
            super(message, innerException);
        }

        public PmdErrorExitException(String message, int row, int col) {
            super(message, row, col);
        }
    }
}
