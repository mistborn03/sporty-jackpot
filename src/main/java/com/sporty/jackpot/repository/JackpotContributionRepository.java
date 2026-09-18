package com.sporty.jackpot.repository;

import com.sporty.jackpot.domain.JackpotContribution;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One contribution per bet is the assumption here (a bet contributes at
 * most once, keyed by betId). Contributions are never modified or deleted
 * once written - they're a permanent audit trail.
 */
@Repository
public class JackpotContributionRepository {

    private final Map<String, JackpotContribution> store = new ConcurrentHashMap<>();

    public JackpotContribution save(JackpotContribution contribution) {
        store.put(contribution.betId(), contribution);
        return contribution;
    }

    public Optional<JackpotContribution> findByBetId(String betId) {
        return Optional.ofNullable(store.get(betId));
    }
}
