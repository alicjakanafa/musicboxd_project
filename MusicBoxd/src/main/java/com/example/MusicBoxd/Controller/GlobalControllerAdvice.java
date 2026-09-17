package com.example.MusicBoxd.Controller;

import com.example.MusicBoxd.Model.User;
import com.example.MusicBoxd.Repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Optional;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final UserRepository userRepository;

    public GlobalControllerAdvice(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @ModelAttribute("currentUser")
    public User currentUser(Authentication authentication) {

        if (authentication == null ||
                !authentication.isAuthenticated()) {
            return null;
        }

        String oktaUserId = authentication.getName();

        Optional<User> user =
                userRepository.findByOktaUserId(oktaUserId);

        return user.orElse(null);
    }
}