package com.sporty.jackpot.repository;

import com.sporty.jackpot.domain.Jackpot;
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

    public Jackpot save(Jackpot jackpot) {
        store.put(jackpot.getJackpotId(), jackpot);
        return jackpot;
    }

    public Optional<Jackpot> findById(String jackpotId) {
        return Optional.ofNullable(store.get(jackpotId));
    }

    public List<Jackpot> findAll() {
        return new ArrayList<>(store.values());
    }
}
