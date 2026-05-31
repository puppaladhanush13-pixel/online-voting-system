package com.example.demo.Entity;

import jakarta.persistence.*;

@Entity
@Table(name = "candidates")
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String candidateName;

    @Column(nullable = false)
    private String partyName;

    private String partySymbolPath;
    private String candidatePhotoPath;

    @Column(nullable = false)
    private int voteCount = 0;

    public Candidate() {}

    public Candidate(String candidateName, String partyName, String partySymbolPath, String candidatePhotoPath) {
        this.candidateName = candidateName;
        this.partyName = partyName;
        this.partySymbolPath = partySymbolPath;
        this.candidatePhotoPath = candidatePhotoPath;
        this.voteCount = 0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
    }

    public String getPartyName() {
        return partyName;
    }

    public void setPartyName(String partyName) {
        this.partyName = partyName;
    }

    public String getPartySymbolPath() {
        return partySymbolPath;
    }

    public void setPartySymbolPath(String partySymbolPath) {
        this.partySymbolPath = partySymbolPath;
    }

    public String getCandidatePhotoPath() {
        return candidatePhotoPath;
    }

    public void setCandidatePhotoPath(String candidatePhotoPath) {
        this.candidatePhotoPath = candidatePhotoPath;
    }

    public int getVoteCount() {
        return voteCount;
    }

    public void setVoteCount(int voteCount) {
        this.voteCount = voteCount;
    }
}
