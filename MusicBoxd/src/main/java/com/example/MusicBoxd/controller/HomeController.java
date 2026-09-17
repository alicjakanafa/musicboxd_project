package com.example.MusicBoxd.Controller;

import com.example.MusicBoxd.api.itunes.ItunesAlbum;
import com.example.MusicBoxd.api.itunes.ItunesService;
import com.example.MusicBoxd.api.lastfm.LastFmResponse;
import com.example.MusicBoxd.api.lastfm.LastFmService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final LastFmService lastFmService;
    private final SpotifyController spotifyController;
    private final ItunesService itunesService;

    public HomeController(
            LastFmService lastFmService,
            SpotifyController spotifyController,
            ItunesService itunesService
    ) {
        this.lastFmService = lastFmService;
        this.spotifyController = spotifyController;
        this.itunesService = itunesService;
    }

    @GetMapping("/")
    public String index(
            Model model,
            HttpSession session
    ) {

        // Global Top 40
        LastFmResponse response =
                lastFmService.getTopArtists();

        model.addAttribute(
                "topArtists",
                response.getArtists()
                        .getArtist()
                        .subList(0, 10)
        );

        // Suggested Album of the Day
        ItunesAlbum suggestedAlbum =
                itunesService.getDailyAlbum();

        model.addAttribute(
                "suggestedAlbum",
                suggestedAlbum
        );

        // Spotify
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
}
