package com.example.demo.Service;

import com.example.demo.Entity.PendingVoter;
import com.example.demo.Entity.Voter;
import com.example.demo.Repository.PendingVoterRepository;
import com.example.demo.Repository.VoterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class VoterService {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_REJECTED = "REJECTED";

    @Autowired
    private VoterRepository voterRepository;

    @Autowired
    private PendingVoterRepository pendingVoterRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Registers a new voter as a PENDING request. The voter is NOT added to the
     * active voters table yet — only after an admin accepts the request.
     * Returns null if the username is already taken (in active or pending lists).
     */
    public PendingVoter registerVoter(String username, String password, String aadhaarImagePath) {
        if (isUsernameTaken(username)) {
            return null; // username already exists somewhere
        }
        String hashed = passwordEncoder.encode(password);

        PendingVoter pending = new PendingVoter();
        pending.setUsername(username);
        pending.setOriginalPassword(password);
        pending.setEncryptedPassword(hashed);
        pending.setAadhaarImagePath(aadhaarImagePath);
        pending.setStatus(STATUS_PENDING);
        pending.setRegistrationDate(LocalDateTime.now());
        return pendingVoterRepository.save(pending);
    }

    /** A username is taken if it exists in active voters or as a pending/rejected request. */
    public boolean isUsernameTaken(String username) {
        return voterRepository.findByUsername(username) != null
                || pendingVoterRepository.existsByUsername(username);
    }

    /**
     * Securely verifies the supplied raw password against the stored BCrypt hash.
     * Falls back to legacy plaintext comparison for voters created before hashing
     * was introduced, and transparently upgrades them to a hash on success.
     */
    public boolean verifyPassword(Voter voter, String rawPassword) {
        if (voter == null || rawPassword == null) {
            return false;
        }

        String hash = voter.getEncryptedPassword();
        if (hash != null && !hash.isBlank()) {
            return passwordEncoder.matches(rawPassword, hash);
        }

        // Legacy voter without a hash yet: compare against stored plaintext,
        // then upgrade to a secure hash so future logins use BCrypt.
        String legacy = voter.getOriginalPassword() != null
                ? voter.getOriginalPassword()
                : voter.getPassword();
        if (legacy != null && legacy.equals(rawPassword)) {
            upgradeToHashed(voter, rawPassword);
            return true;
        }
        return false;
    }

    /** Backfills the original + encrypted password fields for a legacy voter. */
    public void upgradeToHashed(Voter voter, String rawPassword) {
        if (voter.getOriginalPassword() == null) {
            voter.setOriginalPassword(rawPassword);
        }
        voter.setEncryptedPassword(passwordEncoder.encode(rawPassword));
        voterRepository.save(voter);
    }

    public Voter authenticateVoter(String username, String password) {
        Voter voter = voterRepository.findByUsername(username);
        if (voter != null && verifyPassword(voter, password)) {
            return voter;
        }
        return null;
    }

    public Voter findByUsername(String username) {
        return voterRepository.findByUsername(username);
    }

    public PendingVoter findPendingByUsername(String username) {
        return pendingVoterRepository.findByUsername(username);
    }

    /** Registration requests still awaiting an admin decision. */
    public List<PendingVoter> getPendingVoters() {
        return pendingVoterRepository.findByStatus(STATUS_PENDING);
    }

    /**
     * Approve a pending request: create the official (active) voter and remove
     * the pending request. The new voter can then log in and vote.
     */
    public boolean activateVoter(Long pendingId) {
        PendingVoter pending = pendingVoterRepository.findById(pendingId).orElse(null);
        if (pending == null) {
            return false;
        }

        // Guard against duplicates if an active voter already exists.
        if (voterRepository.findByUsername(pending.getUsername()) == null) {
            Voter voter = new Voter(pending.getUsername(), pending.getOriginalPassword());
            voter.setOriginalPassword(pending.getOriginalPassword());
            voter.setEncryptedPassword(pending.getEncryptedPassword());
            voter.setAadhaarImagePath(pending.getAadhaarImagePath());
            voter.setHasVoted(false);
            voter.setActivated(true);
            voter.setRejected(false);
            voter.setRegistrationDate(pending.getRegistrationDate());
            voterRepository.save(voter);
        }

        // Approved request leaves the pending list entirely.
        pendingVoterRepository.delete(pending);
        return true;
    }

    /**
     * Reject a pending request: it must never enter the active voters table.
     * The request is kept with status = REJECTED so login can display a
     * rejection message.
     */
    public boolean rejectVoter(Long pendingId) {
        PendingVoter pending = pendingVoterRepository.findById(pendingId).orElse(null);
        if (pending == null) {
            return false;
        }
        pending.setStatus(STATUS_REJECTED);
        pendingVoterRepository.save(pending);
        return true;
    }
}
