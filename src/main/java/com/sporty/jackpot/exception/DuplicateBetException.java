package com.sporty.jackpot.exception;

public class DuplicateBetException extends RuntimeException {
    public DuplicateBetException(String betId) {
        super("A bet with id " + betId + " has already been published.");
    }
}
