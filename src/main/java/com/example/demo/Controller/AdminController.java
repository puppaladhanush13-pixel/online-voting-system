package com.example.demo.Controller;

import com.example.demo.Entity.Admin;
import com.example.demo.Entity.Candidate;
import com.example.demo.Entity.Voter;
import com.example.demo.Entity.VotingConfig;
import com.example.demo.Repository.AdminRepository;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

@Controller
public class AdminController {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private VoterRepository voterRepository;

    @Autowired
    private VotingConfigRepository votingConfigRepository;

    @Autowired
    private VoterService voterService;

    @GetMapping("/admin/login")
    public String showLoginPage(HttpSession session) {
        if (session.getAttribute("adminUsername") != null) {
            return "redirect:/admin/dashboard";
        }
        return "adminlogin";
    }

    @PostMapping("/admin/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {
        Admin admin = adminRepository.findByUsername(username);
        if (admin != null && admin.getPassword().equals(password)) {
            session.setAttribute("adminUsername", admin.getUsername());
            return "redirect:/admin/dashboard";
        } else {
            model.addAttribute("error", "Invalid username or password");
            return "adminlogin";
        }
    }

    @GetMapping("/admin/dashboard")
    public String showAdminDashboard(HttpSession session, Model model) {
        if (session.getAttribute("adminUsername") == null) {
            return "redirect:/admin/login";
        }
        model.addAttribute("candidates", candidateRepository.findAll());
        model.addAttribute("totalVoters", voterRepository.count());
        model.addAttribute("votesCast", voterRepository.countByHasVoted(true));
        return "admindashboard";
    }

    @PostMapping("/admin/addCandidate")
    public String addCandidate(@RequestParam("candidateName") String candidateName,
                               @RequestParam("partyName") String partyName,
                               @RequestParam("partySymbol") MultipartFile partySymbol,
                               @RequestParam("candidatePhoto") MultipartFile candidatePhoto,
                               Model model) {
        try {
            String partySymbolPath = saveImage(partySymbol);
            String candidatePhotoPath = saveImage(candidatePhoto);

            Candidate candidate = new Candidate(candidateName, partyName, partySymbolPath, candidatePhotoPath);
            candidate.setVoteCount(0);
            candidateRepository.save(candidate);

            return "redirect:/admin/dashboard";
        } catch (IOException e) {
            model.addAttribute("error", "Failed to upload files");
            model.addAttribute("candidates", candidateRepository.findAll());
            model.addAttribute("totalVoters", voterRepository.count());
            model.addAttribute("votesCast", voterRepository.countByHasVoted(true));
            return "admindashboard";
        }
    }

    private String saveImage(MultipartFile file) throws IOException {
        String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path uploadPath = Paths.get("uploads");

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return "/uploads/" + filename;
    }

    @PostMapping("/admin/deleteCandidate")
    public String deleteCandidate(@RequestParam Long id, RedirectAttributes redirectAttributes) {
        candidateRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("toastMessage", "Candidate deleted successfully!");
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/admin/editCandidate")
    public String showEditForm(@RequestParam("id") Long id, Model model) {
        Candidate candidate = candidateRepository.findById(id).orElse(null);
        if (candidate == null) {
            model.addAttribute("error", "Candidate not found.");
            return "redirect:/admin/dashboard";
        }
        model.addAttribute("candidate", candidate);
        return "editcandidate";
    }

    @PostMapping("/admin/updateCandidate")
    public String updateCandidate(@RequestParam("id") Long id,
                                  @RequestParam("candidateName") String candidateName,
                                  @RequestParam("partyName") String partyName,
                                  @RequestParam(value = "partySymbol", required = false) MultipartFile partySymbol,
                                  @RequestParam(value = "candidatePhoto", required = false) MultipartFile candidatePhoto,
                                  RedirectAttributes redirectAttributes) {
        Candidate candidate = candidateRepository.findById(id).orElse(null);
        if (candidate == null) {
            redirectAttributes.addFlashAttribute("error", "Candidate not found.");
            return "redirect:/admin/dashboard";
        }

        candidate.setCandidateName(candidateName);
        candidate.setPartyName(partyName);

        try {
            if (partySymbol != null && !partySymbol.isEmpty()) {
                String partySymbolPath = saveImage(partySymbol);
                candidate.setPartySymbolPath(partySymbolPath);
            }
            if (candidatePhoto != null && !candidatePhoto.isEmpty()) {
                String candidatePhotoPath = saveImage(candidatePhoto);
                candidate.setCandidatePhotoPath(candidatePhotoPath);
            }
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Image upload failed.");
            return "redirect:/admin/dashboard";
        }

        candidateRepository.save(candidate);
        redirectAttributes.addFlashAttribute("toastMessage", "Candidate updated successfully!");
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/admin/voters")
    public String viewVoters(Model model) {
        model.addAttribute("voters", voterRepository.findAll());
        return "voterlist";
    }

    // === Voting time configuration ===

    @GetMapping("/admin/votingtime")
    public String showVotingTimeForm(Model model) {
        VotingConfig config = votingConfigRepository.findAll().stream().findFirst().orElse(new VotingConfig());
        model.addAttribute("config", config);
        return "votingtime";
    }

    @PostMapping("/admin/votingtime")
    public String updateVotingTime(@ModelAttribute VotingConfig config) {
        // Keep a single voting-config row: reuse the existing row's id if present.
        VotingConfig existing = votingConfigRepository.findAll().stream().findFirst().orElse(null);
        if (existing != null) {
            config.setId(existing.getId());
        }
        votingConfigRepository.save(config);
        return "redirect:/admin/votingtime";
    }

    @PostMapping("/admin/votingtime/delete")
    public String deleteVotingTime() {
        votingConfigRepository.deleteAll();
        return "redirect:/admin/votingtime"; // ✅ fixed redirect
    }
    @PostMapping("/admin/resetVotes")
    public String resetVotes(HttpSession session, RedirectAttributes redirectAttributes) {
        if (session.getAttribute("adminUsername") == null) {
            return "redirect:/admin/login";
        }
        // Reset vote count for all candidates
        List<Candidate> candidates = candidateRepository.findAll();
        for (Candidate candidate : candidates) {
            candidate.setVoteCount(0);
        }
        candidateRepository.saveAll(candidates);

        // Reset voting status for all voters
        List<Voter> voters = voterRepository.findAll();
        for (Voter voter : voters) {
            voter.setHasVoted(false);
        }
        voterRepository.saveAll(voters);

        redirectAttributes.addFlashAttribute("toastMessage", "Votes and voter status reset successfully.");
        return "redirect:/admin/dashboard";
    }

    // === Voter Activation (Aadhaar approval workflow) ===

    @GetMapping("/admin/voterActivation")
    public String showVoterActivation(HttpSession session, Model model) {
        if (session.getAttribute("adminUsername") == null) {
            return "redirect:/admin/login";
        }
        model.addAttribute("pendingVoters", voterService.getPendingVoters());
        return "voteractivation";
    }

    @PostMapping("/admin/voterActivation/accept")
    public String acceptVoter(@RequestParam("id") Long id,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        if (session.getAttribute("adminUsername") == null) {
            return "redirect:/admin/login";
        }
        boolean ok = voterService.activateVoter(id);
        redirectAttributes.addFlashAttribute("toastMessage",
                ok ? "Voter activated successfully." : "Voter not found.");
        return "redirect:/admin/voterActivation";
    }

    @PostMapping("/admin/voterActivation/reject")
    public String rejectVoter(@RequestParam("id") Long id,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        if (session.getAttribute("adminUsername") == null) {
            return "redirect:/admin/login";
        }
        boolean ok = voterService.rejectVoter(id);
        redirectAttributes.addFlashAttribute("toastMessage",
                ok ? "Voter registration rejected." : "Voter not found.");
        return "redirect:/admin/voterActivation";
    }

    @PostMapping("/admin/logout")
    public String adminLogout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

}
