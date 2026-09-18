package com.sporty.jackpot.repository;

import com.sporty.jackpot.domain.Bet;
import com.sporty.jackpot.exception.DuplicateBetException;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Idempotency store for published betIds.
 * <p>
 * The bet itself lives on the jackpot-bets topic, not here - this exists so
 * a betId can only be published once, and a replayed bet cannot contribute
 * to a pool twice. The check has to happen at the API edge: with a real
 * Kafka producer the consumer has not processed the bet by the time publish
 * returns, so this is the only point that can reject a duplicate while the
 * caller is still waiting.
 */
@Repository
public class BetRepository {

    private final Map<String, Bet> store = new ConcurrentHashMap<>();

    /**
     * Rejects a betId that was already published. Enforced here with a single
     * atomic putIfAbsent rather than a check in the caller: a check-then-save
     * would let two concurrent publishes of the same betId both through, and
     * each would contribute to the pool again.
     */
    public Bet save(Bet bet) {
        Bet existing = store.putIfAbsent(bet.betId(), bet);
        if (existing != null) {
            throw new DuplicateBetException(bet.betId());
        }
        return bet;
    }

    public Optional<Bet> findById(String betId) {
        return Optional.ofNullable(store.get(betId));
    }
}
