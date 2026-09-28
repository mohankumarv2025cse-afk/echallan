package com.traffic.echallan.controller;

import com.traffic.echallan.dto.Dtos.*;
import com.traffic.echallan.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Admin only (enforced in SecurityConfig). */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping public List<UserResponse> list() { return userService.list(); }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody UserRequest r) { return userService.create(r); }

    @PatchMapping("/{id}/active")
    public UserResponse setActive(@PathVariable Long id, @RequestParam boolean value) { return userService.setActive(id, value); }
}
