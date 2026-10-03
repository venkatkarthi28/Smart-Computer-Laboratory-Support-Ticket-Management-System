package com.example.labsupport.service;

import com.example.labsupport.dto.request.ResetPasswordRequest;
import com.example.labsupport.dto.request.UserCreateRequest;
import com.example.labsupport.dto.request.UserUpdateRequest;
import com.example.labsupport.dto.response.PageResponse;
import com.example.labsupport.dto.response.UserResponse;
import com.example.labsupport.entity.Laboratory;
import com.example.labsupport.entity.Role;
import com.example.labsupport.entity.User;
import com.example.labsupport.exception.ConflictException;
import com.example.labsupport.exception.DuplicateResourceException;
import com.example.labsupport.exception.ResourceNotFoundException;
import com.example.labsupport.mapper.UserMapper;
import com.example.labsupport.repository.LaboratoryRepository;
import com.example.labsupport.repository.TicketRepository;
import com.example.labsupport.repository.UserRepository;
import com.example.labsupport.util.Pages;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Admin management of students and technicians. The role always comes from the URL, never from the body. */
@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final LaboratoryRepository laboratoryRepository;
    private final TicketRepository ticketRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, LaboratoryRepository laboratoryRepository,
                       TicketRepository ticketRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.laboratoryRepository = laboratoryRepository;
        this.ticketRepository = ticketRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public PageResponse<UserResponse> list(Role role, String keyword, int page, int size) {
        String k = keyword == null ? "" : keyword.trim();
        return PageResponse.from(
                userRepository.searchByRole(role, k, Pages.of(page, size, org.springframework.data.domain.Sort.by("fullName"))).map(UserMapper::toResponse));
    }

    public UserResponse get(Role role, Long id) {
        return UserMapper.toResponse(find(role, id));
    }

    @Transactional
    public UserResponse create(Role role, UserCreateRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Email is already registered");
        }
        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(role);
        user.setActive(true);
        return UserMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(Role role, Long id, UserUpdateRequest request) {
        User user = find(role, id);
        String email = request.email().trim().toLowerCase();
        boolean emailTaken = userRepository.findByEmailIgnoreCase(email)
                .filter(other -> !other.getId().equals(id)).isPresent();
        if (emailTaken) {
            throw new DuplicateResourceException("Email is already registered");
        }
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setActive(request.active());
        return UserMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public void resetPassword(Role role, Long id, ResetPasswordRequest request) {
        User user = find(role, id);
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    /** Deleting is blocked when the user has tickets: deactivate instead. */
    @Transactional
    public void delete(Role role, Long id) {
        User user = find(role, id);
        if (role == Role.STUDENT && ticketRepository.existsByStudentId(id)) {
            throw new ConflictException("This student has tickets and cannot be deleted. Deactivate the account instead");
        }
        if (role == Role.TECHNICIAN) {
            if (ticketRepository.existsByAssignedTechnicianId(id)) {
                throw new ConflictException("This technician has tickets and cannot be deleted. Deactivate the account instead");
            }
            for (Laboratory lab : laboratoryRepository.findAllByTechnicianId(id)) {
                lab.removeTechnician(user);
            }
        }
        userRepository.delete(user);
        userRepository.flush();
    }

    private User find(Role role, Long id) {
        return userRepository.findById(id)
                .filter(u -> u.getRole() == role)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
