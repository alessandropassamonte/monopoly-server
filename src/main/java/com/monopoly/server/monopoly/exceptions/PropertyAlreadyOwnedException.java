package com.monopoly.server.monopoly.exceptions;

public class PropertyAlreadyOwnedException extends RuntimeException {
    public PropertyAlreadyOwnedException(String message) {
        super(message);
    }
}