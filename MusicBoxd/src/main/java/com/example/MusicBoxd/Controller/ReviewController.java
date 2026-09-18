package com.example.MusicBoxd.Controller;

import com.example.MusicBoxd.Model.Album;
import com.example.MusicBoxd.Model.Artist;
import com.example.MusicBoxd.Model.Friend;
import com.example.MusicBoxd.Model.Review;
import com.example.MusicBoxd.Model.User;
import com.example.MusicBoxd.Repository.AlbumRepository;
import com.example.MusicBoxd.Repository.ArtistRepository;
import com.example.MusicBoxd.Repository.FriendRepository;
import com.example.MusicBoxd.Repository.ReviewRepository;
import com.example.MusicBoxd.Repository.UserRepository;
import com.example.MusicBoxd.api.lastfm.LastFmService;
import com.example.MusicBoxd.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ReviewController {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private AlbumRepository albumRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FriendRepository friendRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private LastFmService lastFmService;


    // =========================================================
    // GET USER REVIEWS
    // =========================================================

    @GetMapping("/users/{userId}/reviews")
    public String getUserReviews(
            @PathVariable Long userId,
            Model model
    ) {

        List<Review> reviews =
                reviewRepository.findByUserId(userId);

        Map<Long, Album> albums =
                new HashMap<>();

        for (Review review : reviews) {

            Long albumId =
                    review.getAlbumId();

            if (albumId != null) {

                Album album =
                        albumRepository
                                .findById(albumId)
                                .orElse(null);

                if (album != null) {
                    albums.put(
                            albumId,
                            album
                    );
                }
            }
        }

        model.addAttribute(
                "reviews",
                reviews
        );

        model.addAttribute(
                "albums",
                albums
        );

        return "profile-page";
    }


    // =========================================================
    // SHOW REVIEW PAGE
    // =========================================================

    @GetMapping("/reviews/{id}")
    public String reviewAlbum(
            @PathVariable Long id,
            Model model
    ) {

        // -----------------------------------------------------
        // Find album
        // -----------------------------------------------------

        Album album =
                albumRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Album not found"
                                )
                        );

        model.addAttribute(
                "album",
                album
        );


        // -----------------------------------------------------
        // Find artist
        // -----------------------------------------------------

        Artist artist = null;

        if (album.getArtistId() != null) {

            artist =
                    artistRepository
                            .findById(
                                    album.getArtistId()
                            )
                            .orElse(null);
        }

        model.addAttribute(
                "artist",
                artist
        );


        // -----------------------------------------------------
        // Get album artwork from Last.fm
        // -----------------------------------------------------

        if (artist != null) {

            try {

                var lastFmAlbum =
                        lastFmService.getAlbumInfo(
                                artist.getName(),
                                album.getTitle()
                        );

                model.addAttribute(
                        "lastFmAlbum",
                        lastFmAlbum
                );

            } catch (Exception e) {

                // If Last.fm cannot find the album,
                // don't crash the review page.

                model.addAttribute(
                        "lastFmAlbum",
                        null
                );
            }

        } else {

            model.addAttribute(
                    "lastFmAlbum",
                    null
            );
        }


        return "reviews";
    }


    // =========================================================
    // SAVE REVIEW
    // =========================================================

    @PostMapping("/reviews/{id}")
    public String saveReview(
            @PathVariable Long id,
            @RequestParam BigDecimal rating,
            @RequestParam String content,
            Authentication authentication
    ) {

        // -----------------------------------------------------
        // Find album
        // -----------------------------------------------------

        Album album =
                albumRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Album not found"
                                )
                        );


        // -----------------------------------------------------
        // Get currently logged-in user
        // -----------------------------------------------------

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


        // -----------------------------------------------------
        // Create review
        // -----------------------------------------------------

        Review review =
                new Review();

        review.setUserId(
                user.getId()
        );

        review.setAlbumId(
                album.getId()
        );

        review.setRating(
                rating
        );

        review.setContent(
                content
        );

        review.setCreatedAt(
                LocalDateTime.now()
        );


        // -----------------------------------------------------
        // Save review
        // -----------------------------------------------------

        Review savedReview =
                reviewRepository.save(
                        review
                );


        // -----------------------------------------------------
        // Notify friends
        // -----------------------------------------------------

        List<Friend> friendships =
                friendRepository
                        .findByStatus("ACCEPTED");


        for (
                Friend friendship :
                friendships
        ) {

            Long friendId = null;


            // Current user sent the original request
            if (
                    user.getId().equals(
                            friendship.getRequesterId()
                    )
            ) {

                friendId =
                        friendship.getReceiverId();
            }


            // Current user received the original request
            else if (
                    user.getId().equals(
                            friendship.getReceiverId()
                    )
            ) {

                friendId =
                        friendship.getRequesterId();
            }


            // Not one of this user's friendships
            if (friendId == null) {
                continue;
            }


            notificationService.notifyFriendReviewed(
                    friendId,
                    user.getId(),
                    savedReview.getId(),
                    user.getUsername()
            );
        }


        // -----------------------------------------------------
        // Return to user's profile
        // -----------------------------------------------------

        return "redirect:/profile/" + user.getId();
    }
}