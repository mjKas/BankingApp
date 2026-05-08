package com.manuja.bankingapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
@Getter
@Setter
@Entity
@Table(name = "login_attempts")
public class LoginAttempt {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    private String email;

    private String ipAddress;

    private boolean successful;

    private LocalDateTime attemptTime;

    private String status;
}
