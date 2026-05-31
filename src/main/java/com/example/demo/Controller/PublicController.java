package com.example.demo.Controller;

import com.example.demo.Entity.VotingConfig;
import com.example.demo.Repository.CandidateRepository;
import com.example.demo.Repository.VotingConfigRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime;

@Controller
public class PublicController {

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private VotingConfigRepository votingConfigRepository;

    @GetMapping("/results")
    public String showPublicResults(Model model) {
        VotingConfig config = votingConfigRepository.findAll().stream().findFirst().orElse(null);

        if (config == null || config.getEndTime() == null) {
            model.addAttribute("message", "Voting deadline not configured.");
            return "publicresults";
        }

        LocalDateTime deadline = config.getEndTime();
        LocalDateTime availableTime = deadline.plusMinutes(30);

        if (LocalDateTime.now().isBefore(availableTime)) {
            model.addAttribute("message", "Results will be available 30 minutes after voting ends.");
            return "publicresults";
        }

        model.addAttribute("candidates", candidateRepository.findAll());
        return "publicresults";
    }
}
