package com.demo.notify.entity;

import jakarta.persistence.*;

@Entity @Table(name = "users")
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(unique = true, nullable = false)
    public String email;
    public String displayName;
}
