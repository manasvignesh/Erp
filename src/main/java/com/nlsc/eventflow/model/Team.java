package com.nlsc.eventflow.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "teams",
    uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "captain_id"})
)
public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "event_id", nullable = false)
    public Long eventId;

    @Column(nullable = false)
    public String teamName;

    @Column(name = "captain_id", nullable = false)
    public Long captainId;

    // Competition sheet explicitly asks for Team Members as a JSON list.
    @Lob
    @Column(nullable = false)
    public String memberIdsJson = "[]";

    @Column(nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    public Team() {}
}
