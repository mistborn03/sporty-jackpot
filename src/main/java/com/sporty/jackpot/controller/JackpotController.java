package com.sporty.jackpot.controller;

import com.sporty.jackpot.dto.JackpotResponse;
import com.sporty.jackpot.repository.JackpotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Not required by the spec - included so pool state can be inspected
 * during local testing without a DB console to look at.
 */
@RestController
@RequestMapping("/jackpots")
@RequiredArgsConstructor
public class JackpotController {

    private final JackpotRepository jackpotRepository;

    @GetMapping
    public List<JackpotResponse> listJackpots() {
        return jackpotRepository.findAll().stream()
                .map(JackpotResponse::from)
                .toList();
    }
}
