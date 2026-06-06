package com.finadvise.crm.documents;

public class DmsUnavailableException extends RuntimeException {
    public DmsUnavailableException(String message) {
        super(message);
    }

    public DmsUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
