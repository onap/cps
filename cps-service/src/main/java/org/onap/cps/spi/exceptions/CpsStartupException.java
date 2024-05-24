package org.onap.cps.spi.exceptions;

public class CpsStartupException extends  CpsException {

    public CpsStartupException(final String message, final String details) {
        super(message, details);
    }
}
