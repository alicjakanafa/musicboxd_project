package com.example.MusicBoxd.Controller;

import com.example.MusicBoxd.Model.Artist;
import com.example.MusicBoxd.Repository.ArtistRepository;
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

    public ArtistController(
            ArtistRepository artistRepository,
            LastFmService lastFmService,
            TicketmasterService ticketmasterService
    ) {
        this.artistRepository = artistRepository;
        this.lastFmService = lastFmService;
        this.ticketmasterService = ticketmasterService;
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

        /*
         * Get artist information from Last.fm
         */
        LastFmArtistResponse artistResponse =
                lastFmService.getArtistInfo(
                        artistName
                );

        /*
         * Get all of the artist's albums from Last.fm
         */
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

        /*
         * Get upcoming concerts from Ticketmaster.
         */
        List<Concert> concerts = new ArrayList<>();

        String attractionId =
                ticketmasterService.getAttractionId(artistName);

        if (attractionId != null) {
            concerts =
                    ticketmasterService.getShowsByAttractionId(
                            attractionId
                    );
        }

        model.addAttribute("artist", currentArtist);

        model.addAttribute(
                "artistInfo",
                artistResponse != null
                        ? artistResponse.getArtist()
                        : null
        );

        model.addAttribute("albums", albums);
        model.addAttribute("concerts", concerts);

        return "artist-profile";
    }


    /*
     * Find an artist by their name and
     * send them to their MusicBoxd profile.
     *
     * Used by the Top 40 page.
     */
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

        return "redirect:/artists/" + artist.getId();
    }
}