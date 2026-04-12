package com.chronos.auth_service_chronos.dto;

import lombok.Data;

@Data
public class RegisterRequestDto {

    private String username;
    private String email;
    private String password;
}
