package com.sporty.jackpot.exception;

public class BetNotFoundException extends RuntimeException {
    public BetNotFoundException(String betId) {
        super("No bet found with id: " + betId);
    }
}
