package com.example.MusicBoxd.Controller;

import com.example.MusicBoxd.Model.Notification;
import com.example.MusicBoxd.Model.User;
import com.example.MusicBoxd.Repository.NotificationRepository;
import com.example.MusicBoxd.Repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationController(
            NotificationRepository notificationRepository,
            UserRepository userRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/notifications")
    public String notifications(
            Authentication authentication,
            Model model
    ) {

        User currentUser =
                getCurrentUser(authentication);

        if (currentUser == null) {
            return "redirect:/";
        }

        List<Notification> notifications =
                notificationRepository
                        .findByUserIdOrderByCreatedAtDesc(
                                currentUser.getId()
                        );

        long unreadNotificationCount =
                notificationRepository
                        .countByUserIdAndReadFalse(
                                currentUser.getId()
                        );

        model.addAttribute(
                "notifications",
                notifications
        );

        model.addAttribute(
                "unreadNotificationCount",
                unreadNotificationCount
        );

        model.addAttribute(
                "currentUser",
                currentUser
        );

        return "notifications";
    }

    @GetMapping("/notifications/{id}/open")
    public String openNotification(
            @PathVariable Long id,
            Authentication authentication
    ) {

        User currentUser =
                getCurrentUser(authentication);

        if (currentUser == null) {
            return "redirect:/";
        }

        Notification notification =
                notificationRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Notification not found"
                                )
                        );

        if (
                !currentUser.getId().equals(
                        notification.getUserId()
                )
        ) {
            throw new RuntimeException(
                    "You cannot open another user's notification"
            );
        }

        notification.setRead(true);

        notificationRepository.save(
                notification
        );

        String type =
                notification.getType();

        if ("FRIEND_REQUEST".equals(type)
                || "FRIEND_ACCEPTED".equals(type)) {

            return "redirect:/profile/"
                    + notification.getActorId();
        }

        if ("ALBUM_REVIEWED".equals(type)) {

            return "redirect:/";
        }

        return "redirect:/notifications";
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