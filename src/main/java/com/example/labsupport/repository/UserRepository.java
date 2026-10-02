package com.example.labsupport.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.labsupport.entity.Role;
import com.example.labsupport.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    // Used at login and when loading the current user from the JWT
    Optional<User> findByEmailIgnoreCase(String email);

    // Used at registration to reject duplicate emails
    boolean existsByEmailIgnoreCase(String email);

    // Admin: list all students or all technicians, page by page
    Page<User> findByRole(Role role, Pageable pageable);

    // Dropdowns, for example "choose a technician to assign"
    List<User> findByRoleAndActiveTrue(Role role);

    long countByRole(Role role);

    // Admin: search users of one role by name or email.
    // Pass an empty string "" as keyword to match everyone.
    @Query("""
            SELECT u FROM User u
            WHERE u.role = :role
              AND (LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<User> searchByRole(@Param("role") Role role,
                            @Param("keyword") String keyword,
                            Pageable pageable);
}