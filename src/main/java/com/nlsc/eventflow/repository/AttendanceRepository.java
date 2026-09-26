package com.nlsc.eventflow.repository;

import com.nlsc.eventflow.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    Optional<Attendance> findByEventIdAndUserId(Long eventId, Long userId);
    List<Attendance> findByUserIdOrderByMarkedAtDesc(Long userId);
}
