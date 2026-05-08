package com.manuja.bankingapp.respository;

import com.manuja.bankingapp.model.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface LoginAttemptsRepository extends JpaRepository <LoginAttempt, Integer> {
    List<LoginAttempt> findByEmailAndAttemptTimeAfter(
            String email,
            LocalDateTime time
    );
}
