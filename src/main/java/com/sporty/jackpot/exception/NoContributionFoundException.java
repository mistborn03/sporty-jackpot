package com.sporty.jackpot.exception;

public class NoContributionFoundException extends RuntimeException {
    public NoContributionFoundException(String betId) {
        super("No contribution found for bet " + betId
                + " - the bet may not exist, or its jackpotId did not match any known jackpot.");
    }
}
