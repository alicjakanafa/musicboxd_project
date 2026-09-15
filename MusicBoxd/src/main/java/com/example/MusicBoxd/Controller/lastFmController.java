package com.example.MusicBoxd.Controller;

import com.example.MusicBoxd.Model.Artist;
import com.example.MusicBoxd.Repository.ArtistRepository;
import com.example.MusicBoxd.api.lastfm.LastFmResponse;
import com.example.MusicBoxd.api.lastfm.LastFmService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class lastFmController {

    private final LastFmService lastFmService;
    private final ArtistRepository artistRepository;

    public lastFmController(
            LastFmService lastFmService,
            ArtistRepository artistRepository
    ) {
        this.lastFmService = lastFmService;
        this.artistRepository = artistRepository;
    }

    @GetMapping("/top-40")
    public String top40(Model model) {

        LastFmResponse response =
                lastFmService.getTopArtists();

        List<?> artists =
                response.getArtists().getArtist();

        model.addAttribute(
                "artists",
                artists
        );

        return "top-40";
    }
}