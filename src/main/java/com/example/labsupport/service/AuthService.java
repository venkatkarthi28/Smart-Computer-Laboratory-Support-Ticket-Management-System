package com.example.labsupport.service;

import com.example.labsupport.dto.request.LoginRequest;
import com.example.labsupport.dto.request.RegisterRequest;
import com.example.labsupport.dto.response.AuthResponse;
import com.example.labsupport.dto.response.UserResponse;
import com.example.labsupport.entity.Role;
import com.example.labsupport.entity.User;
import com.example.labsupport.exception.DuplicateResourceException;
import com.example.labsupport.exception.UnauthorizedException;
import com.example.labsupport.mapper.UserMapper;
import com.example.labsupport.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ActorService actorService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, ActorService actorService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.actorService = actorService;
    }

    /** Public registration: the role is ALWAYS STUDENT. */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Email is already registered");
        }
        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.STUDENT);
        user.setActive(true);
        userRepository.save(user);
        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }
        if (!user.isActive()) {
            throw new UnauthorizedException("Account is deactivated. Contact the administrator");
        }
        return buildAuthResponse(user);
    }

    public UserResponse me(Long userId) {
        return UserMapper.toResponse(actorService.requireActive(userId));
    }

    private AuthResponse buildAuthResponse(User user) {
        return new AuthResponse(jwtService.createToken(user), "Bearer",
                jwtService.getExpiresInSeconds(), UserMapper.toResponse(user));
    }
}
