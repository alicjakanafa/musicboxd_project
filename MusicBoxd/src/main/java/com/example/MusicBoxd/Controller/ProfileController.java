package com.example.MusicBoxd.Controller;

import com.example.MusicBoxd.Model.Album;
import com.example.MusicBoxd.Model.Artist;
import com.example.MusicBoxd.Model.Review;
import com.example.MusicBoxd.Model.User;
import com.example.MusicBoxd.Model.UserFavouriteAlbum;
import com.example.MusicBoxd.Repository.AlbumRepository;
import com.example.MusicBoxd.Repository.ArtistRepository;
import com.example.MusicBoxd.Repository.ReviewRepository;
import com.example.MusicBoxd.Repository.UserFavouriteAlbumRepository;
import com.example.MusicBoxd.Repository.UserRepository;
import com.example.MusicBoxd.api.lastfm.LastFmAlbum;
import com.example.MusicBoxd.api.lastfm.LastFmService;
import com.example.MusicBoxd.api.spotify.SpotifyTopArtistsResponse;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ProfileController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private AlbumRepository albumRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private UserFavouriteAlbumRepository favouriteAlbumRepository;

    @Autowired
    private LastFmService lastFmService;

    @Autowired
    private SpotifyController spotifyController;


    @GetMapping("/profile/{id}")
    public String profiles(
            @PathVariable Long id,
            Model model,
            HttpSession session,
            @RequestParam(
                    name = "artistRange",
                    defaultValue = "medium_term"
            )
            String artistRange
    ) {

        User user =
                userRepository.findById(id).orElse(null);

        if (user == null) {

            System.out.println(
                    "PROFILE ERROR: User not found: " + id
            );

            return "redirect:/";
        }


        System.out.println(
                "LOADING PROFILE FOR USER: " +
                        user.getUsername() +
                        " ID: " +
                        user.getId()
        );


        /*
         * =========================
         * REVIEWS
         * =========================
         */

        List<Review> reviews =
                reviewRepository
                        .findByUserIdOrderByCreatedAtDesc(id);

        System.out.println(
                "REVIEWS FOUND: " + reviews.size()
        );


        Map<Long, Album> albums =
                new HashMap<>();

        Map<Long, String> artwork =
                new HashMap<>();


        for (Review review : reviews) {

            System.out.println(
                    "REVIEW ID: " + review.getId() +
                            " USER ID: " + review.getUserId() +
                            " ALBUM ID: " + review.getAlbumId()
            );


            Long albumId =
                    review.getAlbumId();


            if (albumId == null) {

                System.out.println(
                        "REVIEW " +
                                review.getId() +
                                " HAS NO ALBUM ID"
                );

                continue;
            }


            Album album =
                    albumRepository
                            .findById(albumId)
                            .orElse(null);


            if (album == null) {

                System.out.println(
                        "ALBUM NOT FOUND: " +
                                albumId
                );

                continue;
            }


            albums.put(
                    albumId,
                    album
            );


            String artworkUrl =
                    getArtwork(album);


            if (
                    artworkUrl != null &&
                            !artworkUrl.isBlank()
            ) {

                artwork.put(
                        albumId,
                        artworkUrl
                );
            }
        }


        /*
         * =========================
         * TOP 4
         * =========================
         */

        List<UserFavouriteAlbum> favouriteRecords =
                favouriteAlbumRepository
                        .findByUserIdOrderByPositionAsc(id);


        List<Album> favouriteAlbums =
                new ArrayList<>();


        for (
                UserFavouriteAlbum favourite :
                favouriteRecords
        ) {

            albumRepository
                    .findById(
                            favourite.getAlbumId()
                    )
                    .ifPresent(album -> {

                        favouriteAlbums.add(
                                album
                        );


                        String artworkUrl =
                                getArtwork(album);


                        if (
                                artworkUrl != null &&
                                        !artworkUrl.isBlank()
                        ) {

                            artwork.put(
                                    album.getId(),
                                    artworkUrl
                            );
                        }

                    });
        }


        /*
         * =========================
         * BASIC PROFILE DATA
         * =========================
         */

        model.addAttribute(
                "user",
                user
        );

        model.addAttribute(
                "reviews",
                reviews
        );

        model.addAttribute(
                "albums",
                albums
        );

        model.addAttribute(
                "lastFmArtwork",
                artwork
        );

        model.addAttribute(
                "favouriteAlbums",
                favouriteAlbums
        );


        /*
         * =========================
         * SPOTIFY DEFAULT VALUES
         * =========================
         */

        String spotifyAccessToken =
                (String) session.getAttribute(
                        "spotifyAccessToken"
                );


        model.addAttribute(
                "spotifyConnected",
                false
        );

        model.addAttribute(
                "recentTracksCount",
                0
        );

        model.addAttribute(
                "uniqueTracksCount",
                0
        );

        model.addAttribute(
                "uniqueArtistsCount",
                0
        );

        model.addAttribute(
                "listeningMinutes",
                0
        );

        model.addAttribute(
                "topArtists",
                null
        );


        /*
         * =========================
         * DEFAULT ARTIST RANGE
         * =========================
         *
         * medium_term = Last 6 months
         */

        if (
                !artistRange.equals("short_term")
                        && !artistRange.equals("medium_term")
                        && !artistRange.equals("long_term")
        ) {

            artistRange = "medium_term";
        }


        model.addAttribute(
                "artistRange",
                artistRange
        );


        /*
         * =========================
         * LOAD SPOTIFY DATA
         * =========================
         */

        if (
                spotifyAccessToken != null &&
                        !spotifyAccessToken.isBlank()
        ) {

            try {

                System.out.println(
                        "SPOTIFY CONNECTED - LOADING PROFILE DATA"
                );


                /*
                 * Recently played statistics
                 */

                spotifyController.getSpotifyData(
                        spotifyAccessToken,
                        model
                );


                /*
                 * Top artists using selected
                 * time period.
                 */

                SpotifyTopArtistsResponse topArtistsResponse =
                        spotifyController.getTopArtists(
                                spotifyAccessToken,
                                artistRange
                        );


                if (
                        topArtistsResponse != null &&
                                topArtistsResponse.getItems() != null
                ) {

                    model.addAttribute(
                            "topArtists",
                            topArtistsResponse.getItems()
                    );
                }


                /*
                 * Tell Thymeleaf Spotify is connected.
                 */

                model.addAttribute(
                        "spotifyConnected",
                        true
                );


                System.out.println(
                        "SPOTIFY PROFILE DATA LOADED"
                );

                System.out.println(
                        "TOP ARTIST RANGE: " +
                                artistRange
                );

            } catch (Exception e) {

                System.out.println(
                        "SPOTIFY PROFILE ERROR: " +
                                e.getMessage()
                );

                e.printStackTrace();


                model.addAttribute(
                        "spotifyConnected",
                        false
                );

            }

        } else {

            System.out.println(
                    "SPOTIFY NOT CONNECTED"
            );
        }


        /*
         * =========================
         * DEBUG INFORMATION
         * =========================
         */

        System.out.println(
                "PROFILE REVIEWS SENT TO THYMELEAF: " +
                        reviews.size()
        );

        System.out.println(
                "PROFILE ARTWORK FOUND: " +
                        artwork.size()
        );

        System.out.println(
                "PROFILE TOP 4: " +
                        favouriteAlbums.size()
        );


        return "profile-page";
    }


    /*
     * =========================
     * GET ALBUM ARTWORK
     * =========================
     */

    private String getArtwork(
            Album album
    ) {

        /*
         * First use artwork already stored
         * in the database.
         */

        if (
                album.getArtworkUrl() != null &&
                        !album.getArtworkUrl().isBlank()
        ) {

            System.out.println(
                    "USING DATABASE ARTWORK FOR: " +
                            album.getTitle()
            );

            return album.getArtworkUrl();
        }


        /*
         * No artist ID means we cannot
         * search Last.fm.
         */

        if (
                album.getArtistId() == null
        ) {

            System.out.println(
                    "NO ARTIST ID FOR: " +
                            album.getTitle()
            );

            return null;
        }


        /*
         * Find artist.
         */

        Artist artist =
                artistRepository
                        .findById(
                                album.getArtistId()
                        )
                        .orElse(null);


        if (artist == null) {

            System.out.println(
                    "ARTIST NOT FOUND FOR: " +
                            album.getTitle()
            );

            return null;
        }


        /*
         * Ask Last.fm for artwork.
         */

        try {

            System.out.println(
                    "GETTING LAST.FM ARTWORK FOR: " +
                            artist.getName() +
                            " - " +
                            album.getTitle()
            );


            var response =
                    lastFmService.getAlbumInfo(
                            artist.getName(),
                            album.getTitle()
                    );


            if (
                    response == null ||
                            response.getAlbum() == null
            ) {

                System.out.println(
                        "LAST.FM RETURNED NO ALBUM FOR: " +
                                album.getTitle()
                );

                return null;
            }


            LastFmAlbum lastFmAlbum =
                    response.getAlbum();


            if (
                    lastFmAlbum.getImage() == null ||
                            lastFmAlbum.getImage().isEmpty()
            ) {

                System.out.println(
                        "LAST.FM RETURNED NO IMAGES FOR: " +
                                album.getTitle()
                );

                return null;
            }


            /*
             * Start from the largest image
             * and work backwards.
             */

            for (
                    int i =
                    lastFmAlbum
                            .getImage()
                            .size() - 1;

                    i >= 0;

                    i--
            ) {

                String imageUrl =
                        lastFmAlbum
                                .getImage()
                                .get(i)
                                .getText();


                if (
                        imageUrl != null &&
                                !imageUrl.isBlank()
                ) {

                    System.out.println(
                            "LAST.FM ARTWORK FOUND: " +
                                    imageUrl
                    );


                    album.setArtworkUrl(
                            imageUrl
                    );

                    albumRepository.save(
                            album
                    );


                    return imageUrl;
                }
            }

        } catch (Exception e) {

            System.out.println(
                    "LAST.FM ARTWORK ERROR FOR " +
                            album.getTitle() +
                            ": " +
                            e.getMessage()
            );
        }


        return null;
    }
}