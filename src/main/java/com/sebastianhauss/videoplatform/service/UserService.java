package com.sebastianhauss.videoplatform.service;

import com.sebastianhauss.videoplatform.domain.user.User;
import com.sebastianhauss.videoplatform.dto.user.UserCreateDto;
import com.sebastianhauss.videoplatform.dto.user.UserResponseDto;
import com.sebastianhauss.videoplatform.dto.user.UserUpdateDto;
import com.sebastianhauss.videoplatform.exception.DuplicateEmailException;
import com.sebastianhauss.videoplatform.exception.DuplicateUsernameException;
import com.sebastianhauss.videoplatform.exception.UserNotFoundException;
import com.sebastianhauss.videoplatform.mapper.UserMapper;
import com.sebastianhauss.videoplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserResponseDto getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return userMapper.toResponseDto(user);
    }

    public UserResponseDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(UserNotFoundException::new);
        return userMapper.toResponseDto(user);
    }

    public UserResponseDto getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(UserNotFoundException::new);
        return userMapper.toResponseDto(user);
    }

    public Page<UserResponseDto> getAllUsers(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);
        return users.map(userMapper::toResponseDto);
    }

    public Page<UserResponseDto> searchUsers(String username, Pageable pageable) {
        Page<User> users = userRepository.findByUsernameContainingIgnoreCase(username, pageable);
        return users.map(userMapper::toResponseDto);
    }

    @Transactional
    public UserResponseDto createUser(UserCreateDto dto) {
        if (userRepository.existsByUsername(dto.username())) {
            throw new DuplicateUsernameException();
        }

        if (userRepository.existsByEmail(dto.email())) {
            throw new DuplicateEmailException(dto.email());
        }

        User user = userMapper.toEntity(dto);
        user.setPasswordHash(passwordEncoder.encode(dto.password()));

        User saved = userRepository.save(user);
        return userMapper.toResponseDto(saved);
    }

    @Transactional
    public UserResponseDto updateUser(UUID userId, UserUpdateDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 2. Check if email is being changed and is duplicate
        if (dto.email() != null && !dto.email().equals(user.getEmail())) {
            if (userRepository.existsByEmail(dto.email())) {
                throw new DuplicateEmailException(dto.email());
            }
        }

        userMapper.updateEntity(dto, user);
        User updated = userRepository.save(user);
        return userMapper.toResponseDto(updated);
    }

    @Transactional
    public void deleteUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        userRepository.delete(user);
    }

    public boolean validatePassword(String rawPassword, String hashedPassword) {
        return passwordEncoder.matches(rawPassword, hashedPassword);
    }
}
