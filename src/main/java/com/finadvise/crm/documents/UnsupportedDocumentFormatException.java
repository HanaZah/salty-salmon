package com.finadvise.crm.documents;

public class UnsupportedDocumentFormatException extends RuntimeException {
    public UnsupportedDocumentFormatException(String message) {
        super(message);
    }

    // Required for Exception Chaining
    public UnsupportedDocumentFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
