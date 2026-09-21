package com.example.MusicBoxd.Controller;

import com.example.MusicBoxd.Model.Notification;
import com.example.MusicBoxd.Model.User;
import com.example.MusicBoxd.Repository.NotificationRepository;
import com.example.MusicBoxd.Repository.UserRepository;
import com.example.MusicBoxd.api.itunes.ItunesAlbum;
import com.example.MusicBoxd.api.itunes.ItunesService;
import com.example.MusicBoxd.api.lastfm.LastFmResponse;
import com.example.MusicBoxd.api.lastfm.LastFmService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {

    private final LastFmService lastFmService;
    private final SpotifyController spotifyController;
    private final ItunesService itunesService;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    public HomeController(
            LastFmService lastFmService,
            SpotifyController spotifyController,
            ItunesService itunesService,
            UserRepository userRepository,
            NotificationRepository notificationRepository
    ) {
        this.lastFmService = lastFmService;
        this.spotifyController = spotifyController;
        this.itunesService = itunesService;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
    }

    @GetMapping("/")
    public String index(
            Model model,
            HttpSession session,
            Authentication authentication
    ) {

        LastFmResponse response =
                lastFmService.getTopArtists();

        model.addAttribute(
                "topArtists",
                response.getArtists()
                        .getArtist()
                        .subList(0, 10)
        );

        /*
         * Find the currently logged-in MusicBoxd user.
         */
        User currentUser = null;

        if (
                authentication != null &&
                        authentication.isAuthenticated()
        ) {

            OidcUser principal =
                    (OidcUser) authentication.getPrincipal();

            String oktaUserId =
                    principal.getSubject();

            currentUser =
                    userRepository
                            .findByOktaUserId(oktaUserId)
                            .orElse(null);
        }

        /*
         * Get a daily album based on the user's ID.
         */
        Long userId = null;

        if (currentUser != null) {
            userId = currentUser.getId();
        }

        ItunesAlbum suggestedAlbum =
                itunesService.getDailyAlbum(userId);

        model.addAttribute(
                "suggestedAlbum",
                suggestedAlbum
        );

        /*
         * Notifications
         */
        if (currentUser != null) {

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
        }

        /*
         * Spotify
         */
        String spotifyAccessToken =
                (String) session.getAttribute(
                        "spotifyAccessToken"
                );

        if (spotifyAccessToken != null) {

            spotifyController.getSpotifyData(
                    spotifyAccessToken,
                    model
            );
        }

        return "index";
    }

    @GetMapping("/profile")
    public String profile(
            Authentication authentication
    ) {

        OidcUser principal =
                (OidcUser) authentication.getPrincipal();

        String oktaUserId =
                principal.getSubject();

        User user =
                userRepository
                        .findByOktaUserId(oktaUserId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found for Okta ID: "
                                                + oktaUserId
                                )
                        );

        return "redirect:/profile/" + user.getId();
    }
}