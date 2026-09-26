package com.nlsc.eventflow.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public String name;
    public String email;
    public String mobile;
    public String role;
    public String password;

    public User() {}

    public User(String name, String email, String mobile, String role, String password) {
        this.name = name;
        this.email = email;
        this.mobile = mobile;
        this.role = role;
        this.password = password;
    }
}
