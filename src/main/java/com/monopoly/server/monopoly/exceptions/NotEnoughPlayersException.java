package com.monopoly.server.monopoly.exceptions;

public class NotEnoughPlayersException extends RuntimeException {
    public NotEnoughPlayersException(String message) {
        super(message);
    }
}