package com.example.demo.Repository;

import com.example.demo.Entity.Voter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VoterRepository extends JpaRepository<Voter, Long> {
    Voter findByUsername(String username);
    long countByHasVoted(boolean hasVoted);

    // Pending = not yet activated and not rejected (awaiting admin decision)
    List<Voter> findByIsActivatedFalseAndIsRejectedFalse();

    // Legacy rows created before the activation workflow existed
    List<Voter> findByRegistrationDateIsNullAndIsActivatedFalseAndIsRejectedFalse();
}
