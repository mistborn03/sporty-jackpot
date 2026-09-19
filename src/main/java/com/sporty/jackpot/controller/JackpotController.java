package com.sporty.jackpot.controller;

import com.sporty.jackpot.domain.Jackpot;
import com.sporty.jackpot.dto.JackpotRequest;
import com.sporty.jackpot.dto.JackpotResponse;
import com.sporty.jackpot.exception.JackpotNotFoundException;
import com.sporty.jackpot.repository.JackpotRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/jackpots")
@RequiredArgsConstructor
@Tag(name = "Jackpots", description = "Create and inspect jackpots")
public class JackpotController {

    private final JackpotRepository jackpotRepository;

    @PostMapping
    @Operation(
            summary = "Create a jackpot",
            description = """
                    Which fields are required depends on the types chosen.
                    FIXED contribution needs fixedContributionPct; VARIABLE needs
                    baseContributionPct, contributionDecayRate, minContributionPct and
                    contributionStepAmount. FIXED reward needs fixedRewardChance;
                    VARIABLE needs baseRewardChance, rewardGrowthRate, rewardStepAmount
                    and poolLimit. A missing field returns 400 naming it, and a
                    jackpotId that already exists returns 409.""")
    public ResponseEntity<JackpotResponse> createJackpot(@Valid @RequestBody JackpotRequest request) {
        Jackpot saved = jackpotRepository.save(request.toJackpot());
        return ResponseEntity.status(HttpStatus.CREATED).body(JackpotResponse.from(saved));
    }

    @GetMapping
    @Operation(summary = "List all jackpots with their current pool state")
    public List<JackpotResponse> listJackpots() {
        return jackpotRepository.findAll().stream()
                .map(JackpotResponse::from)
                .toList();
    }

    @GetMapping("/{jackpotId}")
    @Operation(summary = "Fetch a single jackpot")
    public JackpotResponse getJackpot(@PathVariable String jackpotId) {
        return jackpotRepository.findById(jackpotId)
                .map(JackpotResponse::from)
                .orElseThrow(() -> new JackpotNotFoundException(jackpotId));
    }
}
