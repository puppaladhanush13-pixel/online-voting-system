package com.example.demo.Entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "voters")
public class Voter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    // ===== Secure password storage =====
    // originalPassword  -> plaintext, ONLY visible via direct DB access (never shown in UI)
    // encryptedPassword -> BCrypt hash, used for authentication and shown (masked) in admin UI
    private String originalPassword;
    private String encryptedPassword;

    @Column(nullable = false)
    private boolean hasVoted;

    // ===== Aadhaar verification / activation workflow (new) =====
    private String aadhaarImagePath;

    @Column(nullable = false)
    private boolean isActivated = false;

    @Column(nullable = false)
    private boolean isRejected = false;

    private LocalDateTime registrationDate;

    public Voter() {}

    public Voter(String username, String password) {
        this.username = username;
        this.password = password;
        this.hasVoted = false;
    }

    // Getters and setters

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public boolean isHasVoted() {
        return hasVoted;
    }

    public void setHasVoted(boolean hasVoted) {
        this.hasVoted = hasVoted;
    }

    public String getAadhaarImagePath() {
        return aadhaarImagePath;
    }

    public void setAadhaarImagePath(String aadhaarImagePath) {
        this.aadhaarImagePath = aadhaarImagePath;
    }

    public boolean isActivated() {
        return isActivated;
    }

    public void setActivated(boolean activated) {
        this.isActivated = activated;
    }

    public boolean isRejected() {
        return isRejected;
    }

    public void setRejected(boolean rejected) {
        this.isRejected = rejected;
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(LocalDateTime registrationDate) {
        this.registrationDate = registrationDate;
    }
}
