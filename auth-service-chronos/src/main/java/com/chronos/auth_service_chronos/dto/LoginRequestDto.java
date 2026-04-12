package com.chronos.auth_service_chronos.dto;

import lombok.Data;

@Data // Lombok automatically creates getters, setters, and constructors behind the scenes!
public class LoginRequestDto {
    private String username;
    private String password;
}