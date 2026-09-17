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
import com.example.MusicBoxd.api.itunes.ItunesService;
import com.example.MusicBoxd.api.itunes.ItunesTrackResponse;
import com.example.MusicBoxd.api.lastfm.LastFmService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/albums")
public class AlbumController {

    private final AlbumRepository albumRepository;
    private final ArtistRepository artistRepository;
    private final UserFavouriteAlbumRepository favouriteAlbumRepository;
    private final UserRepository userRepository;
    private final LastFmService lastFmService;
    private final ItunesService itunesService;
    private final ReviewRepository reviewRepository;

    public AlbumController(
            AlbumRepository albumRepository,
            ArtistRepository artistRepository,
            UserFavouriteAlbumRepository favouriteAlbumRepository,
            UserRepository userRepository,
            ReviewRepository reviewRepository,
            LastFmService lastFmService,
            ItunesService itunesService
    ) {
        this.albumRepository = albumRepository;
        this.artistRepository = artistRepository;
        this.favouriteAlbumRepository = favouriteAlbumRepository;
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
        this.lastFmService = lastFmService;
        this.itunesService = itunesService;
    }


    // =========================
    // ALBUM PROFILE
    // =========================

    @GetMapping("/{id}")
    public String showAlbum(
            @PathVariable Long id,
            Model model
    ) {

        Optional<Album> album =
                albumRepository.findById(id);

        if (album.isEmpty()) {
            return "redirect:/";
        }

        Album currentAlbum =
                album.get();


        // =========================
        // FIND ARTIST
        // =========================

        Optional<Artist> artist =
                artistRepository.findById(
                        currentAlbum.getArtistId()
                );

        if (artist.isEmpty()) {
            return "redirect:/";
        }

        Artist currentArtist =
                artist.get();


        // =========================
        // LAST.FM ALBUM INFORMATION
        // =========================

        var lastFmAlbumResponse =
                lastFmService.getAlbumInfo(
                        currentArtist.getName(),
                        currentAlbum.getTitle()
                );


        model.addAttribute(
                "album",
                currentAlbum
        );

        model.addAttribute(
                "artist",
                currentArtist
        );


        if (lastFmAlbumResponse != null) {

            model.addAttribute(
                    "lastFmAlbum",
                    lastFmAlbumResponse.getAlbum()
            );

        } else {

            model.addAttribute(
                    "lastFmAlbum",
                    null
            );
        }


        // =========================
        // SONG PREVIEWS
        // =========================

        Map<String, String> trackPreviews =
                new HashMap<>();

        if (lastFmAlbumResponse != null &&
                lastFmAlbumResponse.getAlbum() != null &&
                lastFmAlbumResponse.getAlbum().getTracks() != null &&
                lastFmAlbumResponse.getAlbum().getTracks().getTrack() != null) {

            for (var track :
                    lastFmAlbumResponse
                            .getAlbum()
                            .getTracks()
                            .getTrack()) {

                ItunesTrackResponse response =
                        itunesService.searchTracks(
                                currentArtist.getName()
                                        + " "
                                        + track.getName()
                        );

                if (response != null &&
                        response.getResults() != null) {

                    response.getResults()
                            .stream()
                            .filter(result ->
                                    result.getPreviewUrl() != null
                            )
                            .findFirst()
                            .ifPresent(result ->
                                    trackPreviews.put(
                                            track.getName(),
                                            result.getPreviewUrl()
                                    )
                            );
                }
            }
        }

        model.addAttribute(
                "trackPreviews",
                trackPreviews
        );


        // =========================
        // REVIEWS
        // =========================

        List<Review> reviews =
                reviewRepository
                        .findByAlbumIdOrderByCreatedAtDesc(
                                currentAlbum.getId()
                        );

        model.addAttribute(
                "reviews",
                reviews
        );


        // =========================
        // REVIEW USERS
        // =========================

        Map<Long, User> reviewUsers =
                new HashMap<>();

        for (Review review : reviews) {

            userRepository
                    .findById(review.getUserId())
                    .ifPresent(user ->
                            reviewUsers.put(
                                    review.getUserId(),
                                    user
                            )
                    );
        }

        model.addAttribute(
                "reviewUsers",
                reviewUsers
        );


        return "album-profile";
    }


    // =========================
    // ADD TO TOP 4
    // =========================

    @PostMapping("/{id}/favourite")
    public String addFavouriteAlbum(
            @PathVariable Long id,
            @RequestParam Integer position,
            Authentication authentication
    ) {

        String oktaUserId =
                authentication.getName();

        User user =
                userRepository
                        .findByOktaUserId(oktaUserId)
                        .orElseThrow();

        favouriteAlbumRepository
                .findByUserIdAndAlbumId(
                        user.getId(),
                        id
                )
                .ifPresent(
                        favouriteAlbumRepository::delete
                );

        favouriteAlbumRepository
                .findByUserIdOrderByPositionAsc(
                        user.getId()
                )
                .stream()
                .filter(favourite ->
                        favourite.getPosition().equals(position)
                )
                .findFirst()
                .ifPresent(
                        favouriteAlbumRepository::delete
                );

        UserFavouriteAlbum favourite =
                new UserFavouriteAlbum(
                        user.getId(),
                        id,
                        position
                );

        favouriteAlbumRepository.save(favourite);

        return "redirect:/profile/" + user.getId();
    }


    // =========================
    // REMOVE FROM TOP 4
    // =========================

    @PostMapping("/{id}/favourite/remove")
    public String removeFavouriteAlbum(
            @PathVariable Long id,
            Authentication authentication
    ) {

        String oktaUserId =
                authentication.getName();

        User user =
                userRepository
                        .findByOktaUserId(oktaUserId)
                        .orElseThrow();

        favouriteAlbumRepository
                .findByUserIdAndAlbumId(
                        user.getId(),
                        id
                )
                .ifPresent(
                        favouriteAlbumRepository::delete
                );

        return "redirect:/profile/" + user.getId();
    }
}
