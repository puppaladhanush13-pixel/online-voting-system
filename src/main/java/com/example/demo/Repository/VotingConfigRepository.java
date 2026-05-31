package com.example.demo.Repository;

import com.example.demo.Entity.VotingConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VotingConfigRepository extends JpaRepository<VotingConfig, Long> {
}
