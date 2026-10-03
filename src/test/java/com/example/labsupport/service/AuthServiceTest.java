package com.example.labsupport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.transaction.annotation.Transactional;

import com.example.labsupport.dto.request.LoginRequest;
import com.example.labsupport.dto.request.RegisterRequest;
import com.example.labsupport.dto.response.AuthResponse;
import com.example.labsupport.entity.Role;
import com.example.labsupport.exception.DuplicateResourceException;
import com.example.labsupport.exception.UnauthorizedException;

@SpringBootTest
@Transactional
class AuthServiceTest {

    private final AuthService authService;
    private final JwtDecoder jwtDecoder;

    @Autowired
    AuthServiceTest(AuthService authService, JwtDecoder jwtDecoder) {
        this.authService = authService;
        this.jwtDecoder = jwtDecoder;
    }

    private String uniqueEmail() {
        return "auth-test-" + Long.toString(System.nanoTime(), 36) + "@example.com";
    }

    @Test
    void register_alwaysCreatesStudent_andTokenCarriesRole() {
        String email = uniqueEmail();
        AuthResponse r = authService.register(new RegisterRequest("Auth Test", email, "Passw0rdOk"));

        assertThat(r.user().role()).isEqualTo(Role.STUDENT);
        assertThat(r.tokenType()).isEqualTo("Bearer");

        Jwt jwt = jwtDecoder.decode(r.token());
        assertThat(jwt.getClaimAsString("role")).isEqualTo("STUDENT");
        assertThat(jwt.getSubject()).isEqualTo(String.valueOf(r.user().id()));
    }

    @Test
    void register_duplicateEmail_isRejected() {
        String email = uniqueEmail();
        authService.register(new RegisterRequest("Auth Test", email, "Passw0rdOk"));
        assertThatThrownBy(() -> authService.register(new RegisterRequest("Other", email.toUpperCase(), "Passw0rdOk")))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void login_worksWithRightPassword_failsWithWrongOne() {
        String email = uniqueEmail();
        authService.register(new RegisterRequest("Auth Test", email, "Passw0rdOk"));

        assertThat(authService.login(new LoginRequest(email, "Passw0rdOk")).token()).isNotBlank();
        assertThatThrownBy(() -> authService.login(new LoginRequest(email, "WrongPass1")))
                .isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "Passw0rdOk")))
                .isInstanceOf(UnauthorizedException.class);
    }
}
