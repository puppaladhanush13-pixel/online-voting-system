package com.example.demo.Repository;

import com.example.demo.Entity.PendingVoter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PendingVoterRepository extends JpaRepository<PendingVoter, Long> {
    PendingVoter findByUsername(String username);
    List<PendingVoter> findByStatus(String status);
    boolean existsByUsername(String username);
}
