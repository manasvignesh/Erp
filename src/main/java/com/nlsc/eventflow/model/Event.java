package com.nlsc.eventflow.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "events")
public class Event {
    public enum Status { OPEN, UPCOMING, ONGOING, COMPLETED, CLOSED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false)
    public String eventName;

    @Column(nullable = false, length = 2000)
    public String description;

    @Column(nullable = false)
    public LocalDate eventDate;

    @Column(nullable = false)
    public String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public Status status;

    @Column(nullable = false)
    public String organizerEmail;

    @Column(nullable = false)
    public String supportMobile;

    @Column(unique = true)
    public String attendanceCode;

    public LocalDate attendanceCodeDate;

    public Event() {}

    public Event(String eventName, String description, LocalDate eventDate, String location,
                 Status status, String organizerEmail, String supportMobile) {
        this.eventName = eventName;
        this.description = description;
        this.eventDate = eventDate;
        this.location = location;
        this.status = status;
        this.organizerEmail = organizerEmail;
        this.supportMobile = supportMobile;
    }
}
