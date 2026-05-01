package com.manuja.bankingapp.service;

import com.manuja.bankingapp.model.User;
import com.manuja.bankingapp.responses.UserRepository;

import java.util.ArrayList;
import java.util.List;

public class UserService {
    private final UserRepository userRepository;
    public UserService(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
    }

    public List<User> allUsers() {
        List<User> users = new ArrayList<>();
        userRepository.findAll().forEach(users::add);
        return users;
    }
}
