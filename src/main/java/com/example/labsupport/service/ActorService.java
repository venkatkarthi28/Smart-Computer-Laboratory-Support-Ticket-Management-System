package com.example.labsupport.service;

import com.example.labsupport.entity.User;
import com.example.labsupport.exception.UnauthorizedException;
import com.example.labsupport.repository.UserRepository;
import org.springframework.stereotype.Service;

/** Loads the logged-in user from the database and makes sure the account is still active. */
@Service
public class ActorService {

    private final UserRepository userRepository;

    public ActorService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User requireActive(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User no longer exists"));
        if (!user.isActive()) {
            throw new UnauthorizedException("Account is deactivated");
        }
        return user;
    }
}
