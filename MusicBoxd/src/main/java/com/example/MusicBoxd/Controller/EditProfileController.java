package com.example.MusicBoxd.Controller;

import com.example.MusicBoxd.Model.User;
import com.example.MusicBoxd.Repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Controller
@RequestMapping("/profile/edit")
public class EditProfileController {

    private final UserRepository userRepository;

    private final Path uploadDirectory =
            Paths.get("uploads/profile-pictures");

    public EditProfileController(
            UserRepository userRepository
    ) {
        this.userRepository =
                userRepository;
    }

    @GetMapping
    public String editProfile(
            Authentication authentication,
            Model model
    ) {

        User user =
                getCurrentUser(authentication);

        if (user == null) {
            return "redirect:/";
        }

        model.addAttribute(
                "user",
                user
        );

        return "edit-profile";
    }

    @PostMapping
    public String updateProfile(
            Authentication authentication,
            @RequestParam("username") String username,
            @RequestParam("bio") String bio,
            @RequestParam(
                    value = "profilePicture",
                    required = false
            )
            MultipartFile profilePicture
    ) {

        User user =
                getCurrentUser(authentication);

        if (user == null) {
            return "redirect:/";
        }

        /*
         * Update username
         */
        if (username != null) {

            username =
                    username.trim();

            if (!username.isEmpty()) {

                user.setUsername(
                        username
                );
            }
        }

        /*
         * Update bio
         */
        if (bio != null) {

            user.setBio(
                    bio.trim()
            );
        }

        /*
         * Update profile picture
         */
        if (
                profilePicture != null
                        && !profilePicture.isEmpty()
        ) {

            try {

                Files.createDirectories(
                        uploadDirectory
                );

                String originalFilename =
                        profilePicture
                                .getOriginalFilename();

                String extension = "";

                if (
                        originalFilename != null
                                && originalFilename.contains(".")
                ) {

                    extension =
                            originalFilename.substring(
                                    originalFilename
                                            .lastIndexOf(".")
                            );
                }

                String filename =
                        UUID.randomUUID()
                                + extension;

                Path filePath =
                        uploadDirectory.resolve(
                                filename
                        );

                Files.copy(
                        profilePicture.getInputStream(),
                        filePath,
                        StandardCopyOption.REPLACE_EXISTING
                );

                user.setProfilePictureUrl(
                        "/uploads/profile-pictures/"
                                + filename
                );

            } catch (IOException e) {

                System.out.println(
                        "PROFILE PICTURE UPLOAD ERROR: "
                                + e.getMessage()
                );
            }
        }

        userRepository.save(user);

        return "redirect:/profile/"
                + user.getId();
    }

    private User getCurrentUser(
            Authentication authentication
    ) {

        if (
                authentication == null
                        || !authentication.isAuthenticated()
        ) {

            return null;
        }

        Object principal =
                authentication.getPrincipal();

        if (!(principal instanceof OidcUser oidcUser)) {

            return null;
        }

        String oktaUserId =
                oidcUser.getSubject();

        return userRepository
                .findByOktaUserId(oktaUserId)
                .orElse(null);
    }
}