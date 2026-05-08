package com.manuja.bankingapp.controller;

import com.manuja.bankingapp.dto.LoginUserDto;
import com.manuja.bankingapp.dto.RegisterUserDto;
import com.manuja.bankingapp.dto.VerifyUserDto;
import com.manuja.bankingapp.model.User;
import com.manuja.bankingapp.reponses.LoginResponse;
import com.manuja.bankingapp.reponses.MessageResponse;
import com.manuja.bankingapp.reponses.SignupResponse;
import com.manuja.bankingapp.service.AuthenticationService;
import com.manuja.bankingapp.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthenticationController {
    private final JwtService jwtService;

    private final AuthenticationService authenticationService;

    public AuthenticationController(JwtService jwtService, AuthenticationService authenticationService) {
        this.jwtService = jwtService;
        this.authenticationService = authenticationService;
    }

    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> register(@RequestBody RegisterUserDto registerUserDto, HttpServletRequest request) {
        String ipAddress = request.getRemoteAddr();
        User registeredUser = authenticationService.signup(registerUserDto, ipAddress);
        SignupResponse response = new SignupResponse(
                "User registered successfully",
                registeredUser.getEmail()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> authenticate(@RequestBody LoginUserDto loginUserDto,  HttpServletRequest request){
        String ipAddress = request.getRemoteAddr();
        User authenticatedUser = authenticationService.authenticate(loginUserDto, ipAddress);
        String jwtToken = jwtService.generateToken(authenticatedUser);
        LoginResponse loginResponse = new LoginResponse(jwtToken, jwtService.getExpirationTime(),"Login Successful");

        return ResponseEntity.ok(loginResponse);

    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyUser(@RequestBody VerifyUserDto verifyUserDto) {
        try {
            authenticationService.verifyUser(verifyUserDto);
            return ResponseEntity.ok(  new MessageResponse(
                    "Account verified successfully"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/resend")
    public ResponseEntity<?> resendVerificationCode(@RequestParam String email) {
        try {
            authenticationService.resendVerificationCode(email);
            return ResponseEntity.ok("Verification code sent");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
