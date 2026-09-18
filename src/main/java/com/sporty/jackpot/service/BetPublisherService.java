package com.sporty.jackpot.service;

import com.sporty.jackpot.domain.Bet;

/**
 * Publishes a bet onto the jackpot-bets topic.
 * <p>
 * Kept as an interface so a real Kafka-backed implementation
 * (KafkaTemplate.send("jackpot-bets", ...)) could be dropped in later
 * without touching the caller (BetController) or the consumer side.
 */
public interface BetPublisherService {
    void publish(Bet bet);
}
