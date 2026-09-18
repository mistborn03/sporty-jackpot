package com.sporty.jackpot.exception;

public class JackpotNotFoundException extends RuntimeException {
    public JackpotNotFoundException(String jackpotId) {
        super("No jackpot found with id: " + jackpotId);
    }
}
