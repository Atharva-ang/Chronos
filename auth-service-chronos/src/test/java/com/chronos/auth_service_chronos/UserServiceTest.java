package com.chronos.auth_service_chronos;

import com.chronos.auth_service_chronos.dto.RegisterRequestDto;
import com.chronos.auth_service_chronos.model.User;
import com.chronos.auth_service_chronos.repository.UserRepository;
import com.chronos.auth_service_chronos.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// 1. This annotation tells JUnit to enable Mockito's @Mock and @InjectMocks magic
@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    // 2. Mock the dependencies that AuthService needs
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    // 3. Inject those mocks directly into the service we want to test
    @InjectMocks
    private AuthService authService;

    private RegisterRequestDto requestDto;

    // 4. Set up dummy data before each test runs
    @BeforeEach
    void setUp() {
        requestDto = new RegisterRequestDto();
        requestDto.setUsername("atharva_g");
        requestDto.setEmail("atharva@example.com");
        requestDto.setPassword("plainTextPassword");
    }

    @Test
    void testRegisterUser_Success() {
        // --- ARRANGE ---
        // Tell the mocks how to behave when the service calls them
        when(userRepository.existsByUsername(requestDto.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(requestDto.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(requestDto.getPassword())).thenReturn("hashed_password_123");

        // Create the user that the repository *should* return after saving
        User savedUser = User.builder()
                .id(1L)
                .username("atharva_g")
                .email("atharva@example.com")
                .password("hashed_password_123")
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // --- ACT ---
        // Actually call the method we are testing
        User result = authService.registerUser(requestDto);

        // --- ASSERT ---
        // Verify the results match our expectations
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("atharva_g", result.getUsername());
        assertEquals("hashed_password_123", result.getPassword());

        // Pro-move: Verify that the save method was actually called exactly one time
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testRegisterUser_ThrowsExceptionWhenUsernameExists() {
        // --- ARRANGE ---
        // Force the repository to say the username is already taken
        when(userRepository.existsByUsername(requestDto.getUsername())).thenReturn(true);

        // --- ACT & ASSERT ---
        // Check that a RuntimeException is thrown
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.registerUser(requestDto);
        });

        assertEquals("Username already exists", exception.getMessage());

        // Verify that the save method was NEVER called because the code stopped at the exception
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testRegisterUser_ThrowsExceptionWhenEmailExists() {
        // --- ARRANGE ---
        // Username is fine, but email is taken
        when(userRepository.existsByUsername(requestDto.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(requestDto.getEmail())).thenReturn(true);

        // --- ACT & ASSERT ---
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.registerUser(requestDto);
        });

        assertEquals("Email already in use", exception.getMessage());

        // Verify save was never called
        verify(userRepository, never()).save(any(User.class));
    }
}