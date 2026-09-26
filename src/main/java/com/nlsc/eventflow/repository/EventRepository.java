package com.nlsc.eventflow.repository;

import com.nlsc.eventflow.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findAllByOrderByEventDateAsc();
    Optional<Event> findByAttendanceCode(String attendanceCode);
}
