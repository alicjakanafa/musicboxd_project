package com.example.MusicBoxd.Controller;

import com.example.MusicBoxd.Model.User;
import com.example.MusicBoxd.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.time.LocalDateTime;

@RestController
public class UserController {

    @Autowired
    UserRepository userRepository;

    @GetMapping("/users/after-login")
    public RedirectView afterLogin() {
        DefaultOidcUser principal = (DefaultOidcUser) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        String oktaUserId = principal.getSubject();
        String username = principal.getEmail();

        userRepository
                .findByOktaUserId(oktaUserId)
                .orElseGet(() -> {
                    User user = new User(
                            oktaUserId,
                            username,
                            principal.getEmail(),
                            "",
                            principal.getPicture()
                    );
                    user.setCreatedAt(LocalDateTime.now());
                    return userRepository.save(user);
                });

        return new RedirectView("/");
    }
}
