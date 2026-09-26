package com.nlsc.eventflow.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "attendance",
    uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "user_id"})
)
public class Attendance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "event_id", nullable = false)
    public Long eventId;

    @Column(name = "user_id", nullable = false)
    public Long userId;

    @Column(nullable = false)
    public LocalDateTime markedAt = LocalDateTime.now();

    public Attendance() {}

    public Attendance(Long eventId, Long userId) {
        this.eventId = eventId;
        this.userId = userId;
    }
}
