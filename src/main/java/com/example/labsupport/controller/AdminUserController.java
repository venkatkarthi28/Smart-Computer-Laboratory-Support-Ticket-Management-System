package com.example.labsupport.controller;

import com.example.labsupport.dto.request.ResetPasswordRequest;
import com.example.labsupport.dto.request.UserCreateRequest;
import com.example.labsupport.dto.request.UserUpdateRequest;
import com.example.labsupport.dto.response.PageResponse;
import com.example.labsupport.dto.response.UserResponse;
import com.example.labsupport.entity.Role;
import com.example.labsupport.security.CurrentUser;
import com.example.labsupport.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Admin manages students and technicians. The role is decided by the URL, never by the request body. */
@RestController
@RequestMapping("/api/admin")
public class AdminUserController {

    private final UserService userService;
    private final CurrentUser currentUser;

    public AdminUserController(UserService userService, CurrentUser currentUser) {
        this.userService = userService;
        this.currentUser = currentUser;
    }

    // =============== students ===============
    @GetMapping("/students")
    public PageResponse<UserResponse> students(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        currentUser.require(Role.ADMIN);
        return userService.list(Role.STUDENT, search, page, size);
    }

    @GetMapping("/students/{id}")
    public UserResponse student(@PathVariable("id") Long id) {
        currentUser.require(Role.ADMIN);
        return userService.get(Role.STUDENT, id);
    }

    @PostMapping("/students")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createStudent(@Valid @RequestBody UserCreateRequest request) {
        currentUser.require(Role.ADMIN);
        return userService.create(Role.STUDENT, request);
    }

    @PutMapping("/students/{id}")
    public UserResponse updateStudent(@PathVariable("id") Long id, @Valid @RequestBody UserUpdateRequest request) {
        currentUser.require(Role.ADMIN);
        return userService.update(Role.STUDENT, id, request);
    }

    @PutMapping("/students/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetStudentPassword(@PathVariable("id") Long id, @Valid @RequestBody ResetPasswordRequest request) {
        currentUser.require(Role.ADMIN);
        userService.resetPassword(Role.STUDENT, id, request);
    }

    @DeleteMapping("/students/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStudent(@PathVariable("id") Long id) {
        currentUser.require(Role.ADMIN);
        userService.delete(Role.STUDENT, id);
    }

    // =============== technicians ===============
    @GetMapping("/technicians")
    public PageResponse<UserResponse> technicians(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        currentUser.require(Role.ADMIN);
        return userService.list(Role.TECHNICIAN, search, page, size);
    }

    @GetMapping("/technicians/{id}")
    public UserResponse technician(@PathVariable("id") Long id) {
        currentUser.require(Role.ADMIN);
        return userService.get(Role.TECHNICIAN, id);
    }

    @PostMapping("/technicians")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createTechnician(@Valid @RequestBody UserCreateRequest request) {
        currentUser.require(Role.ADMIN);
        return userService.create(Role.TECHNICIAN, request);
    }

    @PutMapping("/technicians/{id}")
    public UserResponse updateTechnician(@PathVariable("id") Long id, @Valid @RequestBody UserUpdateRequest request) {
        currentUser.require(Role.ADMIN);
        return userService.update(Role.TECHNICIAN, id, request);
    }

    @PutMapping("/technicians/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetTechnicianPassword(@PathVariable("id") Long id, @Valid @RequestBody ResetPasswordRequest request) {
        currentUser.require(Role.ADMIN);
        userService.resetPassword(Role.TECHNICIAN, id, request);
    }

    @DeleteMapping("/technicians/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTechnician(@PathVariable("id") Long id) {
        currentUser.require(Role.ADMIN);
        userService.delete(Role.TECHNICIAN, id);
    }
}
