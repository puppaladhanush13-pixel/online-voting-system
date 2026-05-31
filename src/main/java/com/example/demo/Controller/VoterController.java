package com.example.demo.Controller;

import com.example.demo.Entity.Candidate;
import com.example.demo.Entity.Voter;
import com.example.demo.Entity.VotingConfig;
import com.example.demo.Repository.CandidateRepository;
import com.example.demo.Repository.VoterRepository;
import com.example.demo.Repository.VotingConfigRepository;
import com.example.demo.Service.VoterService;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Controller
public class VoterController {

    @Autowired
    private VoterRepository voterRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private VotingConfigRepository votingConfigRepository;

    @Autowired
    private VoterService voterService;

    private static final Set<String> ALLOWED_IMAGE_TYPES =
            Set.of("image/jpeg", "image/jpg", "image/png");

    private String getVotingStatus() {
        VotingConfig config = votingConfigRepository.findAll().stream().findFirst().orElse(null);

        if (config == null || config.getStartTime() == null || config.getEndTime() == null) {
            return "not_configured";
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(config.getStartTime())) return "not_started";
        else if (now.isAfter(config.getEndTime())) return "closed";
        else return "open";
    }

    @GetMapping("/voter/login")
    public String showLoginPage(HttpSession session) {
        if (session.getAttribute("voterUsername") != null) {
            return "redirect:/vote";
        }
        return "login";
    }

    @PostMapping("/voter/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {
        Voter voter = voterRepository.findByUsername(username);

        // Approved (active) voter: verify password securely and log in.
        if (voter != null) {
            if (!voterService.verifyPassword(voter, password)) {
                model.addAttribute("error", "Invalid username or password");
                return "login";
            }
            session.setAttribute("voterUsername", voter.getUsername());
            return "redirect:/vote";
        }

        // Not an active voter: check if there is a pending/rejected request.
        com.example.demo.Entity.PendingVoter pending = voterService.findPendingByUsername(username);
        if (pending != null) {
            if (com.example.demo.Service.VoterService.STATUS_REJECTED.equals(pending.getStatus())) {
                // CASE 2: rejected registration
                model.addAttribute("rejected", "Your registration request has been rejected.");
            } else {
                // CASE 1: still pending admin approval
                model.addAttribute("pending",
                        "Your account is pending admin approval. Please wait up to 24 hours.");
            }
            return "login";
        }

        // Unknown user / bad credentials
        model.addAttribute("error", "Invalid username or password");
        return "login";
    }

    @GetMapping("/voter/register")
    public String showRegisterPage(HttpSession session) {
        if (session.getAttribute("voterUsername") != null) {
            return "redirect:/vote";
        }
        return "register";
    }

    @PostMapping("/voter/register")
    public String register(@RequestParam String username,
                           @RequestParam String password,
                           @RequestParam(value = "aadhaarImage", required = false) MultipartFile aadhaarImage,
                           Model model) {

        // Duplicate username check (active voters + pending/rejected requests)
        if (voterService.isUsernameTaken(username)) {
            model.addAttribute("error", "Username already registered");
            return "register";
        }

        // Aadhaar image is mandatory
        if (aadhaarImage == null || aadhaarImage.isEmpty()) {
            model.addAttribute("error", "Please upload your Aadhaar card image.");
            return "register";
        }

        // Validate file type (JPG / JPEG / PNG only)
        String contentType = aadhaarImage.getContentType();
        String originalName = aadhaarImage.getOriginalFilename();
        boolean validType = contentType != null && ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase());
        boolean validExt = originalName != null && originalName.toLowerCase()
                .matches(".*\\.(jpg|jpeg|png)$");
        if (!validType || !validExt) {
            model.addAttribute("error", "Only JPG, JPEG or PNG image files are allowed.");
            return "register";
        }

        // Save the Aadhaar image safely with a unique name
        String aadhaarPath;
        try {
            aadhaarPath = saveAadhaarImage(aadhaarImage);
        } catch (IOException e) {
            model.addAttribute("error", "Failed to upload Aadhaar image. Please try again.");
            return "register";
        }

        // Persist as a PENDING request only — NOT in the active voters table.
        com.example.demo.Entity.PendingVoter saved =
                voterService.registerVoter(username, password, aadhaarPath);
        if (saved == null) {
            model.addAttribute("error", "Username already registered");
            return "register";
        }

        // Show success popup on the login page, do NOT log the voter in
        model.addAttribute("registered",
                "Registration successful. Your account is pending admin approval. Please wait up to 24 hours for activation.");
        return "login";
    }

    private String saveAadhaarImage(MultipartFile file) throws IOException {
        String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path uploadPath = Paths.get("uploads", "aadhaar");

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return "/uploads/aadhaar/" + filename;
    }

    @GetMapping("/vote")
    public String showVotePage(HttpSession session, Model model) {
        String username = (String) session.getAttribute("voterUsername");
        if (username == null) {
            return "redirect:/voter/login";
        }

        Voter voter = voterRepository.findByUsername(username);
        if (voter == null) {
            session.invalidate();
            return "redirect:/voter/login";
        }

        String status = getVotingStatus();
        if (status.equals("not_configured")) {
            model.addAttribute("error", "Voting configuration not set by admin.");
            return "message";
        } else if (status.equals("not_started")) {
            model.addAttribute("error", "Voting has not started yet.");
            return "message";
        } else if (status.equals("closed")) {
            model.addAttribute("error", "Voting is closed.");
            return "message";
        }

        if (voter.isHasVoted()) {
            model.addAttribute("message", "You have already voted.");
        }

        List<Candidate> candidates = candidateRepository.findAll();
        model.addAttribute("voter", voter);
        model.addAttribute("username", username);
        model.addAttribute("candidates", candidates);
        return "vote";
    }

    @PostMapping("/vote")
    public String castVote(@RequestParam("candidateId") Long candidateId,
                           HttpSession session,
                           Model model) {

        String username = (String) session.getAttribute("voterUsername");
        if (username == null) {
            return "redirect:/voter/login";
        }

        String status = getVotingStatus();
        if (!status.equals("open")) {
            model.addAttribute("error", "Voting is currently not open.");
            return "message";
        }

        Voter voter = voterRepository.findByUsername(username);
        if (voter == null) {
            session.invalidate();
            model.addAttribute("error", "Voter not found.");
            return "message";
        }

        if (voter.isHasVoted()) {
            model.addAttribute("message", "You have already voted.");
            return "message";
        }

        Candidate candidate = candidateRepository.findById(candidateId).orElse(null);
        if (candidate == null) {
            model.addAttribute("error", "Candidate not found.");
            return "message";
        }

        candidate.setVoteCount(candidate.getVoteCount() + 1);
        candidateRepository.save(candidate);

        voter.setHasVoted(true);
        voterRepository.save(voter);

        model.addAttribute("message", "Your vote has been submitted successfully!");
        return "message";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    // Used by the "Go back to Login" button on the status/message page.
    // Ends the current voter session so the login page is actually shown
    // (a still-active session would otherwise redirect straight to /vote).
    @PostMapping("/voter/exit")
    public String exitToLogin(HttpSession session) {
        session.invalidate();
        return "redirect:/voter/login";
    }
}
