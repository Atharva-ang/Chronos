package com.chronos.auth_service_chronos.service;

import com.chronos.auth_service_chronos.dto.LoginRequestDto;
import com.chronos.auth_service_chronos.dto.LoginResponseDto;
import com.chronos.auth_service_chronos.dto.RegisterRequestDto;
import com.chronos.auth_service_chronos.model.User;
import com.chronos.auth_service_chronos.repository.UserRepository;
import com.chronos.auth_service_chronos.util.JwtUtil;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }
    @Transactional
    public User registerUser(RegisterRequestDto request) {


        if(userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
        return userRepository.save(user);
    }

    public LoginResponseDto loginUser(LoginRequestDto request){
        User user = userRepository.findByUsername(request.getUsername()).orElseThrow(() ->
                new IllegalArgumentException("Username not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        String fullToken = jwtUtil.generateToken(user.getUsername(), user.getEmail());
        return LoginResponseDto.builder()
                .token(fullToken)
                .message("Here's your token baccha")
                .build();
    }
}
