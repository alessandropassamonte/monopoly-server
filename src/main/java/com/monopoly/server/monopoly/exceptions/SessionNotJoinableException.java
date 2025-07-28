package com.monopoly.server.monopoly.exceptions;

public class SessionNotJoinableException extends RuntimeException {
    public SessionNotJoinableException(String message) {
        super(message);
    }
}
