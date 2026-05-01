package com.manuja.bankingapp.responses;

import com.manuja.bankingapp.model.User;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface UserRepository extends CrudRepository<User,Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByVerificationCode(String VerificationCd);
}
