package com.sporty.jackpot.repository;

import com.sporty.jackpot.domain.Jackpot;
import com.sporty.jackpot.exception.DuplicateJackpotException;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory jackpot store. ConcurrentHashMap rather than plain HashMap
 * because bets on the same jackpot can be processed concurrently.
 */
@Repository
public class JackpotRepository {

    private final Map<String, Jackpot> store = new ConcurrentHashMap<>();

    /**
     * Rejects a jackpotId that already exists. Atomic putIfAbsent rather than a
     * check in the caller: a check-then-save would let two concurrent creates
     * through, and the second would silently replace the first - discarding a
     * live pool that bets had already contributed to.
     */
    public Jackpot save(Jackpot jackpot) {
        Jackpot existing = store.putIfAbsent(jackpot.getJackpotId(), jackpot);
        if (existing != null) {
            throw new DuplicateJackpotException(jackpot.getJackpotId());
        }
        return jackpot;
    }

    public Optional<Jackpot> findById(String jackpotId) {
        return Optional.ofNullable(store.get(jackpotId));
    }

    public List<Jackpot> findAll() {
        return new ArrayList<>(store.values());
    }
}
