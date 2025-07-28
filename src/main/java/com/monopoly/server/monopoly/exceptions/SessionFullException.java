package com.monopoly.server.monopoly.exceptions;

public class SessionFullException extends RuntimeException {
    public SessionFullException(String message) {
        super(message);
    }
}