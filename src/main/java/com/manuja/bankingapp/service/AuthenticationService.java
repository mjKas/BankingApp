package com.manuja.bankingapp.service;

import com.manuja.bankingapp.dto.LoginUserDto;
import com.manuja.bankingapp.dto.RegisterUserDto;
import com.manuja.bankingapp.dto.VerifyUserDto;
import com.manuja.bankingapp.model.LoginAttempt;
import com.manuja.bankingapp.model.User;
import com.manuja.bankingapp.respository.LoginAttemptsRepository;
import com.manuja.bankingapp.respository.UserRepository;
import jakarta.mail.MessagingException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@Service
public class AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final LoginAttemptsRepository loginAttemptRepository;

    public AuthenticationService(
            UserRepository userRepository,
            AuthenticationManager authenticationManager,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            LoginAttemptsRepository loginAttemptRepository
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.loginAttemptRepository = loginAttemptRepository;
    }

    public User signup(RegisterUserDto input, String ipAddress) {
        //creating new user
        User user = new User(input.getUsername(), input.getEmail(), passwordEncoder.encode(input.getPassword()));
        user.setVerificationCode(generateVerificationCode());
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(15));
        user.setEnabled(false);
        sendVerificationEmail(user);
        //setting the IP for the signup as a login attempt
        LoginAttempt signupAttempt = new LoginAttempt();
        signupAttempt.setEmail(user.getEmail());
        signupAttempt.setIpAddress(ipAddress);
        signupAttempt.setSuccessful(true);
        signupAttempt.setAttemptTime(LocalDateTime.now());
        signupAttempt.setStatus("SIGNUP");

        loginAttemptRepository.save(signupAttempt);
        return userRepository.save(user);
        //returning and saving  user repository
    }

    public void authenticate(LoginUserDto input, String ipAddress) {
        List<LoginAttempt> recentAttempts = loginAttemptRepository.findByEmailAndAttemptTimeAfter(
                input.getEmail(),
                LocalDateTime.now().minusMinutes(2)
        );
        long failedAttempts = recentAttempts.stream()
                .filter(attempt -> !attempt.isSuccessful())
                .count();
        if (failedAttempts >= 5) {
            SendLoginWarning(input.getEmail());
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Suspicious activity detected. Too many failed attempts."
            );
        }
        User user = userRepository.findByEmail(input.getEmail())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));
        if (!user.isEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Account not verified. Please verify your account."
            );
        }
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            input.getEmail(),
                            input.getPassword()
                    )
            );
            // Generate LOGIN OTP
            user.setVerificationCode(generateVerificationCode());
            user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(5));
            userRepository.save(user);
            sendLoginOtp(user);
            /*
            LoginAttempt successAttempt = new LoginAttempt();
            successAttempt.setEmail(input.getEmail());
            successAttempt.setIpAddress(ipAddress);
            successAttempt.setSuccessful(true);
            successAttempt.setAttemptTime(LocalDateTime.now());
            successAttempt.setStatus("NORMAL");
            loginAttemptRepository.save(successAttempt);
            return user;*/
        } catch (Exception e) {

            LoginAttempt failedAttempt = new LoginAttempt();
            failedAttempt.setEmail(input.getEmail());
            failedAttempt.setIpAddress(ipAddress);
            failedAttempt.setSuccessful(false);
            failedAttempt.setAttemptTime(LocalDateTime.now());
            failedAttempt.setStatus("FAILED");
            loginAttemptRepository.save(failedAttempt);
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password"
            );
        }
    }

    public User verifyLoginOtp(VerifyUserDto input) {

        User user = userRepository.findByEmail(input.getEmail())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        if (user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "OTP expired"
            );
        }

        if (!user.getVerificationCode().equals(input.getVerificationCode())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid OTP"
            );
        }

        // Clear OTP after successful login verification
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);
        userRepository.save(user);

        return user;
    }

    public void  verifyUserEmail(VerifyUserDto input) {
        Optional<User> optionalUser = userRepository.findByEmail(input.getEmail());
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Verification code has expired"
                );
            }
            if (user.getVerificationCode().equals(input.getVerificationCode())) {
                user.setEnabled(true);
                user.setVerificationCode(null);
                user.setVerificationCodeExpiresAt(null);
                userRepository.save(user);
            } else {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid verification code"
                );
            }
        } else {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "User not found"
            );
        }
    }

    public void resendVerificationCode(String email) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (user.isEnabled()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Account is already verified"
                );
            }
            user.setVerificationCode(generateVerificationCode());
            user.setVerificationCodeExpiresAt(LocalDateTime.now().plusHours(1));
            sendVerificationEmail(user);
            userRepository.save(user);
        } else {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "User not found"
            );
        }
    }
    private String generateVerificationCode() {
        Random random = new Random();
        int code = random.nextInt(900000) + 100000;
        return String.valueOf(code);
    }

    private void sendVerificationEmail(User user) {
        String subject = "Email Verification";
        String verificationCode = "Email VERIFICATION CODE " + user.getVerificationCode();
        String htmlMessage = "<html>"
                + "<body style=\"font-family: Arial, sans-serif;\">"
                + "<div style=\"background-color: #f5f5f5; padding: 20px;\">"
                + "<h2 style=\"color: #333;\">Welcome to Banking app!</h2>"
                + "<p style=\"font-size: 16px;\">Please enter the verification code below to continue:</p>"
                + "<div style=\"background-color: #fff; padding: 20px; border-radius: 5px; box-shadow: 0 0 10px rgba(0,0,0,0.1);\">"
                + "<h3 style=\"color: #333;\">Verification Code:</h3>"
                + "<p style=\"font-size: 18px; font-weight: bold; color: #007bff;\">" + verificationCode + "</p>"
                + "</div>"
                + "</div>"
                + "</body>"
                + "</html>";

        try {
            emailService.sendVerificationEmail(user.getEmail(), subject, htmlMessage);
        } catch (MessagingException e) {
            // Handle email sending exception
            e.printStackTrace();
        }
    }

    private void SendLoginWarning(String email) {
        String subject = "Login warning";
        String htmlMessage = "<html>"
                + "<body style=\"font-family: Arial, sans-serif;\">"
                + "<div style=\"background-color: #f5f5f5; padding: 20px;\">"
                + "<h2 style=\"color: #FF0000;\">False Login!</h2>"
                + "<p style=\"font-size: 16px;\">Unsuccessful Login attempts detected.</p>"
                + "<div style=\"background-color: #fff; padding: 20px; border-radius: 5px; box-shadow: 0 0 10px rgba(0,0,0,0.1);\">"

                + "</div>"
                + "</div>"
                + "</body>"
                + "</html>";

        try {
            emailService.sendVerificationEmail(email, subject, htmlMessage);
        } catch (MessagingException e) {
            // Handle email sending exception
            e.printStackTrace();
        }
    }

    private void sendLoginOtp(User user){
        String subject = "Account Verification";
        String verificationCode = "VERIFICATION CODE " + user.getVerificationCode();
        String htmlMessage = "<html>"
                + "<body style=\"font-family: Arial, sans-serif;\">"
                + "<div style=\"background-color: #f5f5f5; padding: 20px;\">"
                + "<h2 style=\"color: #333;\">Welcome to Banking app!</h2>"
                + "<p style=\"font-size: 16px;\">Please enter the verification code below to continue:</p>"
                + "<div style=\"background-color: #fff; padding: 20px; border-radius: 5px; box-shadow: 0 0 10px rgba(0,0,0,0.1);\">"
                + "<h3 style=\"color: #333;\">Verification Code:</h3>"
                + "<p style=\"font-size: 18px; font-weight: bold; color: #007bff;\">" + verificationCode + "</p>"
                + "</div>"
                + "</div>"
                + "</body>"
                + "</html>";

        try {
            emailService.sendVerificationEmail(user.getEmail(), subject, htmlMessage);
        } catch (MessagingException e) {
            // Handle email sending exception
            e.printStackTrace();
        }
    }
}
