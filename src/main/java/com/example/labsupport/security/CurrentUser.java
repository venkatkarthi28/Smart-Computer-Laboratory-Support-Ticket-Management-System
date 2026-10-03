package com.example.labsupport.security;

import com.example.labsupport.entity.Role;
import com.example.labsupport.entity.User;
import com.example.labsupport.exception.ForbiddenException;
import com.example.labsupport.exception.UnauthorizedException;
import com.example.labsupport.service.ActorService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/** Tells controllers WHO is calling: the user id comes from the verified JWT (subject claim). */
@Component
public class CurrentUser {

    private final ActorService actorService;

    public CurrentUser(ActorService actorService) {
        this.actorService = actorService;
    }

    public Long id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            try {
                return Long.valueOf(jwtAuth.getToken().getSubject());
            } catch (NumberFormatException e) {
                throw new UnauthorizedException("Invalid token");
            }
        }
        throw new UnauthorizedException("Login required");
    }

    /** The logged-in user, who must be active and have one of the given roles (no roles = any role). */
    public User require(Role... allowedRoles) {
        User user = actorService.requireActive(id());
        if (allowedRoles.length > 0 && !Arrays.asList(allowedRoles).contains(user.getRole())) {
            throw new ForbiddenException("You do not have permission to use this feature");
        }
        return user;
    }
}
