package com.manuja.bankingapp.model;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
@Getter
@Setter
public class LoginAttempt {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    private String email;

    private String ipAddress;

    private boolean successful;

    private LocalDateTime attemptTime;

    private String status;
}
