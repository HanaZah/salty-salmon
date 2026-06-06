package com.finadvise.crm.documents;

public class UnreadableDocumentException extends RuntimeException {
    public UnreadableDocumentException(String message) {
        super(message);
    }

    // Required for Exception Chaining
    public UnreadableDocumentException(String message, Throwable cause) {
        super(message, cause);
    }
}
