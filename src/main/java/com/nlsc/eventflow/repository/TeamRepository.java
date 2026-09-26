package com.nlsc.eventflow.repository;

import com.nlsc.eventflow.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {
    List<Team> findByEventIdOrderByCreatedAtAsc(Long eventId);
    Optional<Team> findByEventIdAndCaptainId(Long eventId, Long captainId);
}
