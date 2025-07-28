package com.monopoly.server.monopoly.exceptions;

public class ColorTakenException extends RuntimeException {
    public ColorTakenException(String message) {
        super(message);
    }
}
