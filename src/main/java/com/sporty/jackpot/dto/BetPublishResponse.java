package com.sporty.jackpot.dto;

public record BetPublishResponse(
        String betId,
        String status,
        String message
) {
}
