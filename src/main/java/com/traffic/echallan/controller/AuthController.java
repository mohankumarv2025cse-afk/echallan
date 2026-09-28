package com.traffic.echallan.controller;

import com.traffic.echallan.dto.Dtos.*;
import com.traffic.echallan.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;

    /** Verifies credentials and returns the user profile. Later API calls use HTTP Basic (Authorization header). */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request);
    }
}
