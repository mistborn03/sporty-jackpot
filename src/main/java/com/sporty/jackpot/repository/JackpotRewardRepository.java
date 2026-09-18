package com.sporty.jackpot.repository;

import com.sporty.jackpot.domain.JackpotReward;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Keyed by betId so evaluate() can be made idempotent per bet. */
@Repository
public class JackpotRewardRepository {

    private final Map<String, JackpotReward> store = new ConcurrentHashMap<>();

    public JackpotReward save(JackpotReward reward) {
        store.put(reward.betId(), reward);
        return reward;
    }

    public Optional<JackpotReward> findByBetId(String betId) {
        return Optional.ofNullable(store.get(betId));
    }
}
