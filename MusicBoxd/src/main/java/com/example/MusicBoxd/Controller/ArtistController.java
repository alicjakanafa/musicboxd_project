package com.example.MusicBoxd.Controller;

import com.example.MusicBoxd.Model.Artist;
import com.example.MusicBoxd.Model.User;
import com.example.MusicBoxd.Model.UserFavouriteArtist;
import com.example.MusicBoxd.Repository.ArtistRepository;
import com.example.MusicBoxd.Repository.UserFavouriteArtistRepository;
import com.example.MusicBoxd.Repository.UserRepository;
import com.example.MusicBoxd.api.itunes.ItunesService;
import com.example.MusicBoxd.api.itunes.ItunesTrackResponse;
import com.example.MusicBoxd.api.lastfm.LastFmArtistResponse;
import com.example.MusicBoxd.api.lastfm.LastFmService;
import com.example.MusicBoxd.api.lastfm.LastFmTopAlbum;
import com.example.MusicBoxd.api.lastfm.LastFmTopAlbumsResponse;
import com.example.MusicBoxd.api.ticketmaster.Concert;
import com.example.MusicBoxd.api.ticketmaster.TicketmasterService;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/artists")
public class ArtistController {

    private final ArtistRepository artistRepository;

    private final UserRepository userRepository;

    private final UserFavouriteArtistRepository
            favouriteArtistRepository;

    private final LastFmService lastFmService;

    private final TicketmasterService ticketmasterService;

    private final ItunesService itunesService;


    public ArtistController(
            ArtistRepository artistRepository,
            UserRepository userRepository,
            UserFavouriteArtistRepository favouriteArtistRepository,
            LastFmService lastFmService,
            TicketmasterService ticketmasterService,
            ItunesService itunesService
    ) {

        this.artistRepository = artistRepository;

        this.userRepository = userRepository;

        this.favouriteArtistRepository =
                favouriteArtistRepository;

        this.lastFmService = lastFmService;

        this.ticketmasterService =
                ticketmasterService;

        this.itunesService = itunesService;
    }


    @GetMapping("/{id}")
    public String showArtist(
            @PathVariable Long id,
            Model model,
            Authentication authentication
    ) {

        Optional<Artist> artist =
                artistRepository.findById(id);

        if (artist.isEmpty()) {
            return "redirect:/";
        }


        Artist currentArtist =
                artist.get();

        String artistName =
                currentArtist.getName();


        boolean isFavourite = false;

        if (
                authentication != null &&
                        authentication.getPrincipal()
                                instanceof OidcUser principal
        ) {

            String oktaUserId =
                    principal.getSubject();

            User user =
                    userRepository
                            .findByOktaUserId(
                                    oktaUserId
                            )
                            .orElse(null);

            if (user != null) {

                isFavourite =
                        favouriteArtistRepository
                                .existsByUserIdAndArtistId(
                                        user.getId(),
                                        currentArtist.getId()
                                );
            }
        }

        LastFmArtistResponse artistResponse =
                lastFmService.getArtistInfo(
                        artistName
                );


        System.out.println(
                "ARTIST NAME: " + artistName
        );


        if (
                artistResponse != null &&
                        artistResponse.getArtist() != null &&
                        artistResponse
                                .getArtist()
                                .getStats() != null
        ) {

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

        LastFmTopAlbumsResponse albumResponse =
                lastFmService.getArtistAlbums(
                        artistName
                );


        List<LastFmTopAlbum> albums =
                new ArrayList<>();


        if (
                albumResponse != null &&
                        albumResponse.getTopalbums() != null &&
                        albumResponse
                                .getTopalbums()
                                .getAlbum() != null
        ) {

            albums =
                    albumResponse
                            .getTopalbums()
                            .getAlbum();
        }


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

        model.addAttribute(
                "artist",
                currentArtist
        );


        model.addAttribute(
                "isFavourite",
                isFavourite
        );


        model.addAttribute(
                "artistInfo",
                artistResponse != null
                        ? artistResponse.getArtist()
                        : null
        );


        model.addAttribute(
                "listeners",
                artistResponse != null &&
                        artistResponse.getArtist() != null &&
                        artistResponse
                                .getArtist()
                                .getStats() != null
                        ? artistResponse
                        .getArtist()
                        .getStats()
                        .getListeners()
                        : null
        );

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

    @PostMapping("/{id}/favourite")
    public String favouriteArtist(
            @PathVariable Long id,
            Authentication authentication
    ) {

        OidcUser principal =
                (OidcUser)
                        authentication.getPrincipal();


        String oktaUserId =
                principal.getSubject();


        User user =
                userRepository
                        .findByOktaUserId(
                                oktaUserId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found"
                                )
                        );


        Artist artist =
                artistRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Artist not found"
                                )
                        );


        boolean alreadyFavourite =
                favouriteArtistRepository
                        .existsByUserIdAndArtistId(
                                user.getId(),
                                artist.getId()
                        );


        if (!alreadyFavourite) {

            UserFavouriteArtist favourite =
                    new UserFavouriteArtist(
                            user.getId(),
                            artist.getId()
                    );


            favouriteArtistRepository.save(
                    favourite
            );
        }


        return "redirect:/artists/" + id;
    }


    @PostMapping("/{id}/unfavourite")
    public String unfavouriteArtist(
            @PathVariable Long id,
            Authentication authentication
    ) {

        OidcUser principal =
                (OidcUser)
                        authentication.getPrincipal();


        String oktaUserId =
                principal.getSubject();


        User user =
                userRepository
                        .findByOktaUserId(
                                oktaUserId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found"
                                )
                        );


        favouriteArtistRepository
                .deleteByUserIdAndArtistId(
                        user.getId(),
                        id
                );


        return "redirect:/artists/" + id;
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


        if (
                response != null &&
                        response.getTopalbums() != null &&
                        response
                                .getTopalbums()
                                .getAlbum() != null
        ) {

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