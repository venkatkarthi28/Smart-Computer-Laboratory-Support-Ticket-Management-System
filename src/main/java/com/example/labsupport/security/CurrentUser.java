package com.example.labsupport.security;

import com.example.labsupport.entity.Role;
import com.example.labsupport.entity.User;
import com.example.labsupport.exception.ForbiddenException;
import com.example.labsupport.exception.UnauthorizedException;
import com.example.labsupport.service.ActorService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

/**
 * Tells controllers WHO is calling.
 *
 * TEMPORARY (Phase 7): the caller is identified by the request header  X-User-Id: <number>.
 * This is NOT secure and exists only so we can test the APIs in Postman before Phase 9.
 * In Phase 9 only the id() method changes: it will read the user id from the verified JWT.
 */
@Component
public class CurrentUser {

    private final ActorService actorService;

    public CurrentUser(ActorService actorService) {
        this.actorService = actorService;
    }

    public Long id() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            throw new UnauthorizedException("No request context");
        }
        String header = attrs.getRequest().getHeader("X-User-Id");
        if (header == null || header.isBlank()) {
            throw new UnauthorizedException("Missing X-User-Id header (temporary login for Phase 7 testing)");
        }
        try {
            return Long.valueOf(header.trim());
        } catch (NumberFormatException e) {
            throw new UnauthorizedException("X-User-Id must be a number");
        }
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
