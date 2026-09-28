package com.traffic.echallan.service;

import com.traffic.echallan.dto.Dtos.*;
import com.traffic.echallan.entity.User;
import com.traffic.echallan.exception.ConflictException;
import com.traffic.echallan.exception.NotFoundException;
import com.traffic.echallan.exception.UnauthorizedException;
import com.traffic.echallan.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public LoginResponse login(LoginRequest r) {
        User u = users.findByUsername(r.username().trim())
                .filter(x -> x.isActive() && encoder.matches(r.password(), x.getPasswordHash()))
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));
        return new LoginResponse(u.getId(), u.getUsername(), u.getFullName(), u.getRole());
    }

    @Transactional
    public UserResponse create(UserRequest r) {
        String name = r.username().trim();
        if (users.existsByUsername(name)) throw new ConflictException("Username already exists: " + name);
        User u = new User();
        u.setUsername(name);
        u.setPasswordHash(encoder.encode(r.password()));
        u.setFullName(r.fullName().trim());
        u.setBadgeNumber(r.badgeNumber());
        u.setRole(r.role());
        return toResponse(users.save(u));
    }

    @Transactional
    public UserResponse setActive(Long id, boolean active) {
        User u = users.findById(id).orElseThrow(() -> new NotFoundException("User not found: " + id));
        u.setActive(active);
        return toResponse(u);
    }

    public List<UserResponse> list() { return users.findAll().stream().map(UserService::toResponse).toList(); }

    static UserResponse toResponse(User u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getFullName(), u.getBadgeNumber(), u.getRole(), u.isActive());
    }
}
