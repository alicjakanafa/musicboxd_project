package com.example.MusicBoxd.Controller;

import com.example.MusicBoxd.api.itunes.ItunesAlbum;
import com.example.MusicBoxd.api.itunes.ItunesService;
import com.example.MusicBoxd.api.lastfm.LastFmResponse;
import com.example.MusicBoxd.api.lastfm.LastFmService;
import com.example.MusicBoxd.Model.User;
import com.example.MusicBoxd.Repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final LastFmService lastFmService;
    private final SpotifyController spotifyController;
    private final ItunesService itunesService;
    private final UserRepository userRepository;


    public HomeController(
            LastFmService lastFmService,
            SpotifyController spotifyController,
            ItunesService itunesService,
            UserRepository userRepository
    ) {
        this.lastFmService = lastFmService;
        this.spotifyController = spotifyController;
        this.itunesService = itunesService;
        this.userRepository = userRepository;
    }


    // =========================================================
    // HOME PAGE
    // =========================================================

    @GetMapping("/")
    public String index(
            Model model,
            HttpSession session
    ) {

        LastFmResponse response =
                lastFmService.getTopArtists();

        model.addAttribute(
                "topArtists",
                response.getArtists()
                        .getArtist()
                        .subList(0, 10)
        );


        ItunesAlbum suggestedAlbum =
                itunesService.getDailyAlbum();

        model.addAttribute(
                "suggestedAlbum",
                suggestedAlbum
        );


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


    // =========================================================
    // CURRENT USER PROFILE
    // =========================================================

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


        /*
         * Redirect to the user's actual profile.
         *
         * ProfileController then loads:
         *
         * - User information
         * - Reviews
         * - Review artwork
         * - Top 4 albums
         * - Album artwork
         */

        return "redirect:/profile/" + user.getId();
    }
}
