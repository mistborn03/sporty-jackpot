package com.sporty.jackpot.exception;

public class DuplicateJackpotException extends RuntimeException {
    public DuplicateJackpotException(String jackpotId) {
        super("A jackpot with id " + jackpotId + " already exists.");
    }
}
