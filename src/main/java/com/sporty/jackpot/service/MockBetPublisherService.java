package com.sporty.jackpot.service;

import com.sporty.jackpot.domain.Bet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Stands in for a real Kafka producer on the jackpot-bets topic.
 * <p>
 * Per the assignment's own conditions ("if the Kafka setup is too complex,
 * use mocks for the Kafka producer, just log the payload"), this logs the
 * payload the way a producer would, then synchronously hands the bet to
 * the consumer. A real implementation would instead call
 * {@code kafkaTemplate.send("jackpot-bets", bet)} and let a
 * {@code @KafkaListener} pick it up asynchronously - the seam
 * (BetPublisherService) is designed so that swap doesn't touch any other
 * class.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MockBetPublisherService implements BetPublisherService {

    private final BetConsumerService betConsumerService;

    @Override
    public void publish(Bet bet) {
        log.info("[jackpot-bets] publishing bet={} user={} jackpot={} amount={}",
                bet.betId(), bet.userId(), bet.jackpotId(), bet.betAmount());

        // Stands in for the async @KafkaListener that would normally pick this up.
        betConsumerService.consume(bet);
    }
}
