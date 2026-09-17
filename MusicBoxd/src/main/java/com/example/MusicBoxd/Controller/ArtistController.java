package com.example.MusicBoxd.Controller;

import com.example.MusicBoxd.Model.Artist;
import com.example.MusicBoxd.Repository.ArtistRepository;
import com.example.MusicBoxd.api.itunes.ItunesService;
import com.example.MusicBoxd.api.itunes.ItunesTrackResponse;
import com.example.MusicBoxd.api.lastfm.LastFmArtistResponse;
import com.example.MusicBoxd.api.lastfm.LastFmService;
import com.example.MusicBoxd.api.lastfm.LastFmTopAlbum;
import com.example.MusicBoxd.api.lastfm.LastFmTopAlbumsResponse;
import com.example.MusicBoxd.api.ticketmaster.Concert;
import com.example.MusicBoxd.api.ticketmaster.TicketmasterService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/artists")
public class ArtistController {

    private final ArtistRepository artistRepository;
    private final LastFmService lastFmService;
    private final TicketmasterService ticketmasterService;
    private final ItunesService itunesService;

    public ArtistController(
            ArtistRepository artistRepository,
            LastFmService lastFmService,
            TicketmasterService ticketmasterService,
            ItunesService itunesService
    ) {
        this.artistRepository = artistRepository;
        this.lastFmService = lastFmService;
        this.ticketmasterService = ticketmasterService;
        this.itunesService = itunesService;
    }

    @GetMapping("/{id}")
    public String showArtist(
            @PathVariable Long id,
            Model model
    ) {

        Optional<Artist> artist =
                artistRepository.findById(id);

        if (artist.isEmpty()) {
            return "redirect:/";
        }

        Artist currentArtist = artist.get();

        String artistName =
                currentArtist.getName();

        // Get artist information from Last.fm
        LastFmArtistResponse artistResponse =
                lastFmService.getArtistInfo(
                        artistName
                );

        // Debug listener information
        System.out.println(
                "ARTIST NAME: " + artistName
        );

        if (artistResponse != null &&
                artistResponse.getArtist() != null &&
                artistResponse.getArtist().getStats() != null) {

            System.out.println(
                    "LISTENERS: " +
                            artistResponse
                                    .getArtist()
                                    .getStats()
                                    .getListeners()
            );

        } else {

            System.out.println(
                    "LAST.FM ARTIST RESPONSE IS NULL"
            );
        }

        // Get artist albums from Last.fm
        LastFmTopAlbumsResponse albumResponse =
                lastFmService.getArtistAlbums(
                        artistName
                );

        List<LastFmTopAlbum> albums =
                new ArrayList<>();

        if (albumResponse != null &&
                albumResponse.getTopalbums() != null &&
                albumResponse.getTopalbums().getAlbum() != null) {

            albums =
                    albumResponse
                            .getTopalbums()
                            .getAlbum();
        }

        // Get popular songs from iTunes
        ItunesTrackResponse trackResponse =
                itunesService.searchTracks(
                        artistName
                );

        model.addAttribute(
                "popularSingles",
                trackResponse != null &&
                        trackResponse.getResults() != null
                        ? trackResponse
                        .getResults()
                        .stream()
                        .filter(track ->
                                track.getPreviewUrl() != null
                        )
                        .limit(5)
                        .toList()
                        : List.of()
        );

        // Get upcoming concerts from Ticketmaster
        List<Concert> concerts =
                new ArrayList<>();

        String attractionId =
                ticketmasterService.getAttractionId(
                        artistName
                );

        if (attractionId != null) {

            concerts =
                    ticketmasterService
                            .getShowsByAttractionId(
                                    attractionId
                            );
        }

        // Add artist to model
        model.addAttribute(
                "artist",
                currentArtist
        );

        // Add Last.fm artist information
        model.addAttribute(
                "artistInfo",
                artistResponse != null
                        ? artistResponse.getArtist()
                        : null
        );

        // Add listener count separately
        model.addAttribute(
                "listeners",
                artistResponse != null &&
                        artistResponse.getArtist() != null &&
                        artistResponse.getArtist().getStats() != null
                        ? artistResponse
                        .getArtist()
                        .getStats()
                        .getListeners()
                        : null
        );

        // Add albums and concerts
        model.addAttribute(
                "albums",
                albums
        );

        model.addAttribute(
                "concerts",
                concerts
        );

        return "artist-profile";
    }

    @GetMapping("/from-name")
    public String showArtistByName(
            @RequestParam String name
    ) {

        Artist artist =
                artistRepository
                        .findByNameIgnoreCase(name)
                        .orElseGet(() ->
                                artistRepository.save(
                                        new Artist(name)
                                )
                        );

        return "redirect:/artists/" +
                artist.getId();
    }

    @GetMapping("/{id}/albums")
    public String showAllAlbums(
            @PathVariable Long id,
            Model model
    ) {

        Optional<Artist> artist =
                artistRepository.findById(id);

        if (artist.isEmpty()) {
            return "redirect:/";
        }

        Artist currentArtist =
                artist.get();

        List<LastFmTopAlbum> albums =
                new ArrayList<>();

        LastFmTopAlbumsResponse response =
                lastFmService.getArtistAlbums(
                        currentArtist.getName()
                );

        if (response != null &&
                response.getTopalbums() != null &&
                response.getTopalbums().getAlbum() != null) {

            albums =
                    response
                            .getTopalbums()
                            .getAlbum();
        }

        model.addAttribute(
                "artist",
                currentArtist
        );

        model.addAttribute(
                "albums",
                albums
        );

        return "artist-albums";
    }
}
