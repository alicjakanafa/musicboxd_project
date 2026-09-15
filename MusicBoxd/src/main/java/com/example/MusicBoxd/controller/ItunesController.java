package com.example.MusicBoxd.controller;

import com.example.MusicBoxd.api.itunes.ItunesAlbumResponse;
import com.example.MusicBoxd.api.itunes.ItunesService;
import com.example.MusicBoxd.api.itunes.ItunesTrackResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ItunesController {

    private final ItunesService itunesService;

    public ItunesController(ItunesService itunesService) {
        this.itunesService = itunesService;
    }

    @GetMapping("/search")
    public String searchAlbums(
            @RequestParam String query,
            Model model
    ) {

        ItunesAlbumResponse response =
                itunesService.searchAlbums(query);

        model.addAttribute(
                "albums",
                response.getResults()
        );

        model.addAttribute(
                "query",
                query
        );

        return "album-search";
    }

    @GetMapping("/search/tracks")
    public String searchTracks(
            @RequestParam String query,
            Model model
    ) {

        ItunesTrackResponse response =
                itunesService.searchTracks(query);

        model.addAttribute(
                "tracks",
                response.getResults()
        );

        model.addAttribute(
                "query",
                query
        );

        return "track-search";
    }
}
