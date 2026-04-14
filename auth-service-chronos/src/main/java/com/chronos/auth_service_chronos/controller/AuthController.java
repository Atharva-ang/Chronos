package com.chronos.auth_service_chronos.controller;

import com.chronos.auth_service_chronos.dto.LoginRequestDto;
import com.chronos.auth_service_chronos.dto.LoginResponseDto;
import com.chronos.auth_service_chronos.dto.RegisterRequestDto;
import com.chronos.auth_service_chronos.model.User;
import com.chronos.auth_service_chronos.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor // Automatically creates the constructor for 'final' fields
public class AuthController {


    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody RegisterRequestDto registerRequestDto) {

        authService.registerUser(registerRequestDto);

        return ResponseEntity.ok("User successfully registered");
    }
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> loginUser(@RequestBody LoginRequestDto loginRequestDto){
        LoginResponseDto response = authService.loginUser(loginRequestDto);

        return ResponseEntity.ok(response);
    }
}