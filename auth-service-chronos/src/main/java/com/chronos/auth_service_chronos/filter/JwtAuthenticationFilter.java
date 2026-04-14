package com.chronos.auth_service_chronos.filter;


import com.chronos.auth_service_chronos.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthenticationFilter {
    @Autowired
    private JwtUtil jwtUtil;


}
