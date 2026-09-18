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


        if (album.getArtistId() != null) {

            Artist artist =
                    artistRepository
                            .findById(
                                    album.getArtistId()
                            )
                            .orElse(null);

            model.addAttribute(
                    "artist",
                    artist
            );

        } else {

            model.addAttribute(
                    "artist",
                    null
            );
        }


        return "reviews";
    }




    @PostMapping("/reviews/{id}")
    public String saveReview(
            @PathVariable Long id,
            @RequestParam BigDecimal rating,
            @RequestParam String content,
            Authentication authentication
    ) {



        Album album =
                albumRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Album not found"
                                )
                        );



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



        Review savedReview =
                reviewRepository.save(
                        review
                );



        List<Friend> friendships =
                friendRepository
                        .findByStatus("ACCEPTED");


        for (
                Friend friendship :
                friendships
        ) {

            Long friendId = null;


            if (
                    user.getId().equals(
                            friendship.getRequesterId()
                    )
            ) {

                friendId =
                        friendship.getReceiverId();
            }

            else if (
                    user.getId().equals(
                            friendship.getReceiverId()
                    )
            ) {

                friendId =
                        friendship.getRequesterId();
            }

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


        return "redirect:/profile/" + user.getId();
    }
}