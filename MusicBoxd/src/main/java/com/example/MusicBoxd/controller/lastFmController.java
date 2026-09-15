package com.example.MusicBoxd.controller;

import com.example.MusicBoxd.api.lastfm.LastFmResponse;
import com.example.MusicBoxd.api.lastfm.LastFmService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class lastFmController {

    private final LastFmService lastFmService;

    public lastFmController(LastFmService lastFmService) {
        this.lastFmService = lastFmService;
    }

    @GetMapping("/top-40")
    public String top40(Model model) {

        LastFmResponse response = lastFmService.getTopArtists();

        model.addAttribute(
                "artists",
                response.getArtists().getArtist()
        );

        return "top-40";
    }
}