package com.nlsc.eventflow.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class AppUser {
    public enum Role { ADMIN, COLLEGE, STUDENT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false)
    public String name;

    @Column(nullable = false, unique = true)
    public String email;

    @Column(nullable = false)
    public String mobile;

    @Column(nullable = false)
    public String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public Role role;

    public AppUser() {}

    public AppUser(String name, String email, String mobile, String passwordHash, Role role) {
        this.name = name;
        this.email = email;
        this.mobile = mobile;
        this.passwordHash = passwordHash;
        this.role = role;
    }
}
