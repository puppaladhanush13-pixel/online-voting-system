package com.example.demo;

import com.example.demo.Entity.Admin;
import com.example.demo.Entity.PendingVoter;
import com.example.demo.Entity.Voter;
import com.example.demo.Repository.AdminRepository;
import com.example.demo.Repository.PendingVoterRepository;
import com.example.demo.Repository.VoterRepository;
import com.example.demo.Service.VoterService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class OnlineVotingSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(OnlineVotingSystemApplication.class, args);
    }

    @Bean
    public CommandLineRunner seedAdmin(AdminRepository adminRepository) {
        return args -> {
            if (adminRepository.count() == 0) {
                Admin defaultAdmin = new Admin("admin", "admin1234");
                adminRepository.save(defaultAdmin);
            }
        };
    }

    /**
     * One-time, safe migration for voters that existed before the Aadhaar
     * activation workflow was introduced. Such rows have no registrationDate.
     * They are auto-activated so existing accounts keep working and are not
     * accidentally locked out by the new pending/approval logic.
     */
    @Bean
    public CommandLineRunner migrateLegacyVoters(VoterRepository voterRepository) {
        return args -> {
            List<Voter> legacy =
                    voterRepository.findByRegistrationDateIsNullAndIsActivatedFalseAndIsRejectedFalse();
            for (Voter voter : legacy) {
                voter.setActivated(true);
            }
            if (!legacy.isEmpty()) {
                voterRepository.saveAll(legacy);
            }
        };
    }

    /**
     * One-time, safe migration to enforce the corrected workflow: the active
     * voters table must contain ONLY approved voters. Any rows left there that
     * are not activated, or that were rejected, are moved out into the
     * pending_voters table (rejected rows keep status REJECTED) and removed
     * from the active voters table.
     */
    @Bean
    public CommandLineRunner moveUnapprovedVotersToPending(VoterRepository voterRepository,
                                                           PendingVoterRepository pendingVoterRepository) {
        return args -> {
            List<Voter> all = voterRepository.findAll();
            for (Voter voter : all) {
                boolean approved = voter.isActivated() && !voter.isRejected();
                if (approved) {
                    continue; // legitimate active voter, leave untouched
                }
                if (!pendingVoterRepository.existsByUsername(voter.getUsername())) {
                    PendingVoter pending = new PendingVoter();
                    pending.setUsername(voter.getUsername());
                    pending.setOriginalPassword(voter.getOriginalPassword() != null
                            ? voter.getOriginalPassword() : voter.getPassword());
                    pending.setEncryptedPassword(voter.getEncryptedPassword());
                    pending.setAadhaarImagePath(voter.getAadhaarImagePath());
                    pending.setStatus(voter.isRejected()
                            ? VoterService.STATUS_REJECTED : VoterService.STATUS_PENDING);
                    pending.setRegistrationDate(voter.getRegistrationDate());
                    pendingVoterRepository.save(pending);
                }
                voterRepository.delete(voter);
            }
        };
    }

    /**
     * One-time, safe backfill of secure password fields for voters created
     * before BCrypt hashing was introduced. For each voter missing an
     * encrypted password, we hash their existing plaintext password and
     * populate originalPassword + encryptedPassword. Existing logins keep
     * working and immediately use BCrypt afterwards.
     */
    @Bean
    public CommandLineRunner backfillVoterPasswordHashes(VoterRepository voterRepository,
                                                         VoterService voterService) {
        return args -> {
            for (Voter voter : voterRepository.findAll()) {
                String hash = voter.getEncryptedPassword();
                if (hash == null || hash.isBlank()) {
                    String plain = voter.getOriginalPassword() != null
                            ? voter.getOriginalPassword()
                            : voter.getPassword();
                    if (plain != null && !plain.isBlank()) {
                        voterService.upgradeToHashed(voter, plain);
                    }
                }
            }
        };
    }
}
