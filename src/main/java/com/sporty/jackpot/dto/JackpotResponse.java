package com.sporty.jackpot.dto;

import com.sporty.jackpot.domain.ContributionType;
import com.sporty.jackpot.domain.Jackpot;
import com.sporty.jackpot.domain.RewardType;

import java.math.BigDecimal;

public record JackpotResponse(
        String jackpotId,
        BigDecimal initialPoolAmount,
        BigDecimal currentPoolAmount,
        ContributionType contributionType,
        RewardType rewardType
) {

    public static JackpotResponse from(Jackpot jackpot) {
        return new JackpotResponse(
                jackpot.getJackpotId(),
                jackpot.getInitialPoolAmount(),
                jackpot.getCurrentPoolAmount(),
                jackpot.getContributionType(),
                jackpot.getRewardType()
        );
    }
}
