package com.chronos.auth_service_chronos.util;


import com.chronos.auth_service_chronos.repository.UserRepository;

public class JwtUtil {
    private final UserRepository userRepository;

    public JwtUtil(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String generateToken(userRepository.getUsername()){

    }
}
