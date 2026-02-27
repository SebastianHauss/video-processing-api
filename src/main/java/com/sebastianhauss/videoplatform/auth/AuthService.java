package com.sebastianhauss.videoplatform.auth;

import com.sebastianhauss.videoplatform.auth.dto.AuthResponse;
import com.sebastianhauss.videoplatform.auth.dto.LoginRequest;
import com.sebastianhauss.videoplatform.domain.user.User;
import com.sebastianhauss.videoplatform.domain.user.UserRole;
import com.sebastianhauss.videoplatform.dto.user.UserCreateDto;
import com.sebastianhauss.videoplatform.dto.user.UserResponseDto;
import com.sebastianhauss.videoplatform.exception.UserAlreadyExistsException;
import com.sebastianhauss.videoplatform.exception.UserNotFoundException;
import com.sebastianhauss.videoplatform.mapper.UserMapper;
import com.sebastianhauss.videoplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserResponseDto register(UserCreateDto dto) {
        if (userRepository.existsByUsername(dto.username())) {
            throw new UserAlreadyExistsException();
        }
        if (userRepository.existsByEmail(dto.email())) {
            throw new UserAlreadyExistsException();
        }
        User user = new User();
        user.setUsername(dto.username());
        user.setEmail(dto.email());
        user.setPasswordHash(passwordEncoder.encode(dto.password()));
        user.setRole(UserRole.USER);

        userRepository.save(user);

        return userMapper.toResponseDto(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(UserNotFoundException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username or password");
        }
        return new AuthResponse(jwtService.generateToken(user.getUsername()));
    }
}
