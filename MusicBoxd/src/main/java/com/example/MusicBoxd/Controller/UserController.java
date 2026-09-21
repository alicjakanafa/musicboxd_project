package com.example.MusicBoxd.Controller;

import com.example.MusicBoxd.Model.List;
import com.example.MusicBoxd.Model.ListType;
import com.example.MusicBoxd.Model.User;
import com.example.MusicBoxd.Repository.ListRepository;
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

    @Autowired
    ListRepository listRepository;

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
                    User savedUser = userRepository.save(user);
                    createDefaultLists(savedUser);
                    return savedUser;
                });

        return new RedirectView("/");
    }

    private void createDefaultLists(User user) {

        List wantToListen = new List();
        wantToListen.setUserId(user.getId());
        wantToListen.setTitle("Want to Listen");
        wantToListen.setDescription("Albums and songs you want to listen to.");
        wantToListen.setListType(ListType.WANT_TO_LISTEN);
        wantToListen.setCreatedAt(LocalDateTime.now());

        listRepository.save(wantToListen);


        List favorites = new List();
        favorites.setUserId(user.getId());
        favorites.setTitle("Favorites");
        favorites.setDescription("Your favorite albums and songs.");
        favorites.setListType(ListType.FAVOURITES);
        favorites.setCreatedAt(LocalDateTime.now());

        listRepository.save(favorites);
    }
}
