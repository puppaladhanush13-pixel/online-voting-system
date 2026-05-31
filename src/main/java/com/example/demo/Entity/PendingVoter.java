package com.example.demo.Entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * A voter registration request awaiting admin decision.
 * Lives in its own table so unapproved voters never appear in the
 * main (active) voters table. Approved requests are copied into the
 * Voter table and removed from here; rejected requests are kept with
 * status = REJECTED so the login screen can show a rejection message.
 */
@Entity
@Table(name = "pending_voters")
public class PendingVoter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    // Secure password storage (mirrors Voter): plaintext kept DB-only, hash used for auth.
    private String originalPassword;
    private String encryptedPassword;

    private String aadhaarImagePath;

    @Column(nullable = false)
    private String status; // PENDING or REJECTED

    private LocalDateTime registrationDate;

    public PendingVoter() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getOriginalPassword() {
        return originalPassword;
    }

    public void setOriginalPassword(String originalPassword) {
        this.originalPassword = originalPassword;
    }

    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    public void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }

    public String getAadhaarImagePath() {
        return aadhaarImagePath;
    }

    public void setAadhaarImagePath(String aadhaarImagePath) {
        this.aadhaarImagePath = aadhaarImagePath;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(LocalDateTime registrationDate) {
        this.registrationDate = registrationDate;
    }
}
